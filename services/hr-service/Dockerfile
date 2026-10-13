# Build the jar first from the repository root: mvn -B -pl services/hr-service -am package
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 --no-create-home hr
WORKDIR /app
COPY target/hr-service-*.jar app.jar
USER 10001
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
