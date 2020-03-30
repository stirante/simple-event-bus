package com.stirante.eventbus;

public class DefaultEventExecutor implements EventExecutor {
    @Override
    public void execute(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }
}
