package com.btvn.serviceprojectfinal.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PerformanceLoggingAspect {
    @Around("execution(* com.btvn.serviceprojectfinal.controller..*(..))")
    public Object logControllerPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return measureExecutionTime(joinPoint, "CONTROLLER");
    }

    @Around("execution(* com.btvn.serviceprojectfinal.service..*(..))")
    public Object logServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return measureExecutionTime(joinPoint, "SERVICE");
    }

    private Object measureExecutionTime(ProceedingJoinPoint joinPoint,
                                        String layer) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className  = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            if (duration < 1000) {
                log.info("[AOP - PERF] [{}] {}.{}() completed in {} ms",
                        layer, className, methodName, duration);
            } else {
                log.warn("[AOP - PERF - SLOW] [{}] {}.{}() completed in {} ms — SLOW RESPONSE WARNING",
                        layer, className, methodName, duration);
            }

            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[AOP - PERF] [{}] {}.{}() FAILED after {} ms | Exception: {}",
                    layer, className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }
}