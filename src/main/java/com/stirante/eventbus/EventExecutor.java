package com.stirante.eventbus;

public interface EventExecutor {

    EventExecutor DEFAULT = Runnable::run;

    void execute(Runnable runnable);

}
