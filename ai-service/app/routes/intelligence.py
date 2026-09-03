from fastapi import APIRouter, HTTPException
from app.models.intelligence import GenerateInsightsRequest, GenerateInsightsResponse
from app.services.intelligence_service import IntelligenceService

router = APIRouter(prefix="/intelligence", tags=["intelligence"])


@router.post("/generate", response_model=GenerateInsightsResponse)
async def generate_insights(request: GenerateInsightsRequest) -> GenerateInsightsResponse:
    """
    Generate evidence-backed insights from intelligence context and deterministic flags.

    Takes structured context (current results + historical trends + existing analysis)
    and rule-based flags, returns AI-generated natural-language insights grounded
    in the provided evidence.
    """

    service = IntelligenceService()

    try:
        response = await service.generate_insights(
            context=request.context,
            flags=request.flags
        )

        if response.status == "FAILED":
            raise HTTPException(
                status_code=500,
                detail=response.error_message or "Intelligence generation failed"
            )

        return response

    except ValueError as e:
        raise HTTPException(
            status_code=500,
            detail=f"Configuration error: {str(e)}"
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Intelligence generation failed: {str(e)}"
        )
