# modernizado/

Esta pasta foi reservada para artefatos de saída do processo de modernização —
por exemplo, JARs ou imagens Docker geradas por scripts externos.

No estado atual do projeto, a pasta `moderno/` contém o código-fonte completo
da API Spring Boot 3 já modernizada (pronta para build e execução).

Para gerar o artefato final:

```bash
cd moderno
docker compose up --build
```

Ou para build local:

```bash
cd moderno
mvn package -DskipTests
# O JAR fica em moderno/target/pedidos-service-1.0.0.jar
```
