# Operational Guide: Configuration

## Frontend bundle {#frontend-bundle}

`LINKLIFT_WEBAPP_DIR` (env, a directory path; unset by default) names the built React bundle the server serves (RQ-SFFA-001). The image built from the root `Dockerfile` sets it (NF-SFFA-001); leave it unset for tests and local JAR runs. A value that names no usable bundle is logged as a warning at startup and the server runs without the frontend (RQ-SFFA-001).
