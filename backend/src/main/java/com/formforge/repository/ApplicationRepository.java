package com.formforge.repository;

import com.formforge.model.Application;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    Page<Application> findByType(ApplicationType type, Pageable pageable);

    List<Application> findByStatus(ApplicationStatus status);

    List<Application> findByType(ApplicationType type);

    List<Application> findByAcademicYear(String academicYear);

    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT a.type, COUNT(a) FROM Application a GROUP BY a.type")
    List<Object[]> countGroupByType();

    @Query("SELECT a.semester, COUNT(a) FROM Application a GROUP BY a.semester")
    List<Object[]> countGroupBySemester();
}
