package com.formforge.service;

import com.formforge.dto.CreateApplicationRequest;
import com.formforge.dto.PageResponse;
import com.formforge.dto.UpdateApplicationRequest;
import com.formforge.exception.ApplicationNotFoundException;
import com.formforge.model.Application;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository repository;

    public PageResponse<Application> getAll(int page, int size) {
        Page<Application> jpaPage = repository.findAll(PageRequest.of(page, size));
        return new PageResponse<>(
                jpaPage.getContent(),
                page,
                size,
                jpaPage.getTotalElements(),
                jpaPage.getTotalPages()
        );
    }

    public PageResponse<Application> getByStatus(ApplicationStatus status, int page, int size) {
        Page<Application> jpaPage = repository.findByStatus(status, PageRequest.of(page, size));
        return new PageResponse<>(jpaPage.getContent(), page, size, jpaPage.getTotalElements(), jpaPage.getTotalPages());
    }

    public PageResponse<Application> getByType(ApplicationType type, int page, int size) {
        Page<Application> jpaPage = repository.findByType(type, PageRequest.of(page, size));
        return new PageResponse<>(jpaPage.getContent(), page, size, jpaPage.getTotalElements(), jpaPage.getTotalPages());
    }

    public Application getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    public Application create(CreateApplicationRequest request) {
        Application application = new Application();
        application.setType(request.getType());
        application.setAcademicYear(request.getAcademicYear());
        application.setSemester(request.getSemester());
        application.setStatus(request.getStatus());
        return repository.save(application);
    }

    public Application update(Long id, UpdateApplicationRequest request) {
        Application existing = repository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        if (request.getType() != null) existing.setType(request.getType());
        if (request.getAcademicYear() != null) existing.setAcademicYear(request.getAcademicYear());
        if (request.getSemester() != null) existing.setSemester(request.getSemester());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());

        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ApplicationNotFoundException(id);
        }
        repository.deleteById(id);
    }

    public Map<String, Map<String, Long>> getStatistics() {
        Map<String, Long> byStatus = toMap(repository.countGroupByStatus());
        Map<String, Long> byType = toMap(repository.countGroupByType());
        Map<String, Long> bySemester = toMap(repository.countGroupBySemester());

        Map<String, Map<String, Long>> stats = new LinkedHashMap<>();
        stats.put("byStatus", byStatus);
        stats.put("byType", byType);
        stats.put("bySemester", bySemester);
        return stats;
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }
}
