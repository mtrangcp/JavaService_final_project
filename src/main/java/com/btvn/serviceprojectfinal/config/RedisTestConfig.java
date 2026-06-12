package com.btvn.serviceprojectfinal.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisTestConfig {

    @Bean
    CommandLineRunner testRedisConnection(StringRedisTemplate redisTemplate) {
        return args -> {
            System.out.println("====== ĐANG KIỂM TRA KẾT NỐI REDIS CLOUD ======");
            try {
                // 1. Thử ghi một dữ liệu tạm thời vào Redis Cloud
                redisTemplate.opsForValue().set("test_key", "Kết nối Redis Cloud thành công 100%!");

                // 2. Thử đọc lại dữ liệu đó ra
                String value = redisTemplate.opsForValue().get("test_key");

                System.out.println(">>> KẾT QUẢ: " + value);
                System.out.println("===============================================");
            } catch (Exception e) {
                System.err.println(">>> LỖI: Không thể kết nối tới Redis Cloud!");
                System.err.println(">>> Vui lòng kiểm tra lại cấu hình .env hoặc mật khẩu.");
                System.err.println("===============================================");
                e.printStackTrace();
            }
        };
    }
}