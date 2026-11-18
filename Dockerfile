# Build stage: compile the WAR inside the image so the build never depends on
# a target/ folder produced by an earlier pipeline step.
FROM maven:3.10-eclipse-temurin-8 AS build
WORKDIR /app

# No dependency:go-offline step: it resolves every transitive descriptor,
# including webwork -> tiles-core 0.2-SNAPSHOT, which only exists in old
# plain-HTTP repos that Maven 3.8+ blocks. "package" only needs what it uses.
COPY pom.xml .
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# Runtime stage
From tomcat:8-jre8 as prod

# Delete existing ROOT folder
RUN rm -rf /usr/local/tomcat/webapps/ROOT

MAINTAINER "Pranay <pranay.shah@harness.io>"


# Copy to images tomcat path
COPY --from=build /app/target/todolist.war /usr/local/tomcat/webapps/ROOT.war
