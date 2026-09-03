package com.medscope.intelligence.repository;

import com.medscope.intelligence.entity.InsightGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsightGenerationRepository extends JpaRepository<InsightGeneration, Long> {

    List<InsightGeneration> findAllByReportIdOrderByGenerationNumberDesc(Long reportId);

    Optional<InsightGeneration> findTopByReportIdOrderByGenerationNumberDesc(Long reportId);

    int countByReportId(Long reportId);
}