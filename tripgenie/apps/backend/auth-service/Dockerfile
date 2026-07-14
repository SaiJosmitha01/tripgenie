FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY apps/backend/pom.xml apps/backend/pom.xml
COPY apps/backend/common-library/pom.xml apps/backend/common-library/pom.xml
COPY apps/backend/api-gateway/pom.xml apps/backend/api-gateway/pom.xml
COPY apps/backend/auth-service/pom.xml apps/backend/auth-service/pom.xml
COPY apps/backend/user-service/pom.xml apps/backend/user-service/pom.xml
COPY apps/backend/trip-service/pom.xml apps/backend/trip-service/pom.xml
COPY apps/backend/ai-planner-service/pom.xml apps/backend/ai-planner-service/pom.xml
COPY apps/backend/notification-service/pom.xml apps/backend/notification-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl apps/backend/auth-service -am -DskipTests dependency:go-offline
COPY apps/backend/common-library apps/backend/common-library
COPY apps/backend/auth-service apps/backend/auth-service
RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl apps/backend/auth-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S tripgenie && adduser -S tripgenie -G tripgenie
COPY --from=build /workspace/apps/backend/auth-service/target/auth-service-*.jar app.jar
USER tripgenie
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
