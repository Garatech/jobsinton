# Stage 1: Build the Spring Boot executable JAR with Java 21 & Maven
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests

# Stage 2: Minimal Java 21 JRE runtime running as a non-root user
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S jobsinton && adduser -S jobsinton -G jobsinton \
    && mkdir -p /app/data && chown -R jobsinton:jobsinton /app

COPY --from=build --chown=jobsinton:jobsinton /workspace/target/*.jar /app/app.jar

USER jobsinton
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=15s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/api/status || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
