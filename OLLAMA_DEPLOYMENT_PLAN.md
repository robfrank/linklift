# Ollama Deployment Integration Plan

## Overview

This document outlines the plan to add Ollama as a container service in both Docker Compose (local development) and Kamal deployment (production).

---

## Current State Analysis

### Existing Configuration

**Docker Compose Services:**

- `arcadedb` - Database (port 2480, 2424)
- `linklift` - API (port 7070)
- `webapp` - Frontend (port 80)

**Kamal Accessories:**

- `arcadedb` - Database with persistent volumes

**Ollama Configuration:**

- Environment variables: `LINKLIFT_OLLAMA_URL`, `LINKLIFT_OLLAMA_MODEL`, `LINKLIFT_OLLAMA_DIMENSIONS`
- Default URL: `http://localhost:11434`
- Current model for E2E tests: `all-minilm:l6-v2` (384 dimensions)
- Default model in config: `nomic-embed-text` (768 dimensions) ❌ **Mismatch!**

**Configuration Issues to Fix:**

1. Default model `nomic-embed-text` produces 768 dimensions but schema expects 384
2. Need to change default to `all-minilm:l6-v2` to match schema

---

## Part 1: Docker Compose Configuration

### 1.1 Add Ollama Service

**Location:** `docker-compose.yml`

**Service Configuration:**

```yaml
ollama:
  image: ollama/ollama:latest
  container_name: ollama
  ports:
    - "11434:11434"
  volumes:
    - ollama_models:/root/.ollama
  healthcheck:
    test: "curl -f http://localhost:11434/ || exit 1"
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 30s
  restart: unless-stopped
```

**Why this configuration:**

- Port 11434: Standard Ollama API port
- Volume `ollama_models`: Persistent storage for downloaded models (~23MB for all-minilm:l6-v2)
- Health check: Ensures Ollama is ready before dependent services start
- `start_period: 30s`: Allows time for initial startup

### 1.2 Add Model Initialization Service

**Service Configuration:**

```yaml
ollama-init:
  image: ollama/ollama:latest
  container_name: ollama-init
  volumes:
    - ollama_models:/root/.ollama
  depends_on:
    ollama:
      condition: service_healthy
  entrypoint: /bin/sh -c "ollama pull all-minilm:l6-v2 && echo 'Model ready'"
  restart: "no"
```

**Why this approach:**

- One-time initialization service
- Pulls `all-minilm:l6-v2` model on first start
- Shares volume with main ollama service
- `restart: "no"`: Runs once then stops
- Ensures model is available before application starts

### 1.3 Update LinkLift Service

**Update environment variables:**

```yaml
linklift:
  image: robfrank/linklift:latest
  container_name: linklift
  environment:
    - LINKLIFT_JWT_SECRET=whatthefuckisajwtsecretandwhyishouldsetit
    - LINKLIFT_OLLAMA_URL=http://ollama:11434
    - LINKLIFT_OLLAMA_MODEL=all-minilm:l6-v2
    - LINKLIFT_OLLAMA_DIMENSIONS=384
    - JAVA_OPTS=-Dlinklift.arcadedb.host=arcadedb -Xmx512M -XX:+UseZGC -XX:+ZGenerational
  ports:
    - "7070:7070"
  depends_on:
    arcadedb:
      condition: service_healthy
    ollama:
      condition: service_healthy
    ollama-init:
      condition: service_completed_successfully
  restart: unless-stopped
```

**Key changes:**

- Added `LINKLIFT_OLLAMA_URL=http://ollama:11434` (container network hostname)
- Added `LINKLIFT_OLLAMA_MODEL=all-minilm:l6-v2` (matches schema dimensions)
- Added `LINKLIFT_OLLAMA_DIMENSIONS=384` (explicit dimension declaration)
- Added dependency on `ollama` and `ollama-init` services

### 1.4 Add Volume Declaration

```yaml
volumes:
  arcadedb_data:
    driver: local
  ollama_models:
    driver: local
```

---

## Part 2: Kamal Deployment Configuration

### 2.1 Add Ollama Accessory

**Location:** `config/deploy.yml`

**Accessory Configuration:**

