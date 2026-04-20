package com.formforge.controller;

import com.formforge.dto.CreateApplicationRequest;
import com.formforge.dto.PageResponse;
import com.formforge.dto.UpdateApplicationRequest;
import com.formforge.model.Application;
import com.formforge.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService service;

    @GetMapping
    public ResponseEntity<PageResponse<Application>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(service.getAll(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<Application> create(@Valid @RequestBody CreateApplicationRequest request) {
        Application created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Application> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
