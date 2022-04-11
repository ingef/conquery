FROM maven:3.8-openjdk-11-slim AS builder

COPY . /app


WORKDIR /app
RUN ./scripts/build_version.sh


FROM openjdk:11.0.12-slim AS runner

WORKDIR /app
COPY --from=builder /app/executable/target/executable*jar ./conquery.jar

ENV CLUSTER_PORT=${CLUSTER_PORT:-8082}
ENV ADMIN_PORT=${ADMIN_PORT:-8081}
ENV API_PORT=${API_PORT:-8080}

ENTRYPOINT [ "java", "-jar", "conquery.jar" ]

CMD [ "standalone" ]

EXPOSE $CLUSTER_PORT $ADMIN_PORT $API_PORT

