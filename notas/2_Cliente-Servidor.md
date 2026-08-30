---
materia: pod
tipo: apuntes
---

# Cliente / Servidor e introducción a gRPC

Del modelo concurrente al **modelo distribuido**: cómo se comunican dos nodos, qué es un servicio, y cómo un middleware RPC (concretamente **gRPC**) nos abstrae de la comunicación por red.

---

## Sistemas distribuidos

### Necesidad

Tanto el modelo multiproceso como el de [[1_Programacion-concurrente|programación concurrente]] pretenden paralelizar tareas **dentro de un sistema**. Más allá de sus beneficios y problemas ya vistos, tienen una cierta cantidad de **limitantes dadas por los recursos del sistema**:

- No se pueden correr concurrentemente más procesos de los que permite el procesador.
- El número de threads está limitado.
- La memoria también.
- Los procesos pueden llegar a competir por otros recursos compartidos como inodos, puertos, etc.

En algunos casos, cuando se necesitan más recursos, potencia y/o eficiencia, se puede utilizar el **modelo distribuido**.

> [!IMPORTANT] Sistema distribuido
> Es un sistema computacional cuyos **componentes están distribuidos en diferentes computadoras** (o elementos computacionales), que se conectan y comunican por medio de **mensajes de una red**.
> Este sistema se presenta ante sus usuarios como un **único sistema coherente**.

**Objetivo:** las componentes del sistema dividen una tarea coordinando sus recursos para completarla de manera más eficiente que si se ejecutara en una sola computadora.

