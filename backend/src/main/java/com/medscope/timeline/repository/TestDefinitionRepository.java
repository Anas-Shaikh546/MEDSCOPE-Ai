package com.medscope.timeline.repository;

import com.medscope.timeline.entity.TestDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TestDefinitionRepository extends JpaRepository<TestDefinition, Long> {

    Optional<TestDefinition> findByCanonicalName(String canonicalName);
}