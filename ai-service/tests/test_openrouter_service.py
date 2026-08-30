import os
import unittest
from unittest.mock import patch

from app.models.analysis import ReportResultInput, ResultStatus
from app.services.openrouter_service import OpenRouterService


class OpenRouterServiceVersioningTest(unittest.TestCase):
    def test_defaults_are_used_when_version_environment_variables_are_absent(self):
        with patch.dict(os.environ, {"OPENROUTER_API_KEY": "test-key"}, clear=True):
            service = OpenRouterService()

        self.assertEqual(OpenRouterService.DEFAULT_MODEL, service.model)
        self.assertEqual(OpenRouterService.DEFAULT_MODEL_VERSION, service.model_version)
        self.assertEqual(OpenRouterService.DEFAULT_PROMPT_VERSION, service.prompt_version)

    def test_environment_values_override_the_recorded_metadata(self):
        environment = {
            "OPENROUTER_API_KEY": "test-key",
            "OPENROUTER_MODEL": "provider/model-id",
            "OPENROUTER_MODEL_VERSION": "model-release-2",
            "ANALYSIS_PROMPT_VERSION": "v2.0",
        }
        with patch.dict(os.environ, environment, clear=True):
            service = OpenRouterService()

        self.assertEqual("provider/model-id", service.model)
        self.assertEqual("model-release-2", service.model_version)
        self.assertEqual("v2.0", service.prompt_version)

    def test_report_result_input_accepts_all_statuses_sent_by_spring_boot(self):
        for status in ResultStatus:
            result = ReportResultInput(
                test_name="Hemoglobin",
                normalized_test_name="hemoglobin",
                raw_value="13.8",
                status=status.value,
                confidence=0.95,
            )
            self.assertEqual(status, result.status)

    def test_prompt_includes_confidence_clear_missing_range_and_safety_rules(self):
        with patch.dict(os.environ, {"OPENROUTER_API_KEY": "test-key"}, clear=True):
            service = OpenRouterService()

        result = ReportResultInput(
            test_name="Hemoglobin",
            normalized_test_name="hemoglobin",
            raw_value="13.8",
            status="UNKNOWN",
            confidence=0.42,
        )
        prompt = service._build_prompt([result])

        self.assertIn("reference range: not provided", prompt)
        self.assertIn("extraction confidence: 0.42", prompt)
        self.assertNotIn("None-None", prompt)
        self.assertIn("Do not diagnose a condition", prompt)
        self.assertIn("Do not invent, infer, or change test values", prompt)
        self.assertIn("qualified healthcare professional", prompt)
