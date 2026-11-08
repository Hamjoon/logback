package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getDependeeNamesForModel_Test {

    @Mock
    Context // mock for the required Context dependency
    mockContext;

    // object under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor of ModelInterpretationContext expects a Context.
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Verify that an empty dependency list is returned when no
     * {@link DependencyDefinition}s have been added.
     */
    @Test
    void returnsEmptyListWhenNoDependencies() {
        Model model = Mockito.mock(Model.class);
        List<String> result = mic.getDependeeNamesForModel(model);
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Result should be empty when no dependencies are defined");
    }

    /**
     * Verify that only the dependee names belonging to the supplied model are
     * returned.
     */
    @Test
    void returnsOnlyDependeesForGivenModel() {
        // ----- arrange -------------------------------------------------------
        Model modelA = Mockito.mock(Model.class);
        Model modelB = Mockito.mock(Model.class);
        // Create three mock DependencyDefinition objects.
        DependencyDefinition dd1 = Mockito.mock(DependencyDefinition.class);
        DependencyDefinition dd2 = Mockito.mock(DependencyDefinition.class);
        DependencyDefinition dd3 = Mockito.mock(DependencyDefinition.class);
        // Stub their behaviour.
        Mockito.when(dd1.getDepender()).thenReturn(modelA);
        Mockito.when(dd1.getDependee()).thenReturn("firstDependee");
        Mockito.when(dd2.getDepender()).thenReturn(modelA);
        Mockito.when(dd2.getDependee()).thenReturn("secondDependee");
        Mockito.when(dd3.getDepender()).thenReturn(modelB);
        Mockito.when(dd3.getDependee()).thenReturn("otherDependee");
        // Populate the MIC with the mocked definitions.
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        mic.addDependencyDefinition(dd3);
        // ----- act -----------------------------------------------------------
        List<String> resultForA = mic.getDependeeNamesForModel(modelA);
        List<String> resultForB = mic.getDependeeNamesForModel(modelB);
        // ----- assert --------------------------------------------------------
        // For modelA we expect exactly the two dependee names we added.
        assertEquals(2, resultForA.size(), "Model A should have two dependee names");
        assertEquals("firstDependee", resultForA.get(0));
        assertEquals("secondDependee", resultForA.get(1));
        // For modelB we expect the single dependee name.
        assertEquals(1, resultForB.size(), "Model B should have one dependee name");
        assertEquals("otherDependee", resultForB.get(0));
    }

    /**
     * Verify that the method is robust when the same {@link DependencyDefinition}
     * instance is added multiple times – the returned list will contain duplicate
     * entries, reflecting the internal collection semantics.
     */
    @Test
    void handlesDuplicateDependencyDefinitions() {
        Model model = Mockito.mock(Model.class);
        DependencyDefinition dd = Mockito.mock(DependencyDefinition.class);
        Mockito.when(dd.getDepender()).thenReturn(model);
        Mockito.when(dd.getDependee()).thenReturn("dupDep");
        // Add the same definition twice.
        mic.addDependencyDefinition(dd);
        mic.addDependencyDefinition(dd);
        List<String> result = mic.getDependeeNamesForModel(model);
        assertEquals(2, result.size(), "Both added definitions should be reflected in the result");
        assertEquals("dupDep", result.get(0));
        assertEquals("dupDep", result.get(1));
    }

    /**
     * Verify that a single matching dependency is returned correctly.
     */
    @Test
    void returnsSingleMatchingDependee() {
        Model modelA = Mockito.mock(Model.class);
        DependencyDefinition dd = Mockito.mock(DependencyDefinition.class);
        Mockito.when(dd.getDepender()).thenReturn(modelA);
        Mockito.when(dd.getDependee()).thenReturn("dependee1");
        mic.addDependencyDefinition(dd);
        List<String> result = mic.getDependeeNamesForModel(modelA);
        assertEquals(1, result.size(), "Exactly one dependee should be returned");
        assertEquals("dependee1", result.get(0));
    }

    /**
     * Verify that the method throws {@link NullPointerException} when the
     * internal list contains a {@code null} entry.
     *
     * The list itself is populated via reflection because the public API does
     * not allow adding a {@code null} definition.
     */
    @Test
    void throwsNpeWhenNullDependencyDefinitionInList() throws Exception {
        // ----- arrange -------------------------------------------------------
        // Obtain the private field and make it accessible.
        Field listField = ModelInterpretationContext.class.getDeclaredField("dependencyDefinitionList");
        listField.setAccessible(true);
        // Cast to the concrete list implementation.
        @SuppressWarnings("unchecked")
        List<DependencyDefinition> list = (List<DependencyDefinition>) listField.get(mic);
        // Insert a null entry.
        list.add(null);
        Model anyModel = Mockito.mock(Model.class);
        // ----- act & assert --------------------------------------------------
        assertThrows(NullPointerException.class, () -> mic.getDependeeNamesForModel(anyModel), "Iterating over a null entry should cause NPE");
    }

    /**
     * Verify that the method throws {@link NullPointerException} when the
     * internal {@code dependencyDefinitionList} field itself is {@code null}.
     *
     * This situation is artificial (normally the list is always initialised) but
     * useful to ensure the method does not silently swallow such a programming
     * error.
     */
    @Test
    void throwsNpeWhenDependencyDefinitionListIsNull() throws Exception {
        // ----- arrange -------------------------------------------------------
        Field listField = ModelInterpretationContext.class.getDeclaredField("dependencyDefinitionList");
        listField.setAccessible(true);
        // Set the field to null via reflection.
        listField.set(mic, null);
        Model anyModel = Mockito.mock(Model.class);
        // ----- act & assert --------------------------------------------------
        assertThrows(NullPointerException.class, () -> mic.getDependeeNamesForModel(anyModel), "Iterating over a null collection should cause NPE");
    }
}
