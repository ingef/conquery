# Version Extractor
FROM bitnami/git:2.38.1 AS version-extractor

WORKDIR /app
COPY .git .

RUN git describe --tags |  sed 's/^v//' > git_describe.txt

# Builder
FROM maven:3.8-openjdk-17-slim AS builder

WORKDIR /app

# Fetch dependencies first
COPY ./pom.xml .
COPY ./backend/pom.xml ./backend/
COPY ./executable/pom.xml ./executable/
COPY ./autodoc/pom.xml ./autodoc/

RUN mvn dependency:go-offline -Dsilent=true -DexcludeGroupIds="com.bakdata.conquery" -pl backend -am

# Then copy the rest
COPY . .

# Get the version from previous step
COPY --from=version-extractor /app/git_describe.txt .

# Build
RUN ./scripts/build_backend_version.sh `cat git_describe.txt`


# Runner
FROM eclipse-temurin:17-jre-alpine AS runner

## Apache POI needs some extra libs to auto-size columns
RUN apk add --no-cache fontconfig ttf-dejavu
RUN ln -s /usr/lib/libfontconfig.so.1 /usr/lib/libfontconfig.so && \
    ln -s /lib/libuuid.so.1 /usr/lib/libuuid.so.1 && \
    ln -s /lib/libc.musl-x86_64.so.1 /usr/lib/libc.musl-x86_64.so.1
ENV LD_LIBRARY_PATH /usr/lib

WORKDIR /app
COPY --from=builder /app/executable/target/executable*jar ./conquery.jar

ENV CLUSTER_PORT=${CLUSTER_PORT:-8082}
ENV ADMIN_PORT=${ADMIN_PORT:-8081}
ENV API_PORT=${API_PORT:-8080}

RUN mkdir /app/logs
VOLUME /app/logs

ENTRYPOINT [ "java", "-jar", "conquery.jar" ]

CMD [ "standalone" ]

EXPOSE $CLUSTER_PORT $ADMIN_PORT $API_PORT

