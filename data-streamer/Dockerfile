FROM amazoncorretto:21

RUN yum update -y && yum clean all
LABEL maintainer="whumphreys@gmail.com"

COPY target/data-streamer-0.0.1-SNAPSHOT.jar data-streamer-0.0.1-SNAPSHOT.jar



EXPOSE 8080 9092 9010 9011

HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT java \
    -Dspring.profiles.active=${SPRING_PROFILE} \
    -jar data-streamer-0.0.1-SNAPSHOT.jar

