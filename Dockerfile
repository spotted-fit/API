FROM amazoncorretto:17
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean distZip
EXPOSE 8080
CMD ["java", "-jar", "build/libs/API-1.0-SNAPSHOT.jar"]