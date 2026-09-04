# Multi-stage Dockerfile for Spring Boot WhatsApp Nutrition Bot
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Install fontconfig & dejavu fonts for Graphics2D dynamic meal card generation
RUN apk add --no-cache fontconfig ttf-dejavu

COPY --from=build /workspace/target/*.jar app.jar

ENV PORT=8085
EXPOSE 8085

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
