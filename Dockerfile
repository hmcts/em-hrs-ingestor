ARG APP_INSIGHTS_AGENT_VERSION=3.4.18

# Application image

FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/em-hrs-ingestor.jar /opt/app/

CMD [ "em-hrs-ingestor.jar" ]
EXPOSE 8090

