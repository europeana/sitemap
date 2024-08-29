# Sitemap is not a standard web application but a command-line runner so we deploy and start it as jar
# When started without command-line arguments we'll start the embedded Tomcat.
FROM eclipse-temurin:17-jre-alpine
LABEL org.opencontainers.image.vendor="Europeana Foundation" \
      org.opencontainers.image.authors="api@europeana.eu" \
      org.opencontainers.image.documentation="https://pro.europeana.eu/page/apis" \
      org.opencontainers.image.source="https://github.com/europeana/" \
      org.opencontainers.image.licenses="EUPL-1.2"

# Required by snappy (newer versions)
RUN apk update && apk add --no-cache libc6-compat

# Configure APM and add APM agent
ENV ELASTIC_APM_VERSION 1.48.1
ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$ELASTIC_APM_VERSION/elastic-apm-agent-$ELASTIC_APM_VERSION.jar /opt/app/elastic-apm-agent.jar

COPY target/sitemap.jar /opt/app/sitemap.jar
ENTRYPOINT ["java","-jar","/opt/app/sitemap.jar"]
