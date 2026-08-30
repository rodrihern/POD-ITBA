---
materia: pod
tipo: apuntes
---

# Programación concurrente

Más de una instrucción corriendo "al mismo tiempo". Por ejemplo usando *threads*.

Lógicamente todo es un tradeoff: es muy difícil testear cosas de concurrencia y hay veces que no se puede testear directamente (los bugs son no determinísticos).

> [!IMPORTANT] Programa vs. Proceso
> - **Programa** (o algoritmo): secuencia de instrucciones escritas para realizar una tarea específica. Es un elemento **estático**, reside en almacenamiento no volátil esperando un pedido de ejecución.
> - **Proceso**: es un programa **en ejecución**. El SO le asigna ALU, RAM y los recursos que requiera. Es un elemento **dinámico**: el programa se carga en memoria volátil y la misma cambia durante la ejecución.

> [!NOTE] PARALELO $\neq$ CONCURRENTE
> **Concurrente** significa que las tareas progresan de manera intercalada (aunque haya un solo core). **Paralelo** significa que se ejecutan literalmente al mismo tiempo, lo cual requiere hardware multicore.

Los tres modelos de programación que se ven en la materia son: **secuencial**, **concurrente** y **distribuido** (ver [[2_Cliente-Servidor]]).

---

## Scheduling de procesos

En el **modelo secuencial** se corre cada proceso "completo". Es el modelo natural de un procesador que ejecuta las instrucciones de un proceso en el orden que él mismo determina.

Con el tiempo los sistemas operativos comenzaron a implementar estrategias de **time slice** y **context switching**:

| Estrategia | Qué hace |
|---|---|
| **Time slice** | El sistema divide el tiempo de procesador en "slices" pequeños que va repartiendo entre los procesos. |
| **Context switching** | Cada vez que el slice se acaba (o el proceso actual cede su tiempo), el SO se encarga de disponibilizar los recursos y el estado del proceso que va a usar el siguiente slice. |

Esto permite una operación multiproceso que da la ilusión de que los programas están corriendo "al mismo tiempo" en un procesador unicore.

> [!EXAMPLE]+ Comparación de schedulers (unicore, 3 procesos)
> Procesos: $P_1 = 120$ s, $P_2 = 60$ s, $P_3 = 30$ s.
>
> **A) Secuencial, por orden de llegada ($P_1, P_2, P_3$)**
> - $P_1$ termina a los $120$ s
> - $P_2$ termina a los $120 + 60 = 180$ s
> - $P_3$ termina a los $180 + 30 = 210$ s
>
> **B) Secuencial, por menor tiempo de ejecución ($P_3, P_2, P_1$)**
> - $P_3$ termina a los $30$ s
> - $P_2$ termina a los $30 + 60 = 90$ s
> - $P_1$ termina a los $90 + 120 = 210$ s
>
> **C) Intercalando (time slicing), orden de llegada**
> - $P_3$ termina a los $90$ s
> - $P_2$ termina a los $150$ s
> - $P_1$ termina a los $210$ s
>
> Si votaran los procesos según **cuánto demoran en que se les asigne CPU por primera vez**:
>
> | Proceso | A | B | C |
> |---|---|---|---|
> | $P_1$ | 0 s | 90 s | 0 s |
> | $P_2$ | 120 s | 30 s | 10 s |
> | $P_3$ | 180 s | 0 s | 20 s |
> | **Promedio** | **100 s** | **40 s** | **10 s** |
>
> Gana **C**. Además la estrategia B requeriría que todos los procesos lleguen al scheduler antes de asignarlos, lo cual no es realista.

Beneficios del intercalamiento: cada proceso **comienza antes**, puede obtener **resultados parciales** en tiempos razonables, puede beneficiarse de tiempos cedidos por otros procesos y en general la mayoría termina antes.

> [!NOTE]
> Estos beneficios aplican a **varios** procesos corriendo al mismo tiempo. Un solo proceso no puede beneficiarse del intercalamiento.

---

## Scheduling de threads

Un proceso puede tener **threads**: subunidades de ejecución que pueden correr independientemente del proceso principal (y de otros threads). Las partes que pueden correr independientemente está bueno que corran separado (concurrentemente); tiene las mismas ventajas que cuando lo hacemos con procesos.

| | Threads | Procesos |
|---|---|---|
| **Memoria** | Comparten el espacio de memoria, file descriptors y demás recursos del proceso principal | Espacios de memoria separados |
| **Comunicación** | Directa, vía memoria compartida; hasta pueden afectar el comportamiento uno de otro | Solo vía IPC |
| **Creación / context switch** | Mucho más simple, menos tiempo y recursos | Costoso |
| **Control de inicio y fin** | Responsabilidad del **programador** | Lo maneja el sistema operativo |

