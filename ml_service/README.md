# SP_Estimation ML Service

FastAPI inference layer for the Spring Boot backend. Loads three pickled
research bundles from `models/` once at startup and exposes two HTTP endpoints.

## Run

```bash
cd ml_service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

Models directory can be overridden with `ML_MODELS_DIR=/path/to/models`.

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET    | `/health`              | model load status, list of fine-tuned domains |
| POST   | `/predict/story-point` | XGBoost point + LGBM quantile interval, transfer-learning adapter |
| POST   | `/predict/velocity`    | XGBoost velocity regressor + DomainAdapter |

## Story-point request example

```json
POST /predict/story-point
{
  "user_story": "Integrate payment gateway with PCI-DSS compliance",
  "task_type": "feature",
  "domain": "finance",
  "tech_stack": "springboot",
  "dev_experience_level": "mid",
  "num_components": 4,
  "external_apis": 2,
  "has_integration": 1,
  "has_security": 1,
  "has_ui_complexity": 0,
  "team_size": 7,
  "sprint_duration_days": 14,
  "similar_task_count": 3,
  "team_velocity_avg": 24,
  "recent_completion_rate": 0.82,
  "task_age_days": 4
}
```

## Velocity request example

```json
POST /predict/velocity
{
  "avg_velocity_last3": 24.7,
  "avg_velocity_last5": 23.2,
  "team_size": 5,
  "sprint_duration_days": 14,
  "completion_rate": 0.82,
  "carryover_tasks": 4,
  "developer_availability": 0.85,
  "leave_days": 1,
  "planned_story_points": 28,
  "num_bugs": 3,
  "avg_experience_years": 4.5,
  "domain": "finance"
}
```
