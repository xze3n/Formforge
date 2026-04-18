package com.formforge.service;

import com.formforge.dto.CreateApplicationRequest;
import com.formforge.dto.PageResponse;
import com.formforge.dto.UpdateApplicationRequest;
import com.formforge.exception.ApplicationNotFoundException;
import com.formforge.model.Application;
import com.formforge.repository.InMemoryApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final InMemoryApplicationRepository repository;

    public PageResponse<Application> getAll(int page, int size) {
        List<Application> all = repository.findAll();
        long totalElements = all.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, all.size());

        List<Application> content;
        if (fromIndex >= all.size()) {
            content = List.of();
        } else {
            content = all.subList(fromIndex, toIndex);
        }

        return new PageResponse<>(content, page, size, totalElements, totalPages);
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

        if (request.getType() != null) {
            existing.setType(request.getType());
        }
        if (request.getAcademicYear() != null) {
            existing.setAcademicYear(request.getAcademicYear());
        }
        if (request.getSemester() != null) {
            existing.setSemester(request.getSemester());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.deleteById(id)) {
            throw new ApplicationNotFoundException(id);
        }
    }
}
