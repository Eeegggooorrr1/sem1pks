FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src

FROM build AS test
CMD ["mvn", "-B", "test"]

FROM build AS package
RUN mvn -B package

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S client && adduser -S client -G client && mkdir /exports && chown client:client /exports
WORKDIR /app
COPY --from=package /workspace/target/console.jar console.jar
USER client
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/console.jar"]
