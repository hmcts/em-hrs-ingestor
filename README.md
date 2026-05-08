=======
# Hearing Recording Service - Ingestor

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Hearing Recording Service (HRS) Ingestor is a backend service responsible for retrieving and ingesting files from a source Azure storage bucket (CVP Blobstore).

Its primary functions include:
* Listing folders in the source Azure storage bucket (CVP Blobstore).
* Asking the HRS-API which files it already has or is currently ingesting.
* Parsing filenames of files to be ingested to create metadata.
* Sending metadata and filename sources to the HRS-API to ingest.

## Prerequisites

Before setting up the project, ensure you have the following installed:

- **Java** - Required for building and running the application
- **Docker & Docker Compose** - Required for running local dependencies and containers
- **Azure CLI** - Required for authenticating and pulling images from ACR
- **Make** - Required for executing project lifecycle commands

## Quickstart

#### To clone repo and prepare the environment:
```bash
git clone https://github.com/hmcts/em-hrs-ingestor.git
cd em-hrs-ingestor
```

#### Clean and build the application:
```bash
./gradlew clean
./gradlew build
```

Create the application image (optional):
```bash
./gradlew assemble
docker-compose build
```

## Running the Application Locally

**Important**: You must have `em-hrs-api` running (along with its dependencies) before running this application.

### 1. Start the HRS-API
Follow the `em-hrs-api` README to get it running.

### 2. Start the HRS-Ingestor

First, ensure you are logged into Azure to pull the necessary ACR container images:
```bash
az login
az acr login --name hmctsprod
```

Next, fire up the ingestor dependencies. This will start the required services and prime the CVP blob store with test files:
```bash
./docker/dependencies/start-local-environment.sh
```

Finally, run the application. Running the app will immediately invoke the ingest method, attempting to send the primed file to the HRS-API:
```bash
make app-run
```
*(Alternatively, you can run the application in docker using `docker-compose up`)*

### Verifying the Application

To test if the application is up, you can call its health endpoint (port `8090`):
```bash
curl http://localhost:8090/health
```
You should get a response similar to this:
```json
{"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

**Expected Log Output:**
After running `make app-run`, you should see ingestion logs similar to this:
```text
2021-09-30 15:40:11.253  INFO 23274 --- [main] u.g.h.r.e.h.i.s.DefaultIngestorService   : Ingestion Complete
2021-09-30 15:40:11.254  INFO 23274 --- [main] u.g.h.r.e.h.i.s.DefaultIngestorService   : Total files Attempted: 0
2021-09-30 15:40:11.254  INFO 23274 --- [main] u.g.h.r.e.h.i.s.DefaultIngestorService   : Total files Parsed Ok: 0
2021-09-30 15:40:11.254  INFO 23274 --- [main] u.g.h.r.e.h.i.s.DefaultIngestorService   : Total files Ignored Ok: 0
2021-09-30 15:40:11.254  INFO 23274 --- [main] u.g.h.r.e.h.i.s.DefaultIngestorService   : Total files Submitted Ok: 0
2021-09-30 15:40:11.254  INFO 23274 --- [main] u.g.h.r.e.h.i.s.DefaultIngestorService   : VALIDATION REPORT: CVP Files:27, HRS Files:27, To Ingest:0, INGESTION-STATUS:COMPLETE
2021-09-30 15:40:11.254  INFO 23274 --- [main] h.i.l.IngestWhenApplicationReadyListener : Initial Ingestion Complete
```

## Testing

*Note: This project does not contain functional tests.*

### Unit Tests
Run standard unit tests:
```bash
./gradlew test
```

### Integration Tests
Run integration tests:
```bash
./gradlew integration
```

### Run All Checks
To run all major checks (tests, checkstyle, etc.) and open the Jacoco test report in your browser:
```bash
make check-all
```

## Local Development & IDE Setup

### IntelliJ IDEA Setup
To avoid conflicts with Checkstyle validation:
1. **Import limits:** Increase `import *` to `200` (Settings -> Editor -> Code Style -> Java -> Imports). [Reference](https://intellij-support.jetbrains.com/hc/en-us/community/posts/206203659-Turn-off-Wildcard-imports-)
2. **Auto-import:** Enable auto-import of non-ambiguous imports.
3. **Checkstyle Scheme:** Import the checkstyle code scheme into the Java code settings.
4. **Import Layout:** Reverse the import layout settings/modify them until Checkstyle passes.
5. **Comments:** Uncheck "Comment at first column".

### Sonarqube (Optional)

If you wish to use local Sonarqube for code quality analysis:

#### First Time Build:
1. Fetch the latest image, run it, and prepare the admin account:
   ```bash
   make sonarqube-fetch-and-run-sonarqube-latest-with-password-as-admin
   make report-sonarqube
   ```
2. Open your browser to `http://localhost:9000/account/security/` and log in as `admin` (password: `admin`).
3. Change the password to a password of your choice.
4. Generate a User Token within the SonarQube UI.

#### Subsequent Builds:
To run the Sonarqube server and execute tests using your token, pass the token as an argument to the make command:
```bash
make sonarqube-run-local-sonarqube-server
make sonarqube-run-tests-with-token SONAR_TOKEN=your_generated_token_here
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
