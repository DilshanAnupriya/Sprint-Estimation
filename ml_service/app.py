"""
FastAPI ML inference service for the SP_Estimation Spring Boot backend.

Loads the three pickled model bundles produced by the research notebooks
and exposes two prediction endpoints:

  POST /predict/story-point   -- XGBoost point + LGBM quantile interval
                                 + DomainAdapter (transfer learning bundle)
  POST /predict/velocity      -- XGBoost velocity regressor + DomainAdapter

The Spring Boot side calls these via WebClient (see MLClient.java).
"""

from __future__ import annotations

import os
from pathlib import Path
from typing import Optional

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from scipy.sparse import csr_matrix, hstack

import sys
import types

MODELS_DIR = Path(os.environ.get("ML_MODELS_DIR", Path(__file__).parent / "models"))

# ---------------------------------------------------------------------------
# DomainAdapter stub — the velocity pickle was created in a Jupyter notebook
# where this class lived in __main__. We register a compatible stub here so
# joblib can unpickle the bundle without a module lookup error.
# ---------------------------------------------------------------------------
class DomainAdapter:
    """Minimal stub that mirrors the interface used in predict_velocity()."""

    def __init__(self):
        self.base_model = None
        self.tuned: dict = {}

    def predict(self, X, domain=None):
        if domain and domain in self.tuned:
            return self.tuned[domain].predict(X)
        if self.base_model is not None:
            return self.base_model.predict(X)
        raise ValueError("DomainAdapter has no base_model loaded")


# Register this class as __main__.DomainAdapter so pickle can locate it
_main_module = sys.modules.get("__main__") or types.ModuleType("__main__")
_main_module.DomainAdapter = DomainAdapter
sys.modules["__main__"] = _main_module

# ---------------------------------------------------------------------------
# Load bundles once at startup
# ---------------------------------------------------------------------------
sp_bundle  = joblib.load(MODELS_DIR / "model_xgb_lgbm_uncertainty.pkl")
sp_transfer = joblib.load(MODELS_DIR / "transfer_learning_bundle.pkl")
vel_bundle  = joblib.load(MODELS_DIR / "velocity_transfer_bundle.pkl")


# SP main bundle artifacts
SP_MODEL = sp_bundle["model"]                       # XGBRegressor (point)
SP_QUANTILES = sp_bundle["quantile_models"]         # dict P20/P50/P80
SP_TFIDF = sp_bundle["tfidf"]
SP_STRUCT_COLS = sp_bundle["struct_columns"]
SP_NUMERIC_COLS = sp_bundle["numeric_cols"]
SP_CATEGORICAL_COLS = sp_bundle["categorical_cols"]
FIB = np.array(sp_bundle["fib"])

# SP transfer bundle artifacts
SP_TRANSFER_ADAPTER = sp_transfer.get("tuned_models", {})       # dict domain -> XGBRegressor
SP_TRANSFER_BASE = sp_transfer["base_model"]
SP_TRANSFER_TFIDF = sp_transfer["tfidf_transfer"]
SP_TRANSFER_STRUCT_COLS = sp_transfer["struct_columns"]
SP_TRANSFER_NUMERIC_COLS = sp_transfer["numeric_cols"]
SP_TRANSFER_CAT_COLS = sp_transfer["transfer_cat_cols"]

# Velocity bundle artifacts
VEL_BASE = vel_bundle["base_model"]
VEL_ADAPTER = vel_bundle["adapter"]                  # DomainAdapter instance
VEL_FEATURES = vel_bundle["features"]                # 16 cols
VEL_KNOWN_DOMAINS = vel_bundle["known_domains"]


# ---------------------------------------------------------------------------
# Helpers — Fibonacci snapping
# ---------------------------------------------------------------------------
def snap_to_fib(v: float) -> int:
    return int(FIB[np.argmin(np.abs(FIB - v))])


def snap_floor_fib(v: float) -> int:
    below = FIB[FIB <= v]
    return int(below[-1]) if len(below) else int(FIB[0])


def snap_ceil_fib(v: float) -> int:
    above = FIB[FIB >= v]
    return int(above[0]) if len(above) else int(FIB[-1])


# ---------------------------------------------------------------------------
# Request / response schemas
# ---------------------------------------------------------------------------
class StoryPointRequest(BaseModel):
    user_story: str = Field(..., min_length=1)
    task_type: str = Field(..., description="feature | bug | enhancement")
    domain: str = Field(..., description="finance | ecommerce | healthcare | education | logistics | <new>")
    tech_stack: str
    dev_experience_level: str = Field(..., description="junior | mid | senior")
    num_components: int = Field(1, ge=1, le=15)
    external_apis: int = Field(0, ge=0, le=10)
    has_integration: int = Field(0, ge=0, le=1)
    has_security: int = Field(0, ge=0, le=1)
    has_ui_complexity: int = Field(0, ge=0, le=1)
    team_size: int = Field(5, ge=1, le=30)
    sprint_duration_days: int = Field(14, ge=1, le=60)
    similar_task_count: int = Field(0, ge=0, le=200)
    team_velocity_avg: int = Field(20, ge=1, le=200)
    recent_completion_rate: float = Field(0.85, ge=0.0, le=1.5)
    task_age_days: int = Field(0, ge=0, le=365)


