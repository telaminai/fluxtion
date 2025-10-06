# How to Unit Test a DataFlow

This guide explains how to effectively unit test Fluxtion DataFlow applications using JUnit 5.

## Overview

DataFlow applications are designed to be easy to test. The key insight is that **a DataFlow is single-threaded** and processes events synchronously, making it straightforward to write deterministic, fast-running unit tests.

For a complete working example, see the [JUnit Testing Demo](../../../fluxtion-examples/sample-apps/junit-testing-demo) sample application.

## Why DataFlow is Easy to Test

### Single-Threaded Execution

DataFlow processes events in a single thread, one at a time. This eliminates:

- Race conditions
- Timing issues
- Flaky tests from async behavior
- Need for complex test synchronization

When you call `dataFlow.onEvent(event)`, the event is processed completely before the method returns. You can immediately make assertions without waiting or polling.

### Separation of Business Logic and Infrastructure

DataFlow nodes contain pure business logic:

- No database connections
- No message queue clients
- No file I/O handlers
- No network sockets

Infrastructure concerns (Kafka consumers, database writers, file readers) are handled separately through:

- **Services** - Injectable dependencies that can be mocked
- **EventFeeds** - Event sources that can be substituted
- **Sinks** - Output targets that can be captured

This separation means your tests run in milliseconds without external dependencies.

## Setting Up Your Test Project

### Add Dependencies

Add JUnit 5 and Mockito to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>com.telamin.fluxtion</groupId>
        <artifactId>fluxtion-builder</artifactId>
        <version>${fluxtion.version}</version>
    </dependency>
    <dependency>
        <groupId>com.telamin.fluxtion</groupId>
        <artifactId>fluxtion-runtime</artifactId>
        <version>${fluxtion.version}</version>
    </dependency>
    
    <!-- Testing dependencies -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-api</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-engine</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.11.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.11.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Configure Maven Surefire

Ensure tests run with Maven:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

## Basic Testing Pattern

### Create Your Test Class

```java
@ExtendWith(MockitoExtension.class)
class MyProcessorTest {
    
    private DataFlow dataFlow;
    private MyProcessor processor;
    
    @BeforeEach
    void setUp() {
        // Build DataFlow with your business logic
        EventProcessor<?> eventProcessor = DataFlowBuilder.newBuilder("myFlow")
                .addNode(new MyProcessor(), "processor")
                .buildAndCompile();
        
        dataFlow = eventProcessor.getDataFlow();
        dataFlow.init();
        
        // Get reference to processor for assertions
        processor = dataFlow.getNodeById("processor");
    }
    
    @Test
    void testBasicEventProcessing() {
        // Arrange
        MyEvent event = new MyEvent("test-data");
        
        // Act
        dataFlow.onEvent(event);
        
        // Assert
        assertEquals(1, processor.getProcessedCount());
    }
}
```

### Key Points

1. **Use `@BeforeEach`** to create a fresh DataFlow for each test
2. **Call `dataFlow.init()`** to initialize the flow before testing
3. **Get node references** with `dataFlow.getNodeById()` for assertions
4. **Process events** with `dataFlow.onEvent()`
5. **Assert immediately** - no waiting needed

## Testing with Services

Services are injectable dependencies that can be mocked in tests.

### Define Service Interfaces

```java
public interface PaymentService extends Service<PaymentService> {
    boolean processPayment(String customerId, double amount);
}

public interface InventoryService extends Service<InventoryService> {
    boolean checkStock(String productId, int quantity);
    void reduceStock(String productId, int quantity);
}
```

### Inject Services into Your Node

```java
public class OrderProcessor {
    
    @Inject
    private PaymentService paymentService;
    
    @Inject
    private InventoryService inventoryService;
    
    @OnEventHandler
    public boolean processOrder(Order order) {
        if (!inventoryService.checkStock(order.getProductId(), order.getQuantity())) {
            return false;
        }
        
        if (!paymentService.processPayment(order.getCustomerId(), order.getTotal())) {
            return false;
        }
        
        inventoryService.reduceStock(order.getProductId(), order.getQuantity());
        return true;
    }
}
```

