FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

RUN apk add --no-cache bash

COPY target/grpc-user-client-2026.1Q-bin.tar.gz .

RUN tar -xzf grpc-user-client-2026.1Q-bin.tar.gz \
    && rm grpc-user-client-2026.1Q-bin.tar.gz \
    && chmod +x grpc-user-client-2026.1Q/run-client.sh

WORKDIR /app/grpc-user-client-2026.1Q

ENTRYPOINT ["bash", "run-client.sh"]