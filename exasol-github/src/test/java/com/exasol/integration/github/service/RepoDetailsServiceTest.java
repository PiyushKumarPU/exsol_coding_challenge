package com.exasol.integration.github.service;

import com.exasol.integration.github.config.OrganizationRepoProperties;
import com.exasol.integration.github.dto.RepositoryInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
@SpringBootTest
class RepoDetailsServiceTest {


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RepoDetailsService repoDetailsService;

    private MockRestServiceServer mockServer;

    @MockitoBean
    private OrganizationRepoProperties organizationRepoProperties;

    private final String baseUrl = "https://api.github.com";

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testValidRepositoryWithTags() {
        String repo = "sample-repo";
        String org = "exasol";

        // Mocking /tags response
        mockServer.expect(once(), requestTo(baseUrl + "/repos/exasol/" + repo + "/tags"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                    [
                        {
                            "name": "v1.0.0",
                            "commit": { "sha": "abc123" }
                        }
                    ]
                """, MediaType.APPLICATION_JSON));

        // Mocking /commits/sha response
        mockServer.expect(once(), requestTo(baseUrl + "/repos/exasol/" + repo + "/commits/abc123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                    {
                        "commit": {
                            "committer": {
                                "date": "2025-04-09T08:52:31Z"
                            }
                        }
                    }
                """, MediaType.APPLICATION_JSON));

        Map<String, List<String>> repoMap = new HashMap<>();
        repoMap.put(org, List.of(repo));
        when(organizationRepoProperties.getProcessedRepositories()).thenReturn(repoMap);
        List<RepositoryInfo> results = repoDetailsService.retrieveLatestTagDetails();

        assertThat(results).hasSize(1);
        RepositoryInfo info = results.get(0);
        assertThat(info.getOrganization()).isEqualTo(org);
        assertThat(info.getRepository()).isEqualTo(repo);
        assertThat(info.getLatestReleaseVersion()).isEqualTo("v1.0.0");
        assertThat(info.getLatestReleaseDate()).isEqualTo("2025-04-09");

        mockServer.verify();
    }

    @Test
    void testRepositoryWithNoTags() {
        String repo = "empty-tags-repo";
        String org = "exasol";

        mockServer.expect(once(), requestTo(baseUrl + "/repos/exasol/" + repo + "/tags"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        Map<String, List<String>> repoMap = new HashMap<>();
        repoMap.put(org, List.of(repo));
        when(organizationRepoProperties.getProcessedRepositories()).thenReturn(repoMap);

        List<RepositoryInfo> results = repoDetailsService.retrieveLatestTagDetails();

        assertThat(results).hasSize(1);
        RepositoryInfo info = results.get(0);
        assertThat(info.getLatestReleaseVersion()).isEqualTo("");
        assertThat(info.getLatestReleaseDate()).isEqualTo("");
    }

    @Test
    void testMissingCommitField() {
        String repo = "missing-commit-repo";
        String org = "exasol";

        mockServer.expect(once(), requestTo(baseUrl + "/repos/exasol/" + repo + "/tags"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                    [
                        {
                            "name": "v1.1.0"
                        }
                    ]
                """, MediaType.APPLICATION_JSON));

        Map<String, List<String>> repoMap = new HashMap<>();
        repoMap.put(org, List.of(repo));
        when(organizationRepoProperties.getProcessedRepositories()).thenReturn(repoMap);

        List<RepositoryInfo> results = repoDetailsService.retrieveLatestTagDetails();

        assertThat(results).hasSize(1);
        RepositoryInfo info = results.get(0);
        assertThat(info.getLatestReleaseVersion()).isEqualTo("v1.1.0");
        assertThat(info.getLatestReleaseDate()).isEqualTo("");
    }

    @Test
    void testGitHubApi404Error() {
        String repo = "non-existent-repo";
        String org = "exasol";

        mockServer.expect(once(), requestTo(baseUrl + "/repos/exasol/" + repo + "/tags"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Map<String, List<String>> repoMap = new HashMap<>();
        repoMap.put(org, List.of(repo));
        when(organizationRepoProperties.getProcessedRepositories()).thenReturn(repoMap);

        List<RepositoryInfo> results = repoDetailsService.retrieveLatestTagDetails();

        assertThat(results).hasSize(1);
        RepositoryInfo info = results.get(0);
        assertThat(info.getLatestReleaseVersion()).isEqualTo("");
        assertThat(info.getLatestReleaseDate()).isEqualTo("");
    }

    @Test
    void testGitHubApi500Error() {
        String repo = "error-repo";
        String org = "exasol";

        mockServer.expect(once(), requestTo(baseUrl + "/repos/exasol/" + repo + "/tags"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        Map<String, List<String>> repoMap = new HashMap<>();
        repoMap.put(org, List.of(repo));
        when(organizationRepoProperties.getProcessedRepositories()).thenReturn(repoMap);

        List<RepositoryInfo> results = repoDetailsService.retrieveLatestTagDetails();

        assertThat(results).hasSize(1);
        RepositoryInfo info = results.get(0);
        assertThat(info.getLatestReleaseVersion()).isEqualTo("");
        assertThat(info.getLatestReleaseDate()).isEqualTo("");
    }

    @Test
    void testEmptyRepositoriesMap_throwsException() {
        when(organizationRepoProperties.getProcessedRepositories()).thenReturn(Collections.emptyMap());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            repoDetailsService.retrieveLatestTagDetails();
        });

        assertThat(exception.getMessage()).isEqualTo("No repositories configured");
    }
}