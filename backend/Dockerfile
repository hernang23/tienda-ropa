# ---------- Etapa de compilacion ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Se omiten las pruebas en la imagen; ejecutarlas en CI o localmente con `mvn test`
RUN mvn -q -B clean package -DskipTests

# ---------- Etapa de ejecucion ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r tienda && useradd -r -g tienda tienda \
    && mkdir -p /app/logs && chown -R tienda:tienda /app
COPY --from=build /app/target/*.jar /app/app.jar
USER tienda
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
