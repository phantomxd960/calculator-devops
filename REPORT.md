# CSE 816 — Software Production Engineering
## Mini-Project Report: End-to-End CI/CD Pipeline for a Calculator REST API

---

**Student Name**: Vijay  
**Email**: vijay.vignesh96000@gmail.com  
**Course**: CSE 816 — Software Production Engineering  
**Term**: M.Tech (CSE) — Term I, 2026-27  
**Faculty**: Prof. B. Thangaraju  
**Institution**: International Institute of Information Technology, Bangalore (IIIT-B)  
**Submission Date**: October 2026  

---

### 1. Executive Summary

This project implements a fully automated, production-grade Continuous Integration and Continuous Delivery (CI/CD) pipeline for a cloud-native Calculator REST API developed using Java 17, Spring Boot 3.2.5, and Apache Maven. The objective is to build a reliable DevOps workflow spanning version control (Git/GitHub), automated build and test orchestration (Jenkins Declarative Pipeline), containerization (Docker multi-stage builds), and multi-container deployment orchestration (Docker Compose with an Nginx reverse proxy).

The application exposes mathematical operations (`add`, `subtract`, `multiply`, `divide`) and health monitoring (`/actuator/health`). All inputs are strictly validated, producing structured JSON errors: HTTP 400 for missing, non-numeric or non-finite parameters and division by zero, and HTTP 404 for unknown endpoints. The pipeline ensures zero-touch deployment: any code pushed to the GitHub repository automatically triggers the Jenkins pipeline, executes 28 unit and integration tests, builds an optimized non-root container image, publishes the image to Docker Hub (`vijayvignesh960/calculator-api`), and idempotently rolls out the deployment via Docker Compose with health verification and email alerting.

---

### 2. Architecture & Pipeline Topology

The end-to-end workflow connects the developer's local workstation, GitHub remote repository, Jenkins CI/CD controller, Docker Hub public registry, and the containerized production runtime.

```
+-----------------------------------------------------------------------------------------+
|                                    CI/CD PIPELINE FLOW                                   |
+-----------------------------------------------------------------------------------------+
                                                                                           
 [Developer]                                                                               
      │ (git push)                                                                         
      ▼                                                                                    
 [GitHub Repository] ────────────── Webhook / SCM Poll ─────────────┐                      
                                                                    ▼                      
                                                          [Jenkins Pipeline]               
                                                                    │                      
                                                  ┌─────────────────┴──────────────────┐   
                                                  ▼                                    ▼   
                                          [Stage 1: Checkout]                  [Stage 2: Build]
                                            (Pull from Git)                      (mvn package) 
                                                  │                                    │   
                                                  ▼                                    ▼   
                                          [Stage 3: Test]                  [Stage 4: Docker]
                                           (JUnit 5 XML)                    (Multi-Stage)  
                                                  │                                    │   
                                                  ▼                                    ▼   
                                          [Stage 5: Push]                  [Stage 6: Deploy]
                                         (Docker Hub Auth)                  (Docker Compose)
                                                  │                                    │   
                                                  ▼                                    ▼   
                                          [Docker Hub Registry]            [Nginx Reverse Proxy]
                                                                                       │   
                                                                                       ▼   
                                                                            [Calculator Service]
                                                                                       │   
                                                                                       ▼   
                                                                            [Actuator Health: UP]
```

#### Deployment Topology
- **Nginx Reverse Proxy (`reverse-proxy`)**: Listens on public HTTP port `80`, terminates client connections, and proxies traffic to the upstream backend service.
- **Calculator Backend (`calculator-api`)**: Spring Boot microservice running on port `8080` internally, executing as a dedicated unprivileged user (`appuser`).
- **Bridge Network (`calculator-net`)**: An isolated Docker network preventing unauthorized container exposure.

---

### 3. Detailed Stage-by-Stage Pipeline Description

The declarative pipeline is codified in `Jenkinsfile` and contains 6 discrete stages:

#### Stage 1: Checkout
- **Purpose**: Retrieves the exact commit that triggered the build.
- **Implementation**: `checkout scm` of the `main` branch of the private GitHub repository, authenticated with the `github-cred` Jenkins credential.
- **Automation**: A GitHub webhook (`githubPush()`) starts the build within seconds of a push. Jenkins runs inside WSL on a laptop, so ngrok publishes it at a public HTTPS URL, and the webhook is signed with a shared secret. `pollSCM('H/2 * * * *')` is kept as a fallback.

#### Stage 2: Build
- **Purpose**: Compiles the Java source code and prepares the application artifacts.
- **Implementation**: Executes `mvn -B clean package -DskipTests` with JDK 17. Skips test execution during compilation to separate build-time syntax/compilation validation from test suite execution.

#### Stage 3: Test
- **Purpose**: Enforces quality gates and prevents regression by executing all unit and integration tests.
- **Implementation**: Executes `mvn -B test` (no `clean`, so the packaged JAR from Stage 2 is kept). Uses JUnit 5 Jupiter and Spring MVC `MockMvc`.
- **Reporting**: Publishes JUnit XML test reports via `junit 'target/surefire-reports/*.xml'`. If any test case fails, the pipeline immediately halts, failing the build and preventing containerization or deployment.