### Mock Services in Tests

```java
@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {
    
    @Mock
    private PaymentService paymentService;
    
    @Mock
    private InventoryService inventoryService;
    
    private DataFlow dataFlow;
    private OrderProcessor processor;
    
    @BeforeEach
    void setUp() {
        EventProcessor<?> eventProcessor = DataFlowBuilder.newBuilder("orderProcessing")
                .addNode(new OrderProcessor(), "orderProcessor")
                .buildAndCompile();
        
        dataFlow = eventProcessor.getDataFlow();
        dataFlow.init();
        
        processor = dataFlow.getNodeById("orderProcessor");
        
        // Register mock services
        dataFlow.registerService(paymentService);
        dataFlow.registerService(inventoryService);
    }
    
    @Test
    void testSuccessfulOrder() {
        // Arrange - setup mock behavior
        when(inventoryService.checkStock(anyString(), anyInt())).thenReturn(true);
        when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(true);
        
        Order order = new Order("ORD-001", "CUST-123", "PROD-456", 2, 50.0);
        
        // Act
        dataFlow.onEvent(order);
        
        // Assert - verify business logic
        assertEquals(1, processor.getTotalOrdersProcessed());
        
        // Verify service interactions
        verify(inventoryService).checkStock("PROD-456", 2);
        verify(paymentService).processPayment("CUST-123", 100.0);
        verify(inventoryService).reduceStock("PROD-456", 2);
    }
    
    @Test
    void testOrderFailsWhenOutOfStock() {
        // Arrange - simulate out of stock
        when(inventoryService.checkStock(anyString(), anyInt())).thenReturn(false);
        
        Order order = new Order("ORD-002", "CUST-123", "PROD-456", 10, 50.0);
        
        // Act
        dataFlow.onEvent(order);
        
        // Assert
        assertEquals(0, processor.getTotalOrdersProcessed());
        
        // Verify payment was never attempted
        verify(inventoryService).checkStock("PROD-456", 10);
        verify(paymentService, never()).processPayment(anyString(), anyDouble());
    }
}
```

### Benefits of Service Mocking

- **Fast tests** - No real service connections needed
- **Controlled behavior** - Simulate any scenario (success, failure, errors)
- **Verify interactions** - Ensure services are called correctly
- **Test isolation** - Each test is independent

## Testing with Event Feeds

In production, your application might use `DataConnector` to consume events from Kafka, files, HTTP endpoints, etc. In tests, you can substitute these with `DataFlow.addEventFeed()`.

### Create a Test Event Feed

```java
@Test
void testWithEventFeed() {
    // Arrange
    when(inventoryService.checkStock(anyString(), anyInt())).thenReturn(true);
    when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(true);
    
    // Create test data
    List<Order> orders = List.of(
        new Order("ORD-001", "CUST-123", "PROD-456", 2, 50.0),
        new Order("ORD-002", "CUST-124", "PROD-789", 1, 100.0),
        new Order("ORD-003", "CUST-125", "PROD-456", 3, 50.0)
    );
    
    // Create a simple test event feed
    EventFeed<Order> testFeed = new EventFeed<>() {
        @Override
        public void subscribe() {
            orders.forEach(dataFlow::onEvent);
        }

        @Override
        public void unSubscribe() {
            // No-op for test
        }
    };
    
    // Act - add feed and subscribe
    dataFlow.addEventFeed(testFeed);
    testFeed.subscribe();
    
    // Assert
    assertEquals(3, orderProcessor.getTotalOrdersProcessed());
    assertEquals(350.0, orderProcessor.getTotalRevenue(), 0.01);
}
```

### Benefits of Event Feed Substitution

