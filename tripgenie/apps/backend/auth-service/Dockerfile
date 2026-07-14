FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY apps/backend/pom.xml apps/backend/pom.xml
COPY apps/backend/common-library/pom.xml apps/backend/common-library/pom.xml
COPY apps/backend/auth-service/pom.xml apps/backend/auth-service/pom.xml
COPY apps/backend/common-library apps/backend/common-library
COPY apps/backend/auth-service apps/backend/auth-service
RUN mvn -pl apps/backend/auth-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S tripgenie && adduser -S tripgenie -G tripgenie
COPY --from=build /workspace/apps/backend/auth-service/target/auth-service-*.jar app.jar
USER tripgenie
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
