package com.medscope.interpretation.client;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.interpretation.dto.ReportResultDto;

import java.util.List;

/**
 * Maps ReportResult entities to DTOs for AI service communication.
 */
public class ReportResultMapper {

    public static ReportResultDto toDto(ReportResult entity) {
        return ReportResultDto.builder()
                .testName(entity.getTestName())
                .normalizedTestName(entity.getNormalizedTestName())
                .rawValue(entity.getRawValue())
                .numericValue(entity.getNumericValue())
                .unit(entity.getUnit())
                .referenceLow(entity.getReferenceLow())
                .referenceHigh(entity.getReferenceHigh())
                .status(entity.getStatus().name())
                .confidence(entity.getConfidence())
                .pageNumber(entity.getPageNumber())
                .build();
    }

    public static List<ReportResultDto> toDtoList(List<ReportResult> entities) {
        return entities.stream()
                .map(ReportResultMapper::toDto)
                .toList();
    }
}