- **No infrastructure** - Test without Kafka, databases, or file systems
- **Controlled data** - Use exactly the test data you need
- **Fast execution** - No network or I/O delays
- **Deterministic** - Same input always produces same output

## Testing with Sinks

In production, your application might publish results to databases, message queues, files, or APIs. In tests, you can capture outputs using `DataFlow.addSink()` and `DataFlow.addIntSink()`.

### Capture Primitive Outputs

```java
@Test
void testWithIntSink() {
    // Arrange
    when(inventoryService.checkStock(anyString(), anyInt())).thenReturn(true);
    when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(true);
    
    // Add a sink to capture the count
    AtomicInteger processedCount = new AtomicInteger(0);
    dataFlow.addIntSink("orderCount", processedCount::set);
    
    // Act - process orders and publish to sink
    dataFlow.onEvent(new Order("ORD-001", "CUST-123", "PROD-456", 2, 50.0));
    dataFlow.publishIntSignal("orderCount", orderProcessor.getTotalOrdersProcessed());
    
    dataFlow.onEvent(new Order("ORD-002", "CUST-124", "PROD-789", 1, 100.0));
    dataFlow.publishIntSignal("orderCount", orderProcessor.getTotalOrdersProcessed());
    
    // Assert - verify captured value
    assertEquals(2, processedCount.get());
}
```

### Capture Object Outputs

```java
@Test
void testWithObjectSink() {
    // Arrange
    when(inventoryService.checkStock(anyString(), anyInt())).thenReturn(true);
    when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(true);
    
    // Add a sink to capture summaries
    List<OrderSummary> summaries = new ArrayList<>();
    dataFlow.addSink("summaries", summaries::add);
    
    // Act - process orders and publish summaries
    dataFlow.onEvent(new Order("ORD-001", "CUST-123", "PROD-456", 2, 50.0));
    dataFlow.publishObjectSignal(orderProcessor.getSummary());
    
    dataFlow.onEvent(new Order("ORD-002", "CUST-124", "PROD-789", 1, 100.0));
    dataFlow.publishObjectSignal(orderProcessor.getSummary());
    
    // Assert
    assertEquals(2, summaries.size());
    assertEquals(1, summaries.get(0).getTotalOrdersProcessed());
    assertEquals(100.0, summaries.get(0).getTotalRevenue(), 0.01);
    assertEquals(2, summaries.get(1).getTotalOrdersProcessed());
    assertEquals(200.0, summaries.get(1).getTotalRevenue(), 0.01);
}
```

### Available Sink Methods

- `addSink(String id, Consumer<T> sink)` - Capture objects
- `addIntSink(String id, IntConsumer sink)` - Capture integers
- `addDoubleSink(String id, DoubleConsumer sink)` - Capture doubles
- `addLongSink(String id, LongConsumer sink)` - Capture longs
- `removeSink(String id)` - Remove a sink

### Benefits of Sink Capture

- **Verify outputs** - Ensure correct data is published
- **No infrastructure** - No databases, queues, or APIs needed
- **Simple assertions** - Direct access to captured values
- **Multiple captures** - Test output sequences easily

## Testing Multiple Events

DataFlow processes events synchronously, making batch testing straightforward:

```java
@Test
void testMultipleOrders() {
    // Arrange
    when(inventoryService.checkStock(anyString(), anyInt())).thenReturn(true);
    when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(true);
    
    // Act - process multiple orders
    dataFlow.onEvent(new Order("ORD-001", "CUST-123", "PROD-456", 2, 50.0));
    dataFlow.onEvent(new Order("ORD-002", "CUST-124", "PROD-789", 1, 100.0));
    dataFlow.onEvent(new Order("ORD-003", "CUST-125", "PROD-456", 3, 50.0));
    
    // Assert
    assertEquals(3, orderProcessor.getTotalOrdersProcessed());
    assertEquals(350.0, orderProcessor.getTotalRevenue(), 0.01);
    assertEquals(0, orderProcessor.getFailedOrders());
}
```

