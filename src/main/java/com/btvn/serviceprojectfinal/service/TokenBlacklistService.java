package com.btvn.serviceprojectfinal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    // Prefix để tránh xung đột key với các feature khác trong Redis
    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    // ===== Thêm token vào blacklist =====
    // TTL = thời gian còn lại của token → Redis tự xóa khi hết hạn
    public void blacklistToken(String token, Date tokenExpiration) {
        String key = BLACKLIST_PREFIX + token;

        // Tính thời gian sống còn lại của token
        long now = System.currentTimeMillis();
        long expireAt = tokenExpiration.getTime();
        long ttlMillis = expireAt - now;

        if (ttlMillis <= 0) {
            // Token đã hết hạn tự nhiên → không cần lưu
            log.debug("[BLACKLIST] Token đã hết hạn, không cần blacklist.");
            return;
        }

        // Lưu vào Redis với TTL chính xác bằng thời gian còn lại của token
        // Khi token hết hạn → Redis tự động xóa key → không tốn bộ nhớ
        redisTemplate.opsForValue().set(
                key,
                "revoked",
                Duration.ofMillis(ttlMillis)
        );

        log.info("[BLACKLIST] Token đã được blacklist. TTL: {} giây",
                ttlMillis / 1000);
    }

    // ===== Kiểm tra token có trong blacklist không =====
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}