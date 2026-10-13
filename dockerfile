FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY demo/pom.xml .
COPY demo/src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

ENV APP_PORT=3001
ENV APP_ENV=production
ENV CSV_PATH=/app/data/compras.csv

COPY --from=build /app/target/*.jar app.jar

USER appuser

EXPOSE ${APP_PORT}

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${APP_PORT}"]