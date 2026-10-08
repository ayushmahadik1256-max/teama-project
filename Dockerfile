# Stage 1: Build the application with Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run with lightweight JRE
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8081}"]