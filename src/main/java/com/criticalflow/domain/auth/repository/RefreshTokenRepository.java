package com.criticalflow.domain.auth.repository;

import com.criticalflow.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenValue(String tokenValue);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
