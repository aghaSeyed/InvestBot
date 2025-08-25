package com.coin.demo.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.coin.demo.domain.Investment;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserId(Long userId);

    List<Investment> findByUserIdAndDateBetween(Long userId, Instant start, Instant end);

    Page<Investment> findByUserId(Long userId, Pageable pageable);
}
