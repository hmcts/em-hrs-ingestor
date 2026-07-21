ARG APP_INSIGHTS_AGENT_VERSION=3.7.9

# Application image

FROM hmctsprod.azurecr.io/base/java:25-distroless

USER hmcts
COPY lib/applicationinsights.json /opt/app/
COPY build/libs/em-hrs-ingestor.jar /opt/app/

CMD [ "em-hrs-ingestor.jar" ]
EXPOSE 8090

