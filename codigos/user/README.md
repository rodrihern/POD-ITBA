# Clase 5 - gRPC User

## Spring gRPC _unary_ con _future stub_, _async stub_ y Health Service

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

- `user.proto`: con la definición del servicio UserService

#### server

- `Servant.java`: heredando de UserServiceGrpc.UserServiceImplBase
- `Server.java`: Main
- `HealthConfig.java`: Para inicializar el Health Service que viene por defecto
- `application.yml`: puerto y nivel de log (Opcional)
- `UserTest.java`: Test Unitario

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
tar -xzf grpc-user-server-2026.1Q-bin.tar.gz 
```
```shell
cd grpc-user-server-2026.1Q 
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
tar -xzf grpc-user-client-2026.1Q-bin.tar.gz 
```
```shell
cd grpc-user-client-2026.1Q 
```
```shell
chmod u+x run-client.sh 
```
```shell
sh run-client.sh 
```
