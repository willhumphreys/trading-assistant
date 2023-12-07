FROM amazoncorretto:21

RUN yum update -y
LABEL maintainer="whumphreys@gmail.com"

#    COPY mochi-graphs mochi-graphs
COPY ./accounts accounts

COPY target/darwinex-client-0.0.1-SNAPSHOT.jar darwinex-client-0.0.1-SNAPSHOT.jar

EXPOSE 8080
EXPOSE 3306

ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "darwinex-client-0.0.1-SNAPSHOT.jar"]
