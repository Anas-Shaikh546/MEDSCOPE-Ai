from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    """First smoke test for the AI service. No model, no report
    processing yet - that's later steps."""
    return {"status": "UP"}
