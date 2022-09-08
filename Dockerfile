ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

# Application image

FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY lib/applicationinsights-agent-2.5.1.jar /opt/app/
COPY build/libs/em-hrs-ingestor.jar /opt/app/

CMD [ "em-hrs-ingestor.jar" ]
EXPOSE 8090

