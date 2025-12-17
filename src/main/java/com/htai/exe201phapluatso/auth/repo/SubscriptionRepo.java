package com.htai.exe201phapluatso.auth.repo;

import com.htai.exe201phapluatso.auth.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepo extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findTopByUserIdOrderByStartAtDesc(Long userId);
}
