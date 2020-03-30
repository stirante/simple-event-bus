package com.stirante.eventbus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventBusTest {

    @BeforeEach
    public void clearEventBus() {
        EventBus.clearAll();
    }

    @Test
    public void registering_method_with_2_parameters_throws_error() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> EventBus.register(new Subscriber2Parameters()));
    }

    @Test
    public void registering_method_with_string_parameter_throws_error() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> EventBus.register(new SubscriberWrongParameter()));
    }

    @Test
    public void registering_method_with_good_parameter_passes() {
        EventBus.register(new SubscriberGoodParameter());
    }

    @Test
    public void registering_method_with_no_parameter_passes() {
        EventBus.register(new SubscriberNoParameter());
    }

    @Test
    public void publishing_event_triggers_subscriber() {
        SubscriberOk subscriber = new SubscriberOk();
        EventBus.register(subscriber);

        EventBus.publish("test");

        Assertions.assertTrue(subscriber.received);
    }

    @Test
    public void publishing_event_triggers_subscribers_in_specified_order() {
        SubscriberOk subscriber = new SubscriberOk();
        SubscriberPriority subscriber1 = new SubscriberPriority(subscriber);
        EventBus.register(subscriber);
        EventBus.register(subscriber1);

        EventBus.publish("test");

        Assertions.assertTrue(subscriber1.received && subscriber.received);
    }

    public static class Subscriber2Parameters {
        @Subscribe("test")
        public void test(EventBus e1, EventBus e2) {

        }
    }

    public static class SubscriberWrongParameter {
        @Subscribe("test")
        public void test(String s) {

        }
    }

    public static class SubscriberGoodParameter {
        @Subscribe("test")
        public void test(BusEvent e) {

        }
    }

    public static class SubscriberNoParameter {
        @Subscribe("test")
        public void test() {

        }
    }

    public static class SubscriberOk {

        public boolean received = false;

        @Subscribe("test")
        public void test() {
            received = true;
        }
    }

    public static class SubscriberPriority {

        private final SubscriberOk ok;
        public boolean received = false;

        public SubscriberPriority(SubscriberOk ok) {
            this.ok = ok;
        }

        @Subscribe(value = "test", priority = EventPriority.HIGHEST)
        public void test() {
            received = !ok.received;
        }
    }
}