```yaml
accessories:
  arcadedb:
    image: arcadedata/arcadedb-headless:25.12.1@sha256:5743b157ed0ffe2e466d476d6da404eee23e209d8107459b1529e25373b1787c
    host: <%= ENV["HOST_IP"] %>
    volumes:
      - /home/player/linklift/config:/home/arcadedb/config
      - /home/player/linklift/databases:/home/arcadedb/databases
      - /home/player/linklift/log:/home/arcadedb/log
    env:
      clear:
        JAVA_OPTS: "-Darcadedb.server.plugins=Postgres:com.arcadedb.postgres.PostgresProtocolPlugin"

  ollama:
    image: ollama/ollama:latest
    host: <%= ENV["HOST_IP"] %>
    port: 11434
    volumes:
      - /home/player/linklift/ollama:/root/.ollama
    cmd: serve
```

**Why this configuration:**

- `port: 11434`: Exposes Ollama API
- Volume `/home/player/linklift/ollama`: Persistent model storage on host
- `cmd: serve`: Explicitly starts Ollama server
- Matches accessory pattern used for ArcadeDB

### 2.2 Update Application Environment Variables

**Update in `config/deploy.yml`:**

```yaml
env:
  clear:
    JAVA_OPTS: "-Xmx2G -Dlinklift.arcadedb.host=linklift-arcadedb"
    LINKLIFT_OLLAMA_URL: "http://linklift-ollama:11434"
    LINKLIFT_OLLAMA_MODEL: "all-minilm:l6-v2"
    LINKLIFT_OLLAMA_DIMENSIONS: "384"
```

**Why these values:**

- `linklift-ollama`: Kamal naming convention for accessory (service_name-accessory_name)
- Port 11434: Standard Ollama port
- Model and dimensions match schema configuration

### 2.3 Model Initialization Strategy

**Option A: Manual Pre-deployment (Recommended)**

```bash
# SSH to production server
ssh player@<HOST_IP>

# Pull model manually
docker run -v /home/player/linklift/ollama:/root/.ollama ollama/ollama pull all-minilm:l6-v2
```

**Option B: Init Container (Advanced)**

Add a one-shot job in Kamal deployment hook:

```yaml
# config/deploy.yml
hooks:
  post-deploy:
    - docker run --rm -v /home/player/linklift/ollama:/root/.ollama ollama/ollama pull all-minilm:l6-v2 || true
```

**Recommendation:** Use Option A for initial setup, Option B for automation

---

## Part 3: Application Configuration Update

### 3.1 Fix Default Model in SecureConfiguration

**Location:** `src/main/java/it/robfrank/linklift/config/SecureConfiguration.java`

**Current code (line ~180):**

```java
public static String getOllamaModel() {
  return System.getenv().getOrDefault(OLLAMA_MODEL_ENV, "nomic-embed-text");
}

```

**Updated code:**

```java
public static String getOllamaModel() {
  return System.getenv().getOrDefault(OLLAMA_MODEL_ENV, "all-minilm:l6-v2");
}

```

**Why:**

- `all-minilm:l6-v2` produces 384 dimensions (matches schema)
- `nomic-embed-text` produces 768 dimensions (schema mismatch)
- Prevents dimension errors in production

---

## Part 4: Implementation Steps

### Phase 1: Local Development (Docker Compose)

1. **Update docker-compose.yml**

   - Add ollama service
   - Add ollama-init service
   - Update linklift environment variables
   - Add ollama_models volume

2. **Test locally**

   ```bash
   docker-compose down -v
   docker-compose up -d
   docker-compose logs -f ollama-init  # Wait for model pull
   docker-compose logs -f linklift     # Check connection
   ```

3. **Verify Ollama connection**

   ```bash
   curl http://localhost:11434/
   curl -X POST http://localhost:11434/api/embeddings \
     -H "Content-Type: application/json" \
     -d '{"model":"all-minilm:l6-v2","prompt":"test"}'
   ```

4. **Run E2E tests**
   ```bash
   # E2E tests should now work with local Ollama
   mvn test -Pe2e-tests
   ```

### Phase 2: Production Deployment (Kamal)

1. **Update config/deploy.yml**

   - Add ollama accessory
   - Update environment variables

