package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getDefaultNestedComponentRegistry_Test {

    private Context context;

    private ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Use a real Context implementation (ContextBase) – it has no external side‑effects
        context = new ContextBase();
        mic = new ModelInterpretationContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse between tests
        mic = null;
        context = null;
    }

    @Test
    @DisplayName("getDefaultNestedComponentRegistry() should return a non‑null singleton instance")
    void testGetDefaultNestedComponentRegistry_NotNullAndSingleton() {
        // Act
        DefaultNestedComponentRegistry registryFirstCall = mic.getDefaultNestedComponentRegistry();
        DefaultNestedComponentRegistry registrySecondCall = mic.getDefaultNestedComponentRegistry();
        // Assert
        assertNotNull(registryFirstCall, "The registry should never be null after construction");
        assertSame(registryFirstCall, registrySecondCall, "Subsequent calls must return the same DefaultNestedComponentRegistry instance");
    }

    @Test
    @DisplayName("DefaultNestedComponentRegistry instance should be the one created by the constructor")
    void testRegistryCreatedByConstructor() {
        // The constructor creates a new DefaultNestedComponentRegistry internally.
        // We verify that the returned instance is of the expected type.
        DefaultNestedComponentRegistry registry = mic.getDefaultNestedComponentRegistry();
        assertTrue(registry instanceof DefaultNestedComponentRegistry, "Returned object should be an instance of DefaultNestedComponentRegistry");
    }

    @Test
    @DisplayName("Returned registry must be the same object stored in the private field")
    void testRegistryMatchesPrivateField() throws Exception {
        // Use reflection to obtain the private field value
        Field field = ModelInterpretationContext.class.getDeclaredField("defaultNestedComponentRegistry");
        field.setAccessible(true);
        Object fieldValue = field.get(mic);
        // The getter should return exactly the same reference
        DefaultNestedComponentRegistry registryFromGetter = mic.getDefaultNestedComponentRegistry();
        assertSame(fieldValue, registryFromGetter, "The registry returned by the getter must be the exact same instance as the private field");
    }

    @Test
    @DisplayName("Mutations on the returned registry are reflected on subsequent calls")
    void testRegistryMutationReflected() {
        // Arrange: obtain the registry and perform a mutation.
        // DefaultNestedComponentRegistry provides a register(String, Class<?>) method.
        DefaultNestedComponentRegistry registry = mic.getDefaultNestedComponentRegistry();
        String componentName = "dummyComponent";
        // any class works for the test
        Class<?> componentClass = String.class;
        // Act: register a dummy component
        registry.register(componentName, componentClass);
        // Retrieve the registry again and verify the mutation persists
        DefaultNestedComponentRegistry registryAgain = mic.getDefaultNestedComponentRegistry();
        Class<?> retrievedClass = registryAgain.getComponentClass(componentName);
        // Assert
        assertEquals(componentClass, retrievedClass, "The component class registered earlier must be visible on subsequent registry retrievals");
    }

    @Test
    @DisplayName("Two distinct ModelInterpretationContext objects have different registries")
    void testDifferentContextsHaveDifferentRegistries() {
        // Arrange: create a second context and its ModelInterpretationContext
        Context anotherContext = new ContextBase();
        ModelInterpretationContext anotherMic = new ModelInterpretationContext(anotherContext);
        // Act: obtain registries from both contexts
        DefaultNestedComponentRegistry registryFirst = mic.getDefaultNestedComponentRegistry();
        DefaultNestedComponentRegistry registrySecond = anotherMic.getDefaultNestedComponentRegistry();
        // Assert: they must not be the same instance (registry is per‑context)
        assertNotSame(registryFirst, registrySecond, "Each ModelInterpretationContext should own its own DefaultNestedComponentRegistry instance");
        // Cleanup the second context explicitly (not strictly required but keeps symmetry)
        anotherMic = null;
    }
}
