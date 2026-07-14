FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY apps/backend/pom.xml apps/backend/pom.xml
COPY apps/backend/common-library/pom.xml apps/backend/common-library/pom.xml
COPY apps/backend/trip-service/pom.xml apps/backend/trip-service/pom.xml
COPY apps/backend/common-library apps/backend/common-library
COPY apps/backend/trip-service apps/backend/trip-service
RUN mvn -pl apps/backend/trip-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S tripgenie && adduser -S tripgenie -G tripgenie
COPY --from=build /workspace/apps/backend/trip-service/target/trip-service-*.jar app.jar
USER tripgenie
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