2. **Pre-deployment setup**

   ```bash
   # SSH to production
   ssh player@<HOST_IP>

   # Create ollama directory
   mkdir -p /home/player/linklift/ollama

   # Pull model manually (first time only)
   docker run -v /home/player/linklift/ollama:/root/.ollama \
     ollama/ollama pull all-minilm:l6-v2
   ```

3. **Deploy with Kamal**

   ```bash
   kamal accessory boot ollama
   kamal accessory logs ollama  # Verify startup
   kamal deploy                 # Deploy application
   ```

4. **Verify deployment**

   ```bash
   # Check Ollama accessory
   kamal accessory details ollama

   # Test Ollama endpoint
   curl http://<HOST_IP>:11434/

   # Check application logs
   kamal app logs --tail 100
   ```

### Phase 3: Code Updates

1. **Update SecureConfiguration.java**

   - Change default model from `nomic-embed-text` to `all-minilm:l6-v2`

2. **Update documentation**

   - README.md: Document Ollama service
   - OLLAMA_DEPLOYMENT_PLAN.md: This file
   - 530-optional-e2e-tests.md: Update with production config

3. **Commit changes**
   ```bash
   git add docker-compose.yml config/deploy.yml src/main/java/it/robfrank/linklift/config/SecureConfiguration.java
   git commit -m "feat: Add Ollama container to deployment infrastructure"
   ```

---

## Part 5: Verification & Testing

### Local Development Verification

```bash
# 1. Start services
docker-compose up -d

# 2. Check all services healthy
docker-compose ps

# 3. Test Ollama
curl http://localhost:11434/

# 4. Test embedding generation
curl -X POST http://localhost:11434/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "model": "all-minilm:l6-v2",
    "prompt": "test embedding"
  }'

# 5. Run application tests
mvn test

# 6. Run E2E tests
mvn test -Pe2e-tests
```

### Production Verification

```bash
# 1. Check accessory status
kamal accessory details ollama

# 2. Verify Ollama responding
curl http://<HOST_IP>:11434/

# 3. Test embedding endpoint
curl -X POST http://<HOST_IP>:11434/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "model": "all-minilm:l6-v2",
    "prompt": "production test"
  }'

# 4. Check application logs for Ollama connection
kamal app logs | grep -i ollama

# 5. Test vector search functionality
curl http://linklift-api.arcadebrain.ai/api/v1/search?q=test
```

---

## Part 6: Troubleshooting

### Issue: Ollama container fails to start

**Check:**

```bash
docker-compose logs ollama
# or
kamal accessory logs ollama
```

**Common causes:**

- Port 11434 already in use
- Insufficient disk space for models
- Docker resource limits

**Solution:**

```bash
# Check port availability
netstat -an | grep 11434

# Check disk space
df -h

# Restart ollama service
docker-compose restart ollama
# or
kamal accessory restart ollama
```

### Issue: Model not found

**Error:** `Model 'all-minilm:l6-v2' not found`

**Check:**

```bash
# List available models
docker exec ollama ollama list
```

**Solution:**

```bash
# Pull model
docker exec ollama ollama pull all-minilm:l6-v2

# Or for production
ssh player@<HOST_IP>
docker exec linklift-ollama ollama pull all-minilm:l6-v2
```

### Issue: Dimension mismatch

**Error:** `Vector dimension 768 does not match index dimension 384`

**Cause:** Wrong model or misconfigured dimensions

**Solution:**

1. Verify model: Should be `all-minilm:l6-v2` (384 dimensions)
2. Check environment: `LINKLIFT_OLLAMA_MODEL=all-minilm:l6-v2`
3. Check dimensions: `LINKLIFT_OLLAMA_DIMENSIONS=384`

### Issue: Application can't connect to Ollama

**Check:**

```bash
# Test from application container
docker exec linklift curl http://ollama:11434/
```

**Solution:**

- Verify network connectivity between containers
- Check Ollama health status: `docker-compose ps ollama`
- Review LINKLIFT_OLLAMA_URL environment variable

---

## Part 7: Resource Requirements

### Docker Compose (Local)

