package com.materials.quoting.controller;

import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/info")
public class AppInfoController {

    private final BuildProperties buildProperties;

    public AppInfoController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    /**
     * Returns application build information including the version defined in build.gradle.
     * Example response:
     * {
     *   "name":    "quoting",
     *   "version": "0.0.1-SNAPSHOT",
     *   "group":   "com.materials",
     *   "time":    "2026-05-16T10:00:00Z"
     * }
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> info() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("name",    buildProperties.getName());
        body.put("version", buildProperties.getVersion());
        body.put("group",   buildProperties.getGroup());
        body.put("time",    buildProperties.getTime() != null ? buildProperties.getTime().toString() : "N/A");
        return ResponseEntity.ok(body);
    }
}

