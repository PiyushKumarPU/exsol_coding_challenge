
package com.exasol.integration.github.config;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrganizationRepoProperties {

    private static final Map<String, List<String>> repositories = new HashMap<>();

    static {
        repositories.put("exasol", Arrays.asList(
                "advanced-analytics-framework",
                "bucketfs-java",
                "compatibility-test-suite",
                "docker-db",
                "exasol-virtual-schema"
        ));
    }

    public Map<String, List<String>> getProcessedRepositories() {
        return repositories;
    }
}