| Service          | CPU      | Memory | Disk                   |
| ---------------- | -------- | ------ | ---------------------- |
| ollama           | 0.5 core | 512MB  | ~500MB (image + model) |
| Total additional | 0.5 core | 512MB  | ~500MB                 |

### Kamal (Production)

| Accessory        | CPU    | Memory | Disk                   |
| ---------------- | ------ | ------ | ---------------------- |
| ollama           | 1 core | 1GB    | ~500MB (image + model) |
| Total additional | 1 core | 1GB    | ~500MB                 |

**Storage breakdown:**

- Ollama image: ~400MB
- all-minilm:l6-v2 model: ~23MB
- Runtime overhead: ~100MB

---

## Part 8: Security Considerations

### Network Security

**Docker Compose:**

- Ollama not exposed to host (internal network only)
- Access through linklift service only

**Kamal:**

- Ollama exposed on port 11434 (consider firewall rules)
- Recommendation: Restrict access to application server only

**Firewall rules (production):**

```bash
# Allow Ollama only from application server
iptables -A INPUT -p tcp --dport 11434 -s <APP_SERVER_IP> -j ACCEPT
iptables -A INPUT -p tcp --dport 11434 -j DROP
```

### Model Security

**Concerns:**

- Model files stored unencrypted on disk
- API has no authentication

**Mitigations:**

- Network isolation (no public access)
- Regular security updates
- Monitor API access logs

---

## Part 9: Monitoring & Observability

### Health Checks

**Docker Compose:**

```yaml
healthcheck:
  test: "curl -f http://localhost:11434/ || exit 1"
  interval: 10s
  timeout: 5s
  retries: 5
```

**Kamal:**

```yaml
# Add to accessory config
healthcheck:
  interval: 10s
  timeout: 5s
  retries: 5
```

### Metrics Collection

**Ollama Metrics (future enhancement):**

- Request count
- Latency
- Model loading time
- Error rates

**Integration points:**

- Prometheus metrics endpoint
- Application logs
- Javalin metrics

---

## Part 10: Rollback Plan

### If deployment fails:

**Docker Compose:**

```bash
# Revert to previous configuration
git checkout HEAD^ docker-compose.yml
docker-compose up -d
```

**Kamal:**

```bash
# Remove ollama accessory
kamal accessory remove ollama

# Revert environment variables
git checkout HEAD^ config/deploy.yml
kamal env push
kamal deploy
```

**Code rollback:**

```bash
# Revert SecureConfiguration.java
git checkout HEAD^ src/main/java/it/robfrank/linklift/config/SecureConfiguration.java
mvn clean package
# Redeploy
```

---

## Summary

### Changes Required

**Files to modify:**

1. `docker-compose.yml` - Add ollama and ollama-init services
2. `config/deploy.yml` - Add ollama accessory
3. `SecureConfiguration.java` - Change default model
4. `README.md` - Document Ollama service
5. `.env.example` (optional) - Document environment variables

**Infrastructure changes:**

1. Pull ~400MB Ollama image
2. Download ~23MB all-minilm:l6-v2 model
3. Allocate 512MB-1GB memory for Ollama
4. Configure network connectivity

**Testing checklist:**

- [ ] Docker Compose services start successfully
- [ ] Ollama health check passes
- [ ] Model initialization completes
- [ ] Application connects to Ollama
- [ ] Embedding generation works
- [ ] E2E tests pass
- [ ] Kamal accessory deploys successfully
- [ ] Production environment variables set
- [ ] Production Ollama accessible
- [ ] Vector search functionality works

### Timeline Estimate

- **Phase 1 (Local):** 30-60 minutes
- **Phase 2 (Production):** 30-45 minutes
- **Phase 3 (Code updates):** 15-30 minutes
- **Testing & Verification:** 30-60 minutes
- **Total:** 2-3 hours

### Success Criteria

✅ Ollama container running in both environments
✅ Model preloaded and accessible
✅ Application successfully connects to Ollama
✅ Vector search functionality operational
✅ E2E tests pass
✅ Production deployment successful
✅ No dimension mismatch errors
✅ Health checks passing

---

**Next Steps:** Review this plan, then proceed with implementation in phases.