## Testing State Changes

Mock services can simulate state changes between events:

```java
@Test
void testServiceStateChanges() {
    // Arrange - first order succeeds, second fails due to inventory depletion
    when(inventoryService.checkStock("PROD-456", 2))
        .thenReturn(true)   // First call succeeds
        .thenReturn(false); // Second call fails
    when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(true);
    
    // Act
    dataFlow.onEvent(new Order("ORD-001", "CUST-123", "PROD-456", 2, 50.0));
    dataFlow.onEvent(new Order("ORD-002", "CUST-124", "PROD-456", 2, 50.0));
    
    // Assert
    assertEquals(1, orderProcessor.getTotalOrdersProcessed());
    assertEquals(1, orderProcessor.getFailedOrders());
    assertEquals(100.0, orderProcessor.getTotalRevenue(), 0.01);
}
```

## Best Practices

### 1. Follow Arrange-Act-Assert Pattern

```java
@Test
void testExample() {
    // Arrange - setup test data and mocks
    when(service.method()).thenReturn(value);
    Event event = new Event(data);
    
    // Act - execute the code under test
    dataFlow.onEvent(event);
    
    // Assert - verify the results
    assertEquals(expected, actual);
    verify(service).method();
}
```

### 2. Use @BeforeEach for Common Setup

```java
@BeforeEach
void setUp() {
    // Create DataFlow
    // Initialize DataFlow
    // Register services
    // Get node references
}
```

### 3. Keep Tests Focused

Each test should verify one specific behavior or scenario. Small, focused tests are easier to understand and maintain.

### 4. Verify Both State and Interactions

```java
// Verify state changes
assertEquals(1, processor.getCount());

// Verify service calls
verify(service).method("param");

// Verify service was NOT called
verify(service, never()).otherMethod();
```

### 5. Test Edge Cases and Failures

Don't just test happy paths. Test:

- Boundary conditions
- Null/empty inputs
- Service failures
- Invalid data
- State transitions

### 6. Use Descriptive Test Names

```java
@Test
void testOrderProcessingSucceedsWhenInventoryAvailableAndPaymentClears() {
    // ...
}

@Test
void testOrderProcessingFailsWhenInsufficientInventory() {
    // ...
}

@Test
void testOrderProcessingFailsWhenPaymentDeclined() {
    // ...
}
```

## Running Tests

### With Maven

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=OrderProcessorTest

# Run specific test method
mvn test -Dtest=OrderProcessorTest#testSuccessfulOrder
```

### With IDE

All modern Java IDEs (IntelliJ IDEA, Eclipse, VS Code) support running JUnit 5 tests. Use the built-in test runner for:

- Running individual tests
- Running all tests in a class
- Running all tests in the project
- Debugging tests
- Viewing test coverage

## Complete Example

For a complete working example with all these patterns, see:

**[JUnit Testing Demo](../../../fluxtion-examples/sample-apps/junit-testing-demo)**

The example includes:

- Order processing business logic
- Service interfaces (PaymentService, InventoryService)
- Comprehensive test suite with 8 different test scenarios
- Examples of all testing patterns described in this guide
- Full Maven project setup

## Summary

Testing DataFlow applications is straightforward because:

1. **Single-threaded execution** - No async complexity
2. **Synchronous processing** - Immediate assertions
3. **Service injection** - Easy mocking with `registerService()`
4. **Event feed substitution** - Test without infrastructure via `addEventFeed()`
5. **Sink capture** - Verify outputs with `addSink()` and `addIntSink()`
6. **Business logic focus** - Test behavior, not infrastructure

This approach results in tests that are:

- **Fast** - Run in milliseconds
- **Reliable** - No flaky tests
- **Focused** - Test business logic only
- **Maintainable** - Clear and simple
- **Deterministic** - Same input, same output

## Further Reading

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [JUnit Testing Demo Example](../../../fluxtion-examples/sample-apps/junit-testing-demo)
