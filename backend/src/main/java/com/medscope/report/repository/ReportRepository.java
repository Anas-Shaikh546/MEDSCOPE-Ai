package com.medscope.report.repository;

import com.medscope.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Every method here is deliberately user-scoped. There is no findById()
 * usage anywhere in the report/ module for "this user's own resource"
 * operations - always findByIdAndUserId / findAllByUserId, so the
 * database query itself enforces ownership rather than a service-layer
 * check that's easy to forget.
 */
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Report> findByIdAndUserId(Long id, Long userId);

    boolean existsByStoredFilename(String storedFilename);
}