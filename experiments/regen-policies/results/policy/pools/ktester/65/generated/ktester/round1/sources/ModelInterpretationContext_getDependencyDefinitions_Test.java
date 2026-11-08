package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getDependencyDefinitions_Test {

    @Mock
    Context // provided by MockitoExtension
    mockContext;

    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor under test only needs a Context (which we mock)
        mic = new ModelInterpretationContext(mockContext);
        // The constructor of ModelInterpretationContext expects a Context.
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void getDependencyDefinitions_returnsEmptyUnmodifiableListInitially() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        // Should be empty on a fresh instance
        assertTrue(deps.isEmpty(), "Dependency list should be empty on a fresh MIC");
        // Must be unmodifiable – any structural modification must throw
        assertThrows(UnsupportedOperationException.class, () -> deps.add(mock(DependencyDefinition.class)), "Adding to the list must throw UnsupportedOperationException");
        assertThrows(UnsupportedOperationException.class, () -> deps.remove(0), "Removing from the list must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_reflectsAddedDefinitions() {
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        // Use the public API to add definitions
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        // Verify size and order
        assertEquals(2, deps.size(), "Two definitions should be present");
        assertSame(dd1, deps.get(0), "First definition should be the one added first");
        assertSame(dd2, deps.get(1), "Second definition should be the one added second");
        // Ensure the returned list is still unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> deps.clear());
    }

    @Test
    void getDependencyDefinitions_returnsIndependentViewEachCall() {
        DependencyDefinition dd = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd);
        List<DependencyDefinition> firstCall = mic.getDependencyDefinitions();
        List<DependencyDefinition> secondCall = mic.getDependencyDefinitions();
        // Both calls should return equal (content‑wise) lists
        assertEquals(firstCall, secondCall);
        // But they must be distinct objects (different wrappers) – modifying one should not affect the other
        assertNotSame(firstCall, secondCall);
        // Both must still be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> firstCall.add(mock(DependencyDefinition.class)));
        assertThrows(UnsupportedOperationException.class, () -> secondCall.remove(0));
    }

    /* -------------------------------------------------------------
     *  Live view – later additions are visible on previously obtained list
     * ------------------------------------------------------------- */
    @Test
    void getDependencyDefinitions_listReflectsLaterAdditions() {
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        DependencyDefinition dd3 = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        // Obtain the view before the third addition
        List<DependencyDefinition> snapshot = mic.getDependencyDefinitions();
        // Verify snapshot currently contains two elements
        assertEquals(2, snapshot.size());
        // Add a third definition after the snapshot was taken
        mic.addDependencyDefinition(dd3);
        // The previously obtained list must now also contain the third element
        assertEquals(3, snapshot.size(), "Snapshot should reflect the newly added definition");
        assertSame(dd3, snapshot.get(2), "Third element should be the newly added definition");
    }

    /* -------------------------------------------------------------
     *  Unmodifiable list operations – each must throw UnsupportedOperationException
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: operation {0} throws")
    @ValueSource(strings = { "addAll", "removeAll", "retainAll", "clear", "iteratorRemove" })
    void unmodifiableOperations_throwUnsupportedOperationException(String operation) {
        // Prepare a list with two definitions
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        switch(operation) {
            case "addAll" ->
                {
                    Collection<DependencyDefinition> toAdd = List.of(mock(DependencyDefinition.class));
                    assertThrows(UnsupportedOperationException.class, () -> deps.addAll(toAdd), "addAll must throw UnsupportedOperationException");
                }
            case "removeAll" ->
                {
                    Collection<DependencyDefinition> toRemove = List.of(dd1);
                    assertThrows(UnsupportedOperationException.class, () -> deps.removeAll(toRemove), "removeAll must throw UnsupportedOperationException");
                }
            case "retainAll" ->
                {
                    Collection<DependencyDefinition> toRetain = List.of(dd1);
                    assertThrows(UnsupportedOperationException.class, () -> deps.retainAll(toRetain), "retainAll must throw UnsupportedOperationException");
                }
            case "clear" ->
                {
                    assertThrows(UnsupportedOperationException.class, deps::clear, "clear must throw UnsupportedOperationException");
                }
            case "iteratorRemove" ->
                {
                    Iterator<DependencyDefinition> it = deps.iterator();
                    assertTrue(it.hasNext(), "Iterator should have at least one element");
                    // advance to first element
                    it.next();
                    assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove must throw UnsupportedOperationException");
                }
            default ->
                fail("Unexpected operation: " + operation);
        }
    }
}
