# Pull base image
From tomcat:8-jre8

# Delete existing ROOT folder 
RUN rm -rf /usr/local/tomcat/webapps/ROOT

MAINTAINER "Pranay <pranay.shah@harness.io>"


# Copy to images tomcat path
COPY /harness/todolist-demo/target/todolist.war /usr/local/tomcat/webapps/ROOT.war
