package com.btvn.serviceprojectfinal.aspect;

import com.btvn.serviceprojectfinal.model.dto.response.ApplicationResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ApplicationLoggingAspect {

    // Ghi log TRƯỚC khi nộp hồ sơ
    @Before("execution(* com.btvn.serviceprojectfinal.service.CandidateService.applyJob(..))")
    public void logBeforeApply(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        log.info("[AOP - BEFORE] Candidate đang thực hiện nộp hồ sơ. Args: {}", args[0]);
    }

    // Ghi log SAU KHI nộp hồ sơ THÀNH CÔNG — UC-04 yêu cầu @AfterReturning
    @AfterReturning(
            pointcut = "execution(* com.btvn.serviceprojectfinal.service.CandidateService.applyJob(..))",
            returning = "result"
    )
    public void logAfterApplySuccess(JoinPoint joinPoint, Object result) {
        if (result instanceof ApplicationResponse response) {
            log.info("[AOP - SUCCESS] Candidate ID: {} đã nộp hồ sơ thành công " +
                            "vào Job ID: {} | Application ID: {} | Status: {}",
                    response.getCandidateId(),
                    response.getJobId(),
                    response.getId(),
                    response.getStatus());
        }
    }

    // Ghi log KHI CÓ EXCEPTION — @AfterThrowing
    @AfterThrowing(
            pointcut = "execution(* com.btvn.serviceprojectfinal.service.CandidateService.applyJob(..))",
            throwing = "exception"
    )
    public void logAfterApplyFailed(JoinPoint joinPoint, Exception exception) {
        log.error("[AOP - FAILED] Nộp hồ sơ thất bại. Lý do: {} - {}",
                exception.getClass().getSimpleName(),
                exception.getMessage());
    }

    // Ghi log khi Employer cập nhật trạng thái hồ sơ
    @AfterReturning(
            pointcut = "execution(* com.btvn.serviceprojectfinal.service.employer.EmployerService.updateApplicationStatus(..))",
            returning = "result"
    )
    public void logAfterStatusUpdate(JoinPoint joinPoint, Object result) {
        if (result instanceof ApplicationResponse response) {
            log.info("[AOP - STATUS UPDATE] Application ID: {} được cập nhật " +
                            "sang trạng thái: {} | Feedback: {}",
                    response.getId(),
                    response.getStatus(),
                    response.getEmployerFeedback());
        }
    }
}