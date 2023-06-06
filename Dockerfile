FROM azul/zulu-openjdk:17.0.3

ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && apt-get install -y apt-utils && apt-get update && apt-get install -y lnav iputils-ping

ADD target/Dispatch-1.0-SNAPSHOT-spring-boot.jar Dispatch-1.0-SNAPSHOT-spring-boot.jar

ENTRYPOINT ["java", "-server", "-Xms16M", "-Xmx256M", "-jar", "Dispatch-1.0-SNAPSHOT-spring-boot.jar"]