Al tener menos overhead los threads son más eficientes para correr tareas, por lo cual son muy utilizados aunque requieren ciertos cuidados.

Los threads de Java mapean 1 a 1 con los threads del sistema operativo. También existen *virtual threads*, que son nuevos y no los vamos a ver.

---

## Threads en Java

Hay dos formas de definir la tarea de un thread. La menos usada es **extender `Thread`**, ya que Java no tiene herencia múltiple y extender de `Thread` limita la programación:

```java
public class HelloThread extends Thread {
    @Override
    public void run() {
        System.out.println("Hello from a thread!");
    }
    public static void main(String[] args) {
        Thread thread = new HelloThread();
        thread.start();
    }
}
```

Un poco más genérico es implementar la interfaz **`Runnable`** (se sigue usando `Thread`, solo que se lo construye con el `Runnable`):

```java
public class HelloRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("Hello from a thread!");
    }
    public static void main(String[] args) {
        Thread helloThread = new Thread(new HelloRunnable());
        helloThread.start();
    }
}
```

> [!IMPORTANT]
> Se implementa `run()` pero se ejecuta llamando a `start()`. Llamar a `run()` directamente ejecuta el código en el thread actual, sin crear uno nuevo.

### Métodos estáticos de Thread

| Método | Descripción |
|---|---|
| `Thread currentThread()` | Retorna una instancia del thread actual |
| `void sleep(long millis)` | Provoca que el thread actual quede suspendido |
| `boolean interrupted()` | Indica si el thread actual fue interrumpido |
| `void yield()` | Indica que el thread actual puede liberar el procesador por el momento |
| `boolean holdsLock(Object obj)` | Informa si el thread actual tiene un lock sobre el objeto dado |

Hay métodos estáticos en `Thread` que tiran `InterruptedException` y hay que envolverlos en try-catch.

![](./attachments/image.png)

```java
public class SleeperRunnable implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            try {
                System.out.println("Siesta numero: " + i);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
                return;
            }
        }
    }
}
```

### Métodos de instancia de Thread

| Método | Descripción |
|---|---|
| `String getName()` | Retorna el nombre del thread |
| `void interrupt()` | Interrumpe al thread. Si está suspendido en un `wait` o `join` recibe una `InterruptedException`; si no, se setea el flag *interrupted* |
| `boolean isAlive()` | Indica si el thread está vivo (iniciado pero no terminado) |
| `void join()` | Suspende la ejecución del thread que invoca hasta que el thread sobre el cual se llama termine |

```java
public class SleepyThreads {
    public static void main(String[] args) throws InterruptedException {
        final Thread[] ts = new Thread[2];
        for (int i = 0; i < ts.length; i++) {
            Thread thread = new Thread(new SleeperRunnable(), "sl-" + i);
            thread.start();
            ts[i] = thread;
        }
        ts[1].interrupt();
        ts[0].join();
    }
}
```

> [!TIP]
> Recordar que `Runnable` es una **interfaz funcional**, así que se puede pasar un lambda: `new Thread(() -> System.out.println("hola"))`.

---

## Threads en Java 5

La semántica previa era lanzar un thread y que sea *"fire and forget"*: no importa el resultado ni cuándo termina. Si importara, se pueden usar variables compartidas o colas, pero no es lo más cómodo. Java 5 agrega abstracciones para coordinar y obtener respuestas entre threads de manera simple.

Tiene 3 interfaces:

1. `Callable`
2. `Future`
3. `ExecutorService`

### Callable

Representa una tarea a ejecutar, al igual que `Runnable`, pero **retorna un valor**.

```java
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```

> [!NOTE]
> Un `Callable<Void>` sería equivalente a un `Runnable`.

### Future

Representa el **estado de una tarea asincrónica** que se mandó a ejecutar. El objetivo principal es obtener la respuesta con `get()`, pero como la tarea puede estar corriendo también permite preguntar si terminó.

```java
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
```

### ExecutorService

Permite lanzar los threads (de `Runnable` y `Callable`) y wrapearlos con un `Future` para acceder al resultado.

```java
public interface ExecutorService extends Executor {
    <T> Future<T> submit(Callable<T> task);
    <T> Future<T> submit(Runnable task, T result);
    Future<?> submit(Runnable task);

    // lanzar varios y esperar
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException;
    <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException;

    // ciclo de vida
    void shutdown();
    List<Runnable> shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}
```

