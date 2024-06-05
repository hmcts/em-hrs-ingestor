=======
# Hearing Recording Service - Ingestor

# Overview

- Lists folders in source azure storage bucket (CVP Blobstore)
- Asks HRS-API which files it already has / is currently ingesting
- Parses filenames of files to be ingested, to create metadata
- Sends Metadata and filename source to HRS-API to ingest



#Local Dev

##First Time Build (If you wish to use sonarqube)

You'll need to get sonarqube docker image if you do not have it already, and initialise it and change the password to adminnew

to fetch the latest image, run it and open the browser
run:
- make sonarqube-fetch-and-run-sonarqube-latest-with-password-as-admin
- make report-sonarqube

in the browser, log in as admin (password=admin), go to http://localhost:9000/account/security/ and change password to adminnew


##Subsequent Builds (these must all pass before raising a PR)

This will run all the major checks, and open the jacoco test report in your browser:

- make check-all


To show the sonarcube analysis (master branch only?)
sonarqube:
- make sonarqube-run-local-sonarqube-server
- make sonarqube-run-tests-with-password-as-adminnew


#Smoketest:

for first time use you will need to be logged into the Azure Container repo's using these commands:

az login
az acr login --name hmctspublic && az acr login --name hmctsprivate


You need to have HRS-API running (and its dependencies!) before running this application

#HRS-API

Please fully read the HRS-API readme and work through it, and validate you can run a smoke test.

For convenience, the basic steps are listed here for reference when you are familiar with them

Open a new terminal..
go to the root of hrs-api project,
- cd ../em-hrs-api
then get hrs-api dependencies running
- ./docker/dependencies/start-local-environment.sh
then when the dependencies are showing "ccd-data-store-api_1    | 2021-04-11T10:54:38.861 INFO  [main] o.s.d.r.c.DeferredRepositoryInitializationListener Spring Data repositories initialized"
- make app-run
then prime the CCD via the functional tests with
- make test-functional
finally, smoke test it with:
- make app-smoke-test

#HRS-Ingestor
Now then in this terminal, get the ingestor dependencies running

This will fire up depencies AND prime the CVP blbo store with a file
- ./docker/dependencies/start-local-environment.sh

running the appliction will immediately invoke the ingest method, so will attempt to send the file
to hrs api

- make app-run




#Idea Setup

Increase import star to 200 to avoid conflicts with checkstyle
https://intellij-support.jetbrains.com/hc/en-us/community/posts/206203659-Turn-off-Wildcard-imports-

Auto import of non ambiguous imports
https://mkyong.com/intellij/eclipse-ctrl-shift-o-in-intellij-idea/#:~:text=In%20Eclipse%2C%20you%20press%20CTRL,imports%2C%20never%20imports%20any%20package.

Import the checkstyle code scheme into the java code settings

Reverse the import layout settings / modify until the checkstyle passes
Uncheck "Comment at first column"







NOTE THE BELOW IS NOT YET TESTED!!!
NOTE THE BELOW IS NOT YET TESTED!!!
NOTE THE BELOW IS NOT YET TESTED!!!
NOTE THE BELOW IS NOT YET TESTED!!!
NOTE THE BELOW IS NOT YET TESTED!!!
NOTE THE BELOW IS NOT YET TESTED!!!








## Setup

Simply run the following script to start all application dependencies.

```bash
  ./docker/dependencies/start-local-environment.sh
```

## Building and deploying the application

### Building the application

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the applicaiton in docker by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port [8090]

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:8090/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

After running make app-run, you should a response similar to this:

```
2021-09-30 15:40:11.253  INFO 23274 --- [           main] u.g.h.r.e.h.i.s.DefaultIngestorService   : Ingestion Complete
2021-09-30 15:40:11.254  INFO 23274 --- [           main] u.g.h.r.e.h.i.s.DefaultIngestorService   : Total files Attempted: 0
2021-09-30 15:40:11.254  INFO 23274 --- [           main] u.g.h.r.e.h.i.s.DefaultIngestorService   : Total files Parsed Ok: 0
2021-09-30 15:40:11.254  INFO 23274 --- [           main] u.g.h.r.e.h.i.s.DefaultIngestorService   : Total files Ignored Ok: 0
2021-09-30 15:40:11.254  INFO 23274 --- [           main] u.g.h.r.e.h.i.s.DefaultIngestorService   : Total files Submitted Ok: 0
2021-09-30 15:40:11.254  INFO 23274 --- [           main] u.g.h.r.e.h.i.s.DefaultIngestorService   : VALIDATION REPORT: CVP Files:27, HRS Files:27, To Ingest:0, INGESTION-STATUS:COMPLETE
2021-09-30 15:40:11.254  INFO 23274 --- [           main] h.i.l.IngestWhenApplicationReadyListener : Initial Ingestion Complete
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
