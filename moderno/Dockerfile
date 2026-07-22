# ─────────────────────────────────────────────────────────────────────────────
# Dockerfile — pedidos-service
# Build em dois estágios: compila com Maven e gera imagem mínima em JRE.
# ─────────────────────────────────────────────────────────────────────────────

# ── Estágio 1: build ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# Copia o pom.xml primeiro para aproveitar o cache de camadas do Maven
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B 2>/dev/null || true

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B -q && \
    mv target/pedidos-service-*.jar target/app.jar

# ── Estágio 2: runtime ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Usuário não-root para execução segura
RUN addgroup -S pedidos && adduser -S pedidos -G pedidos
USER pedidos

WORKDIR /app

COPY --from=build /build/target/app.jar app.jar

# Porta padrão da aplicação
EXPOSE 8080

# Inicia com GC virtual threads-friendly e limites de heap conservadores
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
