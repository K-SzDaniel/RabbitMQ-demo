FROM maven:3.9.9-eclipse-temurin-24-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean
RUN mvn package -DskipTests

FROM eclipse-temurin:24-jdk
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]