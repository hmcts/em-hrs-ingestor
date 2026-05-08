#Purpose - common commands and combinations to assist with build and build reports
#Under linux it also enables commandline autocompletion (using TAB key), so most commands can be invoked with just a couple of keystrokes
#
#
# Usage:
# make dependencies-down
# or make d[TAB]d[TAB]
#
# It includes
#  Docker builds
#  Gradle builds
#  Sonar qube
#  Code Coverage Checks (Gradle)
#  Reports
#
#Some reports use "open" to open the html file. This is a native mac command, for linux - add this to your ~/.bashrc
#
#
#case "$OSTYPE" in
#   cygwin*)
#      alias open="cmd /c start"
#      ;;
#   linux*)
#      alias start="xdg-open"
#      alias open="xdg-open"
#      ;;
#   darwin*)
#      alias start="open"
#      ;;
#esac
##

compose-up:
	docker-compose up

compose-down:
	docker-compose down

#fires up dependencies for this project
dependencies-up:
	docker-compose -f docker-compose-dependencies.yml up

#shuts down dependencies for this project - database content will be reset to a vanilla postgres instance
dependencies-down:
	docker-compose -f docker-compose-dependencies.yml down



app-run:
	./gradlew bootRun

app-smoke-test:
	./gradlew smoke -i

app-run-with-ai:
	sudo mkdir -p /opt/app/;sudo chown $$USER /opt/app/;cp lib/* /opt/app/;./gradlew bootRun -i -DJAVA_TOOL_OPTIONS=-javaagent:/opt/app/applicationinsights-agent-2.5.1.jar -DENABLE_SHUTDOWN=false




test-functional:
	./gradlew functional -i

test-integration:
	./gradlew integration -i

test-code:
	./gradlew test -i





check-code:
	./gradlew check -i

check-dependencies:
	./gradlew dependencyCheckAggregate -i

check-coverage:
	./gradlew test integration  jacocoTestCoverageVerification jacocoTestReport && open build/reports/jacoco/test/html/index.html

check-all:
	./gradlew test integration check jacocoTestCoverageVerification jacocoTestReport && open	build/reports/jacoco/test/html/index.html


#convenience first time download and run of sonarqube with default username/password of admin/admin
sonarqube-fetch-and-run-sonarqube-latest-with-password-as-admin:
	docker run -d --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 sonarqube:latest

sonarqube-run-tests-with-token:
	./gradlew sonarqube -Dsonar.token="$(SONAR_TOKEN)" -Dsonar.host.url="http://localhost:9000/" -i && open http://localhost:9000/



#Note this fails if there is already a container running.
sonarqube-run-local-sonarqube-server:
	docker start sonarqube


#convenenience links for all generated reports

report-sonarqube:
	open http://localhost:9000/

report-checkstyle:
	open build/reports/checkstyle/main.html

report-code-tests:
	open build/reports/tests/test/index.html

report-integration-tests:
	open build/reports/tests/integration/index.html

report-smoke-tests:
	open build/reports/tests/smoke/index.html

report-code-pmd-main:
	open build/reports/pmd/main.html

report-code-pmd-test:
	open build/reports/pmd/test.html

report-code-pmd-integration-test:
	open build/reports/pmd/integrationTest.html

report-code-pmd-smoke-test:
	open build/reports/pmd/smokeTest.html

report-dependency-check:
	open build/reports/dependency-check-report.html

report-jacoco:
	open build/reports/jacoco/test/html/index.html
