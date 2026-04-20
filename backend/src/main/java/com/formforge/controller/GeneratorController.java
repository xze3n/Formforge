package com.formforge.controller;

import com.formforge.service.ApplicationGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
public class GeneratorController {

    private final ApplicationGeneratorService generatorService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        boolean started = generatorService.start();
        return ResponseEntity.ok(Map.of(
                "running", true,
                "started", started
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        boolean stopped = generatorService.stop();
        return ResponseEntity.ok(Map.of(
                "running", false,
                "stopped", stopped
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "running", generatorService.isRunning()
        ));
    }
}
