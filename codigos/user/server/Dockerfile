FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

RUN apk add --no-cache bash

COPY target/grpc-user-server-2026.1Q-bin.tar.gz .

RUN tar -xzf grpc-user-server-2026.1Q-bin.tar.gz \
    && rm grpc-user-server-2026.1Q-bin.tar.gz \
    && chmod +x grpc-user-server-2026.1Q/run-server.sh

WORKDIR /app/grpc-user-server-2026.1Q

EXPOSE 50051

ENTRYPOINT ["bash", "run-server.sh"]