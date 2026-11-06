# Build stage: Maven wrapper inside a JDK 17 image.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -DskipTests package

# Run stage: JRE only, non-root user.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home gymtracker
COPY --from=build /app/target/gymtracker-0.0.1-SNAPSHOT.jar app.jar
USER gymtracker
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
