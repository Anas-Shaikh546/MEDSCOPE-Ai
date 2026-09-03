from fastapi import FastAPI

from app.routes import health, analyze, intelligence

app = FastAPI(title="MedScope AI Service", version="0.1.0")

app.include_router(health.router)
app.include_router(analyze.router)
app.include_router(intelligence.router)
