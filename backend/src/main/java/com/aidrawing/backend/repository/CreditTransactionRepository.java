package com.aidrawing.backend.repository;

import com.aidrawing.backend.entity.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, String> {

    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
