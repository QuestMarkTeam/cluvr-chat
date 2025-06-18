FROM gradle:8.4.0-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/cluvr-chat-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]


#FROM openjdk:17
#ARG JAR_FILE=build/libs/*.jar
#COPY ${JAR_FILE} app.jar
#ENTRYPOINT ["java", "-jar", "/app.jar"]