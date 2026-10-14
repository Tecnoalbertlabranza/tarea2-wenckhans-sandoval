FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY demo/pom.xml .
COPY demo/src ./src

RUN mvn clean package -DskipTests

RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,jdk.httpserver,jdk.unsupported \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=zip-6 \
    --output /custom-jre

FROM alpine:3.19

WORKDIR /app

ENV JAVA_HOME=/custom-jre \
    PATH="/custom-jre/bin:$PATH" \
    APP_PORT=3001 \
    APP_ENV=production \
    CSV_PATH=/app/data/compras.csv

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /custom-jre /custom-jre
COPY --from=build /app/target/*.jar app.jar

USER appuser

EXPOSE ${APP_PORT}

ENTRYPOINT ["sh", "-c", "exec java -jar app.jar --server.port=${APP_PORT}"]
