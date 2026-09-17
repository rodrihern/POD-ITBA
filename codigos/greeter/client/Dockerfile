FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

RUN apk add --no-cache bash

COPY target/grpc-greeter-client-2026.1Q-bin.tar.gz .

RUN tar -xzf grpc-greeter-client-2026.1Q-bin.tar.gz \
    && rm grpc-greeter-client-2026.1Q-bin.tar.gz \
    && chmod +x grpc-greeter-client-2026.1Q/run-client.sh

WORKDIR /app/grpc-greeter-client-2026.1Q

ENTRYPOINT ["bash", "run-client.sh"]