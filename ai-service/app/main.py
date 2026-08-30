from fastapi import FastAPI

from app.routes import health, analyze

app = FastAPI(title="MedScope AI Service", version="0.1.0")

app.include_router(health.router)
app.include_router(analyze.router)
