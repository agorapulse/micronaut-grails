# we are extending everything from tomcat:8.0 image ...
FROM tomcat:8.5
MAINTAINER Vladimir Orany
# COPY path-to-your-application-war path-to-webapps-in-docker-tomcat

RUN rm -rf /usr/local/tomcat/webapps/*
ADD ./build/libs/micronaut-grails-example-3.0.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080/tcp
EXPOSE 8000/tcp

ENV CATALINA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000"
