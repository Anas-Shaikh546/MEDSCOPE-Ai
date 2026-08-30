from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum


class ResultStatus(str, Enum):
    NORMAL = "NORMAL"
    HIGH = "HIGH"
    LOW = "LOW"
    UNKNOWN = "UNKNOWN"


class ReportResultInput(BaseModel):
    """Extracted fact from a medical report (Step 4 output)."""
    test_name: str
    normalized_test_name: Optional[str] = None   
    raw_value: str
    numeric_value: Optional[float] = None
    unit: Optional[str] = None
    reference_low: Optional[float] = None
    reference_high: Optional[float] = None
    status: ResultStatus
    confidence: float
    page_number: Optional[int] = None


class AnalyzeRequest(BaseModel):
    """Input to /analyze: structured report facts, not raw PDF text."""
    report_id: int
    results: List[ReportResultInput] = Field(min_length=1)


class AnalysisSeverity(str, Enum):
    NORMAL = "NORMAL"
    ATTENTION = "ATTENTION"
    CONCERN = "CONCERN"
    URGENT = "URGENT"


class AnalysisFinding(BaseModel):
    """One AI interpretation of a specific report result."""
    report_result_index: int = Field(
        description="Index in the input results array that this finding interprets"
    )
    interpretation: str = Field(
        min_length=1,
        max_length=1000,
        description="AI interpretation of this specific result"
    )
    severity: AnalysisSeverity


class AnalysisStatus(str, Enum):
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


class AnalyzeResponse(BaseModel):
    """Structured AI analysis output."""
    model_config = {"protected_namespaces": ()}

    status: AnalysisStatus
    summary: Optional[str] = Field(
        None,
        max_length=2000,
        description="Overall summary of the report"
    )
    recommendations: Optional[str] = Field(
        None,
        max_length=2000,
        description="Newline-separated recommendations"
    )
    findings: List[AnalysisFinding] = []
    model_name: str
    model_version: str
    prompt_version: str
