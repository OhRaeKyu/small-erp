FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew dependencies -q > /dev/null
COPY src src
RUN ./gradlew bootJar -x test -q

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
