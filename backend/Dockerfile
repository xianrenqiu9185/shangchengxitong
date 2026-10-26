# ============================================================
# 邻里鲜生 · 后端镜像（多阶段构建）
# 构建：docker build -t linli-mall-api:local backend
# 说明：基础镜像沿用仓库 Docker Hub 镜像站约定（public.ecr.aws）。
# ============================================================
ARG MAVEN_IMAGE=public.ecr.aws/docker/library/maven:3.9-eclipse-temurin-17
FROM ${MAVEN_IMAGE} AS build
WORKDIR /build

# 先只拷 pom，依赖层可被缓存，改代码时不必重新下载
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

ARG JRE_IMAGE=public.ecr.aws/docker/library/eclipse-temurin:17-jre-alpine
FROM ${JRE_IMAGE}
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar /app/app.jar

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Shanghai"

EXPOSE 8080
USER app

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
