package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getDependencyDefinitions_Test {

    @Mock
    Context // provided by MockitoExtension
    mockContext;

    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor of ModelInterpretationContext expects a Context.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern symmetrical.
        mic = null;
    }

    /* -------------------------------------------------------------
     *  3. Unmodifiable list behaviour – all mutating operations
     * ------------------------------------------------------------- */
    static Stream<Arguments> mutatingOperationsProvider() {
        return Stream.of(Arguments.of("add", (Consumer<List<DependencyDefinition>>) list -> list.add(mock(DependencyDefinition.class))), Arguments.of("addAll", (Consumer<List<DependencyDefinition>>) list -> list.addAll(List.of(mock(DependencyDefinition.class)))), Arguments.of("addAtIndex", (Consumer<List<DependencyDefinition>>) list -> list.add(0, mock(DependencyDefinition.class))), Arguments.of("addAllAtIndex", (Consumer<List<DependencyDefinition>>) list -> list.addAll(0, List.of(mock(DependencyDefinition.class)))), Arguments.of("remove", (Consumer<List<DependencyDefinition>>) list -> list.remove(0)), Arguments.of("removeObject", (Consumer<List<DependencyDefinition>>) list -> list.remove(mock(DependencyDefinition.class))), Arguments.of("removeAll", (Consumer<List<DependencyDefinition>>) list -> list.removeAll(List.of(mock(DependencyDefinition.class)))), Arguments.of("retainAll", (Consumer<List<DependencyDefinition>>) list -> list.retainAll(List.of(mock(DependencyDefinition.class)))), Arguments.of("clear", (Consumer<List<DependencyDefinition>>) List::clear), Arguments.of("set", (Consumer<List<DependencyDefinition>>) list -> list.set(0, mock(DependencyDefinition.class))), Arguments.of("iteratorRemove", (Consumer<List<DependencyDefinition>>) list -> {
            Iterator<DependencyDefinition> it = list.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }), Arguments.of("listIteratorAdd", (Consumer<List<DependencyDefinition>>) list -> {
            ListIterator<DependencyDefinition> it = list.listIterator();
            it.add(mock(DependencyDefinition.class));
        }), Arguments.of("listIteratorSet", (Consumer<List<DependencyDefinition>>) list -> {
            ListIterator<DependencyDefinition> it = list.listIterator();
            if (it.hasNext()) {
                it.next();
                it.set(mock(DependencyDefinition.class));
            }
        }), Arguments.of("replaceAll", (Consumer<List<DependencyDefinition>>) list -> list.replaceAll(d -> d)), Arguments.of("sort", (Consumer<List<DependencyDefinition>>) list -> list.sort(Comparator.naturalOrder())), Arguments.of("removeIf", (Consumer<List<DependencyDefinition>>) list -> list.removeIf(d -> true)), Arguments.of("subListClear", (Consumer<List<DependencyDefinition>>) list -> {
            List<DependencyDefinition> sub = list.subList(0, list.size());
            sub.clear();
        }));
    }

    /**
     * Verify that the list returned by {@code getDependencyDefinitions()}
     * reflects the definitions that have been added via {@code addDependencyDefinition}.
     */
    @Test
    void getDependencyDefinitionsReflectsAddedDefinitions() {
        // given
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        // when
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        List<DependencyDefinition> result = mic.getDependencyDefinitions();
        // then
        assertEquals(2, result.size(), "The list should contain exactly two definitions");
        assertTrue(result.contains(dd1), "The first definition should be present");
        assertTrue(result.contains(dd2), "The second definition should be present");
    }

    /**
     * Verify that the list returned by {@code getDependencyDefinitions()}
     * is unmodifiable – attempts to modify it must throw {@link UnsupportedOperationException}.
     */
    @Test
    void getDependencyDefinitionsReturnsUnmodifiableList() {
        // given
        DependencyDefinition dd = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd);
        List<DependencyDefinition> result = mic.getDependencyDefinitions();
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> result.add(mock(DependencyDefinition.class)), "The returned list must be unmodifiable");
        assertThrows(UnsupportedOperationException.class, () -> result.remove(0), "The returned list must be unmodifiable");
    }

    /**
     * Verify that the returned list is a snapshot view – modifications to the
     * internal list after the call are reflected in subsequent calls, but the
     * previously obtained list remains unchanged (i.e., it is not a live view).
     */
    @Test
    void getDependencyDefinitionsIsSnapshotNotLiveView() {
        // given
        DependencyDefinition first = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(first);
        List<DependencyDefinition> firstSnapshot = mic.getDependencyDefinitions();
        // when
        DependencyDefinition second = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(second);
        List<DependencyDefinition> secondSnapshot = mic.getDependencyDefinitions();
        // then
        // first snapshot should still contain only the first element
        assertEquals(1, firstSnapshot.size(), "First snapshot should retain the size at the time it was taken");
        assertTrue(firstSnapshot.contains(first));
        assertFalse(firstSnapshot.contains(second));
        // second snapshot should contain both
        assertEquals(2, secondSnapshot.size(), "Second snapshot should reflect the additional definition");
        assertTrue(secondSnapshot.contains(first));
        assertTrue(secondSnapshot.contains(second));
    }

    /* -------------------------------------------------------------
     *  1. Empty list
     * ------------------------------------------------------------- */
    @Test
    void emptyDependencyListReturnsEmptyUnmodifiableList() {
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertTrue(list.isEmpty(), "List should be empty when no definitions have been added");
        assertThrows(UnsupportedOperationException.class, () -> list.add(mock(DependencyDefinition.class)), "Returned list must be unmodifiable");
    }

    /* -------------------------------------------------------------
     *  2. Non‑empty list – order preservation
     * ------------------------------------------------------------- */
    @Test
    void nonEmptyListPreservesInsertionOrder_twoElements() {
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        List<DependencyDefinition> result = mic.getDependencyDefinitions();
        assertEquals(List.of(dd1, dd2), result, "The list must contain the added definitions in insertion order");
    }

    @Test
    void nonEmptyListPreservesInsertionOrder_threeElements() {
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        DependencyDefinition dd3 = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        mic.addDependencyDefinition(dd3);
        List<DependencyDefinition> result = mic.getDependencyDefinitions();
        assertEquals(List.of(dd1, dd2, dd3), result, "All added definitions must appear in the same order they were added");
    }

    @ParameterizedTest(name = "Unmodifiable operation ''{0}'' must throw UnsupportedOperationException")
    @MethodSource("mutatingOperationsProvider")
    void unmodifiableListOperationsThrow(String operationName, Consumer<List<DependencyDefinition>> mutator) {
        // Arrange – add a single element so that the list is non‑empty
        mic.addDependencyDefinition(mock(DependencyDefinition.class));
        List<DependencyDefinition> view = mic.getDependencyDefinitions();
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> mutator.accept(view), "Operation '" + operationName + "' should not be supported on the returned list");
    }

    /* -------------------------------------------------------------
     *  4. Live view consistency
     * ------------------------------------------------------------- */
    @Test
    void liveViewReflectsSubsequentAdditions() {
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd1);
        List<DependencyDefinition> firstView = mic.getDependencyDefinitions();
        // The view must already contain dd1
        assertEquals(List.of(dd1), firstView);
        // Add a second definition after the view was obtained
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd2);
        // Both the original view and a fresh view must now contain both elements
        assertEquals(List.of(dd1, dd2), firstView, "The originally obtained view must reflect the later addition (live view)");
        List<DependencyDefinition> secondView = mic.getDependencyDefinitions();
        assertEquals(List.of(dd1, dd2), secondView, "A new call must also reflect the full current state");
    }

    @Test
    void multipleViewsAllReflectLatestState() {
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd1);
        List<DependencyDefinition> view1 = mic.getDependencyDefinitions();
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd2);
        List<DependencyDefinition> view2 = mic.getDependencyDefinitions();
        // Both references must show the same up‑to‑date content
        List<DependencyDefinition> expected = List.of(dd1, dd2);
        assertEquals(expected, view1, "First view must have been updated");
        assertEquals(expected, view2, "Second view must contain the same elements");
    }

    /* -------------------------------------------------------------
     *  5. Identity across successive calls
     * ------------------------------------------------------------- */
    @Test
    void successiveCallsReturnDistinctWrapperInstances() {
        mic.addDependencyDefinition(mock(DependencyDefinition.class));
        List<DependencyDefinition> first = mic.getDependencyDefinitions();
        List<DependencyDefinition> second = mic.getDependencyDefinitions();
        assertNotSame(first, second, "Each call should return a distinct unmodifiable wrapper");
        assertEquals(first, second, "Both wrappers must present equal content");
    }

    /* -------------------------------------------------------------
     *  6. Null handling
     * ------------------------------------------------------------- */
    @Test
    void addingNullDependencyDefinitionIsAllowed() {
        mic.addDependencyDefinition(null);
        List<DependencyDefinition> result = mic.getDependencyDefinitions();
        assertEquals(1, result.size(), "List should contain exactly one element");
        assertNull(result.get(0), "The stored element must be null");
    }

    /* -------------------------------------------------------------
     *  7. Iterator remove operation on the unmodifiable list
     * ------------------------------------------------------------- */
    @Test
    void iteratorRemoveThrowsUnsupportedOperationException() {
        mic.addDependencyDefinition(mock(DependencyDefinition.class));
        List<DependencyDefinition> view = mic.getDependencyDefinitions();
        Iterator<DependencyDefinition> it = view.iterator();
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() must throw UnsupportedOperationException on an unmodifiable list");
    }

    /* -------------------------------------------------------------
     *  8. SubList immutability
     * ------------------------------------------------------------- */
    @Test
    void subListClearThrowsUnsupportedOperationException() {
        mic.addDependencyDefinition(mock(DependencyDefinition.class));
        mic.addDependencyDefinition(mock(DependencyDefinition.class));
        List<DependencyDefinition> view = mic.getDependencyDefinitions();
        List<DependencyDefinition> sub = view.subList(0, view.size());
        assertThrows(UnsupportedOperationException.class, sub::clear, "clear() on a subList of an unmodifiable list must throw");
    }
}
