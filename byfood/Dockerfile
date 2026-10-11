# ---------- build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

COPY --from=build --chown=spring:spring /app/target/byfood-0.0.1-SNAPSHOT.jar app.jar

USER spring

# Ajustado para instâncias pequenas (ex.: Render free com 512 MB).
# Pode ser sobrescrito pela variável de ambiente JAVA_TOOL_OPTIONS no provedor.
ENV TZ=America/Sao_Paulo \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError -Duser.timezone=America/Sao_Paulo"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
