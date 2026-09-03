import os
import httpx
import json
from typing import List
from app.models.intelligence import (
    IntelligenceContext,
    PrioritizationFlag,
    GenerateInsightsResponse,
    GeneratedInsight,
    InsightType,
    InsightPriority
)


class IntelligenceService:
    """Handles intelligence generation via OpenRouter API."""

    DEFAULT_MODEL = "nvidia/nemotron-3-ultra-550b-a55b:free"
    DEFAULT_MODEL_VERSION = "nvidia-nemotron-3-ultra-550b-a55b"
    DEFAULT_PROMPT_VERSION = "v1.0"

    def __init__(self):
        self.api_key = os.getenv("OPENROUTER_API_KEY")
        if not self.api_key:
            raise ValueError("OPENROUTER_API_KEY environment variable is required")

        self.base_url = "https://openrouter.ai/api/v1"
        self.model = os.getenv("OPENROUTER_MODEL", self.DEFAULT_MODEL)
        self.model_version = os.getenv(
            "OPENROUTER_MODEL_VERSION", self.DEFAULT_MODEL_VERSION
        )
        self.prompt_version = os.getenv(
            "INTELLIGENCE_PROMPT_VERSION", self.DEFAULT_PROMPT_VERSION
        )

    async def generate_insights(
        self,
        context: IntelligenceContext,
        flags: List[PrioritizationFlag]
    ) -> GenerateInsightsResponse:
        """
        Generate evidence-backed insights from intelligence context and
        deterministic flags. The AI explains patterns in natural language,
        grounded in structured evidence.
        """

        prompt = self._build_prompt(context, flags)

        try:
            response_json = await self._call_openrouter(prompt)

            return GenerateInsightsResponse(
                status="COMPLETED",
                insights=[
                    GeneratedInsight(
                        type=InsightType(i["type"]),
                        title=i["title"],
                        description=i["description"],
                        priority=InsightPriority(i["priority"]),
                        confidence=i.get("confidence"),
                        source_result_ids=i["source_result_ids"],
                        follow_up_questions=i.get("follow_up_questions")
                    )
                    for i in response_json.get("insights", [])
                ],
                model_name=self.model,
                model_version=self.model_version,
                prompt_version=self.prompt_version
            )

        except Exception as e:
            return GenerateInsightsResponse(
                status="FAILED",
                insights=[],
                model_name=self.model,
                model_version=self.model_version,
                prompt_version=self.prompt_version,
                error_message=str(e)
            )

    def _build_prompt(
        self,
        context: IntelligenceContext,
        flags: List[PrioritizationFlag]
    ) -> str:
        """
        Build the intelligence generation prompt. Separates system instructions,
        safety rules, structured context, and output schema as recommended in
        step7.txt section 11.
        """

        # Format current results
        current_results_text = "\n".join(
            f"- {r.test_name} ({r.canonical_name}): "
            f"{r.numeric_value or r.text_value} {r.unit or ''} "
            f"(ref: {self._format_range(r.reference_low, r.reference_high)}, "
            f"status: {r.status})"
            for r in context.current_results
        )

        # Format historical trends
        trends_text = "\n".join(
            f"- {t.display_name}: {t.trend_direction} "
            f"({len(t.observations)} observations)"
            for t in context.historical_trends
        )

        # Format deterministic flags
        flags_text = "\n".join(
            f"- [{f.priority.value}] {f.evidence} (source IDs: {f.source_result_ids})"
            for f in flags
        )

        return f"""SYSTEM:
You are an evidence-grounded medical report intelligence assistant. Your job is to generate structured insights that help users understand important patterns in their longitudinal health data.

SAFETY RULES:
- Use ONLY the supplied information below
- Do NOT diagnose diseases
- Do NOT prescribe medication or recommend dosage changes
- Do NOT invent missing information, reference ranges, or historical data
- Do NOT fabricate relationships between unrelated tests
- Clearly distinguish observation from interpretation
- Mention uncertainty where appropriate
- All source_result_ids MUST come from the data provided below

CONTEXT:

Report ID: {context.report_id}
Report Date: {context.report_date or "not confirmed"}

Current Results:
{current_results_text or "None"}

Historical Trends:
{trends_text or "None"}

Existing Analysis Summary:
{context.existing_analysis_summary or "None"}

Deterministic Flags (from rule engine):
{flags_text or "None"}

TASK:
Generate structured insights that explain important longitudinal patterns. Focus on:
1. Persistent abnormalities across multiple reports
2. Significant trends (increasing/decreasing patterns)
3. Contextual explanations of what patterns mean
4. Questions the user might want to discuss with a healthcare professional

OUTPUT SCHEMA:
Return ONLY valid JSON matching this exact structure:

{{
    "insights": [
        {{
            "type": "TREND_CONTEXT" | "PERSISTENT_ABNORMALITY" | "SIGNIFICANT_CHANGE" | "MULTI_RESULT_PATTERN" | "FOLLOW_UP" | "GENERAL_CONTEXT",
            "title": "Short summary (max 255 chars)",
            "description": "Detailed explanation of the pattern observed (max 5000 chars)",
            "priority": "HIGH" | "MODERATE" | "LOW" | "INFORMATIONAL",
            "confidence": 0.0 to 1.0,
            "source_result_ids": [list of report_result_id values that support this insight],
            "follow_up_questions": "Optional newline-separated questions for clinical discussion"
        }}
    ]
}}

EXAMPLE INSIGHT:
{{
    "type": "PERSISTENT_ABNORMALITY",
    "title": "Vitamin D consistently below range",
    "description": "Vitamin D levels have remained below the reference range across your last 3 reports (Jan: 18 ng/mL, Apr: 19 ng/mL, Aug: 20 ng/mL; reference: 30-100 ng/mL). This persistent pattern suggests the finding is consistent rather than a one-time occurrence.",
    "priority": "MODERATE",
    "confidence": 0.92,
    "source_result_ids": [123, 155, 189],
    "follow_up_questions": "What could explain this persistent pattern?\\nShould this result be monitored again?\\nAre there other results that should be considered alongside it?"
}}

IMPORTANT:
- Every source_result_id must appear in the context data above
- Do not invent source IDs
- Confidence is YOUR confidence in the pattern observation, NOT medical certainty
- Follow-up questions are suggestions, NOT medical instructions
- If insufficient data exists for meaningful insights, return an empty insights array

Return ONLY the JSON object."""

    @staticmethod
    def _format_range(low: float | None, high: float | None) -> str:
        if low is not None and high is not None:
            return f"{low}-{high}"
        elif low is not None:
            return f">= {low}"
        elif high is not None:
            return f"<= {high}"
        return "not provided"

    async def _call_openrouter(self, prompt: str) -> dict:
        """Make the actual HTTP call to OpenRouter API."""

        import logging
        logger = logging.getLogger(__name__)

        async with httpx.AsyncClient(timeout=60.0) as client:
            request_payload = {
                "model": self.model,
                "messages": [
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                "temperature": 0.3,
                "max_tokens": 4000  # Reduced from 6000 to prevent token limit issues
            }

            logger.info(f"Calling OpenRouter with model: {self.model}")

            response = await client.post(
                f"{self.base_url}/chat/completions",
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": "http://localhost:3000",
                    "X-Title": "MedScope AI Intelligence"
                },
                json=request_payload
            )

            if response.status_code != 200:
                error_detail = response.text
                logger.error(f"OpenRouter API error {response.status_code}: {error_detail}")
                raise Exception(f"{response.status_code}: {error_detail}")

            data = response.json()

            # Log the full response for debugging
            logger.debug(f"OpenRouter response keys: {data.keys()}")

            # Check if response has expected structure
            if "choices" not in data:
                logger.error(f"OpenRouter returned unexpected structure: {json.dumps(data)[:1000]}")

                # Check for error in response
                if "error" in data:
                    error_msg = data["error"].get("message", str(data["error"]))
                    raise Exception(f"OpenRouter API error: {error_msg}")

                raise Exception(f"Unexpected response structure (no 'choices' field): {json.dumps(data)[:500]}")

            if not data["choices"]:
                raise Exception("OpenRouter returned empty choices array")

            content = data["choices"][0]["message"]["content"]

            # Parse the JSON response
            if "```json" in content:
                content = content.split("```json")[1].split("```")[0].strip()
            elif "```" in content:
                content = content.split("```")[1].split("```")[0].strip()

            return json.loads(content)
