# Clase 12 - Hazelcast Map Reduce II

## Bike Rentals

### Tecnologías utilizadas
- JDK 25
- Maven 3
- Hazelcast 3.8.6

**Documentación de Hazelcast:** https://docs.hazelcast.org/docs/3.8.6/manual/html-single/index.html

### Build

```shell
./mvnw clean package
```

### Estructura del proyecto

#### api

- `BikeRentalRow.java`: Modela un registro del CSV
- `RentalsBetweenStationsResult.java`: Modela un registro de salida para la consulta a resolver

#### server

- `Server.java`: Main

#### client

- `Client.java`: Main

### Ejecución Server

Después de haber realizado el build

#### Shell

```shell
cd server/target
```
```shell
tar -xzf hz-bike-rentals-server-2026.1Q-bin.tar.gz
```
```shell
cd hz-bike-rentals-server-2026.1Q
```
```shell
chmod u+x run-node.sh
```
```shell
sh run-node.sh
```

### Ejecución Client

#### Shell

```shell
cd client/target
```
```shell
tar -xzf hz-bike-rentals-client-2026.1Q-bin.tar.gz
```
```shell
cd hz-bike-rentals-client-2026.1Q
```
```shell
chmod u+x run-client.sh
```
```shell
sh run-client.sh bikeRentals.csv
```
