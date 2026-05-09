#!/bin/bash
# Direct FastAPI test — bypasses Spring Boot entirely.
# If this returns point_estimate=8, the model and FastAPI are working correctly.
# If Spring Boot returns a different value with the same logical inputs,
# the bug is in the Spring → FastAPI conversion.

set -e
URL="${1:-http://localhost:8000/predict/story-point}"

echo "POST  $URL"
echo "---"

curl -sS -X POST "$URL" \
  -H 'Content-Type: application/json' \
  -d '{
    "user_story": "Implement User Authentication with JWT (Signup/Login/Logout). Develop secure authentication functionality using JWT tokens. Implement user registration and login endpoints in the backend. Integrate JWT token generation upon successful login and configure token validation middleware for protected routes.",
    "task_type": "feature",
    "domain": "ecommerce",
    "tech_stack": "react",
    "dev_experience_level": "mid",
    "num_components": 2,
    "external_apis": 1,
    "has_integration": 1,
    "has_security": 1,
    "has_ui_complexity": 1,
    "team_size": 3,
    "sprint_duration_days": 7,
    "similar_task_count": 1,
    "team_velocity_avg": 28,
    "recent_completion_rate": 0.8,
    "task_age_days": 1
  }' | python3 -m json.tool

echo ""
echo "Expected (matches notebook): point_estimate=8, lower_bound=5, upper_bound=13, raw_point≈8.76"
