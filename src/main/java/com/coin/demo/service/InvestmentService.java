package com.coin.demo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.coin.demo.domain.Investment;
import com.coin.demo.repository.InvestmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final InvestmentRepository investmentRepository;

    public List<Investment> getInvestmentsForUser(Long userId) {
        return investmentRepository.findByUserId(userId);
    }

    public Page<Investment> getInvestmentsPage(Long userId, Pageable pageable) {
        return investmentRepository.findByUserId(userId, pageable);
    }
}
