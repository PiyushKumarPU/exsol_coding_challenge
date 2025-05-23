package com.exasol.integration.github.service;


import com.exasol.integration.github.config.OrganizationRepoProperties;
import com.exasol.integration.github.dto.RepositoryInfo;
import com.exasol.integration.github.util.DateFormatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepoDetailsService {

    private static final String TAGS_URI_FORMAT = "%s/repos/exasol/%s/tags";
    private static final String COMMIT_URI_FORMAT = "%s/repos/%s/%s/commits/%s";

    private final RestTemplate restTemplate;
    private final OrganizationRepoProperties organizationRepoProperties;

    @Value("${github.api.base-url}")
    private String baseUrl;


    private ExecutorService executor;
    private Map.Entry<String, List<String>> firstEntry;


    /**
     * Retrieves the latest tag details for all configured repositories under each organization.
     * <p>
     * This method typically queries the GitHub API (or similar sources) to fetch:
     * <ul>
     *     <li>The most recent release tag</li>
     *     <li>The release version</li>
     *     <li>The release date</li>
     * </ul>
     *
     * @return a list of repository metadata objects containing organization name, repository name,
     * latest release version, and release date.
     */
    public List<RepositoryInfo> retrieveLatestTagDetails() {
        try {
            if (organizationRepoProperties == null || organizationRepoProperties.getProcessedRepositories().isEmpty())
                throw new IllegalStateException("No repositories configured");
            firstEntry = organizationRepoProperties.getProcessedRepositories().entrySet().iterator().next();
            String organization = firstEntry.getKey();
            List<String> repositories = firstEntry.getValue();
            int repoCount = repositories.size();
            executor = Executors.newFixedThreadPool(repoCount);
            List<RepositoryInfo> results = executeTask(constructTasks(organization, repositories));
            executor.shutdown();
            return results;
        } catch (InterruptedException e) {
            log.error("Error fetching latest tag details", e);
        }
        return List.of();
    }


    private List<Callable<RepositoryInfo>> constructTasks(String organization, List<String> repositories) {
        return repositories.stream().map(repo -> (Callable<RepositoryInfo>) () -> {
            RepositoryInfo.RepositoryInfoBuilder infoBuilder = RepositoryInfo.builder().organization(organization).repository(repo);

            try {
                Map<String, Object> latestTag = fetchLatestTag(repo);
                if (latestTag == null || latestTag.isEmpty()) return infoBuilder.build();

                String version = (String) latestTag.get("name");
                if (version != null)
                    infoBuilder.latestReleaseVersion(version);

                @SuppressWarnings("unchecked")
                Map<String, Object> latestCommitMap = ((Map<String, Object>) latestTag.get("commit"));
                String commitSha = latestCommitMap.isEmpty() ? "" : (String) latestCommitMap.get("sha");

                String releaseDate = fetchCommitDate(organization, repo, commitSha);
                if (releaseDate != null)
                    infoBuilder.latestReleaseDate(DateFormatUtil.formatIsoUtcToYyyyMmDd(releaseDate));


            } catch (Exception e) {
                log.error("Error fetching latest tag details for {}/{}", organization, repo, e);
            }
            return infoBuilder.build();
        }).toList();
    }

    private List<RepositoryInfo> executeTask(List<Callable<RepositoryInfo>> tasks) throws InterruptedException {
        return executor.invokeAll(tasks).stream().map(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error fetching latest tag details", e);
            }
            return null;
        }).filter(Objects::nonNull).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchLatestTag(String repo) {
        String tagsUrl = String.format(TAGS_URI_FORMAT, baseUrl, repo);
        List<Map<String, Object>> tags = restTemplate.getForObject(tagsUrl, List.class);
        return (tags != null && !tags.isEmpty()) ? tags.get(0) : null;
    }

    @SuppressWarnings("unchecked")
    private String fetchCommitDate(String organization, String repo, String commitSha) {
        String commitUrl = String.format(COMMIT_URI_FORMAT, baseUrl, organization, repo, commitSha);
        Map<String, Object> commit = restTemplate.getForObject(commitUrl, Map.class);

        if (commit == null) return null;

        Map<String, Object> commitInfo = (Map<String, Object>) commit.get("commit");
        if (commitInfo == null) return null;

        Map<String, Object> committer = (Map<String, Object>) commitInfo.get("committer");
        return committer != null ? (String) committer.get("date") : null;
    }
}
