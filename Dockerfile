# Builder
FROM maven:3.8-openjdk-17-slim AS builder

COPY . /app


WORKDIR /app
RUN ./scripts/build_backend_version.sh


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

ENTRYPOINT [ "java", "-jar", "conquery.jar" ]

CMD [ "standalone" ]

EXPOSE $CLUSTER_PORT $ADMIN_PORT $API_PORT

