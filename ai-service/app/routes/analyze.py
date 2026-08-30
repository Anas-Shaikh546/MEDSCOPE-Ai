from fastapi import APIRouter, HTTPException
from app.models.analysis import AnalyzeRequest, AnalyzeResponse, AnalysisStatus
from app.services.openrouter_service import OpenRouterService

router = APIRouter(prefix="/analyze", tags=["analysis"])


@router.post("", response_model=AnalyzeResponse)
async def analyze_report(request: AnalyzeRequest) -> AnalyzeResponse:
    """
    Analyze structured medical report results using AI.

    Takes extracted report facts (Step 4 output) and returns AI interpretation
    with findings, summary, and recommendations.
    """

    if not request.results:
        raise HTTPException(
            status_code=400,
            detail="At least one report result is required"
        )

    service = OpenRouterService()

    try:
        analysis = await service.analyze_report(
            report_id=request.report_id,
            results=request.results
        )

        if analysis.status == AnalysisStatus.FAILED:
            raise HTTPException(
                status_code=500,
                detail=analysis.summary or "Analysis failed"
            )

        return analysis

    except ValueError as e:
        raise HTTPException(
            status_code=500,
            detail=f"Configuration error: {str(e)}"
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Analysis failed: {str(e)}"
        )
