FROM amazoncorretto:17
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean build
EXPOSE 8080
CMD ["java", "-jar", "build/libs/API.jar"]