package com.medscope.interpretation.repository;

import com.medscope.interpretation.entity.AnalysisFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisFindingRepository extends JpaRepository<AnalysisFinding, Long> {

    List<AnalysisFinding> findAllByAnalysisIdOrderById(Long analysisId);

    void deleteAllByAnalysisId(Long analysisId);
}
