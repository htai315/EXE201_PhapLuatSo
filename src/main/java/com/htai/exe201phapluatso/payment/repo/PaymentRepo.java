package com.htai.exe201phapluatso.payment.repo;

import com.htai.exe201phapluatso.payment.entity.Payment;
import com.htai.exe201phapluatso.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
    
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.plan WHERE p.user = :user ORDER BY p.createdAt DESC")
    List<Payment> findByUserOrderByCreatedAtDesc(@Param("user") User user);
    
    @Query("SELECT p FROM Payment p WHERE p.user = :user AND p.status = 'SUCCESS' ORDER BY p.createdAt DESC")
    List<Payment> findSuccessfulPaymentsByUser(@Param("user") User user);
}
