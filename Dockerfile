FROM eclipse-temurin:26-jre-alpine

ARG VERSION

WORKDIR /app

COPY target/SingiAttend-Server-$VERSION.jar app.jar

EXPOSE 62811

ENTRYPOINT ["java","-jar","/app/app.jar"]