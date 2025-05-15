FROM amazoncorretto:17
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean shadowJar
EXPOSE 8080
CMD ["java", "-jar", "build/libs/spotted-api-1.0-SNAPSHOT-all.jar"]