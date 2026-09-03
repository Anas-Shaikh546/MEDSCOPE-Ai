from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum


class InsightType(str, Enum):
    TREND_CONTEXT = "TREND_CONTEXT"
    PERSISTENT_ABNORMALITY = "PERSISTENT_ABNORMALITY"
    SIGNIFICANT_CHANGE = "SIGNIFICANT_CHANGE"
    MULTI_RESULT_PATTERN = "MULTI_RESULT_PATTERN"
    FOLLOW_UP = "FOLLOW_UP"
    GENERAL_CONTEXT = "GENERAL_CONTEXT"


class InsightPriority(str, Enum):
    HIGH = "HIGH"
    MODERATE = "MODERATE"
    LOW = "LOW"
    INFORMATIONAL = "INFORMATIONAL"


class TestResultContext(BaseModel):
    """One test result from the current report."""
    result_id: int
    test_name: str
    canonical_name: Optional[str] = None
    numeric_value: Optional[float] = None
    text_value: Optional[str] = None
    unit: Optional[str] = None
    reference_low: Optional[float] = None
    reference_high: Optional[float] = None
    status: Optional[str] = None


class HistoricalObservation(BaseModel):
    """One historical data point for a test."""
    date: str
    date_is_confirmed: bool
    report_result_id: int
    value: Optional[float] = None
    unit: Optional[str] = None
    reference_low: Optional[float] = None
    reference_high: Optional[float] = None
    status: Optional[str] = None


class TestTrendContext(BaseModel):
    """Historical observations for one test with trend direction."""
    canonical_name: str
    display_name: str
    trend_direction: str
    observations: List[HistoricalObservation]


class PrioritizationFlag(BaseModel):
    """Deterministic flag from the rule engine."""
    type: InsightType
    priority: InsightPriority
    test_name: Optional[str] = None
    source_result_ids: List[int]
    evidence: str


class IntelligenceContext(BaseModel):
    """Structured context assembled from ReportResult + Analysis + Timeline."""
    report_id: int
    report_date: Optional[str] = None
    current_results: List[TestResultContext]
    historical_trends: List[TestTrendContext]
    existing_analysis_summary: Optional[str] = None
    existing_recommendations: Optional[str] = None


class GenerateInsightsRequest(BaseModel):
    """Input to /intelligence/generate."""
    context: IntelligenceContext
    flags: List[PrioritizationFlag]


class GeneratedInsight(BaseModel):
    """One AI-generated insight."""
    type: InsightType
    title: str = Field(min_length=1, max_length=255)
    description: str = Field(min_length=1, max_length=5000)
    priority: InsightPriority
    confidence: Optional[float] = Field(None, ge=0.0, le=1.0)
    source_result_ids: List[int] = Field(min_length=1)
    follow_up_questions: Optional[str] = Field(
        None,
        max_length=2000,
        description="Newline-separated questions for clinical discussion"
    )


class GenerateInsightsResponse(BaseModel):
    """Structured intelligence output."""
    model_config = {"protected_namespaces": ()}

    status: str  # "COMPLETED" or "FAILED"
    insights: List[GeneratedInsight]
    model_name: str
    model_version: str
    prompt_version: str
    error_message: Optional[str] = None
