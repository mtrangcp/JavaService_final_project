package com.btvn.serviceprojectfinal.repository;

import com.btvn.serviceprojectfinal.model.entity.JobPosting;
import com.btvn.serviceprojectfinal.model.entity.enums.JobStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    @Query("SELECT j FROM JobPosting j WHERE " +
            "(:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:status IS NULL OR j.status = :status)")
    Page<JobPosting> searchJobs(@Param("keyword") String keyword,
                                @Param("status") JobStatusEnum status,
                                Pageable pageable);


    Page<JobPosting> findByEmployerId(Long employerId, Pageable pageable);
}
