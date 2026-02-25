FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/cuckoo-web-bff-*.jar app.jar

EXPOSE 8091

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
