package com.coin.demo.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coin.demo.domain.PortfolioSnapshot;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {
    List<PortfolioSnapshot> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(Long userId, Instant start, Instant end);
}


