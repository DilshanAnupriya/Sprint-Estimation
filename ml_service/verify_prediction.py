"""
Direct pkl-file prediction test. Bypasses FastAPI AND Spring Boot.

Loads the deployed model bundles and runs the SAME prediction pipeline
your notebook uses. If this prints point_estimate=8 for the JWT example,
the deployed pkl files match your training output exactly.

Usage:
    cd ml_service
    python verify_prediction.py
"""
from __future__ import annotations
import sys, types
from pathlib import Path
import joblib
import numpy as np
import pandas as pd
from scipy.sparse import csr_matrix, hstack


# DomainAdapter stub (same as app.py — pickled class lives in __main__)
class DomainAdapter:
    def __init__(self):
        self.base_model = None
        self.tuned: dict = {}

    def predict(self, X, domain=None):
        if domain and domain in self.tuned:
            return self.tuned[domain].predict(X)
        if self.base_model is not None:
            return self.base_model.predict(X)
        raise ValueError("DomainAdapter has no base_model loaded")


_main = sys.modules.get("__main__") or types.ModuleType("__main__")
_main.DomainAdapter = DomainAdapter
sys.modules["__main__"] = _main


# Load bundles
MODELS = Path(__file__).parent / "models"
sp_bundle = joblib.load(MODELS / "model_xgb_lgbm_uncertainty.pkl")
sp_transfer = joblib.load(MODELS / "transfer_learning_bundle.pkl")

print("=" * 70)
print(f"Loaded:  {MODELS / 'model_xgb_lgbm_uncertainty.pkl'}")
print(f"Loaded:  {MODELS / 'transfer_learning_bundle.pkl'}")
print(f"Fine-tuned domains:  {list(sp_transfer.get('tuned_models', {}).keys())}")
print("=" * 70)


# Pull artifacts
SP_QUANTILES        = sp_bundle["quantile_models"]
SP_TFIDF            = sp_bundle["tfidf"]
SP_STRUCT_COLS      = sp_bundle["struct_columns"]
SP_NUMERIC_COLS     = sp_bundle["numeric_cols"]
SP_CATEGORICAL_COLS = sp_bundle["categorical_cols"]
FIB                 = np.array(sp_bundle["fib"])

SP_TRANSFER_ADAPTER     = sp_transfer.get("tuned_models", {})
SP_TRANSFER_BASE        = sp_transfer["base_model"]
SP_TRANSFER_TFIDF       = sp_transfer["tfidf_transfer"]
SP_TRANSFER_STRUCT_COLS = sp_transfer["struct_columns"]
SP_TRANSFER_NUMERIC_COLS = sp_transfer["numeric_cols"]
SP_TRANSFER_CAT_COLS    = sp_transfer["transfer_cat_cols"]


def snap_to_fib(v):    return int(FIB[np.argmin(np.abs(FIB - v))])
def snap_floor(v):     b = FIB[FIB <= v]; return int(b[-1]) if len(b) else int(FIB[0])
def snap_ceil(v):      a = FIB[FIB >= v]; return int(a[0])  if len(a) else int(FIB[-1])