class StoryPointResponse(BaseModel):
    point_estimate: int
    lower_bound: int
    upper_bound: int
    risk_level: str
    raw_point: float
    raw_p20: float
    raw_p80: float
    used_fine_tuned: bool
    domain_used: str


class VelocityRequest(BaseModel):
    avg_velocity_last3: float = Field(..., ge=0)
    avg_velocity_last5: float = Field(..., ge=0)
    team_size: int = Field(..., ge=1)
    sprint_duration_days: int = Field(..., ge=1)
    completion_rate: float = Field(..., ge=0.0, le=1.5)
    carryover_tasks: int = Field(..., ge=0)
    developer_availability: float = Field(..., ge=0.0, le=1.5)
    leave_days: int = Field(..., ge=0)
    planned_story_points: int = Field(..., ge=0)
    workload_ratio: Optional[float] = None
    num_bugs: int = Field(..., ge=0)
    avg_experience_years: float = Field(..., ge=0.0)
    domain: Optional[str] = None


class VelocityResponse(BaseModel):
    predicted_velocity: int
    raw_prediction: float
    base_prediction: int
    used_fine_tuned: bool
    domain_used: str
    risk_level: str
    stress_score: float
    velocity_delta: float
    effective_capacity: float
    velocity_per_dev: float


# ---------------------------------------------------------------------------
# Feature builders
# ---------------------------------------------------------------------------
def _build_sp_full_features(req: StoryPointRequest):
    raw_row = {
        "task_type": req.task_type.lower(),
        "domain": req.domain.lower(),
        "tech_stack": req.tech_stack.lower(),
        "dev_experience_level": req.dev_experience_level.lower(),
        "num_components": req.num_components,
        "external_apis": req.external_apis,
        "has_integration": req.has_integration,
        "has_security": req.has_security,
        "has_ui_complexity": req.has_ui_complexity,
        "word_count": len(req.user_story.split()),
        "char_count": len(req.user_story),
        "team_size": req.team_size,
        "sprint_duration_days": req.sprint_duration_days,
        "similar_task_count": req.similar_task_count,
        "team_velocity_avg": req.team_velocity_avg,
        "recent_completion_rate": req.recent_completion_rate,
        "task_age_days": req.task_age_days,
    }
    row_df = pd.DataFrame([raw_row])
    row_cat = pd.get_dummies(row_df[SP_CATEGORICAL_COLS], drop_first=False)
    row_num = row_df[SP_NUMERIC_COLS]
    row_struct = pd.concat([row_num, row_cat], axis=1)
    for col in SP_STRUCT_COLS:
        if col not in row_struct.columns:
            row_struct[col] = 0
    row_struct = row_struct[SP_STRUCT_COLS].astype(float)

    text_vec = SP_TFIDF.transform([req.user_story])
    return hstack([text_vec, csr_matrix(row_struct.values)])


def _build_sp_transfer_features(req: StoryPointRequest):
    raw_row = {
        "task_type": req.task_type.lower(),
        "tech_stack": req.tech_stack.lower(),
        "dev_experience_level": req.dev_experience_level.lower(),
        "num_components": req.num_components,
        "external_apis": req.external_apis,
        "has_integration": req.has_integration,
        "has_security": req.has_security,
        "has_ui_complexity": req.has_ui_complexity,
        "word_count": len(req.user_story.split()),
        "char_count": len(req.user_story),
        "team_size": req.team_size,
        "sprint_duration_days": req.sprint_duration_days,
        "similar_task_count": req.similar_task_count,
        "team_velocity_avg": req.team_velocity_avg,
        "recent_completion_rate": req.recent_completion_rate,
        "task_age_days": req.task_age_days,
    }
    row_df = pd.DataFrame([raw_row])
    row_cat = pd.get_dummies(row_df[SP_TRANSFER_CAT_COLS], drop_first=False)
    row_num = row_df[SP_TRANSFER_NUMERIC_COLS]
    row_struct = pd.concat([row_num, row_cat], axis=1)
    for col in SP_TRANSFER_STRUCT_COLS:
        if col not in row_struct.columns:
            row_struct[col] = 0
    row_struct = row_struct[SP_TRANSFER_STRUCT_COLS].astype(float)
    text_vec = SP_TRANSFER_TFIDF.transform([req.user_story])
    return hstack([text_vec, csr_matrix(row_struct.values)])


