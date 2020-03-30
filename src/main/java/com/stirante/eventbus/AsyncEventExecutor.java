package com.stirante.eventbus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncEventExecutor implements EventExecutor {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public CompletableFuture<Void> execute(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executorService);
    }
}
