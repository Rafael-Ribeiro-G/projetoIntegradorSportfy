# ---------- Etapa 1: build (compila e gera o .jar) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# baixa dependências primeiro (fica em cache enquanto o pom.xml não mudar)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# os testes rodam no CI; para rodar aqui também, remova -DskipTests
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagem final, só com o Java runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV TZ=America/Sao_Paulo \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom" \
    UPLOADS_PASTA=/app/uploads

# roda como usuário sem privilégios (boa prática de segurança)
RUN addgroup -S sportfy && adduser -S sportfy -G sportfy \
    && apk add --no-cache wget tzdata \
    && mkdir -p /app/uploads && chown sportfy:sportfy /app/uploads
USER sportfy

COPY --from=build /app/target/sportfy-backend.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