def _build_velocity_row(req: VelocityRequest) -> pd.DataFrame:
    eff_cap = round(req.team_size * req.developer_availability * (req.sprint_duration_days / 14.0), 3)
    workload = req.workload_ratio if req.workload_ratio is not None else (
        round(req.planned_story_points / max(eff_cap * 8, 1), 3)
    )
    delta = round(req.avg_velocity_last3 - req.avg_velocity_last5, 2)
    stress = round(req.carryover_tasks * 0.4 + req.num_bugs * 0.3 + req.leave_days * 0.3, 3)
    vpd = round(req.avg_velocity_last3 / max(req.team_size, 1), 3)

    row = {
        "avg_velocity_last3": req.avg_velocity_last3,
        "avg_velocity_last5": req.avg_velocity_last5,
        "team_size": req.team_size,
        "sprint_duration_days": req.sprint_duration_days,
        "completion_rate": req.completion_rate,
        "carryover_tasks": req.carryover_tasks,
        "developer_availability": req.developer_availability,
        "leave_days": req.leave_days,
        "planned_story_points": req.planned_story_points,
        "workload_ratio": workload,
        "num_bugs": req.num_bugs,
        "avg_experience_years": req.avg_experience_years,
        "velocity_delta": delta,
        "effective_capacity": eff_cap,
        "stress_score": stress,
        "velocity_per_dev": vpd,
    }
    return pd.DataFrame([row])[VEL_FEATURES]


# ---------------------------------------------------------------------------
# Inference
# ---------------------------------------------------------------------------
def predict_story_point(req: StoryPointRequest) -> StoryPointResponse:
    domain_norm = req.domain.lower().strip()

    # Point estimate via transfer-learning adapter (handles new domains)
    X_tl = _build_sp_transfer_features(req)
    if domain_norm in SP_TRANSFER_ADAPTER:
        raw_point = float(SP_TRANSFER_ADAPTER[domain_norm].predict(X_tl)[0])
        used_ft = True
    else:
        raw_point = float(SP_TRANSFER_BASE.predict(X_tl)[0])
        used_ft = False

    # Quantile bounds via main bundle (trained with domain feature)
    X_full = _build_sp_full_features(req)
    raw_lo = float(SP_QUANTILES["P20"].predict(X_full)[0])
    raw_hi = float(SP_QUANTILES["P80"].predict(X_full)[0])

    point_fib = snap_to_fib(raw_point)
    lo_fib = min(snap_floor_fib(raw_lo), point_fib)
    hi_fib = max(snap_ceil_fib(raw_hi), point_fib)

    fib_idx = {sp: i for i, sp in enumerate(FIB.tolist())}
    span = fib_idx[hi_fib] - fib_idx[lo_fib]
    risk = "LOW" if span == 0 else "MEDIUM" if span <= 2 else "HIGH"

    return StoryPointResponse(
        point_estimate=point_fib,
        lower_bound=lo_fib,
        upper_bound=hi_fib,
        risk_level=risk,
        raw_point=round(raw_point, 3),
        raw_p20=round(raw_lo, 3),
        raw_p80=round(raw_hi, 3),
        used_fine_tuned=used_ft,
        domain_used=domain_norm,
    )


def predict_velocity(req: VelocityRequest) -> VelocityResponse:
    domain_norm = (req.domain or "").strip()
    row = _build_velocity_row(req)

    base_pred = int(round(float(VEL_BASE.predict(row)[0])))
    used_ft = bool(domain_norm) and domain_norm in getattr(VEL_ADAPTER, "tuned", {})
    pred_raw = float(VEL_ADAPTER.predict(row, domain=domain_norm or None)[0])
    pred = int(round(pred_raw))

    stress = float(row["stress_score"].iloc[0])
    risk = "LOW" if stress < 4 else "MEDIUM" if stress < 9 else "HIGH"

    return VelocityResponse(
        predicted_velocity=pred,
        raw_prediction=round(pred_raw, 3),
        base_prediction=base_pred,
        used_fine_tuned=used_ft,
        domain_used=domain_norm or "unknown",
        risk_level=risk,
        stress_score=stress,
        velocity_delta=float(row["velocity_delta"].iloc[0]),
        effective_capacity=float(row["effective_capacity"].iloc[0]),
        velocity_per_dev=float(row["velocity_per_dev"].iloc[0]),
    )


# ---------------------------------------------------------------------------
# FastAPI app
# ---------------------------------------------------------------------------
app = FastAPI(title="SP_Estimation ML Service", version="1.0.0")


@app.get("/health")
def health():
    return {
        "status": "ok",
        "models_loaded": {
            "story_point_main": True,
            "story_point_transfer": True,
            "velocity_transfer": True,
        },
        "fine_tuned_sp_domains": list(SP_TRANSFER_ADAPTER.keys()),
        "fine_tuned_velocity_domains": list(getattr(VEL_ADAPTER, "tuned", {}).keys()),
    }


@app.post("/predict/story-point", response_model=StoryPointResponse)
def story_point(req: StoryPointRequest):
    try:
        return predict_story_point(req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {e}")


@app.post("/predict/velocity", response_model=VelocityResponse)
def velocity(req: VelocityRequest):
    try:
        return predict_velocity(req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {e}")
