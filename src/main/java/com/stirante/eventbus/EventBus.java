package com.stirante.eventbus;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class EventBus {

    private static List<SubscriberElement> listeners = new ArrayList<>();
    private static Map<Class<? extends EventExecutor>, EventExecutor> eventExecutorMap = new HashMap<>();

    public static void register(Object obj) {
        for (Method method : obj.getClass().getMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation != null) {
                registerListener(method, annotation, obj);
            }
        }
    }

    public static void register(Class<?> obj) {
        for (Method method : obj.getMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation != null && Modifier.isStatic(method.getModifiers())) {
                registerListener(method, annotation, null);
            }
        }
    }

    private static void registerListener(Method method, Subscribe annotation, Object obj) {
        if (method.getParameterCount() > 1) {
            throw new IllegalArgumentException(
                    "Subscriber method \"" + method.toString() + "\" cannot have more than one parameter");
        }
        if (method.getParameterCount() == 1 &&
                !BusEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Subscriber method \"" + method.toString() +
                    "\" parameter must be an implementation of EventBus");
        }
        SubscriberElement s = new SubscriberElement();
        s.method = method;
        s.eventExecutor = getEventExecutor(annotation.eventExecutor());
        s.target = annotation.value();
        s.object = obj == null ? null : new WeakReference<>(obj);
        s.isStatic = Modifier.isStatic(method.getModifiers());
        s.priority = annotation.priority();
        listeners.add(s);
    }

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

    public static void unregister(Object obj) {
        listeners.removeIf(subscriberElement -> subscriberElement.isInvalid() ||
                (subscriberElement.object != null && subscriberElement.object.get() == obj));
    }

    public static void unregister(Class<?> obj) {
        listeners.removeIf(subscriberElement -> subscriberElement.isInvalid() ||
                subscriberElement.method.getClass() == obj);
    }

    public static void publish(String target, BusEvent event) {
        listeners.stream()
                .filter(subscriberElement -> subscriberElement.target.equals(target) && !subscriberElement.isInvalid())
                .sorted(Comparator.comparingInt(subscriberElement -> subscriberElement.priority.getValue()))
                .forEach(subscriberElement -> subscriberElement.invoke(event));
    }

    public static void publish(String target) {
        publish(target, null);
    }

    public static void clearAll() {
        listeners.clear();
    }

    public static void cleanUp() {
        listeners.removeIf(SubscriberElement::isInvalid);
    }

    private static class SubscriberElement {
        private String target;
        private EventExecutor eventExecutor;
        private WeakReference<?> object;
        private Method method;
        private boolean isStatic;
        private EventPriority priority;

        private void invoke(BusEvent event) {
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
            eventExecutor.execute(run);
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
    }

}
