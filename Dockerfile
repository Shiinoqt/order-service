FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY target/*.jar gestioneordini.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","gestioneordini.jar"]
