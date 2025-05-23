package com.exasol.integration.github.web.controller;

import com.exasol.integration.github.dto.RepositoryInfo;
import com.exasol.integration.github.service.RepoDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/repo-details")
public class RepoDetailsController {

    private final RepoDetailsService repoDetailsService;

    @GetMapping
    public ResponseEntity<List<RepositoryInfo>> getRepoDetails() {
        List<RepositoryInfo> results = repoDetailsService.retrieveLatestTagDetails();
        if (results.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(results);
    }
}
