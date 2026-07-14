FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY apps/backend/pom.xml apps/backend/pom.xml
COPY apps/backend/common-library/pom.xml apps/backend/common-library/pom.xml
COPY apps/backend/notification-service/pom.xml apps/backend/notification-service/pom.xml
COPY apps/backend/common-library apps/backend/common-library
COPY apps/backend/notification-service apps/backend/notification-service
RUN mvn -pl apps/backend/notification-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S tripgenie && adduser -S tripgenie -G tripgenie
COPY --from=build /workspace/apps/backend/notification-service/target/notification-service-*.jar app.jar
USER tripgenie
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