def predict(req: dict) -> dict:
    """Run the exact pipeline from the notebook against the deployed pkl files."""
    raw_row = {
        "task_type":              req["task_type"].lower(),
        "domain":                 req["domain"].lower(),
        "tech_stack":             req["tech_stack"].lower(),
        "dev_experience_level":   req["dev_experience_level"].lower(),
        "num_components":         req["num_components"],
        "external_apis":          req["external_apis"],
        "has_integration":        req["has_integration"],
        "has_security":           req["has_security"],
        "has_ui_complexity":      req["has_ui_complexity"],
        "word_count":             len(req["user_story"].split()),
        "char_count":             len(req["user_story"]),
        "team_size":              req["team_size"],
        "sprint_duration_days":   req["sprint_duration_days"],
        "similar_task_count":     req["similar_task_count"],
        "team_velocity_avg":      req["team_velocity_avg"],
        "recent_completion_rate": req["recent_completion_rate"],
        "task_age_days":          req["task_age_days"],
    }
    row_df = pd.DataFrame([raw_row])

    # Full features (with domain) — for quantile bounds
    cat_full = pd.get_dummies(row_df[SP_CATEGORICAL_COLS], drop_first=False)
    struct_full = pd.concat([row_df[SP_NUMERIC_COLS], cat_full], axis=1)
    for col in SP_STRUCT_COLS:
        if col not in struct_full.columns:
            struct_full[col] = 0
    struct_full = struct_full[SP_STRUCT_COLS].astype(float)
    X_full = hstack([SP_TFIDF.transform([req["user_story"]]), csr_matrix(struct_full.values)])

    # Transfer features (no domain) — for point estimate
    cat_tl = pd.get_dummies(row_df[SP_TRANSFER_CAT_COLS], drop_first=False)
    struct_tl = pd.concat([row_df[SP_TRANSFER_NUMERIC_COLS], cat_tl], axis=1)
    for col in SP_TRANSFER_STRUCT_COLS:
        if col not in struct_tl.columns:
            struct_tl[col] = 0
    struct_tl = struct_tl[SP_TRANSFER_STRUCT_COLS].astype(float)
    X_tl = hstack([SP_TRANSFER_TFIDF.transform([req["user_story"]]), csr_matrix(struct_tl.values)])

    # Predict
    domain_norm = req["domain"].lower().strip()
    if domain_norm in SP_TRANSFER_ADAPTER:
        raw_point = float(SP_TRANSFER_ADAPTER[domain_norm].predict(X_tl)[0])
        used_ft = True
    else:
        raw_point = float(SP_TRANSFER_BASE.predict(X_tl)[0])
        used_ft = False

    raw_lo = float(SP_QUANTILES["P20"].predict(X_full)[0])
    raw_hi = float(SP_QUANTILES["P80"].predict(X_full)[0])

    point_fib = snap_to_fib(raw_point)
    lo_fib = min(snap_floor(raw_lo), point_fib)
    hi_fib = max(snap_ceil(raw_hi), point_fib)

    fib_idx = {sp: i for i, sp in enumerate(FIB.tolist())}
    span = fib_idx[hi_fib] - fib_idx[lo_fib]
    risk = "LOW" if span == 0 else "MEDIUM" if span <= 2 else "HIGH"

    return {
        "point_estimate": point_fib,
        "lower_bound":    lo_fib,
        "upper_bound":    hi_fib,
        "risk_level":     risk,
        "raw_point":      round(raw_point, 2),
        "raw_p20":        round(raw_lo, 2),
        "raw_p80":        round(raw_hi, 2),
        "used_fine_tuned": used_ft,
        "domain_used":    domain_norm,
    }


# ---- Test with the JWT example from your notebook ----
test_input = {
    "user_story": (
        "Implement User Authentication with JWT (Signup/Login/Logout). "
        "Develop secure authentication functionality using JWT tokens. "
        "Implement user registration and login endpoints in the backend. "
        "Integrate JWT token generation upon successful login and configure "
        "token validation middleware for protected routes."
    ),
    "task_type":              "feature",
    "domain":                 "ecommerce",
    "tech_stack":             "react",
    "dev_experience_level":   "mid",
    "num_components":         2,
    "external_apis":          1,
    "has_integration":        1,
    "has_security":           1,
    "has_ui_complexity":      1,
    "team_size":              3,
    "sprint_duration_days":   7,
    "similar_task_count":     1,
    "team_velocity_avg":      28,
    "recent_completion_rate": 0.8,
    "task_age_days":          1,
}

print("\nINPUT:")
for k, v in test_input.items():
    if k == "user_story":
        print(f"  {k:25s} : {v[:60]}…")
    else:
        print(f"  {k:25s} : {v}")

result = predict(test_input)

print("\nDIRECT-PKL PREDICTION:")
for k, v in result.items():
    print(f"  {k:25s} : {v}")

print("\nNOTEBOOK EXPECTED:")
print("  point_estimate            : 8")
print("  lower_bound               : 5")
print("  upper_bound               : 13")
print("  raw_point                 : 8.76")
print("  raw_p20                   : 7.44")
print("  raw_p80                   : 11.02")
print("  used_fine_tuned           : False  (ecommerce known but no fine-tune yet)")

ok = result["point_estimate"] == 8 and result["lower_bound"] == 5 and result["upper_bound"] == 13
print()
print("=" * 70)
if ok:
    print("✓ MATCH — deployed pkls produce the same result as your notebook.")
    print("  If Spring Boot returns something different, the bug is in")
    print("  StoryPointMLService.java (input conversion), not the model.")
else:
    print("✗ MISMATCH — deployed pkls differ from your notebook.")
    print("  Re-export from notebook:")
    print("     cp '/Users/dilshan/Documents/SP V2/model_xgb_lgbm_uncertainty.pkl' \\")
    print("        ml_service/models/")
    print("     cp '/Users/dilshan/Documents/SP V2/transfer_learning_bundle.pkl' \\")
    print("        ml_service/models/")
    print("  Then restart the FastAPI service.")
print("=" * 70)
