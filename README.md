simple-event-bus
----------
[![simple-event-bus](https://jitpack.io/v/stirante/simple-event-bus.svg)](https://jitpack.io/#stirante/simple-event-bus)
----------

Simple library which provides simplest possible event bus I could think of. 

Written mainly for my other projects.

## Requirements

**simple-event-bus** requires at least Java 8.

## Setup

This project is available on [Jitpack](https://jitpack.io/#stirante/simple-event-bus/1.0.0)

### Gradle

Add Jitpack to your root build.gradle at the end of repositories:

```java
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

Add the project as a dependency:

```java
dependencies {
	compile 'com.github.stirante:simple-event-bus:1.0.0'
}
```

### Maven

Add Jitpack as a repository:

```xml
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>
```

Add the project as a dependency:

```xml
<dependency>
    <groupId>com.github.stirante</groupId>
    <artifactId>simple-event-bus</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

Events are identified by strings. An event can also contain additional data.

Publishing an event:
```java
// Publishing without additional data
EventBus.publish("event-name");
// The data variable should hold an instance implementing BusEvent interface.
EventBus.publish("event-name", data);
```

Subscribing to an event:
```java
public class Example {
    public static void main(String[] args){
        // Registering static methods using class reference
        EventBus.register(Example.class);
        // Registering instance methods using an instance reference
        EventBus.register(new Example());
    }

    @Subscribe("test-event")
    public static void onTestEvent() {
        System.out.println("Hello from static test event");
    }

    @Subscribe("test-event")
    public void onTestEvent() {
        System.out.println("Hello from instance test event");
    }

}
```

Subscriber can specify event executor (same thread, async or a custom implementation) and priority (highest to lowest. Order is not guaranteed when using async event executor).

## Contributing
All contributions are appreciated.
If you would like to contribute to this project, please send a pull request.

## Contact
Have a suggestion, complaint, or question? Open an [issue](https://github.com/stirante/simple-event-bus/issues).
