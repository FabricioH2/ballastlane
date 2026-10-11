# ---------- Stage 1: build the jar (needs the full JDK and Gradle) ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# First only the Gradle files: Docker reuses this layer (and the downloaded dependencies)
# as long as these files do not change, so editing Java code does not download everything again.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
# gradlew may have Windows line endings (CRLF) after a git checkout on Windows: Linux cannot run it like that.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew dependencies --no-daemon > /dev/null

COPY src src
# Tests are not run here: they use Testcontainers, which needs Docker. Run them with ./gradlew test.
RUN ./gradlew bootJar --no-daemon

# ---------- Stage 2: the image that runs (only the JRE and the jar) ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Do not run as root inside the container.
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring

COPY --from=build /app/build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