- `invokeAll`: lanza varias tareas y espera a **todas**.
- `invokeAny`: lanza varias y devuelve el resultado de **la primera que termina**.

La manera recomendada de cerrar un `ExecutorService` es usar **ambos** `shutdown` y `shutdownNow`:

```java
executorService.shutdown();
try {
    if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
        executorService.shutdownNow();
    }
} catch (InterruptedException e) {
    executorService.shutdownNow();
}
```

### Thread pool

Un pool es un número finito de cosas que puedo tener, por ejemplo una cantidad de conexiones a una DB abiertas.

A pesar de que la creación de threads es más eficiente que la de un proceso, sigue consumiendo tiempo y recursos. Para mejorar esto se genera un **thread pool**: los threads ya están creados y a medida que terminan sus tareas se les asigna una nueva. `ExecutorService` tiene un pool de threads al que se le van asignando tareas, evitando tener que crear threads cada vez.

La clase `Executors` provee métodos estáticos de construcción:

```java
public class Executors {
    ExecutorService newCachedThreadPool(ThreadFactory threadFactory)
    ExecutorService newFixedThreadPool(int nThreads)
    ScheduledExecutorService newScheduledThreadPool(int corePoolSize)
    ExecutorService newSingleThreadExecutor()
    ScheduledExecutorService newSingleThreadScheduledExecutor()
}
```

> [!EXAMPLE]+ Uso de Future: descarga desde internet
> Caso donde se quiere bajar algo de internet, por lo cual puede tardar mucho. Se usa el `Future` para iniciar la descarga y realizar otras tareas mientras tanto.
>
> ```java
> private final ExecutorService pool = Executors.newFixedThreadPool(10);
>
> public Future<String> startDownloading(final URL url) throws IOException {
>     return pool.submit(new Callable<String>() {
>         @Override
>         public String call() throws Exception {
>             try (InputStream input = url.openStream()) {
>                 return IOUtils.toString(input, StandardCharsets.UTF_8);
>             }
>         }
>     });
> }
> ```
>
> Y el uso:
>
> ```java
> final Future<String> contentsFuture = startDownloading(new URL("http://www.example.com"));
> while (!contentsFuture.isDone()) {
>     askUserToWait();
>     doSomeComputationInTheMeantime();
> }
> String rta = contentsFuture.get();
> ```

---

## Problemas de la programación concurrente

El modelo concurrente presenta nuevos elementos a tener en cuenta:

- **Coordinación**
- **Consistencia / Sincronización**
- **Disponibilidad (liveness)**

Los threads se comunican principalmente mediante **variables compartidas** (de instancia y estáticas). Esto se debe a que viven dentro del espacio de memoria del proceso principal, entonces la comunicación de esta manera es natural y hasta eficiente.

### Coordinación

El primer elemento nativo para coordinar es `Thread#join`: permite que un thread espere a que otro termine.

```java
public class WaitingThreads {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new SleeperRunnable(), "sl-0");
        thread.start();
        thread.join();
        System.out.println("done");
    }
}
```

`join` bloquea al thread principal, pero se pueden aprovechar otras estrategias que usen la memoria compartida para indicar el fin:

```java
public class ComunicatingThreads {
    private boolean done = false;
    public boolean isDone() { return done; }
    public void setDone() { this.done = true; }

    public static void main(String[] args) throws InterruptedException {
        final ComunicatingThreads ct = new ComunicatingThreads();
        final ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(() -> {
            while (!ct.isDone()) {
                System.out.println("not done");
            }
            System.out.println("done");
        });
        Thread.sleep(2000L);
        ct.setDone();
    }
}
```

### Consistencia en memoria

Compartir memoria trae la posibilidad de que la escritura y/o lectura de esa zona compartida dé valores incorrectos. Estos son los **problemas de consistencia de memoria**:

- **Interferencia entre threads**: dos threads se interfieren en sus escrituras, cuando uno escribe en una variable sin detectar que el otro ya la modificó.
- **Errores de consistencia de memoria**: no hay garantías de que la lectura de una variable en un thread vea lo escrito *"aparentemente antes"* por otro thread.

> [!IMPORTANT] Happens-before
> Estos errores se producen porque hay muy pocos casos donde se garantiza que una acción se ejecute antes que otra. Ese tipo de relación se denomina **happens-before**. En muchos casos se requiere que el programador se encargue de establecerla.

Garantías de happens-before en Java:

