---
materia: pod
tipo: apuntes
---

# Sistemas distribuidos

El esquema [[2_Cliente-Servidor#El modelo Cliente / Servidor|cliente/servidor]] con solo dos componentes es rarísimo en la práctica. Lo normal es que la persistencia esté separada, y como mínimo hay **una base de datos** y **archivos de logs**. Esta clase arranca por ahí: qué pasa cuando esa base relacional deja de alcanzar, y por qué terminamos escalando de manera horizontal.

---

## Problemas

Tengo una db relacional y tengo la suerte de tener muchos usuarios. Ahora la db no da a basto. Empiezo a denormalizar para minimizar joins. Las db relacionales son muy rígidas y "difíciles de escalar".

### RDBMS

> [!IMPORTANT] RDBMS
> Un **RDBMS** es un programa/servicio que permite crear, modificar y administrar bases de datos relacionales y la información contenida en las mismas.

Beneficios:

- **Modelo relacional**: la información se guarda como un conjunto de relaciones (tablas). Cada fila es un registro, cada columna un atributo. La **primary key** identifica la fila (puede ser compuesta), las relaciones entre tablas se dan por **foreign keys**, y las constraints validan unicidad y relaciones.
- **Esquema**: la implementación física del modelo. Define tipos por atributo, constraints (unicidad, consistencia), optimizaciones de guardado e **índices** para buscar y filtrar rápido.
- **Transacciones ACID**: varias operaciones como una sola operación lógica.

| Propiedad | Qué garantiza |
|---|---|
| **Atomicidad** | La transacción es todo o nada. No se persisten cambios por la mitad. |
| **Consistencia** | Todo lo escrito es válido según todas las restricciones definidas. |
| **Aislamiento** | Ninguna transacción se ve afectada por las que corren concurrentemente. |
| **Durabilidad** | Una vez escrito, es permanente y las lecturas posteriores devuelven el dato nuevo. |

**SQL** es el lenguaje de dominio específico para manejar y consultar RDBMS. Basado en el álgebra relacional, tiene tres sublenguajes: **DDL** (definición), **DML** (manipulación) y **DCL** (control), más el lenguaje de consulta y bloques procedurales. Fue de los primeros lenguajes comerciales para el modelo relacional de Codd y es estándar ANSI/ISO.

```sql
-- DDL
CREATE TABLE Book(
  isbn INTEGER,
  title VARCHAR(50),
  publication DATE NOT NULL,
  PRIMARY KEY (isbn)
);
ALTER TABLE Book ADD type VARCHAR(3);

-- DML
INSERT INTO Book (isbn, title) VALUES (9781501142970, 'IT');
DELETE FROM Book WHERE isbn = 9781501142970;

-- Query
SELECT Book.title, count(*) AS Authors
FROM Book JOIN Book_author ON Book.isbn = Book_author.isbn
GROUP BY Book.title;
```

Durante décadas el cliente/servidor + RDBMS fue "la norma": mientras los requerimientos se mantienen en el esquema de *book keeping* es "mágico" y *one tool fits all*. El problema es que aparecieron necesidades nuevas y se forzó a este modelo a resolverlas.

### Problema 1: crecimiento orgánico del sistema

| Etapa | Síntoma | "Solución" | Costo |
|---|---|---|---|
| Lanzamiento | Servidor + DB externa con esquema normalizado | — | — |
| Se vuelve popular | Muchas **lecturas** | Memcache para las consultas comunes, o réplicas | Las lecturas dejan de ser ACID, hay que expirar el cache; las réplicas requieren mantenimiento e infra |
| Más popular todavía | Muchas **escrituras** | Escalar la DB verticalmente | Plata en mejores componentes |
| Features nuevas | Demasiados joins, lecturas lentas | Desnormalizar | Mantener la redundancia, más capacidad, updates más complejos |
| Mucho volumen | Escrituras cada vez más lentas | Sacar triggers e índices secundarios | Se pierden las garantías que daba el esquema |

> [!NOTE] ¿Cuál es el problema de los RDBMS?
> - Son demasiado rígidos para los escenarios nuevos; la idea de una herramienta para todo ya no aplica.
> - No son ideales para data no estructurada.
> - Difíciles de escalar y de mantener con millones de registros.
> - Sus estructuras de datos están optimizadas para lecturas usando poca memoria.

### Problema 2: BI y queries analíticas

Surgen nuevas preguntas: para potenciar el negocio quiero tener data, entonces empiezo a hacer queries analíticas. Con la info de la base relacional (y otras fuentes) se puede generar "nuevo conocimiento" y de ahí nuevas decisiones y acciones.

![](attachments/Pasted%20image%2020261007194653.png)

Las preguntas suelen ser "¿cuántos…?", "¿cuánto cuesta…?", "¿cuál es el promedio…?", calculadas combinando un conjunto de **dimensiones**. Vienen estos tipos y te hacen join de 70 tablas, muchos group by, muchos promedios y la db no da a basto. Se resuelven con queries de agregación, y en un RDBMS la performance sufre porque:

- El modelo por fila no es el óptimo para agregar.
- Agregar por dimensiones no indexadas obliga a hacer full scans.
- Las operaciones tardan más, y si hay locks (de tabla o de fila) se mantienen más tiempo.
- Todo esto afecta también a las queries que corren concurrentemente.

**OLAP** (*Online Analytical Processing*): software cuyo modelo mantiene la información por dimensiones y permite agregar fácil. Conceptualmente se arma un **cubo** con la información dividida según la granularidad de las dimensiones.

![](attachments/Pasted%20image%2020261027115901.png)

| Acción | Qué hace (según la slide) |
|---|---|
| **Drill down** | agregar |
| **Roll up** | desagregar |
| **Slice** | fijar una dimensión |
| **Dice** | fijar más de una dimensión |
| **Pivot** | rotar la selección de dimensiones |

> [!WARNING] Drill down / Roll up
> La slide los pone así, pero en la bibliografía general (fuera de la cátedra) es al revés: **drill down** baja a más detalle (desagrega) y **roll up** sube de nivel (agrega). El dibujo de la misma slide muestra el roll up como el cubo grande, o sea agregando. Si lo preguntan, conviene aclarar a qué te referís.

Para llevar la info de los RDBMS a los modelos OLAP se usa un **Data Warehouse**: un pipeline de **ETL** (Extracción, Transformación, Carga), posiblemente con stores intermedios.

![](attachments/Pasted%20image%2020261027115902.png)

Características del modelo DW/OLAP:

- El ETL corre en **batch**, periódicamente, y tarda mucho.
- Por espacio no se guarda la historia granular: se agrega al mínimo necesario para el OLAP.
- Por eso los modelos y preguntas son predefinidos y rígidos. Si quiero hacer una pregunta nueva sobre la data del año pasado, no puedo.

### Problema 3: Big Data

También está el problema del **Big Data**. La frase aparece en 1997 en un paper de la NASA (Cox y Ellsworth): cuando los datasets no entran en memoria, ni siquiera en disco local, la solución típica es conseguir más recursos.

3 V del big data (lo pregunta en el final), definidas por Doug Laney (Gartner) en 2001:

| V | Qué es |
|---|---|
| **Volumen** | La cantidad de datos crece de manera geométrica. |
| **Velocidad** | La velocidad con que se generan los datos crece. |
| **Variedad** | Aparecen tipos de datos nuevos que no cumplen una estructura fija. |

![](attachments/Pasted%20image%2020261007195844.png)

Variedad en detalle:

| Tipo | Descripción | Ejemplos |
|---|---|---|
| Estructurados | Esquema fijo, se replica fácil en un RDBMS | Tablas |
| Semi estructurados | Text-based, con un "esquema" que se puede cumplir o no del todo | JSON, XML |
| Desestructurados | Sin esquema aparente, puede no ser textual | Mails, PDFs, imágenes |

Las fuentes ya no son solo los datos organizacionales (stock, ventas, transacciones): también **máquinas** (sensores, logs, IoT) y **personas** (redes sociales, fotos, videos, comentarios).

Por definición, Big Data excede una máquina, y hay problemas en cada paso de la cadena:

- **Adquisición**: soportar volumen y velocidad para la variedad de fuentes, probablemente buffereando.
- **Procesamiento**: en un tiempo útil para la organización, quizás particionando por prioridad y complejidad.
- **Guardado**: estrategias acordes a los datos y a las necesidades de respuesta.
- **Vista**: mostrar la información de forma útil.

---

## Sistemas distribuidos

Todos estos problemas llevan a aceptar que el esquema base + server y el escalamiento "poniendo fierros más grandes" (vertical) tiene un límite. Vamos a escalar de manera **horizontal**.

> [!IMPORTANT] Sistema distribuido
> Es un sistema computacional cuyos componentes están distribuidos en diferentes computadoras (o elementos computacionales), que se conectan y comunican por medio de mensajes de una red. Este sistema se presenta ante sus usuarios como un único sistema coherente.

**Objetivo**: los componentes dividen una tarea coordinando sus recursos para completarla de manera más eficiente que en una sola computadora.

Se pueden distribuir en 3 áreas:

| Tipo de tarea | Para qué |
|---|---|
| **Almacenamiento** | Guardar elementos, o cantidades de elementos, que no entrarían en un nodo |
| **Procesamiento** | Subdividir una tarea y obtener resultados parciales de cada subset |
| **Responsabilidades** | Dividir una tarea en partes "lógicas", probablemente con sentido semántico independiente |

### Características

- Hay **más de un elemento computacional**, llamado **nodo** (computadoras, VMs, dispositivos, etc.).
- Cada nodo es **autónomo** y **débilmente acoplado**: tiene todo para operar por sí solo.
- Cada proceso en un nodo tiene **su propio espacio de direccionamiento**.
- Las computadoras se comunican por una red.
- Cada nodo ofrece sus recursos a través de **servicios**, a otros nodos o a clientes finales.

### Problemas

Fallas en:

- Algún componente lógico: bugs, falta de memoria, etc.
- Los nodos: hardware o recursos.
- La comunicación por red: los nodos andan pero la comunicación entre ellos está pateada.
- La coordinación de tareas: se pierden mensajes, se distribuyen mal las tareas.

### Modelo de referencia

La ISO definió el **RM-ODP** (*Reference Model for Open Distributed Processing*): una serie de conceptos y buenas prácticas, la mayoría preexistentes, para estandarizar el desarrollo de aplicaciones distribuidas. Es un estándar que nadie sigue.

Sus 4 elementos fundamentales:

1. Se especifica con **modelado de objetos** (análisis y diseño OO, porque es lo más natural para describir elementos independientes que interactúan).
2. Se especifica desde **viewpoints** separados pero interrelacionados.
3. Tiene que proveer una serie de **transparencias**.
4. Tiene que tener un framework para medir la **conformidad** (pruebas manuales/automáticas o revisión de requisitos).

Los **viewpoints** existen porque una única descripción del sistema sería larga e inútil. Cada uno apunta a un objetivo y una audiencia; la forma (texto, diagramas, UML) no está fijada.

| Viewpoint | Describe |
|---|---|
| Enterprise | Propósito |
| Information | Semántica |
| Computational | Descomposición funcional |
| Engineering | Mecanismos de distribución |
| Technology | Selección de tecnologías |

Una parte sí está buena y la toma en el final, que es **Transparencia**.

> [!IMPORTANT] Transparencia
> Una transparencia se "cumple" si el problema que ataca queda oculto para el cliente del sistema. Si se cumplieran todas (un ideal difícil), quedaría oculto que hay comunicación entre sistemas distribuidos.

| Transparencia | El usuario no debe notar… |
|---|---|
| **Acceso** | …diferencias al acceder a los componentes: acceso uniforme, se esconde la invocación remota y la heterogeneidad de representación |
| **Fallas** | …errores, reintentos ni recoveries |
| **Migración** | …que un objeto cambió de ubicación **entre** usos |
| **Reubicación** | …que un objeto cambió de ubicación **mientras** se usa |
| **Ubicación** | …la ubicación física de los componentes: acceso lógico independiente de dónde está el servicio |
| **Transacción** | …que varios objetos interactúan para dar una operación transaccional |
| **Replicación** | …que un componente está replicado: todas las réplicas parecen el mismo |

> [!TIP] Migración vs reubicación
> La diferencia es solo el momento: migración es entre usos, reubicación es durante el uso. Es la trampa típica de la pregunta.

Escalando horizontalmente queremos mantener las transparencias, sobre todo la de **acceso**: usar un servicio sin saber (o sabiendo lo mínimo) que es distribuido. Y hablamos de "sistemas" en plural porque no es una sola aplicación distribuida (una base NoSQL), sino un conjunto de sistemas que interactúan para dar un servicio sin que el cliente conozca sus partes. Ejemplos de la clase: microservicios de LinkedIn pre Kafka, el stack de Hadoop, captura y procesamiento analítico con Twitter como fuente, el stack de Flowics.

---

## Store distribuido

El **almacenamiento distribuido** resuelve guardar información que por su volumen no entra en un nodo, porque:

- un elemento solo no entra en un nodo, o
- la suma de los tamaños de todos los elementos supera a los nodos.

Hay stores distribuidos de distintos tipos según la abstracción que guardan: **file systems**, **bases de datos** y **colecciones de objetos**.

### Particionado

El primer problema es cómo distribuir la info en el cluster: lo lógico es dividir la tarea de almacenar entre los nodos, o sea **particionar**.

| A favor | En contra |
|---|---|
| Escritura más rápida y eficiente | Hay que coordinar el cluster para saber dónde va cada partición y a qué partición va cada valor |
| Lookup del dato más rápido | Con una mala estrategia los datos quedan **sesgados** en uno o más servidores y la carga se vuelve ineficiente |
![](attachments/Pasted%20image%2020261027163103.png)
### Replicación

Tener una sola copia de cada partición es peligroso: si se cae un nodo, esa partición deja de existir. Por eso se tienen varias copias de cada partición en el cluster, o sea se **replican**. Según el sistema puede haber más de una réplica.
![](attachments/Pasted%20image%2020261027163131.png)
### Consistencia

Con particionado y replicación hay que decidir **cuándo considero el dato escrito** (y cuándo se puede leer):

1. Cuando se escribió en la partición principal.
2. Cuando hay al menos una réplica escrita + la partición.
3. Cuando están todas las réplicas escritas.

Y además: ¿se puede leer o escribir en las réplicas? Cualquiera es válida según el sistema. Si necesito mucha capacidad de lectura y data consistente (tipo ACID), va la 3; en otro caso la 1 y la 2 son válidas.

Igual que se configuran réplicas y particiones, hay dos factores de consistencia:

> [!IMPORTANT] Consistencia en escritura y en lectura
> - **Consistencia en escritura**: cuántas copias tienen que reportar la escritura hecha antes de avisarle al cliente que terminó.
> - **Consistencia en lectura**: cuántas particiones tienen que reportar el mismo valor antes de devolverlo como cierto.

### Modelos de consistencia

Se usan en stores distribuidos y en memoria distribuida compartida (**DSM**). Cada modelo es un conjunto de reglas de lectura/escritura que, si el programador las respeta, asegura que las lecturas sean consistentes con las escrituras. Los dos más comunes en store distribuido:

| Modelo | Definición |
|---|---|
| **Estricto** | El ideal: toda escritura está inmediatamente disponible para lectura para todos, y se respeta el orden global de las operaciones. |
| **Eventual** | Después de una escritura, y sin escrituras nuevas, pasado cierto tiempo todas las lecturas reflejan el valor escrito. |

### Elasticidad y tolerancia a fallos

> [!IMPORTANT] Elasticidad
> Capacidad de un cluster de modificar la cantidad de nodos que lo componen, y que estos participen de la operación, sin que el cluster quede offline completamente.

En un cluster de storage, cuando entra un nodo:

1. El cluster detecta al nodo nuevo.
2. Le asigna particiones primarias y de backup.
3. Coordina la transferencia de esas particiones.
4. Lo marca disponible.

**Tolerancia a fallos**: si se cae un nodo, sus particiones (primarias y de backup) se reasignan. Todo esto requiere coordinación; en general hay un nodo **master/coordinador** con o sin redundancia. Lo que varía es si el master es un nodo especial o si cualquier nodo puede asumir el rol.

---

## HDFS

Hadoop tiene tres componentes:

| Componente | Rol |
|---|---|
| **HDFS** | File system distribuido |
| **YARN** | Manejo de recursos y tareas distribuidas |
| **MapReduce** | Procesamiento distribuido (ver [[#MapReduce]]) |

**HDFS** (*Hadoop Distributed File System*):

- Modelado a partir del paper de **Google File System**.
- Optimizado para **high throughput**: anda mejor leyendo y escribiendo archivos muy grandes (gigas o más).
- Usa bloques muy grandes y replicación para **fault tolerance**.
- Diseñado para **commodity hardware** (sin RAID, NFS, etc.).

![](attachments/Pasted%20image%2020261027120001.png)

| Pieza | Qué hace |
|---|---|
| **Bloque** | Los archivos se parten en bloques de tamaño configurable (default 128 MB). Cada bloque se replica en distintos nodos (cantidad configurable). Un archivo más chico que un bloque ocupa solo su tamaño real. Al tener tamaño máximo, un nodo siempre puede manejar un bloque. |
| **Name Node** | Administra el namespace y regula el acceso de los clientes. Mantiene la metadata de carpetas y archivos (permisos, tamaños, ubicaciones, estructura tipo Linux), relaciona archivos con bloques, conoce el estado de los Data Nodes y responde búsquedas y escrituras de bloques. Persiste en disco con **FsImage** (snapshot periódico) y un **transaction log** (escrituras hasta actualizar la FsImage). |
| **Data Node** | Guarda los bloques. Se reporta al Name Node periódicamente (default cada 3 s) para avisar que vive y qué bloques tiene. Se comunica con otros Data Nodes para replicar. |

> [!WARNING] Problemas de HDFS
> - El **Name Node es un SPOF** (*single point of failure*). Primera aproximación con "federación" en 2.X; solución definitiva con HA en 3.X.
> - **Acceso random** malo: está optimizado para leer por bloque, leer un byte del medio no es su uso. Para eso se crearon bases como **HBase** encima de HDFS.
> - **Muchos archivos chicos** degradan la performance: conviene mergearlos o particionar distinto.

```bash
hadoop fs -get hdfs://nn.example.com/user/hadoop/file localfile   # HDFS -> local
hadoop fs -put -f localfile1 localfile2 /user/hadoop/hadoopdir      # local -> HDFS
hadoop fs -ls /user/hadoop/file1                                    # lista hijos directos
hadoop fs -rm hdfs://nn.example.com/file                            # borra
hadoop fs -tail pathname                                            # último KB del archivo
hadoop fs -getmerge -nl /src /opt/output.txt                        # concatena src en un archivo local
```

---

## Hazelcast

> [!IMPORTANT] Hazelcast
> Grid de datos **en memoria**, distribuido y open source. Hecho en Java con mínimas dependencias; puede correr **embebido** o en arquitectura **client-server**.

Redefine estructuras de datos respetando las interfaces conocidas de Java: `IMap`, `IList`, `IQueue`, `ITopic`, etc. Son las que hay que usar para que la data sea distribuida: si uso las colecciones tradicionales, solo las ve el nodo que las declara.

Casos de uso: in-memory data grid, caching, microservicios, web session clustering, messaging, in-memory NoSQL, application scaling, procesamiento distribuido.

### Tipos de nodo

![](attachments/Pasted%20image%2020261027120002.png)

| Tipo | Guarda datos | Para qué |
|---|---|---|
| **Node Member** (default) | Sí | Tiene datos distribuidos y ejecuta tareas distribuidas |
| **Lite Member** | No | Lanzar tareas y otras acciones (ej. agregar listeners) |
| **Native Client** | No, y **no es parte del cluster** | Conectarse al cluster en modo cliente/servidor para poner datos o pedir ejecuciones |

### Coordinación

El cluster es **peer-to-peer**: no hay master fijo, cualquier nodo puede asumir el control, lo que elimina el SPOF (comparar con el Name Node de [[#HDFS]]). Como coordinador se elige al nodo **más antiguo** del cluster.

El coordinador tiene la **tabla de particiones** (qué nodo es owner/backup de qué partición) y la distribuye entre todos. Cuando llega o se va un nodo, la actualiza y la redistribuye. Si se cae el coordinador, asume el siguiente más viejo y se recalcula la tabla.

### Particionado

La estructura global se divide en **271 particiones** repartidas entre los nodos, con réplicas (backups). Aunque los datos estén distribuidos, **cualquier nodo accede a la totalidad**: si no lo tiene, se lo pide a otro.

> [!IMPORTANT] Asignación a particiones
> Para saber qué partición es owner de un objeto (la cátedra lo llama "Consistent Hashing"):
> $$\text{partición}(k) = \text{hash}(k) \bmod 271$$

> [!NOTE]
> Fuera de la cátedra: la fórmula en sí es un hash módulo fijo. Lo que evita re-hashear todo cuando entran o salen nodos es que **la cantidad de particiones no cambia (271)**: lo que se mueve entre nodos son particiones enteras, no claves sueltas.

> [!EXAMPLE]+ Inserción en el mapa `materias`
> ```java
> Map<String, String> materias = hz.getMap("materias");
> materias.put("72.02", "Embedded Systems");
> materias.put("72.23", "Compiladores");
> materias.put("72.14", "POD");
> ```
> 1. `hash("72.02") % 271 = 142` → va a la partición 142.
> 2. `hash("72.23") % 271 = 23` → va a la partición 23.
> 3. `hash("72.14") % 271 = 142` → también a la 142, junto con `72.02`.
>
> Cada partición vive (como owner) en algún nodo según la tabla del coordinador, y tiene su réplica en otro nodo.

> [!WARNING] Ojo con la slide
> El texto de la slide da 142 para `72.02` y 23 para `72.23`, pero el dibujo de la misma slide (y el de migración) pone `72.02` en la 23 y `72.23`/`72.14` en la 142. Los números son ilustrativos: con el `String.hashCode()` real de Java salen otras particiones (204, 267 y 237). Lo que importa es la mecánica.

### Réplicas

| Pregunta | Respuesta |
|---|---|
| ¿Con un solo nodo hay backup? | No |
| 2 nodos con backup, ¿cuántas particiones por nodo? | Aprox. $271/2$ primarias y $271/2$ backup (del otro nodo) |
| 3 nodos | Aprox. $271/3$ primarias y $271/3$ backup (de los otros nodos) |

- Las réplicas se actualizan **sincrónicamente** con la partición principal.
- Por default hay **una** réplica, porque el sincronismo cuesta performance; se puede subir o bajar.
- Si se cae el owner de una partición, el nodo con la réplica pasa a ser owner y otro nodo pasa a tener la réplica.
- Al entrar o salir nodos se **migran** particiones de modo que principal y réplica **nunca** queden en el mismo nodo.

> [!EXAMPLE]+ Migración al agregar un nodo
> Partiendo del ejemplo anterior con un nodo, se agrega un segundo nodo. El cluster le pasa la mitad de las particiones como owner y el resto como réplica:
>
> ![](attachments/Pasted%20image%2020261027120003.png)
>
> - Nodo viejo: sigue owner de la partición 23, y ahora tiene la **réplica** de la 142 y la 270.
> - Nodo nuevo: es owner de la 142 y la 270, y tiene la réplica de la 0 y la 23.

**Réplicas asincrónicas**: para que la escritura responda más rápido se puede configurar el backup asincrónico. Ahí se garantiza solo **consistencia eventual**: es más performante pero se puede leer un dato **stale** (desactualizado).

### Configuración

Se puede configurar declarativamente (`hazelcast.xml`), programáticamente desde Java, con propiedades del sistema o con contexto de Spring.

**Member discovery**: multicast, TCP/IP, frameworks como Zookeeper o Consul, o clouds como AWS/Azure.

```xml
<multicast enabled="false"/>
<tcp-ip enabled="true">
    <member>machine1</member>
    <member>machine3:5799</member>
    <member>192.168.1.0-7</member>
</tcp-ip>
```

> [!WARNING]
> Multicast no se recomienda en producción: va por UDP, puede ser problemático y en general está bloqueado.

**Grupos**: con la misma configuración de discovery se pueden tener clusters independientes definiendo un grupo con nombre y password. Da cierta "seguridad" para unirse.

```xml
<group>
    <name>dev</name>
    <password>dev-pass</password>
</group>
```

**Colecciones**: cada colección se configura por nombre.

```xml
<map name="default">
    <backup-count>1</backup-count>
    <async-backup-count>1</async-backup-count>
    <read-backup-data>false</read-backup-data>
</map>
<map name="materias">
    <backup-count>2</backup-count>
    <async-backup-count>1</async-backup-count>
    <read-backup-data>true</read-backup-data>
</map>
```

`default` tiene 1 backup sincrónico, 1 asincrónico y no permite leer de los backups; `materias` tiene 2 sincrónicos, 1 asincrónico y sí permite leer de los backups.

### Split brain

> [!IMPORTANT] Split brain
> Por problemas de comunicación el cluster se subdivide en dos partes autónomas. Es grave porque las dos siguen funcionando, pueden recibir updates que conflictúan (dos valores para la misma key) y, al reconectarse, hay que mergear.

> [!EXAMPLE]+ Cómo lo resuelve Hazelcast
> 1. Elige un líder entre los dos líderes de los clusters (el más viejo).
> 2. Decide qué cluster se mergea (el más chico, o según una función).
> 3. Por cada miembro del cluster a mergear:
>    1. Pausa su operación (no acepta lecturas ni escrituras).
>    2. Cierra la conexión a su cluster.
>    3. Se une al otro cluster.
>    4. Pide mergear los valores de los **mapas** de los que es dueño, según la política configurada. Para otras colecciones (listas, colas) no mergea: elige una de las dos.
>    5. Reinicia su operación.

Políticas de merge de mapas (`<merge-policy>`):

| Política | Gana… |
|---|---|
| `PassThroughMergePolicy` | la entrada que viene se agrega si no hay una para esa key |
| `PutIfAbsentMapMergePolicy` | se agrega solo si la entrada no existe en el cluster |
| `HigherHitsMapMergePolicy` | la entrada con más hits |
| `LatestUpdateMapMergePolicy` | la última actualizada |

### Hazelcast en Java

Para correr un nodo: agregar la dependencia, tener el XML de configuración (o configurar por código) e instanciar.

```java
// node member
HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
// native client
HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);
```

Configuración por código: tanto server (`Config`) como client (`ClientConfig`) reciben un objeto al que se le agregan configuraciones con setters (seguridad, conexión, autenticación, discovery, red, colecciones).

```java
// Server
Config config = new Config();
GroupConfig groupConfig = new GroupConfig().setName("cluster").setPassword("cluster-pass");
config.setGroupConfig(groupConfig);

NetworkConfig networkConfig = new NetworkConfig();
JoinConfig join = networkConfig.getJoin();
join.getMulticastConfig().setEnabled(false);
join.getTcpIpConfig()                       // listar los nodos: más seguro que multicast
    .addMember("10.45.67.32").addMember("10.45.67.100")
    .setEnabled(true);
networkConfig.setInterfaces(new InterfacesConfig()
    .setInterfaces(List.of("192.168.1.*")).setEnabled(true));
config.setNetworkConfig(networkConfig);

HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
// ...
hz.shutdown();
```

```java
// Client
ClientConfig clientConfig = new ClientConfig();
clientConfig.setGroupConfig(new GroupConfig().setName("cluster").setPassword("cluster-pass"));
ClientNetworkConfig clientNetworkConfig = new ClientNetworkConfig();
clientNetworkConfig.addAddress("192.168.1.51:5701");
clientConfig.setNetworkConfig(clientNetworkConfig);

HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);
// ...
HazelcastClient.shutdownAll();
```

Hay además una **Management Center** (webapp) para monitorear el cluster en tiempo real; se configura con `ManagementCenterConfig` y tiene que coincidir en versión con `hazelcast-all`.

### Serialización

Todo lo que se mete en las colecciones viaja por red entre nodos, así que tiene que poder **serializarse** (ver [[2_Cliente-Servidor#Marshalización / Serialización]]). Opciones:

- `Serializable` de Java.
- `DataSerializable` o `IdentifiedDataSerializable` de Hazelcast (**las recomendadas**: usan Java NIO y son más eficientes).
- `Portable` de Hazelcast o `Externalizable` de Java.
- Serializadores custom.

```java
public class Employee implements DataSerializable {
    private String firstName, lastName;
    private int age;
    private Address address;

    public Employee() {}

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(firstName);
        out.writeUTF(lastName);
        out.writeInt(age);
        address.writeData(out);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        firstName = in.readUTF();
        lastName = in.readUTF();
        age = in.readInt();
        address = new Address();
        address.readData(in); // Address también es DataSerializable
    }
}
```

> [!WARNING]
> `readData` tiene que leer los campos **en el mismo orden** en que `writeData` los escribió: no hay nombres de campo en el stream. Y hace falta el constructor vacío.

### Tipos distribuidos

Lock, Semaphore, AtomicLong, AtomicReference, IdGenerator, CountDownLatch, Map, MultiMap, Cache, Queue, Ringbuffer, Set, List, Replicated Map, Cardinality Estimator, Topic.

Si tienen equivalente en el lenguaje local, intentan mantener la semántica: `IMap<K, V>` extiende `ConcurrentMap<K, V>` (ver [[1_Programacion-concurrente#Concurrent collections]]), así que se usa como si "no fuera distribuido".

```java
IMap<String, String> map = hz.getMap("my-distributed-map");
map.put("key", "value");
map.putIfAbsent("somekey", "somevalue");
map.replace("key", "value", "newvalue");
```

**Entry processor**: procesamiento atómico sobre una entrada del mapa, ejecutado donde está el dato.

```java
IMap<String, Integer> map = hz.getMap("map");
map.put("key", 0);
map.executeOnKey("key", new IncEntryProcessor()); // map.get("key") == 1

private static class IncEntryProcessor extends AbstractEntryProcessor<String, Integer> {
    @Override
    public Object process(Map.Entry<String, Integer> entry) {
        entry.setValue(entry.getValue() + 1);
        return null;
    }
}
```

**Queries** "a la SQL" sobre el mapa:

```java
IMap<String, User> users = hz.getMap("users");
Predicate sqlQuery = new SqlPredicate("active AND age BETWEEN 18 AND 21");
Predicate criteriaQuery = Predicates.and(
        Predicates.equal("active", true),
        Predicates.between("age", 18, 21));
Collection<User> result = users.values(sqlQuery);
```

**Queue** (como una `BlockingQueue` local), **Topic** (pub/sub) y **Locks** distribuidos (como los de [[1_Programacion-concurrente#High level locks]]):

```java
BlockingQueue<String> queue = hz.getQueue("my-distributed-queue");
queue.offer("item");
queue.poll(5, TimeUnit.SECONDS);

ITopic<String> topic = hz.getTopic("my-distributed-topic");
topic.addMessageListener(msg -> System.out.println("Got " + msg.getMessageObject()));
topic.publish("Hello to distributed world");

Lock lock = hz.getLock("testLock");
if (lock.tryLock(5000, TimeUnit.MILLISECONDS)) {
    try {
        // sección crítica
    } finally {
        lock.unlock();
    }
}

map.lock(key);          // lock sobre una key de un IMap
try { /* ... */ } finally { map.unlock(key); }
```

---

## MapReduce

> [!IMPORTANT] MapReduce
> Modelo de programación para el procesamiento de grandes volúmenes de datos de manera paralela y distribuida a partir de primitivas simples. Fue piedra fundacional de Big Data.

Sale del paper de Dean & Ghemawat (Google), a partir del trabajo para mejorar Page Rank. Se basa en:

- Las primitivas funcionales **map** y **reduce**, que le dan el nombre.
- Subdividir la data para procesarla distribuida (*divide & conquer*).
- **Data locality**: trabajar con la info local al nodo lo más posible, evitando moverla por la red.

Cada dato es un par **clave** (clasifica/identifica) y **valor** (contenido), y cada etapa es una función de pares en pares:

$$[k_2, v_2] = F(k_1, v_1)$$

### Etapas

![](attachments/Pasted%20image%2020261027120004.png)

| Etapa | Entrada → salida | Quién la hace |
|---|---|---|
| **Map** | $(k_1, v_1) \to (k_2, v_2)$ | Se programa |
| **Sort / Shuffle** | $(k_2, v_2) \to (k_2, [v_2, v_2, \dots])$ | El framework |
| **Reduce** | $(k_2, [v_2, \dots]) \to (k_3, v_3)$ | Se programa |

**Map** transforma la data inicial en lo útil para la operación final. Recibe un par y emite **0, 1 o más** pares. Puede:

- **Filtrar**: no emite registros innecesarios.
- **Proyectar**: no emite valores innecesarios.
- **Expandir**: agrega info de fuentes externas.
- **Multiplicar**: emite más de un valor (dado un país, un valor por provincia; dada una frase, cada palabra).

**Sort** agrupa por key todos los valores emitidos por los mappers para mandarlos a cada reducer. Es el punto complicado del framework: tiene que conservar los pares hasta que el reducer los consuma y transmitirlos por red al nodo que maneja esa clave.

**Reduce**: se crea (o reutiliza) un reducer **por cada clave** emitida; recibe todos los valores de todos los mappers para esa clave y los transforma en un valor final.

> [!EXAMPLE]+ Word Count, el "Hello World" de Big Data
> ![](attachments/Pasted%20image%2020261027120005.png)
>
> Entrada (`IMap<String, String>`): `Saturn → "Saturn is a planet"`, `Earth → "Earth is a planet"`, `Pluto → "Pluto is not a planet anymore"`.
>
> 1. **Map**: cada frase emite `(palabra, 1)` por palabra.
>    - Saturn: `(saturn,1) (is,1) (a,1) (planet,1)`
>    - Earth: `(earth,1) (is,1) (a,1) (planet,1)`
>    - Pluto: `(pluto,1) (is,1) (not,1) (a,1) (planet,1) (anymore,1)`
> 2. **Sort/Shuffle**: agrupa por clave.
>    - `is → [1,1,1]`, `a → [1,1,1]`, `planet → [1,1,1]`, `saturn → [1]`, `earth → [1]`, `pluto → [1]`, `not → [1]`, `anymore → [1]`
> 3. **Reduce**: suma cada lista.
>    - `is=3, a=3, planet=3, saturn=1, earth=1, pluto=1, not=1, anymore=1`

### MapReduce en Hazelcast

**Mapper**: el método `map` se llama por cada elemento con la clave, el valor y un `Context` para emitir. Si necesita acceder a otras cosas de Hazelcast puede implementar `HazelcastInstanceAware`.

```java
public interface Mapper<KeyIn, ValueIn, KeyOut, ValueOut> extends Serializable {
    void map(KeyIn key, ValueIn value, Context<KeyOut, ValueOut> context);
}

public class TokenizerMapper implements Mapper<String, String, String, Long> {
    private static final Long ONE = 1L;

    @Override
    public void map(String key, String value, Context<String, Long> context) {
        Pattern.compile("\\s+")
                .splitAsStream(value.toLowerCase())
                .forEach(word -> context.emit(word, ONE));
    }
}
```

- A cada mapper se le asigna una partición del nodo donde corre (data locality). Un nodo con varias particiones puede tener varias instancias de mapper, y cuando uno termina se le puede asignar otra partición.
- Por cómo está armado, el map es naturalmente **thread safe**.

**Reducer**: hacen falta dos clases, `ReducerFactory` y `Reducer`. El factory existe porque hay **un reducer por cada clave única** (la clave es el parámetro de `newReducer`).

```java
public interface ReducerFactory<KeyIn, ValueIn, ValueOut> extends Serializable {
    Reducer<ValueIn, ValueOut> newReducer(KeyIn key);
}

public abstract class Reducer<ValueIn, ValueOut> {
    public void beginReduce() {}
    public abstract void reduce(ValueIn value);
    public abstract ValueOut finalizeReduce();
}

public class WordCountReducerFactory implements ReducerFactory<String, Long, Long> {
    @Override
    public Reducer<Long, Long> newReducer(String key) {
        return new WordCountReducer();
    }

    private static class WordCountReducer extends Reducer<Long, Long> {
        private long sum;

        @Override
        public void reduce(Long value) { sum += value; }

        @Override
        public Long finalizeReduce() { return sum; }
    }
}
```

Ciclo de vida de un reducer:

1. Llega una clave sin reducer → el factory instancia uno nuevo.
2. Se llama `beginReduce` una vez.
3. Por cada valor de esa clave se llama `reduce`.
4. Cuando no hay más valores se llama `finalizeReduce` para obtener el resultado.

> [!WARNING] Reducers suspendidos
> Los valores llegan en chunks, así que un reducer puede quedar suspendido esperando más valores y probablemente se reanude **en otro thread**. Antes de la versión 3.3 no había garantías de visibilidad entre threads y había que declarar las variables de instancia `volatile` (ver [[1_Programacion-concurrente#Consistencia en memoria]]).

**JobTracker, KeyValueSource y Job**: la API es un DSL con patrón Builder.

```java
HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);
JobTracker jobTracker = hz.getJobTracker("word-count");

IMap<String, String> wordsIMap = hz.getMap("words");
KeyValueSource<String, String> source = KeyValueSource.fromMap(wordsIMap);
// En colecciones sin clave (IList, ISet...) la clave es el nombre de la colección
// KeyValueSource.fromList(hz.getList("my-list"));

Job<String, String> job = jobTracker.newJob(source);
ICompletableFuture<Map<String, Long>> future = job
        .mapper(new TokenizerMapper())
        .reducer(new WordCountReducerFactory())
        .submit();

// Sincrónico
Map<String, Long> result = future.get();

// Asincrónico
future.andThen(new ExecutionCallback<>() {
    @Override public void onResponse(Map<String, Long> response) { /* ... */ }
    @Override public void onFailure(Throwable t) { logger.error("Error: {}", t.getMessage()); }
});
```

El `ICompletableFuture` es la misma idea que el [[1_Programacion-concurrente#Future|Future]] de Java.

> [!WARNING] Ejecución en el cluster
> No hay carga dinámica de clases: **cada nodo** tiene que tener en el CLASSPATH el mapper, el reducer, las clases de keys/values y cualquier otra clase de apoyo. Y todos los nodos necesitan la misma configuración para formar el cluster. El cliente puede ser parte del cluster o no según cómo se instancie la `HazelcastInstance`.

### MapReduce vs SQL

| SQL | MapReduce |
|---|---|
| `SELECT` (proyección) | Mapper (y proyección definitiva en el Reducer) |
| `WHERE` | Mapper (filtrado) |
| `GROUP BY` | Sort / grouping / shuffling |
| Funciones de agregación | Reducer |
| `HAVING` | Reducer (filtrado) |

> [!WARNING]
> No hay **ordenamiento global** dentro del modelo, porque el reducer emite por clave. Eso (y las queries más complejas) se hace con post procesamiento o encadenando un segundo MapReduce sobre la salida del primero.

> [!TIP] Ejercicio 2 de la clase
> "Para cada letra, la palabra más larga que empieza con esa letra (empate: la menor alfabética)". El mapper filtra con `word.matches("[a-z]+")` y emite `(primeraLetra, palabra)`; el reducer se queda con la más larga, desempatando con `compareTo`.
