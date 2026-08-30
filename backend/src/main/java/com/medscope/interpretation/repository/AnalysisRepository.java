package com.medscope.interpretation.repository;

import com.medscope.interpretation.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByReportId(Long reportId);
}