1. Cada acción en un thread *happens-before* de las acciones posteriores, según el programa, en el **mismo** thread.
2. Un **unlock** (salida de un bloque o método sincronizado) de un monitor *happens-before* de cualquier **lock** sobre el mismo monitor.
3. Una escritura en una variable **volatile** *happens-before* de cualquier lectura sobre el mismo campo.
4. Una llamada a `start()` de un thread *happens-before* de cualquier acción en ese thread.
5. Todas las acciones en un thread *happens-before* de las acciones en otro thread que hizo `join()` sobre el primero.

### Accesos atómicos

> [!IMPORTANT]
> Una **acción atómica** es aquella que no puede detenerse mientras está ocurriendo: no hay estado intermedio ni interrupciones posibles.

En Java se garantiza que son atómicas:

- Lecturas y escrituras de **referencias** a variables y de la mayoría de las **primitivas** (exceptuando `long` y `double`).
- Lecturas y escrituras a variables declaradas como `volatile`.

> [!NOTE]
> Declarar una variable `volatile` reduce la posibilidad de errores de consistencia (todas las escrituras tienen relación happens-before con las lecturas), pero **aún se pueden provocar errores en operaciones no atómicas**. El ejemplo clásico es `c++`, que en realidad son tres operaciones: leer, sumar, escribir. Por lo cual puede ser necesario sincronizar.

---

## Sync

Para sincronizar armamos bloques que agarran un lock; **todos los objetos de Java tienen un mutex**. Los bloques `synchronized` sobre el mismo lock no corren a la vez y se tratan como operaciones atómicas entre sí.

La keyword `synchronized` garantiza:

- Si un thread entra en un bloque sincronizado, cualquier otro thread que intente entrar en cualquier otro bloque sincronizado **con la misma instancia** queda bloqueado.
- Cuando el thread sale del bloque se libera uno de los bloqueados y se genera una relación **happens-before** entre las acciones del que sale y las del que entra.

```java
public class SynchronizedObjectVisitCounter {
    private final Object lock = new Object();
    private int c = 0;

    public void addVisit() {
        synchronized (lock) { c++; }
    }
    public int getVisits() {
        synchronized (lock) { return c; }
    }
    public int peekVisits() {  // NO sincronizado: puede leer un valor viejo
        return c;
    }
}
```

> [!TIP]
> En la slide el lock se declara como `private final Object lock = "lock";`. Funciona, pero **no conviene usar un `String` literal como lock**: los literales están *interned* en un pool compartido de la JVM, así que dos clases distintas que usen `"lock"` estarían compitiendo por el **mismo** monitor sin saberlo. Un `new Object()` privado es el idiom correcto.

### Internals

- En Java cada instancia de un objeto puede tener asociado un único **lock interno** (*monitor lock* o mutex).
- Al acceder a un bloque sincronizado sobre una instancia efectivamente se está adquiriendo dicho lock. Cualquier otro que quiera el lock queda bloqueado hasta que el primero lo libere.
- Los locks son **re-entrantes** para el thread que los posee.

> [!TIP] Cosas a tener en cuenta
> - Una "Clase" en Java también es una instancia de `Class`, por lo tanto si las **variables de clase** (`static`) son recurso compartido entre threads, también puede ser necesario sincronizarlas.
> - No hay que lockear un objeto por más tiempo del necesario, para permitir que otros threads puedan solicitar el lock.
> - Lamentablemente **no hay timeouts** para este tipo de sincronización.

### Métodos sincronizados

Se pueden hacer métodos `synchronized`, en cuyo caso el lock es sobre `this`.

```java
public synchronized void addVisit() {
    visitCount++;
}
```

que es lo mismo que envolver a todo el método en

```java
public void addVisit() {
    synchronized (this) {
        visitCount++;
    }
}
```

---

## Concurrent

Hay una librería `java.util.concurrent` que nos da herramientas de más alto nivel.

### High level locks

En `java.util.concurrent.locks` hay objetos que permiten locks con semánticas más complejas que `synchronized`:

| Clase / Interfaz | Descripción |
|---|---|
| `Lock` | Interfaz básica de un lock |
| `ReadWriteLock` | Mantiene dos locks asociados, uno para lectura y otro para escritura (problema de readers/writers) |
| `ReentrantLock` | Lock re-entrante similar a `synchronized` pero que permite extender su funcionalidad |
| `ReentrantReadWriteLock` | Combinación entre read-write y reentrant |
| `StampedLock` | Lock más complejo con 3 tipos de operaciones (read, write, readOptimistic) |

```java
public class BankAccount {
    final Lock lock = new ReentrantLock();

    static void transfer(BankAccount from, BankAccount to, double amount) {
        from.lock.lock();
        from.withdraw(amount);
        to.lock.lock();
        to.deposit(amount);
        to.lock.unlock();
        from.lock.unlock();
    }
}
```

