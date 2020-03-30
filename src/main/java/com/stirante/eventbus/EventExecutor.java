package com.stirante.eventbus;

import java.util.concurrent.CompletableFuture;

public interface EventExecutor {

    CompletableFuture<Void> execute(Runnable runnable);

}
