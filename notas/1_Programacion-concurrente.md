
# Programacion concurrente

Mas de una instruccion corriendo "al mismo tiempo". Por ejemplo usando *threads*

Logicamente todo es un tradeoff.

Es muy dificil testear cosas de concurrencia y hay veces que no se puede testear directamente

## Scheduling de procesos

time slicing, context switching, ...

Todo lo que ya sabemos que hace el procesador unicore para dar la ilusion de estar corriendo todo al mismo tiempo

## Scheduling de threads

las partes que pueden correr independientemente esta bueno que corran separado (concurrentemente). Tiene las mismas ventajas que cuando lo hacemos con procesos

Es mas barato crear threads y comunicarlos que crear procesos y comunicarlos via IPC.

Los threads de java mapean 1 a 1 con los threads de un del sistema operativo. Tambien existen virtual threads que son nuevitos y no los vamos a ver.

## Threads en java

`Threads` es una clase que implementa `Runnable`

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

Hay metodos estaticos en threads que tiran `InterruptedException` y hay que envolverlos en try-catch

![alt text](./attachments/image.png)

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

### Interfaces

tiene 3 interfaces:
1. Callable
2. Future
3. ExecutorService

### Thread pool

Un pool es un numero finito de cosas que puedo tener, por ejemplo una cantidad de conexiones a una db abiertas

`executorService` tiene un pool de threads al que se les va asignando tareas. Esto es para evitar tener que crear threads cada vez

## Sync

Para sincronizar armamos bloques que agarran un lock, todos los objetos de java tiene un mutex

los bloques `synchronized` sobre el mismo lock no corren a la vez y se tratan como operaciones 

se pueden hacer metodos `synchronized`

```java
public synchronized void addVisit() {
    visitCount++;
}
```

que es lo mismo que envolver a todo el metodo en 

```java
public void addVisit() {
    synchronized (this) {
        visitCount++;
    }
}
```

## Concurrent

Hay una libreria concurrent de java que nos da locks, por ejemplo `ReadWriteLock` para el problema de readers y writers

Tambien hay `AtomicInteger` y otras atomicas con metodos getters y setters atomicos o un `compareAndSet`

Tambien hay `ConcurrentCollections` como `concurrentHashMap` (se pueden usar pero con cuidado y leyendo la documentacion, porque que sea concurrent no quiere decir que todos los metodos sean atomicos/concurrentes. Hay metodos muy utiles)

## Liveness

- deadlock
- livelock: mala suerte infinita de que quedas en un bucle de agarras y soltas locks pero nunca podes acceder al recurso
- starvation

que todo tenga un timeout por las dudas siempre

## Objetos inmutables

Estan buenos porque no hay que sincronizarlos

para mutarlos