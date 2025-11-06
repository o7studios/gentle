package gentle.async;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Scope implements AutoCloseable {
    private final ExecutorService executor;
    private final List<Task<?>> tasks = new ArrayList<>();
    private volatile boolean closed = false;

    public static Scope open() {
        return new Scope(Executors.newCachedThreadPool());
    }

    public static Scope open(@NonNull ExecutorService executor) {
        return new Scope(executor);
    }

    public synchronized <T> Task<T> async(@NonNull Callable<T> supplier) {
        if (closed) throw new IllegalStateException("Scope already closed");
        Task<T> task = new Task<>(executor.submit(supplier));
        tasks.add(task);
        return task;
    }

    public synchronized int size() {
        return tasks.size();
    }

    @Override
    public void close() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        synchronized (this) {
            closed = true;
            for (var t : tasks) {
                try {
                    t.cancel();
                } catch (Throwable ex) {
                    errors.add(ex);
                }
            }
            executor.shutdownNow();
        }
        if (errors.isEmpty()) return;
        var primary = errors.getFirst();
        for (int i = 1; i < errors.size(); i++) {
            primary.addSuppressed(errors.get(i));
        }
        if (primary instanceof Exception e) throw e;
        if (primary instanceof Error err) throw err;
        throw new RuntimeException(primary);
    }

    @Override
    public String toString() {
        return "Scope[size=" + size() + ", closed=" + closed + "]";
    }
}
