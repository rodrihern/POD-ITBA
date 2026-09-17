# Clase 4 - gRPC Greeter

## Spring gRPC _unary_ con _stub_ bloqueante

### Tecnologías utilizadas
- JDK 25
- Maven 3
- Spring gRPC 1.0.2

**Documentación de Spring gRPC:** https://docs.spring.io/spring-grpc/reference/index.html

### Build

```shell
./mvnw clean package
```

### Estructura del proyecto

#### api

- `greeter.proto`: con la definición del servicio Greeter

#### server

- `Servant.java`: heredando de GreeterGrpc.GreeterImplBase
- `Server.java`: Main
- `application.yml`: puerto y nivel de log (Opcional)
- `GreeterTest.java`: Test Unitario

#### client

- `Client.java`: Main
- `application.yml`: host, puerto y nivel de log (Opcional)

### Ejecución Server

Después de haber realizado el build

#### Docker

```shell
docker compose up --build
```

#### Dev

```shell
./mvnw -pl server -am spring-boot:run
```

#### Shell

```shell
cd server/target 
```
```shell
tar -xzf grpc-greeter-server-2026.1Q-bin.tar.gz 
```
```shell
cd grpc-greeter-server-2026.1Q 
```
```shell
chmod u+x run-server.sh 
```
```shell
sh run-server.sh 
```

### Ejecución Client

#### Docker

```shell
docker compose --profile client run --rm client
```

#### Dev

```shell
./mvnw -pl client -am spring-boot:run
```

#### Shell

```shell
cd client/target 
```
```shell
tar -xzf grpc-greeter-client-2026.1Q-bin.tar.gz 
```
```shell
cd grpc-greeter-client-2026.1Q 
```
```shell
chmod u+x run-client.sh 
```
```shell
sh run-client.sh 
```