> [!NOTE]
> Notar el paralelo con la [[1_Programacion-concurrente#Subdivisión de la tarea|subdivisión de tareas]] vista en concurrencia: la misma idea de particionar el trabajo, pero ahora los workers están en **nodos distintos**.

---

## El modelo Cliente / Servidor

> [!IMPORTANT]
> El modelo cliente/servidor se puede considerar como la **unidad atómica en la comunicación** entre los nodos de un sistema distribuido.

A pesar de que pueden ser cientos o miles de máquinas, toda comunicación será **entre dos máquinas**: una actuando como **proveedor de un servicio** (servicio ofrecido) y la otra como **cliente** del mismo (servicio requerido).

### ¿Qué es un servicio?

> [!IMPORTANT] Servicio
> Parte de un sistema que **maneja recursos** y **presenta su funcionalidad** a los usuarios (personas o aplicaciones), restringiendo sus posibilidades a un **conjunto de operaciones definidas en una API**.

### Roles

| Rol | Definición |
|---|---|
| **Servidor** | Proceso en una red de computadoras que **ejecuta** las operaciones ofrecidas por la API del servicio. |
| **Cliente** | Proceso en una red de computadoras que **solicita la ejecución** de la operación de un servicio. |

> [!NOTE]
> El término cliente y el término servidor sólo refieren al **rol que juega en un determinado momento** de la existencia del servicio. Un mismo proceso puede ser servidor de un servicio y cliente de otro.

### Servicios - Patrón de comunicación

La forma de implementar la comunicación entre cliente y servidor generalmente corresponde a dos estilos:

| Estilo | Descripción |
|---|---|
| **Request / Response** | El estilo más común: el cliente envía un pedido (*request*) y el servidor lo procesa **sincrónicamente**; una vez determinada la respuesta la envía hacia el cliente. |
| **Basado en Eventos** | Comunicación **asincrónica** donde el cliente se **registra** para la recepción de cierto tipo de eventos y, cuando estos ocurren, el servidor se los envía. |

La selección del estilo a usar dependerá mucho del tipo de servicio, cliente y tecnología.

> [!TIP]
> El estilo basado en eventos es el mismo patrón [[1_Programacion-concurrente#Pub-Sub|pub/sub]] que veíamos entre threads, ahora llevado a componentes remotos.

### Servicios - Interfaz

> [!IMPORTANT] Interfaz
> Una **interfaz** define un conjunto de operaciones que el servicio provee.

De cada operación, su API/interfaz indica:

- modos de llamado
- nombres
- parámetros
- valores de respuesta
- errores

La interfaz es el **elemento más importante de un servicio**, ya que es lo que le permite al cliente saber **cómo invocar** la operación que requiere.

Una interfaz remota puede definirse usando:

| Mecanismo | Ejemplo |
|---|---|
| Herramientas específicas del lenguaje/framework | Interfaces remotas de Java para **RMI** |
| **IDL** (lenguaje de definición de interfaces) | **protobuf**, utilizado en gRPC y otros |
| Documentación | APIs **REST** |

---

## IPC — Inter Process Communication

¿Cómo programamos la comunicación entre 2 nodos?

```
+------------------------------+
|   Aplicaciones y Servicios   |
+------------------------------+
|      Sistemas Operativos     |
+------------------------------+
|    Computadoras y la Red     |
+------------------------------+
```

El acceso a estos recursos a nivel sistema operativo y/o lenguajes de programación es por medio de **sockets**.

### IPC mediante sockets

- Para la comunicación a través de la red utilizamos **TCP/IP** y **UDP/IP** (punto a punto o multicast si es necesario). Entonces el servicio está en un nodo ubicado por su dirección **IP**, y el servicio en particular escucha en un **puerto** de dicho nodo.
- Abriendo un socket entre 2 nodos se envían los mensajes de **request** y **reply** de forma **binaria**.

El servidor tiene que abrir sockets y escuchar los mensajes:

```java
public void start(int port) throws IOException {
    logger.info("starting server on port {}", port);
    try (ServerSocket server = new ServerSocket(port);
         Socket client = server.accept();
         var out = new PrintWriter(client.getOutputStream(), true);
         var in = new BufferedReader(new InputStreamReader(client.getInputStream()))
    ) {
        boolean loop = true;
        String inputLine;
        while (loop && (inputLine = in.readLine()) != null) {
            loop = handleClient(inputLine, out);
        }
    }
}
```

En el servidor se contemplan dos operaciones: `"1"` para incrementar visitas y `"."` para finalizar la comunicación.

```java
private boolean handleClient(String inputLine, PrintWriter out) {
    logger.debug("received message {}", inputLine);
    if ("1".equals(inputLine)) {
        visitCount++;
    }
    out.println(visitCount);
    return !".".equals(inputLine);
}
```

El cliente se comunica y espera la respuesta:

```java
public void startConnection(String ip, int port) throws IOException {
    client = new Socket(ip, port);
    out = new PrintWriter(client.getOutputStream(), true);
    in = new BufferedReader(new InputStreamReader(client.getInputStream()));
}

public String sendMessage(String msg) throws IOException {
    out.println(msg);
    return in.readLine();
}
```

> [!NOTE] Dificultades del IPC con sockets
> - La implementación **no soporta atender más de un cliente**.
> - Con sockets es necesario **definir y mantener un protocolo propio**: serialización/parsing, múltiples conexiones, concurrencia, manejo de errores, etc. En resumen, se vuelve confuso y *error prone*.
> - Notar que ese `"1"` / `"."` **es** el protocolo: agregar operaciones implica extenderlo y versionarlo a mano.
> - Una de las ideas principales al hacer un servicio es que el desarrollo se haga **lo más transparente posible**.
>
> Para enfocarnos en la **lógica de negocio** subimos un nivel de abstracción y utilizamos un **middleware**.

### Capas y Middleware

Se intenta separar la capa de aplicación de la de comunicación mediante una capa intermedia llamada **middleware**, que se encarga de abstraer a la aplicación de todo lo requerido para realizar la comunicación por medio de una red.

```
+------------------------------+
|   Aplicaciones y Servicios   |
+------------------------------+
|          MIDDLEWARE          |  <- software necesario para proveer abstracción
+------------------------------+
|      Sistemas Operativos     |  \
+------------------------------+   > PLATAFORMA
|    Computadoras y la Red     |  /
+------------------------------+
```

Estos middlewares generalmente vienen provistos por medio de **frameworks estandarizados**.

---

## RPC

> [!IMPORTANT] RPC — Remote Procedure Call
> Es un **protocolo que especifica el llamado de ejecución de funciones remotas** sin tener que preocuparse por la comunicación entre el cliente y el servidor.

RPC define que habrá un middleware que se encarga de:

- **(Cliente)** Establecer la comunicación con el servidor y traducir el llamado al método y sus parámetros a una invocación por la red.
- **(Servidor)** Recibir la invocación, llamar al método correspondiente y transformar la respuesta a una comunicación por red para que la reciba el cliente.
- Todo lo relativo a **comunicación y errores**.

> [!TIP]
> La intención de un framework como RPC es que las llamadas al servicio sean lo más **"transparentes"** posibles, es decir, que **no se diferencien de llamados locales**. Efectivamente el middleware trabaja como un **Proxy** del servicio remoto.

### Definiciones del middleware

| Elemento | Dónde vive | Qué hace |
|---|---|---|
| **Stub** | Cliente | Funciona de **proxy** a un objeto remoto. Implementa los mismos métodos que la interfaz remota. Recibe las invocaciones del cliente, las pasa por la red (el método invocado y los parámetros) y espera los resultados para leerlos y transmitirlos al cliente. |
| **Skeleton** | Servidor | Recibe el pedido del cliente, lo **interpreta**, obtiene los parámetros y llama al objeto remoto con los mismos. Una vez que recibe la respuesta la transmite al cliente, aislando al objeto remoto de la forma de comunicación. |
| **Servant** | Servidor | La **implementación real** de la lógica del servicio, que el skeleton invoca localmente. |

---

## gRPC

**gRPC** (2016) es un sistema de comunicación vía RPC **open source** desarrollado inicialmente por Google que apunta a ser:

- Multilenguaje
- Multiplataforma
- De alto rendimiento
- De uso general

gRPC está basado en:

- **Protocol Buffers** para IDL y formato de serialización
- **HTTP/2** para el transporte

Y ofrece *built-in*: autenticación, streaming bidireccional, control de flujo, timeouts y cancelaciones.

### Protocol Buffers

> [!IMPORTANT]
> **Protocol Buffer** es un mecanismo para **serialización de datos** que es **independiente de lenguaje y plataforma**.

- Al ser independiente del lenguaje es útil como **IDL** (lenguaje de definición de interfaces), es decir, definir servicios multilenguaje.
- Además se puede usar como **formato de serialización** de la información que viaja entre el servicio y el cliente.

Como herramienta de serialización, Protocol Buffer es:

| Característica | Beneficio |
|---|---|
| **Tipado** | Permite validar y optimizar por tipos |
| **Binario** | Al no ser un protocolo textual es más rápido de leer y escribir para las máquinas (*machine readable*) |
| **Provee generadores** | Para la serialización/deserialización en la mayoría de los lenguajes |

### Marshalización / Serialización

| Término | Definición |
|---|---|
| **Serializar** | Convertir el estado de un objeto en un *stream* de bytes que pueda luego ser utilizado para obtener una copia de la información del objeto. |
| **Marshalizar** | Codifica no sólo la información que contiene el objeto, sino también la información de la **definición** del objeto (o cómo construirlo, o al menos cómo encontrar la referencia a la definición). |
| **De-serializar** | Tomar un stream de bytes (que corresponde a un objeto serializado previamente) y obtener a partir de él un objeto con la información que tenía el original. |
| **Un-marshalling** | Similar a de-serializar, salvo que además permite obtener la **definición** del objeto. |

> [!NOTE]
> En algunos lenguajes y situaciones los términos se utilizan como sinónimos.

### Transporte: HTTP/2

**HTTP/2** es el protocolo de transporte. Es una mejora sobre HTTP que permite generar aplicaciones más robustas, rápidas y eficientes. Sus principales características:

- Mantener la **semántica** de HTTP
- Una única conexión **"permanente"**
- **Multiplexed streams**
- **Server push**
- **Compresión de headers**
- Formato **binario**

### Flujo de una llamada gRPC

Cuando un cliente llama a un método del servicio (utilizando el stub), el proceso que se realiza es:

1. El **stub serializa** los parámetros del método del servicio.
2. El stub genera un **POST request** usando HTTP/2, se genera la conexión y envía **headers** que indican el tipo de llamado, el método a correr remotamente, etc.
3. Se **envía el mensaje** por la red.
4. Al recibir el mensaje, el servidor lee los headers para ver el método a llamar e invoca al **skeleton** correspondiente, pasándole el mensaje.
5. El skeleton **de-serializa** los parámetros y realiza la **llamada local** a la implementación del **Servant** del método remoto.
6. Con la respuesta del Servant se genera el **proceso inverso** para que la respuesta le llegue al cliente.

> [!EXAMPLE]+ El flujo completo sobre el ejemplo `Greeter`
> Cliente ejecuta `greeter.sayHello(HelloRequest{name: "Foo"})`:
>
> | Paso | Dónde | Qué pasa |
> |---|---|---|
> | 1 | Cliente (stub) | `HelloRequest{name:"Foo"}` se serializa a bytes con protobuf |
> | 2 | Cliente (stub) | Se arma un `POST /Greeter/SayHello` sobre HTTP/2 con los headers del método |
> | 3 | Red | Viajan los bytes |
> | 4 | Servidor | Lee los headers, identifica `SayHello`, invoca al skeleton `GreeterImplBase` |
> | 5 | Servidor (skeleton) | De-serializa a `HelloRequest` y llama localmente a `Servant#sayHello(req, observer)` |
> | 6 | Servidor (servant) | Construye `HelloReply{message:"Hello Foo"}`, lo pasa por `onNext` + `onCompleted` |
> | 7 | Vuelta | Se serializa la reply, viaja por HTTP/2, el stub la de-serializa y `sayHello` **retorna** |
>
> Desde el punto de vista del código del cliente, todo eso se ve como una sola línea: una llamada a un método que devuelve un valor.

### Spring gRPC

**Spring gRPC** (2025) es un framework que simplifica la implementación de servidores y clientes gRPC. Se basa en la implementación de **gRPC Java** y ofrece:

- Inyección de dependencias para servicios y stubs
- *Starters* de Spring Boot para servidor y cliente
- Autoconfiguración vía propiedades
- Testing simplificado
- Integración con el ecosistema Spring

### Generación de código Java

En Java (como para todos los lenguajes) existe un **compilador** que recibe el archivo `*.proto` con la definición del servicio y genera una clase llamada:

```
<NombreDeServicio>Grpc     // por ejemplo: GreeterGrpc
```

La misma funciona como **middleware** y provee subclases que utilizan el servidor y el cliente para realizar la comunicación entre ellos. Además se generan **clases por cada mensaje** de entrada y salida.

---

## Definición de servicios: el archivo `.proto`

La definición de servicios se realiza en un archivo con extensión `.proto`. Antes del servicio se pueden customizar algunas características generales:

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "ar.edu.itba.pod.grpc";
```

El servicio se define a partir de la palabra `service` y se listan sus métodos:

```protobuf
service Greeter {
  // Sends a greeting
  rpc SayHello (HelloRequest) returns (HelloReply);
}
```

> [!IMPORTANT]
> Todos los métodos siempre se prefijan con `rpc` y tienen **exactamente un mensaje de entrada y uno de salida**.

Para terminar de definir el servicio se agregan los mensajes de entrada y salida:

```protobuf
message HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}
```

> [!NOTE]
> El mensaje **debe existir aún si está vacío**. Los números (`= 1`) son los *field tags*: identifican al campo en la codificación binaria, así que no se deben reutilizar ni cambiar.

---

## gRPC — Client

Para uso en el cliente se proveen **4 versiones de stubs** para realizar llamados a los servicios del server:

| Stub | Semántica |
|---|---|
| `<Servicio>BlockingStub` | Stub **sincrónico** |
| `<Servicio>BlockingV2Stub` | Sincrónico con **checked exceptions** |
| `<Servicio>FutureStub` | **Asincrónico** con `Future` |
| `<Servicio>Stub` | **Asincrónico** con *observers* |

Cada uno se instancia a partir de un **builder** y cuenta con los métodos del servicio, variando sus parámetros y respuesta de acuerdo al tipo de llamado que realizan.

> [!TIP]
> Acá se reencuentran las abstracciones de [[1_Programacion-concurrente#Threads en Java 5|Java 5]]: el `FutureStub` devuelve un `Future<HelloReply>`, así que un llamado remoto se maneja exactamente igual que una tarea asincrónica local.

Para consumir el servicio se debe instanciar el stub deseado a partir de una `GrpcChannelFactory` y el nombre de un **channel**:

```java
@SpringBootApplication
public class Client {
    static void main(String[] args) {
        SpringApplication.run(Client.class, args);
    }

    @Bean
    GreeterGrpc.GreeterBlockingStub greeterStub(GrpcChannelFactory channels) {
        return GreeterGrpc.newBlockingStub(channels.createChannel("local"));
    }
}
```

Spring gRPC nos permite instanciar un cliente **sin indicar la configuración**. En `client/src/main/resources/application.yml` se puede modificar el comportamiento por defecto (por ejemplo cambiar el puerto):

```yaml
spring:
  grpc:
    client:
      channels:
        local:
          address: localhost:50051
        cloud:
          address: grpc-greeter-demo-server.XXX.norwayeast.azurecontainerapps.io:443
          negotiation-type: tls
logging:
  level:
    root: info
```

### Lógica del cliente

Spring Boot permite ejecutar código registrando un `@Bean` de tipo `CommandLineRunner`. Se debe invocar el comando `spring-boot:run`:

```java
@SpringBootApplication
public class Client {
    // ...
    @Bean
    CommandLineRunner run(GreeterGrpc.GreeterBlockingStub greeter) {
        return _ -> {
            var name = "Foo";
            HelloRequest request = HelloRequest.newBuilder().setName(name).build();
            HelloReply response = greeter.sayHello(request);
            log.info("Greeting: {}", response.getMessage());
        };
    }
}
```

### `@ImportGrpcClients`

`@ImportGrpcClients` evita que tengamos que crear el bean del stub a mano. Utiliza el channel indicado en `spring.grpc.client.default-channel`:

```java
@SpringBootApplication
@ImportGrpcClients(types = GreeterGrpc.GreeterBlockingStub.class)
public class Client {
    static void main(String[] args) {
        SpringApplication.run(Client.class, args);
    }

    @Bean
    CommandLineRunner run(GreeterGrpc.GreeterBlockingStub greeter) { /* ... */ }
}
```

```yaml
spring:
  grpc:
    client:
      channels:
        local:
          address: ...
        cloud:
          address: ...
          negotiation-type: tls
      default-channel:
        address: localhost:50051
```

---

## gRPC — Server

Spring gRPC nos permite instanciar un servidor sin indicar la configuración:

```java
@SpringBootApplication
public class Server {
    static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }
}
```

```yaml
spring:
  grpc:
    server:
      port: 50051
logging:
  level:
    root: info
```

### Lógica del servicio (Servant)

Para implementar el servicio se debe **heredar** de la clase `<NombreServicio>Grpc.<NombreServicio>ImplBase`. Para que el Server lo reconozca, es necesaria la anotación `@Service`:

```java
@Service
public class Servant extends GreeterGrpc.GreeterImplBase {
}
```

Se deben **sobreescribir los métodos deseados** de la implementación base. En este caso el método es `void` y recibe dos parámetros:

- El **message de request**.
- Un **`StreamObserver`** donde dejar las respuestas vía `onNext`, y al terminar se usa `onCompleted`. `onError` se invoca en caso de error.

```java
public interface StreamObserver<V> {
    void onNext(V value);
    void onError(Throwable t);
    void onCompleted();
}
```

Implementación completa del Servant:

```java
@Service
public class Servant extends GreeterGrpc.GreeterImplBase {
    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        HelloReply reply = HelloReply.newBuilder()
                .setMessage("Hello " + req.getName())
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
```

> [!NOTE]
> El método es `void` y la respuesta se entrega por el `StreamObserver` justamente porque el modelo soporta **streaming**: en un unary call se llama a `onNext` **una vez** y luego a `onCompleted`, pero en un server-streaming se llamaría a `onNext` muchas veces antes de completar. Si no se llama a `onCompleted` (u `onError`), el cliente **queda esperando**.

---

## Tests unitarios

En el proyecto de sockets el test estaba deshabilitado porque se necesitaba contar con un servidor en ejecución. Spring gRPC puede crear un **server in-process**:

```java
@SpringJUnitConfig(GreeterTest.TestConfig.class)
@AutoConfigureInProcessTransport
public class GreeterTest {

    @Autowired
    private GreeterGrpc.GreeterBlockingStub stub;

    @Test
    void sayHelloTest() {
        var name = "Foo";
        var request = HelloRequest.newBuilder().setName(name).build();
        assertEquals("Hello %s".formatted(name), stub.sayHello(request).getMessage());
    }

    @EnableAutoConfiguration
    @Import({ Servant.class })
    @ImportGrpcClients(types = GreeterGrpc.GreeterBlockingStub.class)
    static class TestConfig {
    }
}
```

> [!TIP]
> `@AutoConfigureInProcessTransport` reemplaza el transporte HTTP/2 por uno **in-process**: no hay socket ni puerto, pero se ejercita todo el camino stub → skeleton → servant. Es la respuesta directa a la dificultad de testear vista con sockets.

---

## Ejercicios de la clase

| # | Ejercicio | Repo / Qué hacer |
|---|---|---|
| 1 | **Sockets con Java** | Clonar `https://github.com/POD-ITBA/sockets`. Levantar `GenericSocketServer` y correr el test `sendMessageTest` de `GenericSocketClient`. |
| 2 | **gRPC Greeter — Build** | Clonar `https://github.com/POD-ITBA/greeter`. Analizar la estructura, el `greeter.proto` en `api/src/main/proto`, buildear y analizar los archivos **generados** en `api/`. |
| 3 | **Greeter — Cliente Cloud** | Implementar `Client` instanciando un `BlockingStub` con el channel `cloud`, poner la URL del servicio en el `application.yml` e invocar `sayHello`. |
| 4 | **Greeter — Cliente Local** | Pasar el `BlockingStub` al channel `local` y hacer que `Servant` extienda `GreeterGrpc.GreeterImplBase`. Correr `Server` y `Client` en dos terminales. ¿Qué resultado da? (el `ImplBase` sin sobreescribir devuelve `UNIMPLEMENTED`) |
| 5 | **Greeter — Servant** | Sobreescribir `sayHello` en `Servant` y verificar que se obtenga el resultado esperado. |

---

## Takeaways

- El modelo **distribuido** aparece cuando los recursos de un solo nodo (procesos, threads, memoria) ya no alcanzan.
- Todo sistema distribuido se descompone en interacciones **cliente/servidor**, y lo central de un servicio es su **interfaz**.
- Programar la comunicación directamente con **sockets** obliga a inventar y mantener un protocolo propio: serialización, concurrencia, múltiples clientes, errores. Es confuso y *error prone*.
- Un **middleware RPC** sube el nivel de abstracción: **stub** en el cliente, **skeleton** en el servidor, y la llamada remota se ve como una llamada local.
- **gRPC** = **protobuf** (IDL + serialización binaria tipada) + **HTTP/2** (transporte binario multiplexado), con autenticación, streaming, timeouts y cancelaciones incluidos.
- Del `.proto` se genera automáticamente `<Servicio>Grpc` con los stubs del cliente y el `ImplBase` del servidor; sólo escribimos el **Servant** con la lógica de negocio.
