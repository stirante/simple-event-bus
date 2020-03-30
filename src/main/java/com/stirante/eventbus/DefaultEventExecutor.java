package com.stirante.eventbus;

import java.util.concurrent.CompletableFuture;

public class DefaultEventExecutor implements EventExecutor {
    @Override
    public CompletableFuture<Void> execute(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
        return CompletableFuture.completedFuture(null);
    }
}
