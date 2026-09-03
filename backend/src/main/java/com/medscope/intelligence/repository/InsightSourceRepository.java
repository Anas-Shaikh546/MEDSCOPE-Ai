package com.medscope.intelligence.repository;

import com.medscope.intelligence.entity.InsightSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsightSourceRepository extends JpaRepository<InsightSource, Long> {

    List<InsightSource> findAllByInsightId(Long insightId);

    void deleteAllByInsightId(Long insightId);
}