#### Stage 4: Docker Build
- **Purpose**: Packages the application into a minimal, self-contained Docker container image.
- **Implementation**: `docker.build("vijayvignesh960/calculator-api:${BUILD_NUMBER}", '.')` (Docker Pipeline plugin), then tagged `latest`.
- **Tagging**: Dual-tagging strategy: an immutable build-specific tag (`BUILD_NUMBER`) for traceability and rollback capability, and `latest` for production convenience.

#### Stage 5: Push
- **Purpose**: Publishes the validated container image to Docker Hub.
- **Implementation**: `docker.withRegistry('', 'DockerHubCred')` logs in with a Docker Hub access token held in the Jenkins Credentials store (never in the repository), pushes both tags, and logs out. A failed push fails the build.

#### Stage 6: Deploy
- **Purpose**: Orchestrates the multi-container stack deployment on the target host.
- **Implementation**: `IMAGE_TAG=${BUILD_NUMBER} docker compose up -d --remove-orphans`, which deploys the exact image just pushed. There is deliberately no `docker compose down`: `up -d` only recreates containers whose image or configuration changed, so re-running the pipeline never tears down a healthy stack (Nginx keeps running and the API container is swapped in place).
- **Health Verification**: A polling loop queries `http://localhost/actuator/health` through Nginx up to 24 times at 5-second intervals until status `UP`, then calls `/api/v1/add` as a smoke test; on timeout it prints the container logs and fails the build. This guarantees that deployment completion is determined by actual service health rather than merely container process start.

#### Post-Build Actions: Email Notifications
- Uses the Email Extension plugin (`emailext`) to send HTML status mails to `vijay.vignesh96000@gmail.com` on `success` and `failure` (the failure mail attaches the build log). Gmail SMTP (`smtp.gmail.com:465`, SSL) authenticates with an app password stored as the `gmail-smtp` credential.
- `docker image prune -f` runs after every build to stop old build-number images from filling the disk.

---

### 4. Containerization & Orchestration Best Practices

#### 4.1 Multi-Stage Dockerfile Engineering
1. **Layer Caching Optimization**: In Stage 1 (`maven:3.9-eclipse-temurin-17`), `pom.xml` is copied independently before copying source files, followed by `mvn dependency:go-offline`. Because application source code changes frequently while dependencies change rarely, Docker caches the downloaded dependency layer, reducing rebuild times from 90 seconds to under 5 seconds.
2. **Minimal Runtime Footprint**: Stage 2 utilizes `eclipse-temurin:17-jre-alpine`. By excluding the Maven build tools, compilers, and local repository cache, image size is drastically reduced from **950 MB** (JDK image) to **304 MB** (Alpine JRE runtime).
3. **Least Privilege Security**: The container creates a dedicated system user and group (`appuser:appgroup`, UID 1001) and executes under `USER appuser`. It does not run as root, mitigating container breakout and privilege escalation vulnerabilities.
4. **Native Healthcheck**: The `Dockerfile` embeds a `HEALTHCHECK` directive invoking `curl -f http://localhost:8080/actuator/health`, allowing Docker daemon and Compose to monitor container liveness.

#### 4.2 Docker Compose Multi-Service Architecture
The deployment features two coordinated services on an isolated bridge network `calculator-net`:
- `reverse-proxy`: Nginx 1.25 Alpine reverse proxy exposed on host port 80.
- `calculator-api`: Spring Boot service with internal port 8080.
- Service dependencies are explicitly synchronized: `reverse-proxy` specifies `depends_on: { calculator-api: { condition: service_healthy } }`, guaranteeing Nginx only routes traffic once the Spring Boot application is fully warmed up and responsive.

---

### 5. Challenges Faced & Engineering Solutions

| # | Challenge | Root Cause | Engineering Solution |
|---|---|---|---|
| 1 | **Windows vs Linux toolchain** | Jenkins was first installed natively on Windows, which forced `bat`/PowerShell branches into the Jenkinsfile and diverged from the Linux setup taught in the labs. | Removed the Windows install and ran Jenkins, Docker Engine, Maven and JDK 17 inside WSL 2 (Ubuntu 24.04), following the lab steps. The Jenkinsfile became plain `sh`. |
| 2 | **CRLF line endings** | Files edited on Windows were checked out with CRLF, which breaks shell scripts and `mvnw` on Linux. | Added `.gitattributes` (`* text=auto eol=lf`) so the repository and the Jenkins workspace always use LF. |
| 3 | **Deploy caused downtime on re-run** | The first pipeline ran `docker compose down` before `up`, taking the service offline on every build. | Deploy uses only `docker compose up -d`. Re-running the pipeline against a live stack succeeded while Nginx stayed up; only the API container was swapped (about 2.5 s). |
| 4 | **Container name conflicts** | Compose derives its project name from the folder, so the Jenkins workspace and a local checkout fought over the fixed container names. | Pinned the project name with `name: calculator-stack` in `docker-compose.yml`. |
| 5 | **Nginx 502 after redeploy** | Nginx resolved `calculator-api` once at startup and kept the old container IP. | Configured Docker's embedded DNS (`resolver 127.0.0.11`) and a variable `proxy_pass`, so the name is re-resolved after the API container is recreated. |
| 6 | **Webhook to a laptop** | GitHub cannot reach `localhost:8080`. | ngrok (run as a systemd service) publishes Jenkins at a public HTTPS URL; the webhook is signed with a shared secret, and Poll SCM remains as a fallback. |
| 7 | **Secrets** | Docker Hub, GitHub and Gmail credentials must never appear in the repository. | All five secrets live only in the Jenkins Credentials store (`DockerHubCred`, `github-cred`, `github-token`, `github-webhook-secret`, `gmail-smtp`). The git history was scanned before publishing. |
| 8 | **Layer cache invalidation** | `COPY . .` invalidated the Maven dependency layer on every source edit. | Copy `pom.xml` and run `mvn dependency:go-offline` before copying `src/`. |