> [!IMPORTANT]
> Los `unlock` generalmente deben ir en un bloque `finally`, si no una excepción deja el lock tomado para siempre.

### Clases atómicas

El paquete `java.util.concurrent.atomic` presenta clases que sustituyen a las primitivas y tienen operaciones atómicas thread safe **sin realizar locks**: `AtomicBoolean`, `AtomicInteger`, `AtomicLong`, `AtomicLongArray`, `AtomicReference<V>`, `DoubleAccumulator`, `DoubleAdder`, `LongAccumulator`, `LongAdder`.

Tienen getters y setters atómicos y un `compareAndSet`.

```java
public class Adder implements Counter {
    private final LongAdder adder = new LongAdder();

    @Override
    public long getCounter() { return adder.longValue(); }

    @Override
    public void increment() { adder.increment(); }
}
```

### Concurrent collections

Colas útiles para el manejo de threads:

- `ConcurrentLinkedQueue` y `ConcurrentLinkedDeque`: cola sin límite de capacidad, thread safe, métodos **no bloqueantes**.
- `BlockingQueue`: interfaz que define los métodos **bloqueantes** `put` y `take`, con varias implementaciones:
    - `LinkedBlockingQueue` (sobre `LinkedList`, sin límite), `ArrayBlockingQueue` (sobre array de tamaño fijo)
    - `SynchronousQueue`: sin capacidad, 1 lectura/escritura bloqueantes
    - `PriorityBlockingQueue`: no aplica FIFO sino que ordena por prioridad
    - `DelayQueue`: los elementos salen de la cola luego de un delay propio de cada uno
    - `LinkedTransferQueue`: el productor se bloquea al producir hasta que el consumidor toma el elemento

Y otras colecciones thread safe: `ConcurrentHashMap`, `ConcurrentSkipListMap`, `ConcurrentSkipListSet`, `CopyOnWriteArrayList`, `CopyOnWriteArraySet`.

> [!NOTE]
> Se pueden usar pero con cuidado y leyendo la documentación: que sea *concurrent* no quiere decir que todos los métodos sean atómicos/concurrentes. Hay métodos muy útiles (por ejemplo `computeIfAbsent`, `merge`) que sí lo son.

### Clases de coordinación

| Clase | Descripción |
|---|---|
| `Semaphore` | Hay un número de "tickets" y se van pidiendo hasta que se acaban |
| `Condition` | Permite que un thread espere a otro |
| `CountDownLatch` | Un thread espera a que $n$ threads indiquen que están en "un punto" |
| `CyclicBarrier` | $N$ threads se esperan hasta que todos llegan al mismo punto |

```java
public class LoginQueue {
    private Semaphore semaphore;

    public LoginQueue(int slotLimit) {
        semaphore = new Semaphore(slotLimit);
    }
    boolean tryLogin() { return semaphore.tryAcquire(); }
    void logout() { semaphore.release(); }
    int availableSlots() { return semaphore.availablePermits(); }
}
```

```java
public boolean callTwiceInSameThread() {
    CountDownLatch countDownLatch = new CountDownLatch(count);
    Thread t = new Thread(() -> {
        countDownLatch.countDown();
        countDownLatch.countDown();
    });
    t.start();
    try {
        countDownLatch.await();
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return countDownLatch.getCount() == 0;
}
```

---

## Liveness

La sincronización puede provocar que los threads no terminen por diversos motivos.

- **Deadlock**: ocurre cuando dos threads quieren adquirir dos locks antes de realizar una acción y cada thread toma uno de los locks e intenta pedir el otro. Ambos quedan esperando que el otro lock se libere, cosa que nunca ocurre ya que ninguno libera el suyo.

```java
void transfer(BankAccount from, BankAccount to, double amount) {
    synchronized (from) {
        from.withdraw(amount);
        synchronized (to) {
            to.deposit(amount);
        }
    }
}
```

- **Livelock**: mala suerte infinita. Se da cuando, para evitar el deadlock, los threads al no poder tomar el segundo lock liberan el primero, esperan un tiempo y reintentan. Puede darse que ese ciclo de probar y reintentar se convierta en un ciclo infinito: agarrás y soltás locks pero nunca podés acceder al recurso.

