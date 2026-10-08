FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml ./
COPY adi-common/pom.xml adi-common/
COPY adi-chat/pom.xml adi-chat/
COPY adi-admin/pom.xml adi-admin/
COPY adi-bootstrap/pom.xml adi-bootstrap/
COPY local-repo/ local-repo/

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn package -DskipTests -B

COPY . .

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn package -DskipTests -B

FROM eclipse-temurin:17-jre

RUN sed -i 's|http://archive.ubuntu.com/ubuntu/|http://mirrors.aliyun.com/ubuntu/|g; s|http://security.ubuntu.com/ubuntu/|http://mirrors.aliyun.com/ubuntu/|g' /etc/apt/sources.list.d/ubuntu.sources \
    && apt-get update && apt-get install -y --no-install-recommends locales curl xz-utils ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Node.js + npm：供 npx 型 stdio MCP 使用（二进制走 npmmirror 国内镜像）
RUN curl -fsSL https://npmmirror.com/mirrors/node/v20.18.0/node-v20.18.0-linux-x64.tar.xz \
    | tar -xJ -C /usr/local --strip-components=1 \
    && npm config set registry https://registry.npmmirror.com \
    && node -v && npm -v

# uv + uvx：供 uvx 型 stdio MCP 使用；PyPI 走清华镜像
# 若构建环境无法访问 astral.sh / GitHub，可改用 ghfast.top 等代理下载 uv 二进制
ENV UV_INSTALL_DIR=/usr/local/bin \
    UV_DEFAULT_INDEX=https://pypi.tuna.tsinghua.edu.cn/simple
RUN curl -LsSf https://astral.sh/uv/install.sh | sh && uv --version

ENV LANG="zh_CN.UTF-8" \
    LANGUAGE="zh_CN:zh" \
    LC_ALL="zh_CN.UTF-8" \
    APP_VERSION="1.0.0-SNAPSHOT" \
    TZ="Asia/Shanghai" \
    APP_PROFILE="dev" \
    JAVA_OPTS="-Xms512m -Xmx1536m -XX:+HeapDumpOnOutOfMemoryError"

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=build /app/adi-bootstrap/target/adi-bootstrap-0.0.1-SNAPSHOT.jar /data/app/aideepin-api.jar

ENTRYPOINT ["sh","-c", "java $JAVA_OPTS -jar /data/app/aideepin-api.jar --spring.profiles.active=$APP_PROFILE"]

EXPOSE 9999
