# Clase 4 - Sockets

## Cliente Servidor con Sockets

## Tecnologías utilizadas
- JDK 25
- Maven 3

## Build

```shell
./mvnw clean package
```

## Estructura del proyecto

#### `src/main/java/ar/edu/itba/pod/socket/server/`

- `GenericSocketServer.java`: servidor que recibe mensajes, actualiza un contador y responde al cliente.

#### `src/main/java/ar/edu/itba/pod/socket/client/`

- `GenericSocketClient.java`: cliente que establece la conexión y envía mensajes al servidor.

#### `src/test/java/ar/edu/itba/pod/socket/client/`

- `GenericSocketClientTest.java`: test de unidad (requiere que el servidor esté ejecutándose, por eso se marca como deshabilitado con `@Disabled` para que no falle en el proceso de build).
