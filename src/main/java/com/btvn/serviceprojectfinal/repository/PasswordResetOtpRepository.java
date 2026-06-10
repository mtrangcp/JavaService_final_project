package com.btvn.serviceprojectfinal.repository;

import com.btvn.serviceprojectfinal.model.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    // Lấy OTP mới nhất còn hiệu lực của email
    Optional<PasswordResetOtp> findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);

    // Xóa toàn bộ OTP cũ của email khi tạo mới
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetOtp o WHERE o.email = :email")
    void deleteAllByEmail(@Param("email") String email);
}