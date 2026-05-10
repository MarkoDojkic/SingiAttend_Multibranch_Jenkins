FROM eclipse-temurin:26-jre-alpine

ARG VERSION

WORKDIR /app

COPY target/SingiAttend-Student_Proxy-$VERSION.jar app.jar

EXPOSE 62814

ENTRYPOINT ["java","-jar","/app/app.jar"]