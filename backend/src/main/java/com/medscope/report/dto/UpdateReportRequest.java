package com.medscope.report.dto;

import java.time.LocalDate;

/**
 * The only mutable field a report has post-upload. testDate accepts
 * either a real date or an explicit JSON null (to clear a previously
 * entered date) - there's nothing else on this DTO, so there's no
 * ambiguity between "field omitted" and "field explicitly null" to
 * worry about the way there would be on a multi-field PATCH.
 */
public record UpdateReportRequest(LocalDate testDate) {
}