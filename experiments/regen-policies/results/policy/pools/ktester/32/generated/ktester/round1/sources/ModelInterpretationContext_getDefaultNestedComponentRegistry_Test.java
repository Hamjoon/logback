package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.model.Model;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getDefaultNestedComponentRegistry_Test {

    @Mock
    Context // Mockito will create a mock Context
    mockContext;

    // primary instance
    ModelInterpretationContext mic;

    // instance created via copy‑constructor
    ModelInterpretationContext micCopy;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required.
        mic = null;
        micCopy = null;
    }

    @Test
    void getDefaultNestedComponentRegistry_returnsNonNullAndSameInstance() {
        // When
        DefaultNestedComponentRegistry registryFirstCall = mic.getDefaultNestedComponentRegistry();
        DefaultNestedComponentRegistry registrySecondCall = mic.getDefaultNestedComponentRegistry();
        // Then
        assertNotNull(registryFirstCall, "The registry should never be null");
        assertSame(registryFirstCall, registrySecondCall, "Multiple calls should return the identical DefaultNestedComponentRegistry instance");
    }

    @Test
    void copyConstructorCreatesSeparateDefaultNestedComponentRegistry() {
        // Given an original context with its registry
        DefaultNestedComponentRegistry originalRegistry = mic.getDefaultNestedComponentRegistry();
        // When a new context is created via the copy‑constructor
        micCopy = new ModelInterpretationContext(mic);
        DefaultNestedComponentRegistry copyRegistry = micCopy.getDefaultNestedComponentRegistry();
        // Then the two registries must be distinct objects (the copy duplicates content, not the reference)
        assertNotNull(copyRegistry, "The copied registry should not be null");
        assertNotSame(originalRegistry, copyRegistry, "The copy constructor should produce a separate DefaultNestedComponentRegistry instance");
    }

    @Test
    void getDefaultNestedComponentRegistry_returnsDifferentInstancesForDifferentContexts() {
        // Create a second mock Context to avoid sharing the same mock instance.
        Context anotherMockContext = mock(Context.class);
        ModelInterpretationContext mic2 = new ModelInterpretationContext(anotherMockContext);
        DefaultNestedComponentRegistry reg1 = mic.getDefaultNestedComponentRegistry();
        DefaultNestedComponentRegistry reg2 = mic2.getDefaultNestedComponentRegistry();
        assertNotSame(reg1, reg2, "Two separate ModelInterpretationContext objects must have distinct registries");
    }

    @Test
    void getDefaultNestedComponentRegistry_returnsExactType() {
        DefaultNestedComponentRegistry registry = mic.getDefaultNestedComponentRegistry();
        assertEquals(DefaultNestedComponentRegistry.class, registry.getClass(), "The returned object must be of type DefaultNestedComponentRegistry");
    }

    @Test
    void getDefaultNestedComponentRegistry_stableAfterAddDependencyDefinition() {
        DefaultNestedComponentRegistry before = mic.getDefaultNestedComponentRegistry();
        // DependencyDefinition is a package‑private class; we can safely mock it.
        DependencyDefinition dd = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd);
        DefaultNestedComponentRegistry after = mic.getDefaultNestedComponentRegistry();
        assertSame(before, after, "Registry reference must stay the same after adding a dependency definition");
    }

    @Test
    void getDefaultNestedComponentRegistry_stableAfterAddImport() {
        DefaultNestedComponentRegistry before = mic.getDefaultNestedComponentRegistry();
        mic.addImport("myStem", "my.FullyQualifiedClassName");
        DefaultNestedComponentRegistry after = mic.getDefaultNestedComponentRegistry();
        assertSame(before, after, "Registry reference must stay the same after adding an import");
    }

    @Test
    void getDefaultNestedComponentRegistry_stableAfterStackOperations() {
        DefaultNestedComponentRegistry before = mic.getDefaultNestedComponentRegistry();
        // Push a dummy object and a dummy Model, then pop them.
        Object dummyObject = new Object();
        Model dummyModel = mock(Model.class);
        mic.pushObject(dummyObject);
        mic.pushModel(dummyModel);
        // Ensure the stacks are not empty before popping.
        assertFalse(mic.isObjectStackEmpty(), "Object stack should contain the dummy object");
        assertFalse(mic.isModelStackEmpty(), "Model stack should contain the dummy model");
        mic.popObject();
        mic.popModel();
        DefaultNestedComponentRegistry after = mic.getDefaultNestedComponentRegistry();
        assertSame(before, after, "Registry reference must stay the same after push/pop operations on the stacks");
    }

    @Test
    void getDefaultNestedComponentRegistry_stableAfterPropertyMapRetrieval() {
        DefaultNestedComponentRegistry before = mic.getDefaultNestedComponentRegistry();
        // This call creates a copy of the internal property map.
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertNotNull(copy, "Property map copy should not be null");
        DefaultNestedComponentRegistry after = mic.getDefaultNestedComponentRegistry();
        assertSame(before, after, "Registry reference must stay the same after retrieving a copy of the property map");
    }

    @Test
    void getDefaultNestedComponentRegistry_isolationBetweenContextsAfterMutation() {
        // Two independent contexts.
        Context anotherMockContext = mock(Context.class);
        ModelInterpretationContext micA = new ModelInterpretationContext(mockContext);
        ModelInterpretationContext micB = new ModelInterpretationContext(anotherMockContext);
        DefaultNestedComponentRegistry regAInitial = micA.getDefaultNestedComponentRegistry();
        DefaultNestedComponentRegistry regBInitial = micB.getDefaultNestedComponentRegistry();
        // Mutate micA.
        DependencyDefinition dd = mock(DependencyDefinition.class);
        micA.addDependencyDefinition(dd);
        // Verify that the registries remain distinct.
        DefaultNestedComponentRegistry regAFinal = micA.getDefaultNestedComponentRegistry();
        DefaultNestedComponentRegistry regBFinal = micB.getDefaultNestedComponentRegistry();
        assertNotSame(regAFinal, regBFinal, "Registries of two contexts must stay distinct even after mutating one context");
        assertSame(regAInitial, regAFinal, "Mutation of a context must not replace its DefaultNestedComponentRegistry instance");
    }
}
