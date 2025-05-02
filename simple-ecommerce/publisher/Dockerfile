# Etapa 1: Build com Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copia o pom.xml primeiro para aproveitar o cache de dependências do Docker
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código-fonte
COPY src ./src/

# Executa o build e empacota o JAR
RUN mvn clean package -DskipTests

# Etapa 2: Runtime com Java (sem Maven)
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Criar diretório para arquivos de entrada
RUN mkdir -p /app/data

# Copia o JAR gerado da etapa anterior
COPY --from=builder /app/target/publisher-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

# Copiar o arquivo JSON para o container
COPY ExampleNotifications.json /app/data/ExampleNotifications.json

# Verificação de conteúdo (opcional)
RUN echo "Verificando existência do arquivo JSON" && \
    ls -la /app/data/

# Executa o Producer (modo interativo)
ENTRYPOINT ["java", "-jar", "app.jar"]