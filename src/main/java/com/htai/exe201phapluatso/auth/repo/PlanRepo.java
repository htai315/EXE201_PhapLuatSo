package com.htai.exe201phapluatso.auth.repo;

import com.htai.exe201phapluatso.auth.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepo extends JpaRepository<Plan, Long> {
    Optional<Plan> findByCode(String code);
}
