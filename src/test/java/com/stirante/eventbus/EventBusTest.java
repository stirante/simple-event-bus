package com.stirante.eventbus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventBusTest {

    @BeforeEach
    public void clearEventBus() {
        EventBus.clearAll().join();
    }

    @Test
    public void registering_method_with_2_parameters_throws_error() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> EventBus.register(new Subscriber2Parameters())
                .join());
    }

    @Test
    public void registering_method_with_good_parameter_passes() {
        EventBus.register(new SubscriberGoodParameter()).join();
    }

    @Test
    public void registering_method_with_no_parameter_passes() {
        EventBus.register(new SubscriberNoParameter()).join();
    }

    @Test
    public void publishing_event_triggers_subscriber() {
        SubscriberOk subscriber = new SubscriberOk();
        EventBus.register(subscriber).join();

        EventBus.publish("test").join();

        Assertions.assertTrue(subscriber.received);
    }

    @Test
    public void publishing_string_event_triggers_string_subscriber() {
        SubscriberStringParameter subscriber = new SubscriberStringParameter();
        SubscriberOk subscriber1 = new SubscriberOk();
        SubscriberIntParameter subscriber2 = new SubscriberIntParameter();
        EventBus.register(subscriber).join();
        EventBus.register(subscriber1).join();
        EventBus.register(subscriber2).join();
        String content = "Hello world";

        EventBus.publish("test", content).join();

        Assertions.assertEquals(content, subscriber.received);
        Assertions.assertEquals(-1, subscriber2.received);
        Assertions.assertTrue(subscriber1.received);
    }

    @Test
    public void publishing_event_triggers_subscribers_in_specified_order() {
        SubscriberOk subscriber = new SubscriberOk();
        SubscriberPriority subscriber1 = new SubscriberPriority(subscriber);
        EventBus.register(subscriber).join();
        EventBus.register(subscriber1).join();

        EventBus.publish("test").join();

        Assertions.assertTrue(subscriber1.received && subscriber.received);
    }

    public static class Subscriber2Parameters {
        @Subscribe("test")
        public void test(Object e1, Object e2) {

        }
    }

    public static class SubscriberStringParameter {
        public String received;

        @Subscribe("test")
        public void test(String s) {
            received = s;
        }
    }

    public static class SubscriberIntParameter {
        public int received = -1;

        @Subscribe("test")
        public void test(int s) {
            received = s;
        }
    }

    public static class SubscriberGoodParameter {
        @Subscribe("test")
        public void test(Object e) {

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
