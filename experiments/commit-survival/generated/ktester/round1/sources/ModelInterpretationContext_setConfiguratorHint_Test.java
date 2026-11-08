package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_setConfiguratorHint_Test {

    @Mock
    private Context // mocked Context dependency
    mockContext;

    // instance under test
    private ModelInterpretationContext mic;

    /**
     * Simple POJO used to verify that user‑defined objects are stored
     * without alteration.
     */
    private static class Dummy {

        final int id;

        Dummy(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Dummy))
                return false;
            Dummy dummy = (Dummy) o;
            return id == dummy.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Dummy{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse
        mic = null;
    }

    /**
     * Provides a stream of simple hint objects together with a description.
     * The test verifies that the exact instance (or value) supplied to
     * {@code setConfiguratorHint} is returned unchanged by {@code getConfiguratorHint}.
     */
    static Stream<Arguments> simpleHintProvider() {
        return Stream.of(// null hint
        Arguments.of(null, "null hint"), // plain String
        Arguments.of("simpleStringHint", "String hint"), // mutable Date instance
        Arguments.of(new Date(1_600_000_000_000L), "Date hint"), // empty List instance
        Arguments.of(new ArrayList<>(), "List hint"), // Integer wrapper
        Arguments.of(Integer.valueOf(42), "Integer hint"), // Boolean wrapper
        Arguments.of(Boolean.TRUE, "Boolean hint"), // User‑defined Dummy object
        Arguments.of(new Dummy(7), "Dummy hint"), // int array
        Arguments.of(new int[] { 1, 2, 3 }, "int[] hint"));
    }

    @Test
    void setConfiguratorHint_shouldStoreGivenObject() {
        // GIVEN
        Object hint = new Object();
        // WHEN
        mic.setConfiguratorHint(hint);
        // THEN
        assertSame(hint, mic.getConfiguratorHint(), "Configurator hint should be stored and returned unchanged");
    }

    @Test
    void setConfiguratorHint_shouldOverrideExistingHint() {
        // GIVEN
        Object initialHint = "initial";
        Object newHint = 12345;
        mic.setConfiguratorHint(initialHint);
        assertSame(initialHint, mic.getConfiguratorHint(), "Pre‑condition: initial hint must be stored");
        // WHEN
        mic.setConfiguratorHint(newHint);
        // THEN
        assertSame(newHint, mic.getConfiguratorHint(), "New configurator hint should replace the previous one");
    }

    @Test
    void setConfiguratorHint_shouldAcceptNull() {
        // GIVEN
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertNotNull(mic.getConfiguratorHint(), "Pre‑condition: hint must be non‑null");
        // WHEN
        mic.setConfiguratorHint(null);
        // THEN
        assertNull(mic.getConfiguratorHint(), "Configurator hint should be null after setting null");
    }

    @Test
    void constructor_withConfiguratorHint_shouldInitializeHintCorrectly() {
        // GIVEN
        Object initialHint = "constructorHint";
        // WHEN
        ModelInterpretationContext micWithHint = new ModelInterpretationContext(mockContext, initialHint);
        // THEN
        assertSame(initialHint, micWithHint.getConfiguratorHint(), "Constructor should initialise configuratorHint correctly");
    }

    @Test
    void copyConstructor_shouldCopyConfiguratorHint() {
        // GIVEN
        Object originalHint = "original";
        ModelInterpretationContext original = new ModelInterpretationContext(mockContext, originalHint);
        // WHEN
        ModelInterpretationContext copy = new ModelInterpretationContext(original);
        // THEN
        assertSame(originalHint, copy.getConfiguratorHint(), "Copy constructor must duplicate the configuratorHint from the original instance");
    }

    @ParameterizedTest(name = "setConfiguratorHint with {1}")
    @MethodSource("simpleHintProvider")
    void setConfiguratorHint_shouldStoreVariousObjectTypes(Object hint, String description) {
        // WHEN
        mic.setConfiguratorHint(hint);
        // THEN
        if (hint == null) {
            assertNull(mic.getConfiguratorHint(), "Configurator hint should be null for " + description);
        } else if (hint instanceof int[]) {
            // For arrays we compare reference and contents
            Object retrieved = mic.getConfiguratorHint();
            assertSame(hint, retrieved, "Array reference must be preserved for " + description);
            assertArrayEquals((int[]) hint, (int[]) retrieved, "Array contents must be identical for " + description);
        } else {
            assertSame(hint, mic.getConfiguratorHint(), "Configurator hint should be the same instance for " + description);
        }
    }

    /**
     * Verifies that a mutable Map hint retains reference semantics.
     * After setting the hint, the map is modified and the change must be visible
     * through {@code getConfiguratorHint()}.
     */
    @Test
    void setConfiguratorHint_withMutableMap_shouldReflectSubsequentModifications() {
        // GIVEN: a map with one entry
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        // WHEN: set as configurator hint
        mic.setConfiguratorHint(map);
        // AND: modify the map after setting the hint
        map.put("newKey", "newValue");
        // THEN: retrieved hint should be the same map instance and contain the new entry
        Object retrieved = mic.getConfiguratorHint();
        assertSame(map, retrieved, "Map hint should be stored by reference");
        @SuppressWarnings("unchecked")
        Map<String, String> retrievedMap = (Map<String, String>) retrieved;
        assertEquals(2, retrievedMap.size(), "Modified map should reflect new entries");
        assertEquals("newValue", retrievedMap.get("newKey"), "Modification after setting hint must be visible");
    }

    /**
     * Checks that a second assignment overwrites the first one.
     */
    @Test
    void setConfiguratorHint_multipleAssignments_shouldKeepLatestValue() {
        // GIVEN: first hint
        mic.setConfiguratorHint("firstHint");
        assertEquals("firstHint", mic.getConfiguratorHint(), "Pre‑condition: first hint must be stored");
        // WHEN: second hint overwrites the first
        mic.setConfiguratorHint("secondHint");
        // THEN: only the second hint is retained
        assertEquals("secondHint", mic.getConfiguratorHint(), "Second assignment must replace the first");
    }

    /**
     * Stores a lambda (Runnable) as hint and verifies that the same instance
     * is returned and that executing it produces the expected side‑effect.
     */
    @Test
    void setConfiguratorHint_withRunnableLambda_shouldStoreAndExecute() {
        // GIVEN: a lambda that flips a flag
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable runnable = () -> executed.set(true);
        // WHEN
        mic.setConfiguratorHint(runnable);
        // THEN: the stored hint is the same instance
        Object retrieved = mic.getConfiguratorHint();
        assertSame(runnable, retrieved, "Runnable hint should be stored by reference");
        // AND: executing the retrieved Runnable triggers the side‑effect
        ((Runnable) retrieved).run();
        assertTrue(executed.get(), "Executing the stored Runnable must set the flag");
    }
}
