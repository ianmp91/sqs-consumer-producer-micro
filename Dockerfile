# Dockerfile (Para B y C)
# Usamos una imagen base ligera compatible con Java.
# Si Java 25 da problemas de compatibilidad en imagen, busca una de "Early Access" o usa Java 21 LTS si es posible.
FROM eclipse-temurin:25-jdk
# O alternativamente: FROM eclipse-temurin:21-jdk-alpine (si puedes bajar a 21)

WORKDIR /app

# Copiamos el JAR compilado (asegúrate de hacer ./gradlew bootJar antes)
COPY build/libs/*micro-0.1.0-SNAPSHOT.jar app.jar

# Copiamos el private key
COPY build/resources/main/private_key_c.pem src/main/resources/private_key_c.pem

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]
