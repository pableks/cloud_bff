# Usamos una imagen base de OpenJDK
FROM openjdk:17-jdk-slim

# Configuramos el directorio de trabajo
WORKDIR /app

# Copiamos el JAR generado al contenedor
COPY target/*.jar app.jar

# Exponemos el puerto en el que corre Spring Boot
EXPOSE 8080

# Comando para ejecutar la aplicación con CORS enabled
ENTRYPOINT ["java", "-jar", "app.jar"]