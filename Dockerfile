# Build stage: JDK and Maven wrapper. Dependencies are resolved in their own layer, so a
# code change does not download them again.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
# Tests need Docker (Testcontainers): they run in CI, not inside the image build
RUN ./mvnw -q package -DskipTests

# Runtime stage: only a JRE and the application jar, run by an unprivileged user
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home --shell /usr/sbin/nologin app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
# Heap sized from the container memory limit, not from the host memory
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
