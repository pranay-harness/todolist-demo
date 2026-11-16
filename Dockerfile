# Build stage: compile the WAR inside the image so the build never depends on
# a target/ folder produced by an earlier pipeline step.
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /app

# Resolve dependencies first so this layer is cached when only source changes
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# Runtime stage
From tomcat:8-jre8 as prod

# Delete existing ROOT folder
RUN rm -rf /usr/local/tomcat/webapps/ROOT

MAINTAINER "Pranay <pranay.shah@harness.io>"


# Copy to images tomcat path
COPY --from=build /app/target/todolist.war /usr/local/tomcat/webapps/ROOT.war
