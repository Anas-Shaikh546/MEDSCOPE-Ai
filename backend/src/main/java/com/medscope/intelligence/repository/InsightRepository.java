package com.medscope.intelligence.repository;

import com.medscope.intelligence.entity.Insight;
import com.medscope.intelligence.entity.InsightPriority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsightRepository extends JpaRepository<Insight, Long> {

    List<Insight> findAllByGenerationIdOrderByPriorityAsc(Long generationId);

    List<Insight> findAllByGenerationId(Long generationId);

    void deleteAllByGenerationId(Long generationId);
}