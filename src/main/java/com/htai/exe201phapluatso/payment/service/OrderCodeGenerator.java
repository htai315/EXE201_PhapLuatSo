package com.htai.exe201phapluatso.payment.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating unique order codes using database sequence.
 * This ensures no collision even in distributed/concurrent environments.
 */
@Service
public class OrderCodeGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Generate next unique order code from database sequence.
     * 
     * @return unique order code (8 digits: 10000000-99999999)
     */
    @Transactional
    public long generateOrderCode() {
        // Use native SQL to get next value from sequence
        // This is thread-safe and works in distributed systems
        Long orderCode = (Long) entityManager
                .createNativeQuery("SELECT NEXT VALUE FOR order_code_sequence")
                .getSingleResult();
        
        return orderCode;
    }
}
