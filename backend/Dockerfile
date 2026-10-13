# Imagen del backend para producción (Render). Se construye desde la carpeta backend/:
#   docker build -t laciturnos-backend backend
#
# 1) Compila con el JDK (sin pruebas: ya corren en CI) y separa las capas del jar de Spring Boot.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests -Dspotless.check.skip=true \
    && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination target/extracted

# 2) Corre con el JRE, como usuario sin privilegios. Las capas que cambian poco (dependencias) van primero,
#    así un cambio de código no vuelve a subir todo.
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /src/target/extracted/dependencies/ ./
COPY --from=build /src/target/extracted/spring-boot-loader/ ./
COPY --from=build /src/target/extracted/snapshot-dependencies/ ./
COPY --from=build /src/target/extracted/application/ ./
USER app
EXPOSE 8080
# UTC para que nada dependa de la zona del servidor; memoria acotada para instancias chicas (512 MB).
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=UTC -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
