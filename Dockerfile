FROM mirror.gcr.io/library/eclipse-temurin:21-jre-alpine

ARG JAR_FILE=census-rm-fieldworkadapter*.jar
CMD ["/opt/java/openjdk/bin/java", "-jar", "/opt/census-rm-fieldworkadapter.jar"]
COPY healthcheck.sh /opt/healthcheck.sh

# Create a system group and user without forcing UID/GID
RUN addgroup --system fieldworkadapter && \
    adduser --system --ingroup fieldworkadapter fieldworkadapter

USER fieldworkadapter

COPY target/$JAR_FILE /opt/census-rm-fieldworkadapter.jar


