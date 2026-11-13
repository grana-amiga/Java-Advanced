FROM eclipse-temurin:25-jdk AS build

WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /app/target/grana-amiga-api-1.0.0.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "app.jar"]
