FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY apps/backend/pom.xml apps/backend/pom.xml
COPY apps/backend/common-library/pom.xml apps/backend/common-library/pom.xml
COPY apps/backend/user-service/pom.xml apps/backend/user-service/pom.xml
COPY apps/backend/common-library apps/backend/common-library
COPY apps/backend/user-service apps/backend/user-service
RUN mvn -pl apps/backend/user-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S tripgenie && adduser -S tripgenie -G tripgenie
COPY --from=build /workspace/apps/backend/user-service/target/user-service-*.jar app.jar
USER tripgenie
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
