# GitHub Repository Latest Release Details API

This Spring Boot application exposes a REST API endpoint to retrieve the latest release information of configured GitHub repositories. It fetches details like the latest release version and its date for each repository.

## Features

- Fetches latest tag and its commit date from public GitHub repositories.
- Returns details as a JSON array.
- Handles empty results gracefully by returning 404.

## Technologies Used

- Java 17+
- Spring Boot 3.x
- Spring Web (REST)
- Lombok
- Jackson for JSON serialization

## API Endpoint

### `GET /api/v1/repo-details`

Fetches the latest release details for all configured repositories.

#### Sample Response

```json
[
  {
    "Organization": "exasol",
    "Repository": "advanced-analytics-framework",
    "Latest_Release_Date": "2025-04-09",
    "Latest_Release_Version": "0.4.1"
  },
  {
    "Organization": "exasol",
    "Repository": "bucketfs-java",
    "Latest_Release_Date": "2025-03-22",
    "Latest_Release_Version": "1.2.0"
  }
]