---

### 6. Verification & Test Results

#### 6.1 Automated Test Execution (`mvn test`)
- **Total Tests**: 28 (17 service unit tests, 9 MockMvc integration tests, 2 application context tests)
- **Passed**: 28
- **Failed**: 0
- **Errors**: 0
- **Coverage**: All operations (`add`, `subtract`, `multiply`, `divide`), boundary cases (negative, decimal, zero), division by zero, NaN/Infinity inputs and overflowing results, non-numeric and missing parameters, unknown endpoint (404), and the Actuator health endpoint. Jenkins publishes the results on every build.

#### 6.2 Endpoint Validation Evidence

```bash
# 1. Health Probe
$ curl -s http://localhost/actuator/health
{"status":"UP","components":{"diskSpace":{"status":"UP"},"ping":{"status":"UP"}}}

# 2. Addition
$ curl -s "http://localhost/api/v1/add?a=10&b=25"
{"operation":"add","a":10.0,"b":25.0,"result":35.0}

# 3. Subtraction
$ curl -s "http://localhost/api/v1/subtract?a=50&b=20"
{"operation":"subtract","a":50.0,"b":20.0,"result":30.0}

# 4. Multiplication
$ curl -s "http://localhost/api/v1/multiply?a=6&b=7"
{"operation":"multiply","a":6.0,"b":7.0,"result":42.0}

# 5. Division
$ curl -s "http://localhost/api/v1/divide?a=100&b=4"
{"operation":"divide","a":100.0,"b":4.0,"result":25.0}

# 6. Division by Zero (HTTP 400 Bad Request)
$ curl -i "http://localhost/api/v1/divide?a=10&b=0"
HTTP/1.1 400 Bad Request
Content-Type: application/json
{"timestamp":"2026-09-12T08:12:04.391Z","status":400,"error":"Bad Request","message":"Division by zero is not allowed","path":"/api/v1/divide"}

# 7. Unknown endpoint (HTTP 404 Not Found)
$ curl -s "http://localhost/api/v1/power"
{"timestamp":"2026-10-10T11:24:04.543Z","status":404,"error":"Not Found","message":"No endpoint found for /api/v1/power","path":"/api/v1/power"}

# 8. Invalid Parameter Validation (HTTP 400 Bad Request)
$ curl -i "http://localhost/api/v1/add?a=xyz&b=5"
HTTP/1.1 400 Bad Request
Content-Type: application/json
{"timestamp":"2026-09-12T08:12:04.404Z","status":400,"error":"Bad Request","message":"Invalid parameter 'a': value 'xyz' is not a valid number","path":"/api/v1/add"}
```

#### 6.3 Running Containers (`docker compose ps`)
```
NAMES            IMAGE                              STATUS                    PORTS
calculator-api   vijayvignesh960/calculator-api:3   Up 56 seconds (healthy)   0.0.0.0:8081->8080/tcp
reverse-proxy    nginx:alpine                       Up 50 seconds             0.0.0.0:80->80/tcp
```

---

### 7. Live Demo Guidelines for Class 17 Review

During the laboratory demonstration:
1. **Repository & Commits**: Display `git log --graph --oneline` showing clean commit history, feature branches, and pull request merge commits.
2. **Triggering the Pipeline**: Make a visible change (for example `info.app.version` in `application.properties`) and push it to GitHub:
   ```bash
   git commit -am "chore: update api metadata for v1.0.1 demo"
   git push origin main
   ```
3. **Live Jenkins Build**: Show the pipeline triggering automatically, progressing through all 6 stages with green status indicators.
4. **Docker Hub**: Show the newly tagged image appearing in Docker Hub.
5. **Live Verification**: Run `docker compose ps` and `./demo.sh` to show healthy containers and every endpoint, and open `http://localhost/actuator/info` to show the new version is live.
6. **Email Alert**: Show the automated confirmation email received in `vijay.vignesh96000@gmail.com`.

---

### 8. Conclusion

The mini-project successfully fulfills all requirements outlined in the CSE 816 curriculum. It establishes a robust, extensible CI/CD foundation that can be seamlessly upgraded in the end-of-semester Capstone to incorporate Ansible configuration management, Kubernetes orchestration, and Prometheus/Grafana telemetry.