```java
boolean withdraw(double amount) {          // si obtengo el lock ejecuto, si no aviso que no pude
    if (this.lock.tryLock()) {
        try { Thread.sleep(10L); } catch (InterruptedException e) { }
        balance -= amount;
        return true;
    }
    return false;
}

public boolean tryTransfer(BankAccountLiveLock destinationAccount, double amount) {
    if (this.withdraw(amount)) {
        if (destinationAccount.deposit(amount)) {
            return true;
        } else {
            this.deposit(amount);   // destination account busy, refund source account
        }
    }
    return false;
}

// mientras no pueda hacer la transferencia reintento -> potencial livelock
while (!sourceAccount.tryTransfer(destinationAccount, amount));
```

- **Starvation**: ocurre cuando algún thread no puede tomar nunca el lock o el recurso que espera, porque siempre está tomado por otros threads que (probablemente) tardan mucho en liberarlo y/o tienen mejor prioridad.

```java
balanceMonitorThread1.setPriority(Thread.MAX_PRIORITY);
transactionThread1.setPriority(Thread.MIN_PRIORITY);
transactionThread2.setPriority(Thread.MIN_PRIORITY);
```

### Timeouts

Que todo tenga un timeout por las dudas, siempre. La mayoría de los métodos bloqueantes del paquete `concurrent` proveen versiones que reciben un `long` y un `TimeUnit`, para que el bloqueo no sea "para siempre" si nunca se libera. Es muy buena práctica usarlos y manejar el caso de timeout (por ejemplo $N$ reintentos y luego excepción).

```java
final Lock lock = new ReentrantLock();

public int processWithLock() {
    for (int i = 0; i < 5; i++) {
        if (lock.tryLock(50L, TimeUnit.SECONDS)) {
            int rta = process();
            lock.unlock();
            return rta;
        }
    }
    throw new IllegalStateException("No se pudo obtener lock: vencida la cantidad de re-tries");
}
```

---

## Alternativas a la sincronización

Al resolver los problemas anteriores con sincronización se pueden ocasionar otros: **sobre-sincronizar**, bloquear threads y/o problemas de memoria. Existen tres estrategias alternativas que en algunos casos son preferibles:

1. Objetos inmutables.
2. Coordinar mediante comunicación **pub/sub**.
3. Hacer **sub-procesamientos independientes** (particionar).

### Objetos inmutables

> [!IMPORTANT]
> Un **objeto inmutable** es aquel cuyo estado no cambia una vez que fue creado.

Están buenos porque no hay que sincronizarlos: no pueden ser corrompidos por interferencia de threads ni tener problemas de consistencia.

Para que un objeto sea inmutable hay que:

- Asegurarse de que todos los campos sean **privados y `final`** (y obviamente no proveer setters).
- Marcar los métodos (o la clase) como `final` para evitar que subclases modifiquen las implementaciones.
- Asegurarse de que las referencias a **objetos mutables** no se modifiquen en métodos de la clase ni se "escapen" para ser modificadas fuera de la clase.

Para mutarlos, en vez de modificar el objeto se **construye uno nuevo** con el estado cambiado, y las copias defensivas se hacen tanto en el constructor como en los getters:

```java
public class Subscriber {
    private final Integer id;
    private final Date dateOfBirth;
    private final List<Subscription> subscriptions;

    public Subscriber(Integer id, Date dateOfBirth) {
        this.id = id;
        this.dateOfBirth = new Date(dateOfBirth.getTime());   // copia defensiva al entrar
    }

    public Integer getId() {
        return id;
    }

    public Date getDateOfBirth() {
        return new Date(dateOfBirth.getTime());               // copia defensiva al salir
    }
}
```

> [!TIP]
> No todo problema se puede resolver con objetos inmutables, pero es buena práctica **empezar haciendo los objetos inmutables y volverlos mutables a medida que sea necesario**.

### Pub-Sub

Una manera de coordinar entre threads, cuando uno depende del trabajo de otro, es armar una estrategia **publisher/subscriber**:

- Ambos threads comparten una **cola** (por eso `java.util.concurrent` tiene tantas opciones).
- Cuando el publisher termina su tarea envía un mensaje mediante la cola.
- El subscriber lee dicho mensaje de la cola e inicia su tarea.

Permite reducir la dependencia entre threads, reducir al mínimo el tiempo *idle* (según la cola elegida) y reducir el riesgo de errores al no usar `notifyAll` ni locks bloqueantes.

