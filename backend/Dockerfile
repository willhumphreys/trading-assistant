FROM amazoncorretto:21

RUN yum update -y && yum clean all
LABEL maintainer="whumphreys@gmail.com"

COPY target/darwinex-client-0.0.1-SNAPSHOT.jar darwinex-client-0.0.1-SNAPSHOT.jar

# Define the JMX monitoring parameters
ENV JMX_PORT=9010 \
    JMX_RMI_PORT=9011 \
    JAVA_RMI_SERVER_HOSTNAME=192.168.1.237

EXPOSE 8080 3306 9010 9011

ENTRYPOINT java \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=${JMX_PORT} \
    -Dcom.sun.management.jmxremote.rmi.port=${JMX_RMI_PORT} \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Djava.rmi.server.hostname=${JAVA_RMI_SERVER_HOSTNAME} \
    -Dspring.profiles.active=${SPRING_PROFILE} \
    -jar darwinex-client-0.0.1-SNAPSHOT.jar

