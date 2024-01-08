# Pull base image
From tomcat:8-jre8

# Delete existing ROOT folder 
RUN rm -rf /usr/local/tomcat/webapps/ROOT

MAINTAINER "Pranay <pranay.shah@harness.io>"

#test
# Copy to images tomcat path
COPY target/todolist.war /usr/local/tomcat/webapps/ROOT.war
