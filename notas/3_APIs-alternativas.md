---
materia: pod
tipo: apuntes
---

# APIs alternativas

Resumen de las dos teóricas de APIs (`7_API-1.pdf` y `8_API-2.pdf`). En [[2_Cliente-Servidor#gRPC|gRPC]] vimos por dentro una forma de comunicar dos nodos; acá se recorren las otras formas populares de exponer un servicio: **tira de bytes**, **SOAP**, **REST**, **GraphQL** y las **APIs de eventos**.

Todas se ilustran con el mismo servicio de ejemplo: un catálogo de **autores** y **posts** con cuatro operaciones — crear autor, crear post, listar autores y listar posts (opcionalmente filtrando por autor).

---

## Por qué hay tantas

La **API** es el punto central a definir de un servicio: indica la semántica y las operaciones posibles a la hora de consumirlo. Casi cualquier tecnología puede resolver casi cualquier API, así que la elección de la "mejor" se hace según criterios:

| Criterio | Pregunta que responde |
|---|---|
| Formas de comunicación y periodicidad | ¿Request/response? ¿El servidor avisa cuando pasa algo? |
| Transporte | ¿TCP crudo, HTTP, SMTP, un broker? |
| Diversidad de clientes | ¿Qué lenguajes y tecnologías tienen que poder consumirla? |
| Descripción de las operaciones | ¿Con qué se define el contrato (`.proto`, WSDL, esquema, documentación)? |
| Diversidad de los payloads | ¿JSON, XML, binario, multimedia? |

> [!TIP] Las dos grandes corrientes
> **Request/Response** (tira de bytes, SOAP, REST, GraphQL, gRPC) vs. **Eventos** (webhooks, WebSockets, SSE, pub/sub). En la primera el cliente pregunta; en la segunda la comunicación se inicia cuando ocurre algo.

---

## Tira de bytes

La forma más **primitiva** de armar una API y, durante mucho tiempo, la única. Todavía aparece en APIs legacy.

> [!IMPORTANT] Tira de bytes
> Se abre una conexión por **socket** y se conviene, **mediante documentación**, de qué manera se envían los distintos valores del request y de la response. No hay contrato formal ni generación de código: el contrato es el documento.

Es lo mismo que hicimos a mano en [[2_Cliente-Servidor#IPC mediante sockets|IPC mediante sockets]].

**Request:** 1 byte de `opcode` y después los campos de ese opcode, en orden.

| Opcode | Valor | Campos de request |
|---|---|---|
| `CREATE_AUTHOR` | `0x01` | `UTF name` |
| `CREATE_POST` | `0x02` | `long authorId`, `UTF title`, `UTF content` |
| `LIST_AUTHORS` | `0x03` | (sin campos) |
| `LIST_POSTS` | `0x04` | `boolean filterByAuthor`; si es `true`, además `long authorId` |

**Response:** 1 byte de `status` (`0x00` OK, `0x01` ERROR). Si es ERROR sigue un `UTF message`; si es OK, el payload según el opcode:

| Opcode | Payload de response (status OK) |
|---|---|
| `CREATE_AUTHOR` | `long id`, `UTF name` |
| `CREATE_POST` | `long id`, `UTF title`, `UTF content`, `long authorId` |
| `LIST_AUTHORS` | `int count`, seguido de `count` autores (`long id`, `UTF name`) |
| `LIST_POSTS` | `int count`, seguido de `count` posts (`long id`, `UTF title`, `UTF content`, `long authorId`) |

> [!EXAMPLE]+ Armar y leer un `LIST_POSTS` filtrado por el autor 7
> El cliente escribe en el socket, en orden:
> 1. `0x04` → opcode `LIST_POSTS`.
> 2. `true` → `filterByAuthor`.
> 3. `7L` → como el flag es `true`, va el `long authorId`.
>
> El servidor responde:
> 1. `0x00` → status OK, así que sigue el payload.
> 2. `int count = 2`.
> 3. Post 1: `long id`, `UTF title`, `UTF content`, `long authorId = 7`.
> 4. Post 2: ídem.
>
> Si el autor no existiera: `0x01` y después `UTF message` con el error.
>
> El cliente tiene que leer **exactamente en el mismo orden y con los mismos tipos**; no hay nada en el mensaje que diga qué es cada cosa.

```java
// Cliente (ejemplo propio, no está en las slides)
out.writeByte(0x04);
out.writeBoolean(true);
out.writeLong(7L);

if (in.readByte() == 0x00) {
    int count = in.readInt();
    for (int i = 0; i < count; i++) {
        long id = in.readLong();
        String title = in.readUTF();
        String content = in.readUTF();
        long authorId = in.readLong();
    }
} else {
    String error = in.readUTF();
}
```

> [!WARNING] Por qué quedó legacy
> Cliente y servidor están acoplados al orden exacto de los bytes. Agregar un campo, cambiar un tipo o equivocarse en la documentación rompe todo, y no hay manera de validar el mensaje ni de generar el código de los dos lados.

---

## SOAP

> [!IMPORTANT] SOAP
> **SOAP** (Simple Object Access Protocol) es un protocolo estándar de comunicación mediante el intercambio de datos utilizando **XML**. La definición de los servicios también se hace mediante XML.

### Características

| Característica | Qué significa |
|---|---|
| **Extensibilidad** | El protocolo puede extenderse para hacer servicios complejos o agregar seguridad y routing. |
| **Neutralidad** | Usa transporte TCP y se le pueden aplicar diversos protocolos de aplicación: HTTP, SMTP o JMS. |
| **Independencia** | Permite cualquier modelo de programación: al definirse vía XML se puede programar en diversos lenguajes y paradigmas. |

### El mensaje

| Parte | Obligatoria | Contenido |
|---|---|---|
| **Envelope** | sí | Elemento que engloba todo el mensaje. |
| **Header** | no | Atributos complementarios al mensaje, por ejemplo autenticación. |
| **Body** | sí | El contenido del mensaje a enviar. |
| **Fault** | no | Información de los errores que se produjeron (si se produjeron). Va dentro del Body. |

Request para crear un autor:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:cat="http://itba.edu.ar/pod/api/soap">
  <soapenv:Header/>
  <soapenv:Body>
    <cat:createAuthorRequest>
      <cat:name>Julio Cortázar</cat:name>
    </cat:createAuthorRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

Response OK y response con error:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Body>
    <cat:createAuthorResponse xmlns:cat="http://itba.edu.ar/pod/api/soap">
      <cat:author>
        <cat:id>1</cat:id>
        <cat:name>Julio Cortázar</cat:name>
      </cat:author>
    </cat:createAuthorResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Body>
    <soapenv:Fault>
      <faultcode>soapenv:Client</faultcode>
      <faultstring>Author not found: 999</faultstring>
    </soapenv:Fault>
  </soapenv:Body>
</soapenv:Envelope>
```

### Definición del servicio: WSDL y XSD

> [!IMPORTANT] WSDL
> Un servicio SOAP se define mediante un archivo de contrato en XML llamado **WSDL** (Web Services Description Language), que especifica las **operaciones disponibles**, los **tipos de datos** y la **ubicación del servidor**. Cumple el rol que en gRPC cumple el `.proto`.

| Etiqueta WSDL | Rol |
|---|---|
| `types` | Tipos de datos y estructuras XML propias (normalmente con esquemas XSD). |
| `message` | Los datos que se intercambian, separando entrada y salida de cada operación. |
| `portType` | La **interfaz abstracta**: qué operaciones existen y qué mensajes entran/salen de cada una. |
| `binding` | **Cómo viajan** los mensajes: protocolo de transporte (ej. SOAP sobre HTTP) y estilo de serialización. |
| `service` / `port` | El **endpoint concreto**: une el `portType` con el `binding` y le da una URL física. |

Los tipos se pueden separar en un archivo **XSD**:

| | WSDL | XSD (XML Schema Definition) |
|---|---|---|
| Propósito | Describir el servicio web completo (operaciones, rutas, protocolos). | Describir el contenido del mensaje (campos, tipos, restricciones). |
| Nivel | **Macro**: interfaz y endpoints. | **Micro**: esqueleto y reglas de cada fragmento XML. |
| Pregunta | ¿A qué URL envío la petición y qué funciones puedo llamar? | ¿Qué campos lleva el XML y de qué tipo son? |
| Componentes | `<portType>`, `<binding>`, `<service>`, `<operation>` | `<element>`, `<complexType>`, `<simpleType>`, `<restriction>` |

> [!EXAMPLE]+ Del XSD al endpoint, pieza por pieza
> **XSD — tipo reutilizable y elemento de request:**
> ```xml
> <xs:complexType name="author">
>     <xs:sequence>
>         <xs:element name="id" type="xs:long"/>
>         <xs:element name="name" type="xs:string"/>
>     </xs:sequence>
> </xs:complexType>
>
> <xsd:element name="createAuthorRequest">
>   <xsd:complexType>
>     <xsd:sequence>
>       <xsd:element name="name" type="xsd:string"/>
>     </xsd:sequence>
>   </xsd:complexType>
> </xsd:element>
> ```
> **`message`** — Spring-WS envuelve cada elemento del XSD como una única `part`:
> ```xml
> <wsdl:message name="createAuthorRequest">
>   <wsdl:part element="tns:createAuthorRequest" name="createAuthorRequest"/>
> </wsdl:message>
> ```
> **`portType`** — la operación usa esos mensajes:
> ```xml
> <wsdl:portType name="CatalogPort">
>   <wsdl:operation name="createAuthor">
>     <wsdl:input message="tns:createAuthorRequest"/>
>     <wsdl:output message="tns:createAuthorResponse"/>
>   </wsdl:operation>
> </wsdl:portType>
> ```
> **`binding`** — SOAP sobre HTTP, estilo `document`, body `literal`:
> ```xml
> <wsdl:binding name="CatalogPortSoap11" type="tns:CatalogPort">
>   <soap:binding transport="http://schemas.xmlsoap.org/soap/http" style="document"/>
>   ...
> </wsdl:binding>
> ```
> **`service`** — la URL donde vive:
> ```xml
> <wsdl:service name="CatalogPortService">
>   <wsdl:port name="CatalogPortSoap11" binding="tns:CatalogPortSoap11">
>     <soap:address location="http://localhost:8082/ws"/>
>   </wsdl:port>
> </wsdl:service>
> ```
> Cadena completa: **tipos (XSD) → mensajes → operaciones (portType) → cómo viajan (binding) → dónde (service)**.

### Generación de código

Igual que con gRPC: los lenguajes traen compiladores que leen el XML y generan las clases de servicio y los stubs (o, al revés, generan el WSDL a partir del código). En Java: **Spring WS**, **CXF**, **Axis**, o soporte nativo vía **JAX-WS**. Cuál usar suele depender del código existente.

```java
@PayloadRoot(namespace = NAMESPACE, localPart = "createPostRequest")
@ResponsePayload
public CreatePostResponse createPost(@RequestPayload CreatePostRequest request) {
    try {
        var post = createPostUseCase.createPost(
                new CreatePostCommand(request.getAuthorId(),
                        request.getTitle(), request.getContent()));
        CreatePostResponse response = new CreatePostResponse();
        response.setPost(toXml(post));
        return response;
    } catch (AuthorNotFoundException ex) {
        throw new AuthorNotFoundSoapFault(ex.getMessage());
    }
}
```

> [!NOTE]
> El `toXml` convierte el objeto de dominio en el DTO generado desde el XSD. Las excepciones de dominio se traducen a un **SOAP Fault**, igual que en gRPC se traducen a un `Status`.

### Ventajas e inconvenientes

- **A favor:** extensibilidad y multi lenguaje; por eso fue durante mucho tiempo el sistema de web services más popular.
- **En contra:** el XML es muy **verboso** y definir servicios es complejo, aun para servicios simples.

> [!TIP] La dicotomía SOAP / REST
> SOAP fue perdiendo terreno contra REST y quedó un reparto: **REST** para servicios web "simples" y **B2C**; **SOAP** para los más complejos con más necesidad de seguridad, como **financieras y B2B**.

---

## REST

> [!IMPORTANT] REST — definición original
> Según la tesis doctoral de Roy T. Fielding, **REST** (Representational State Transfer) es un *estilo arquitectónico para sistemas distribuidos de hipermedia*: un conjunto de **principios de arquitectura**, no un protocolo.

Como Fielding trabajó en HTTP y los principios están muy atados a ese protocolo, el término se amplió. Hoy, informalmente:

> [!IMPORTANT] REST — uso actual
> Una interfaz remota de servicios **basada en recursos** sobre **HTTP**, usada para obtener y modificar datos en diversos formatos (JSON, XML, binario, etc.).

La diferencia clave con RPC (gRPC, SOAP): los servicios se definen con **recursos (sustantivos)** en vez de **verbos (acciones)**. En gRPC hay un `createAuthor(...)`; en REST hay un recurso `/authors` sobre el que se hace un `POST`.

### Principios de arquitectura

| Principio | Qué pide | Para qué |
|---|---|---|
| **Client-Server** | Separar responsabilidades; la dependencia es sólo a través del mensaje. | Cambiar un lado no afecta al otro. |
| **Stateless** | El estado/contexto de la petición lo mantiene el **cliente**; el servidor recibe todo en cada request. | Distintas instancias pueden atender llamadas sucesivas del mismo cliente. |
| **Cacheable** | Todos los recursos deben ser cacheables mientras tenga sentido. | Optimizar la comunicación. |
| **Layered** | El cliente sólo conoce la capa con la que habla. | Poder meter proxies, balanceadores, etc. en el medio. |
| **Code on Demand** | (Opcional, en desuso) ejecutar código en el cliente vía scripts o applets. | — |
| **Interfaz uniforme** | Generalidad para simplificar las interacciones (ver abajo). | — |

Restricciones de la **interfaz uniforme**:

1. **Identificación de recursos en las peticiones:** cada recurso se identifica unívocamente, independientemente de la representación que se pida (por headers).
2. **Manipulación a través de representaciones:** lo que el cliente recibe de un recurso contiene toda la info para modificarlo/borrarlo.
3. **Mensajes auto-descriptivos:** cada mensaje trae todo lo necesario para ejecutarse.
4. **Hipermedia como motor del estado (HATEOAS):** a partir de un pedido inicial el cliente debería poder "descubrir" todos los servicios.

> [!WARNING] Stateless ≠ sin base de datos
> Stateless se refiere al **estado de la sesión/conversación**, no a los datos. El servidor puede tener una base con autores y posts; lo que no guarda es "en qué paso está este cliente". Eso es lo que permite escalar horizontalmente.

### Recursos

Los **recursos** son la información que las aplicaciones proporcionan a sus clientes (imágenes, videos, texto, números, cualquier dato). Hay tres tipos:

| Tipo | Qué es | URI |
|---|---|---|
| **Colección** | Conjunto de elementos similares; sustantivo, generalmente plural. | `/movies` |
| **Singleton / documento** | Una instancia de la colección, identificada por un id natural o interno. | `/movies/{id}` |
| **Subcolección** | Conjunto de singletons subordinados a un elemento. | `/movies/{id}/actors` |

Todo parte de la URI del servicio, con contexto y versión opcional: `http://remote-service/context/v1`.

Además se usan las features de HTTP para precisar el pedido:

- **Status codes** → indicar el resultado.
- **Content-types** → pedir/indicar la representación del recurso.
- **Query params** → criterios de búsqueda, paginación, etc.

> [!EXAMPLE]+ El catálogo en REST
> La tabla de la cátedra con URLs, métodos y status está como imagen en el PDF; esta reconstrucción usa las convenciones estándar de HTTP (**fuente externa**, verificala contra la slide):
>
> | Operación | Método | URI | Status OK | Error típico |
> |---|---|---|---|---|
> | Crear autor | `POST` | `/authors` | `201 Created` | `400 Bad Request` |
> | Listar autores | `GET` | `/authors` | `200 OK` | — |
> | Ver un autor | `GET` | `/authors/{id}` | `200 OK` | `404 Not Found` |
> | Crear post | `POST` | `/authors/{id}/posts` | `201 Created` | `404 Not Found` |
> | Posts de un autor | `GET` | `/authors/{id}/posts` | `200 OK` | `404 Not Found` |
>
> El filtro por autor que en la tira de bytes era un `boolean` + `long` acá es directamente la **subcolección** `/authors/{id}/posts` (o un query param como `/posts?authorId=7`).

Mismo recurso, distintas **representaciones** según el header `Accept`:

```bash
curl -H "Accept: application/json"     http://localhost:8081/authors/1
curl -H "Accept: application/hal+json" http://localhost:8081/authors/1
curl -H "Accept: application/xml"      http://localhost:8081/authors/1
```

### HATEOAS

Cada cliente debería poder **navegar** el servicio a partir de la respuesta al primer request. Para eso la respuesta incluye **links hipermedia** a los recursos dependientes (subcolecciones o singletons):

```json
{
  "id": 1,
  "name": "Julio Cortázar",
  "_links": {
    "self":  { "href": "http://localhost:8081/authors/1" },
    "posts": { "href": "http://localhost:8081/authors/1/posts" }
  }
}
```

### Código (Spring)

Los ejemplos combinan Spring, Jersey y JAX-RS. El recurso extiende `RepresentationModel` para poder llevar links:

```java
public class AuthorModel extends RepresentationModel<AuthorModel> {
    private final Long id;
    private final String name;
    // ...
}
```

```java
@RestController
@RequestMapping("/authors")
public class AuthorController {

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE,
                            MediaTypes.HAL_JSON_VALUE,
                            MediaType.APPLICATION_XML_VALUE})
    public CollectionModel<AuthorModel> listAuthors() {
        var models = listAuthorsUseCase.listAuthors().stream()
                .map(authorAssembler::toModel).toList();
        return CollectionModel.of(models,
                linkTo(methodOn(AuthorController.class).listAuthors()).withSelfRel());
    }
}
```

### Ventajas y desventajas

Tiene enorme soporte en lenguajes, frameworks y conocimiento de la comunidad. Por sus restricciones es:

- **Escalable** → stateless, client-server, cacheable.
- **Flexible** → client-server y las múltiples representaciones.
- **Independiente de la tecnología** → el transporte es HTTP, cliente y servidor pueden estar en lenguajes distintos.

> [!WARNING] Desventajas
> - Al no haber un estándar definido, cuesta describir partes del sistema que no son fácilmente expresables como **sustantivos** u operaciones CRUD (¿"transferir", "aprobar"?).
> - Cuando distintas partes del sistema necesitan **distintas facetas** de un recurso, terminás con muchos endpoints o con uno que devuelve demasiada información. Esto es justamente lo que ataca GraphQL.

---

## GraphQL

> [!IMPORTANT] GraphQL
> **GraphQL** es un **lenguaje de consulta y manipulación de datos** para APIs, y un **entorno de ejecución** para realizar esas consultas sobre datos existentes. A pesar del nombre, **no es un lenguaje para recorrer grafos**.

Permite definir APIs web:

- Con un **esquema tipado**.
- **Agnóstico** del transporte y de la fuente de datos.
- **Explorable y descubrible**.
- Con operaciones de búsqueda (**query**), modificación (**mutation**) y suscripción (**subscription**).
- Donde el **cliente elige qué campos quiere recibir**.

> [!TIP] Ventajas frente a REST
> - Se **reducen los llamados** al servidor para obtener los mismos datos (en REST, post + autor serían dos requests).
> - Se piden **sólo los datos que se precisan** (ni más ni menos).
> - Esos datos pueden venir de **múltiples fuentes**.

### Esquema

La API se define con un **esquema fuertemente tipado** que define los objetos de argumentos y respuestas — esto le da un estilo parecido a una **API RPC**. El esquema se publica y el cliente infiere las operaciones a partir de él.

> [!NOTE] Un solo endpoint
> El servicio se publica en **una URL** y **todas** las operaciones se ejecutan contra ese endpoint con un **`POST`**, variando el body. Opuesto a REST, donde cada recurso tiene su URI.

```graphql
type Author {
   id: ID!
   name: String!
   posts: [Post]!
   thumbnail: String
}

type Post {
   author: Author!
   category: String
   id: ID!
   text: String!
   title: String!
}
```

| Sintaxis | Significado |
|---|---|
| `nombre: Tipo` | Cada campo tiene nombre y tipo. |
| `String`, `Int`, `Float`, `Boolean`, `ID` | Tipos escalares. |
| `!` | Campo obligatorio (**non null**). |
| `[ ]` | Se devuelve un array de valores. |

Las operaciones van en tipos especiales, con **métodos** en vez de campos: `Query` (**obligatorio**), `Mutation` y `Subscription`. Opcionalmente se envuelven en un `schema`:

```graphql
schema {
   query: Query
   mutation: Mutation
}

type Query {
   recentPosts(count: Int, offset: Int): [Post]!
}

type Mutation {
   createPost(authorId: String!, category: String, text: String!, title: String!): Post!
}
```

### Hacer una query

El body arranca con la keyword `query`, opcionalmente declara variables, y dice qué método llamar con qué argumentos y **qué campos** espera en la respuesta. Las variables van en un objeto aparte:

```graphql
query myRecentPosts($count: Int, $offset: Int) {
  recentPosts(count: $count, offset: $offset) {
    id
    title
    text
    category
    author {
      id
      posts {
        id
      }
    }
  }
}
```

```json
{ "count": 2, "offset": 2 }
```

Una **mutation** es igual, pero con la keyword `mutation`:

```graphql
mutation createPost($title: String!, $text: String!, $category: String, $authorId: String!) {
  createPost(title: $title, text: $text, category: $category, authorId: $authorId) {
    id
    title
    text
    category
  }
}
```

### Resolvers

Para responder hace falta implementar **resolvers**. Hay de varios tipos según lo que resuelven — **Query**, **Mutation**, **Subscription** y **Field** — y eso es lo que da granularidad para completar cada campo con datos de fuentes distintas.

Cómo se generan modelos y resolvers depende del lenguaje y la librería (sólo middleware, código desde el esquema, esquema desde el código y anotaciones…). En clase se usa **Spring**, que wrappea la implementación oficial de Java.

| Anotación (Spring) | Resolver | Resuelve |
|---|---|---|
| `@QueryMapping` | Query | Un método de `type Query`. |
| `@MutationMapping` | Mutation | Un método de `type Mutation`. |
| `@SchemaMapping(typeName, field)` | Field | Un campo puntual de un tipo. |
| `@BatchMapping` | Field (batch) | Un campo, para todos los padres de una vez. |

```java
@Controller
public class PostController {

    @QueryMapping
    public List<Post> recentPosts(@Argument int count, @Argument int offset) {
        return ...;
    }

    @SchemaMapping(typeName = "Post", field = "author")
    public Author getAuthor(Post post) {
        return ...;
    }

    @MutationMapping
    public Post createPost(@Argument String title, @Argument String text,
                           @Argument String category, @Argument String authorId) {
        Post post = new Post(UUID.randomUUID().toString(), title, text, category, authorId);
        postDao.savePost(post);
        return post;
    }
}
```

> [!NOTE] Field resolvers
> Permiten decir "**este campo de este tipo** se resuelve con este método". El `Post` puede venir de una base y su `author` de otro servicio: la respuesta se compone con información de diversas fuentes. Y si el cliente no pide `author`, el resolver ni se ejecuta.

### Batch loaders y el problema N + 1

> [!IMPORTANT] Problema de las N + 1 queries
> Pedís un listado de $N$ elementos y, para resolver parte de su contenido, hay que hacer **una query más por cada elemento**. Con un field resolver de `author`, se llama una vez por cada post: $1$ query para los posts $+ \; N$ para los autores. Comportamiento lineal, no óptimo.

Con un **batch loader** registrado en el campo, GraphQL **acumula** los pedidos a ese campo (generalmente por id) y en vez de $N$ llamados con 1 elemento hace **1 llamado con los $N$ elementos** en una lista. Quien implementa puede resolverlo eficientemente (ej. un `WHERE id IN (...)`).

En Spring: anotar con `@BatchMapping`; el parámetro es la lista de "padres" y la respuesta un `Mono<Map<Padre, Hijo>>`:

```java
@BatchMapping
public Mono<Map<Post, Author>> author(List<Post> posts) {
    Map<Post, Author> authors = new HashMap<>();
    // una sola busqueda con todos los authorId
    return Mono.just(authors);
}
```

> [!EXAMPLE]+ `recentPosts(count: 3)` pidiendo el `author`
> Posts devueltos: `P1` (autor `A1`), `P2` (autor `A2`), `P3` (autor `A1`).
>
> **Con `@SchemaMapping`:**
> 1. `recentPosts` → 1 llamado, devuelve `[P1, P2, P3]`.
> 2. `getAuthor(P1)` → busca `A1`.
> 3. `getAuthor(P2)` → busca `A2`.
> 4. `getAuthor(P3)` → busca `A1` **de nuevo**.
>
> Total: $1 + 3 = 4$ llamados. Con 100 posts serían 101.
>
> **Con `@BatchMapping`:**
> 1. `recentPosts` → 1 llamado, devuelve `[P1, P2, P3]`.
> 2. GraphQL junta los tres padres y llama una vez a `author([P1, P2, P3])`.
> 3. La implementación busca `{A1, A2}` de una sola vez y devuelve `{P1→A1, P2→A2, P3→A1}`.
>
> Total: $2$ llamados, sin importar $N$.

---

## APIs de eventos

> [!IMPORTANT] Event driven
> En las APIs **event first** o **event driven**, la comunicación de algo relevante al cliente se **inicia en el momento en que se produce el evento**, no cuando el cliente pregunta.

| Tecnología | Cómo funciona | Dirección | Ejemplos de uso |
|---|---|---|---|
| **Webhooks** | Un "callback" HTTP automatizado: cuando pasa algo en el sistema origen, este hace un request a la URL que el cliente configuró. | Servidor → cliente (el cliente expone un endpoint) | Alertas de pago, notificaciones de Git, CI/CD. |
| **WebSockets** | El cliente abre una conexión "permanente" y por ella hay comunicación **full duplex**. | Bidireccional | Chat, juegos multijugador, herramientas colaborativas tipo Figma. Una implementación de **Subscriptions de GraphQL** usa WebSockets. |
| **Server-Sent Events (SSE)** | El cliente abre una conexión permanente y el servidor le manda actualizaciones en tiempo real. Liviano, se **reconecta solo**. | **Unidireccional** servidor → cliente | Tableros en vivo, cotizaciones, feeds de noticias. |
| **Pub/Sub vía mensajería** | Un **broker** intermedio: el **productor** escribe en la cola y se despreocupa; el mensaje queda guardado hasta que un **consumidor** lo procesa. | Asincrónica, desacoplada | Kafka, RabbitMQ. |

> [!TIP] Cómo distinguirlos
> - ¿El cliente tiene que exponer una URL? → **webhook**.
> - ¿Los dos lados hablan todo el tiempo? → **WebSockets**.
> - ¿Sólo el servidor empuja datos por una conexión abierta? → **SSE**.
> - ¿Querés que si un servicio se cae **no se pierda la data**? → **pub/sub con broker**: es el único que desacopla totalmente (el mensaje persiste en la cola).

> [!NOTE] Relación con gRPC
> SSE se parece a un [[2_Cliente-Servidor#gRPC|gRPC]] con *server streaming* y WebSockets a uno *bidirectional streaming*: las mismas ideas de patrones de comunicación, con otras tecnologías.

---

## Comparativa

| | Tira de bytes | SOAP | REST | GraphQL | gRPC |
|---|---|---|---|---|---|
| Modelo | Opcodes | Operaciones (RPC) | **Recursos** (sustantivos) | Esquema tipado, estilo RPC | Operaciones (RPC) |
| Contrato | Documentación | **WSDL** + XSD | No hay estándar (doc aparte) | **Esquema** GraphQL | `.proto` |
| Formato | Binario ad hoc | **XML** | JSON, XML, binario… | JSON | Protobuf (binario) |
| Transporte | Socket TCP | HTTP, SMTP, JMS… | HTTP | Agnóstico (típicamente HTTP) | HTTP/2 |
| Endpoints | Un socket | Una URL por servicio | **Una URI por recurso** | **Una sola URL** (`POST`) | Un servicio con métodos |
| Quién elige los campos | Servidor | Servidor | Servidor | **Cliente** | Servidor |
| Fuerte en | Legacy | B2B, finanzas, seguridad | Web, B2C, escalabilidad | Clientes con necesidades distintas, menos round trips | Comunicación interna entre servicios |

> [!WARNING] Ojo
> La columna de gRPC y la fila "Fuerte en" de gRPC son una síntesis mía a partir de [[2_Cliente-Servidor]], no una tabla de la cátedra.

---

## Takeaways

- Existen muchas maneras de definir APIs.
- Las dos corrientes más grandes son **Request/Response** y **Eventos**.
- Dentro de cada una hay diversas tecnologías, modos y frameworks para elegir según las necesidades del sistema.
- Muchos de los elementos que discutimos con [[2_Cliente-Servidor#gRPC|gRPC]] (contrato, stubs, serialización, manejo de errores, patrones de comunicación) aplican para entender cualquiera de ellas.
