package com.btvn.serviceprojectfinal.repository;

import com.btvn.serviceprojectfinal.model.entity.Application;
import com.btvn.serviceprojectfinal.model.entity.enums.ApplicationStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // ktra candidate  nộp hồ sơ vào job này chưa
    boolean existsByCandidateIdAndJobPostingId(Long candidateId, Long jobPostingId);

    // ds hồ sơ của candidate
    @Query("SELECT a FROM Application a WHERE a.candidate.id = :candidateId " +
            "AND (:status IS NULL OR a.status = :status)")
    Page<Application> findByCandidateId(@Param("candidateId") Long candidateId,
                                        @Param("status") ApplicationStatusEnum status,
                                        Pageable pageable);

    // ds hồ sơ theo job (Employer xem)
    @Query("SELECT a FROM Application a WHERE a.jobPosting.id = :jobId " +
            "AND (:status IS NULL OR a.status = :status)")
    Page<Application> findByJobPostingId(@Param("jobId") Long jobId,
                                         @Param("status") ApplicationStatusEnum status,
                                         Pageable pageable);

    // all hồ sơ thuộc các job của 1 employer
    @Query("SELECT a FROM Application a WHERE a.jobPosting.employer.id = :employerId " +
            "AND (:status IS NULL OR a.status = :status)")
    Page<Application> findByEmployerId(@Param("employerId") Long employerId,
                                       @Param("status") ApplicationStatusEnum status,
                                       Pageable pageable);
}