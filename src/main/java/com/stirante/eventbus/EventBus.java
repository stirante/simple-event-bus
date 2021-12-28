package com.stirante.eventbus;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class EventBus {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private static final ReentrantLock lock = new ReentrantLock();
    private static final List<SubscriberElement> listeners = new ArrayList<>();
    private static final Map<Class<? extends EventExecutor>, EventExecutor> eventExecutorMap = new HashMap<>();

    /**
     * Registers all subscribers in provided object instance
     *
     * @param obj object instance
     * @return CompletableFuture, which finishes when all subscribers have been registered
     */
    public static CompletableFuture<Void> register(Object obj) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Method method : obj.getClass().getMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation != null) {
                futures.add(registerListener(method, annotation, obj));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Registers all static subscribers in provided class
     *
     * @param obj class
     * @return CompletableFuture, which finishes when all subscribers have been registered
     */
    public static CompletableFuture<Void> register(Class<?> obj) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Method method : obj.getMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation != null && Modifier.isStatic(method.getModifiers())) {
                futures.add(registerListener(method, annotation, null));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Helper method for registering single method as a subscriber
     *
     * @param method     method
     * @param annotation method's @Subscriber annotation
     * @param obj        object instance or null, if methods is static
     * @return CompletableFuture, which finishes after this method registration
     */
    private static CompletableFuture<?> registerListener(Method method, Subscribe annotation, Object obj) {
        if (method.getParameterCount() > 1) {
            throw new IllegalArgumentException(
                    "Subscriber method \"" + method + "\" cannot have more than one parameter");
        }
        SubscriberElement s = new SubscriberElement();
        s.method = method;
        s.eventExecutor = getEventExecutor(annotation.eventExecutor());
        s.target = annotation.value();
        s.type = method.getParameterCount() == 0 ? null : method.getParameterTypes()[0];
        s.object = obj == null ? null : new WeakReference<>(obj);
        s.isStatic = Modifier.isStatic(method.getModifiers());
        s.priority = annotation.priority();
        return CompletableFuture.runAsync(() -> {
            lock.lock();
            listeners.add(s);
            lock.unlock();
        }, EXECUTOR_SERVICE);
    }

    /**
     * Returns event executor for specified class
     *
     * @param cls EventExecutor class
     * @return EventExecutor object implementation
     */
    private static EventExecutor getEventExecutor(Class<? extends EventExecutor> cls) {
        if (!eventExecutorMap.containsKey(cls)) {
            try {
                EventExecutor executor = cls.newInstance();
                eventExecutorMap.put(cls, executor);
            } catch (InstantiationException e) {
                throw new RuntimeException("EventExecutor could not be instantiated", e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("EventExecutor must have public no argument constructor", e);
            }
        }
        return eventExecutorMap.get(cls);
    }

    /**
     * Unregisters all subscriber methods from specified object instance
     *
     * @param obj object instance
     * @return CompletableFuture, which finishes after unregistering all subscriber methods of this object instance
     */
    public static CompletableFuture<Void> unregister(Object obj) {
        return CompletableFuture.runAsync(() -> {
            lock.lock();
            listeners.removeIf(subscriberElement -> subscriberElement.isInvalid() ||
                    (subscriberElement.object != null && subscriberElement.object.get() == obj));
            lock.unlock();
        }, EXECUTOR_SERVICE);
    }


    /**
     * Unregisters all subscriber methods from specified class
     *
     * @param obj class
     * @return CompletableFuture, which finishes after unregistering all subscriber methods of this class
     */
    public static CompletableFuture<Void> unregister(Class<?> obj) {
        return CompletableFuture.runAsync(() -> {
            lock.lock();
            listeners.removeIf(subscriberElement -> subscriberElement.isInvalid() ||
                    subscriberElement.method.getClass() == obj);
            lock.unlock();
        }, EXECUTOR_SERVICE);
    }

    /**
     * Publishes an event to all subscribers of the target event
     *
     * @param target target event type
     * @param event  event data
     * @return CompletableFuture, which finishes after all subscribers have processed the event
     */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Void> publish(String target, Object event) {
        lock.lock();
        CompletableFuture<Void>[] futures = listeners.stream()
                .filter(subscriberElement -> subscriberElement.accepts(target, event) && !subscriberElement.isInvalid())
                .sorted(Comparator.comparingInt(subscriberElement -> subscriberElement.priority.getValue()))
                .map(subscriberElement -> subscriberElement.invoke(event))
                .toArray(CompletableFuture[]::new);
        lock.unlock();
        return CompletableFuture.allOf(futures);
    }

    /**
     * Publishes an event to all subscribers of the target event
     *
     * @param target target event type
     * @return CompletableFuture, which finishes after all subscribers have processed the event
     */
    public static CompletableFuture<Void> publish(String target) {
        return publish(target, null);
    }

    /**
     * Unregisters all subscriber methods
     *
     * @return CompletableFuture, which finishes after all subscribers have been unregistered
     */
    public static CompletableFuture<Void> clearAll() {
        return CompletableFuture.runAsync(() -> {
            lock.lock();
            listeners.clear();
            lock.unlock();
        }, EXECUTOR_SERVICE);
    }


    /**
     * Unregisters all invalid subscriber methods.
     * A subscriber is invalid, when it's object instance has been removed by garbage collector.
     *
     * @return CompletableFuture, which finishes after all invalid subscriber methods have been unregistered
     */
    public static CompletableFuture<Void> cleanUp() {
        return CompletableFuture.runAsync(() -> {
            lock.lock();
            listeners.removeIf(SubscriberElement::isInvalid);
            lock.unlock();
        }, EXECUTOR_SERVICE);
    }

    private static class SubscriberElement {
        public Class<?> type;
        private String target;
        private EventExecutor eventExecutor;
        private WeakReference<?> object;
        private Method method;
        private boolean isStatic;
        private EventPriority priority;

        private CompletableFuture<Void> invoke(Object event) {
            Runnable run;
            if (method.getParameterCount() == 0) {
                run = () -> {
                    try {
                        method.invoke(isStatic ? null : object.get());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                };
            }
            else {
                if (event == null) {
                    throw new IllegalArgumentException("This subscriber requires a bus event");
                }
                if (!method.getParameterTypes()[0].isAssignableFrom(event.getClass())) {
                    throw new IllegalArgumentException(
                            "Incompatible parameters. Expected \"" + method.getParameterTypes()[0].toGenericString() +
                                    "\" but got \"" + event.getClass().toGenericString() + "\"");
                }
                run = () -> {
                    try {
                        method.invoke(isStatic ? null : object.get(), event);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                };
            }
            return eventExecutor.execute(run);
        }

        private boolean isInvalid() {
            return !isStatic && (object == null || object.get() == null);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", SubscriberElement.class.getSimpleName() + "[", "]")
                    .add("target='" + target + "'")
                    .add("method=" + method)
                    .add("eventExecutor=" + eventExecutor.getClass().toGenericString())
                    .add("object=" + (object == null ? "null" : object.get()))
                    .add("isStatic=" + isStatic)
                    .add("priority=" + priority.name())
                    .toString();
        }

        public boolean accepts(String target, Object event) {
            return this.target.equals(target) && (event == null || type == null || type.isAssignableFrom(event.getClass()));
        }
    }

}
