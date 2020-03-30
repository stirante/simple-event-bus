package com.stirante.eventbus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncEventExecutor implements EventExecutor {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void execute(Runnable runnable) {
        executorService.submit(runnable);
    }
}
