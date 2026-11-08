package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getDefaultNestedComponentRegistry_Test {

    // Mocked Logback Context – required by the ModelInterpretationContext constructor
    @Mock
    Context mockContext;

    // Instance created before each test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for every test method
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void getDefaultNestedComponentRegistry_returnsNonNullInstance() {
        // When
        DefaultNestedComponentRegistry registry = mic.getDefaultNestedComponentRegistry();
        // Then
        assertNotNull(registry, "The DefaultNestedComponentRegistry should never be null");
    }

    @Test
    void getDefaultNestedComponentRegistry_returnsSameInstanceOnRepeatedCalls() {
        // When
        DefaultNestedComponentRegistry firstCall = mic.getDefaultNestedComponentRegistry();
        DefaultNestedComponentRegistry secondCall = mic.getDefaultNestedComponentRegistry();
        // Then
        assertSame(firstCall, secondCall, "Multiple invocations of getDefaultNestedComponentRegistry() must return the identical object");
    }

    @Test
    void copyConstructorCreatesDistinctRegistryInstance() {
        // Given: original context with its registry
        DefaultNestedComponentRegistry originalRegistry = mic.getDefaultNestedComponentRegistry();
        // When: a new ModelInterpretationContext is created using the copy‑constructor
        ModelInterpretationContext micCopy = new ModelInterpretationContext(mic);
        DefaultNestedComponentRegistry copiedRegistry = micCopy.getDefaultNestedComponentRegistry();
        // Then: the registry objects must be different (the copy constructor duplicates the registry)
        assertNotSame(originalRegistry, copiedRegistry, "The copy‑constructor should duplicate the DefaultNestedComponentRegistry, not share the same instance");
    }

    /**
     * Using reflection, obtain the private final field {@code defaultNestedComponentRegistry}
     * from the {@link ModelInterpretationContext} instance and compare it with the value
     * returned by {@code getDefaultNestedComponentRegistry()}. They must be identical,
     * proving the getter simply returns the field.
     */
    @Test
    void getDefaultNestedComponentRegistry_returnsFieldInstanceViaReflection() throws Exception {
        // When: retrieve the value via the public getter
        DefaultNestedComponentRegistry viaGetter = mic.getDefaultNestedComponentRegistry();
        // And: obtain the private field value using reflection
        Field field = ModelInterpretationContext.class.getDeclaredField("defaultNestedComponentRegistry");
        field.setAccessible(true);
        Object fieldValue = field.get(mic);
        // Then: both references must point to the same object
        assertSame(viaGetter, fieldValue, "The getter should return the exact instance stored in the private field");
    }
}
