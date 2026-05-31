package com.aidrawing.backend.repository;

import com.aidrawing.backend.entity.Credit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditRepository extends JpaRepository<Credit, String> {

    Optional<Credit> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
