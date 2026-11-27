# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Sửa xuống dòng CRLF nếu repo được checkout trên Windows
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---- runtime ----
# Chỉ JRE (không có compiler, không có Maven) -> image nhỏ, ít công cụ cho kẻ tấn công lợi dụng.
FROM eclipse-temurin:21-jre
# Không chạy bằng root: nếu app bị chiếm quyền, kẻ tấn công cũng chỉ là user thường trong container.
RUN groupadd --system app && useradd --system --gid app --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /src/target/auth-service-*.jar app.jar
USER 10001
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
