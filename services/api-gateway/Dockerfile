# Build the jar first from the repository root: mvn -B -pl services/api-gateway -am package
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 --no-create-home gateway
WORKDIR /app
COPY target/api-gateway-*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