> [!EXAMPLE]+ Pub/Sub con `SynchronousQueue`
> **1. Definir la cola de comunicación:**
> ```java
> SynchronousQueue<Integer> queue = new SynchronousQueue<>();
> ```
>
> **2. El productor realiza su tarea y publica en la cola:**
> ```java
> Runnable producer = () -> {
>     Integer producedElement = ThreadLocalRandom.current().nextInt();
>     try {
>         queue.put(producedElement);
>     } catch (InterruptedException ex) {
>         ex.printStackTrace();
>     }
> };
> ```
>
> **3. El consumidor lee de la cola y ejecuta en consecuencia:**
> ```java
> Runnable consumer = () -> {
>     try {
>         Integer consumedElement = queue.take();
>         // ...
>     } catch (InterruptedException ex) {
>         ex.printStackTrace();
>     }
> };
> ```
>
> **4. Ambos se ejecutan usando threads:**
> ```java
> ExecutorService executor = Executors.newFixedThreadPool(2);
> executor.execute(producer);
> executor.execute(consumer);
> ```

> [!NOTE]
> Esta estrategia se porta a sistemas distribuidos, donde es común usar pub-sub como estrategia de comunicación entre componentes remotos. Ver [[2_Cliente-Servidor#Servicios - Patrón de comunicación|el estilo basado en eventos]].

### Subdivisión de la tarea

Otra estrategia para procesar grandes volúmenes de información es (de ser posible):

1. **Particionar** la información entre unidades de procesamiento (workers o threads).
2. Cada worker calcula un **resultado parcial** sobre su partición.
3. Con los resultados de todas las particiones se calcula el **resultado total**.

Es deseable porque no hay riesgo de interferencia (cada partición es independiente del resto y debería ser inmutable), se optimiza el procesamiento porque se trabaja sobre un subset de la data, y se pueden reutilizar workers si hay más particiones que workers. La principal contra es lograr describir la tarea de forma que se pueda particionar.

El ejemplo clásico es **Mergesort**: requiere particionar la data e ir ordenando dentro de las particiones, y la tarea dentro de cada partición es independiente de las otras.

En Java 8 se incluye nativamente la posibilidad de paralelizar los **Streams**. Las operaciones se calculan en threads obtenidos del `ForkJoinPool`:

```java
double average = roster
        .parallelStream()
        .filter(p -> p.getGender() == Person.Sex.MALE)
        .mapToInt(Person::getAge)
        .average()
        .getAsDouble();
```

La división y generación de threads la realiza la JVM (internamente usando la clase `Spliterator`). Esto trae sus riesgos porque al ser una solución genérica no siempre es la más óptima.

> [!IMPORTANT]
> Al no estar garantizado el orden, los lambdas que se pasan como operaciones a un stream paralelo **no deben**:
> - Tener **side-effects**: modificar valores de la clase que los contiene.
> - Ser **stateful**: guardar estado interno.

> [!EXAMPLE]+ `Arrays#parallelSort` vs `Arrays#sort`
> Promedio de 4 ejecuciones por tamaño de array:
>
> | Tamaño | Serial | Paralelo |
> |---|---|---|
> | 10 M | 293 ms | 46 ms |
> | 25 M | 652 ms | 79 ms |
> | 50 M | 1320 ms | 151 ms |
>
> Las ejecuciones concurrentes son mejores a medida que el número de valores crece. Para valores muy chicos (probar con 10 K) el serial puede ser igual o hasta mejor, porque el **overhead** de particionar, crear/coordinar los threads y unir los resultados supera lo que se gana paralelizando.

---

## Scheduled Futures

Son futures que se corren de manera **periódica** o con algún **delay** (o ambos). La interfaz es la misma que `Future` más `Delayed`:

```java
public interface ScheduledFuture<V> extends Delayed, Future<V> { }

public interface Delayed extends Comparable<Delayed> {
    long getDelay(TimeUnit unit);
}
```

Se instancian a partir de un `ScheduledExecutorService`:

```java
final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

// se corre 1 vez luego de 1 segundo de delay
Future<String> resultFuture = executorService.schedule(callableTask, 1, TimeUnit.SECONDS);

// luego de 100 ms corre una tarea cada 450 ms, a menos que la tarea tarde más
executorService.scheduleAtFixedRate(runnableTask, 100, 450, TimeUnit.MILLISECONDS);

// luego de 100 ms corre una tarea y se repite 150 ms después de que la tarea termine
executorService.scheduleWithFixedDelay(task, 100, 150, TimeUnit.MILLISECONDS);
```

> [!NOTE]
> La diferencia clave: `scheduleAtFixedRate` mide el período **entre inicios**, `scheduleWithFixedDelay` mide el delay **entre el fin de una y el inicio de la siguiente**. En ambos casos las tareas terminan al finalizar el `ExecutorService` o al cancelarlas.

---

## Completable Futures

Representan un `Future` que puede ser **completado programáticamente** y que puede ser **encadenado y combinado** con otros `CompletableFuture`, porque implementa la interfaz `CompletionStage`.

### Construcción

```java
CompletableFuture<String> cf = new CompletableFuture<>();

cf.complete("resultado correcto");                     // completar el future
cf.completeExceptionally(new IllegalArgumentException("este future no termino bien"));
```

Y existen métodos estáticos de construcción según el tipo de acción:

```java
CompletableFuture.completedFuture("resultado");                  // tarea ya terminada y con respuesta
CompletableFuture.runAsync(() -> System.out.println("tarea"));   // tarea como Runnable
CompletableFuture.supplyAsync(() -> "resultado");                // tarea como Supplier (similar a Callable)
```

> [!NOTE]
> `runAsync` y `supplyAsync` usan el `ForkJoinPool` a menos que se usen las versiones que reciben un `ExecutorService`.

### Chaining

Se puede obtener un `CompletableFuture` que es el encadenamiento entre otro `CompletableFuture` y una acción que corre después de que este finalizó:

```java
// un Runnable, que corre sin importar la respuesta del future inicial
CompletableFuture<Void> thenRun = cf.thenRun(() -> System.out.println("tarea"));

// un Consumer, que utiliza la respuesta
CompletableFuture<Void> thenAccept = cf.thenAccept(response -> System.out.println(response));

// una Function que transforma la respuesta en otro valor
CompletableFuture<String> thenApply = cf.thenApply(response -> response.toLowerCase());

// una Function que transforma la respuesta en otro CompletableFuture
CompletableFuture<String> thenCompose =
        cf.thenCompose(response -> CompletableFuture.completedFuture(response.toLowerCase()));
```

> [!TIP]
> Estas versiones corren en el **mismo thread**, pero existen variantes `*Async` para correr en threads diferentes usando el `ForkJoinPool` o un `ExecutorService`.

### Manejo de errores

```java
// transforma la excepción en un valor
CompletableFuture<String> exceptionally = cf.exceptionally(th -> th.getMessage());

// consume tanto el caso ok como el de error (y lo deja para futuros encadenamientos)
CompletableFuture<String> whenComplete = cf.whenComplete((r, e) -> {
    if (r != null) {
        System.out.println(r);
    } else if (e != null) {
        System.out.println(e.getMessage());
    } else {
        System.out.println("don't know what happened");
    }
});
```

> [!IMPORTANT]
> En `whenComplete` **sólo uno de los dos** (`r` o `e`) tiene valor.

### Combine

Se puede encadenar un `CompletableFuture` a la ejecución de **más de un** `CompletableFuture`:

```java
// aplicar una función al resultado de LAS DOS ejecuciones
ExecutorService service = Executors.newCachedThreadPool();
CompletableFuture<String> nameTask = CompletableFuture.supplyAsync(() -> "name", service);
CompletableFuture<String> subscriptorsTask = CompletableFuture.supplyAsync(() -> "2000", service);
CompletableFuture<String> combined =
        nameTask.thenCombineAsync(subscriptorsTask, (f, s) -> f + " with: " + s, service);
```

```java
// aplicar una función al resultado de LA PRIMERA que termine
CompletableFuture<String> nameTask = CompletableFuture.supplyAsync(() -> "name");
CompletableFuture<String> username = CompletableFuture.supplyAsync(() -> "username");
CompletableFuture<String> either = nameTask.applyToEither(username, r -> "obtained: " + r);
```

---

## Takeaways

- La programación concurrente permite aprovechar mejor los recursos del sistema y acelerar tiempos de respuesta. Esto es aún mejor en ambientes multicore, ya que hay ejecución **paralela**.
- Como contrapartida aparecen problemas de **consistencia**, **coordinación** y **liveness**.
- Además de la sincronización existen otras estrategias para resolver thread safety: **objetos inmutables**, **pub/sub** y **subdivisión de tareas**. Varias de estas se vuelven a ver al pasar a programación distribuida (ver [[2_Cliente-Servidor]]).
- Se pueden usar ejecuciones más específicas cuando las tareas tengan características especiales: `ScheduledFuture` para tareas periódicas, `CompletableFuture` para encadenar y combinar.

---

## Referencias

- [Java Tutorial de Concurrencia (Oracle)](https://docs.oracle.com/javase/tutorial/essential/concurrency/index.html)
- *Concurrent Programming in Java: Design Principles and Patterns* (2nd ed.), Doug Lea.
- *Java Concurrency in Practice*, Brian Goetz et al.
- *Effective Java* (2nd ed.), Joshua Bloch — capítulo de threads.
