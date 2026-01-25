package com.touplus.billing_api.admin.repository.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.touplus.billing_api.admin.repository.UserSettlementRepository;
import com.touplus.billing_api.domain.message.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class UserSettlementRepositoryImpl implements UserSettlementRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<User> findUsersBySettlementMonth(LocalDate settlementMonth, int page, int size) {
        // 예시: 이번 달 정산 대상 사용자 조회 (가입 여부, 상태 등 조건에 따라 변경)
        return em.createQuery(
                "SELECT u FROM User u WHERE u.createdAt <= :monthEnd ORDER BY u.userId ASC", User.class)
                .setParameter("monthEnd", settlementMonth.withDayOfMonth(settlementMonth.lengthOfMonth()))
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public long countUsersBySettlementMonth(LocalDate settlementMonth) {
        return em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.createdAt <= :monthEnd", Long.class)
                .setParameter("monthEnd", settlementMonth.withDayOfMonth(settlementMonth.lengthOfMonth()))
                .getSingleResult();
    }
}
