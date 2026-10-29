# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia solo el POM y descarga dependencias para cachear esta capa
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Ahora el código y el empaquetado (los tests ya corren en CI)
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=build /app/target/taskflow-backend-*.jar app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
