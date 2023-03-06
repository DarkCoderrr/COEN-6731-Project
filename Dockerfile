FROM maven:3.9.0-amazoncorretto-17 as builder
COPY . .
RUN mvn clean install

FROM amazoncorretto:17
COPY --from=builder target/*.jar ./application.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","application.jar"]