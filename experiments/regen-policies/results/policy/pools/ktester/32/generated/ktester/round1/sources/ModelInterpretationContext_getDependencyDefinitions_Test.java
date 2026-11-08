package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ModelInterpretationContext#getDependencyDefinitions()}.
 *
 * The tests verify that the returned list:
 * <ul>
 *   <li>is unmodifiable (all mutating operations throw {@link UnsupportedOperationException}),</li>
 *   <li>is a live view of the internal {@code dependencyDefinitionList},</li>
 *   <li>behaves correctly for read‑only operations,</li>
 *   <li>handles edge cases such as null entries and empty lists.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getDependencyDefinitions_Test {

    @Mock
    Context mockContext;                     // mocked Context supplied to the MIC constructor

    ModelInterpretationContext mic;          // instance under test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default create a fresh MIC and populate it with two dummy DependencyDefinition objects.
        mic = new ModelInterpretationContext(mockContext);
        mic.addDependencyDefinition(mock(DependencyDefinition.class));
        mic.addDependencyDefinition(mock(DependencyDefinition.class));
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /* --------------------------------------------------------------------- */
    /*  Basic immutability checks                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void getDependencyDefinitions_isUnmodifiable_add() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        assertThrows(UnsupportedOperationException.class,
                () -> deps.add(mock(DependencyDefinition.class)),
                "add() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_isUnmodifiable_remove() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        assertThrows(UnsupportedOperationException.class,
                () -> deps.remove(0),
                "remove() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_isUnmodifiable_clear() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        assertThrows(UnsupportedOperationException.class,
                deps::clear,
                "clear() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_isUnmodifiable_addAll() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        List<DependencyDefinition> toAdd = List.of(mock(DependencyDefinition.class));
        assertThrows(UnsupportedOperationException.class,
                () -> deps.addAll(toAdd),
                "addAll() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_isUnmodifiable_removeAll() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        List<DependencyDefinition> toRemove = List.of(mock(DependencyDefinition.class));
        assertThrows(UnsupportedOperationException.class,
                () -> deps.removeAll(toRemove),
                "removeAll() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_isUnmodifiable_retainAll() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        List<DependencyDefinition> subset = List.of(mock(DependencyDefinition.class));
        assertThrows(UnsupportedOperationException.class,
                () -> deps.retainAll(subset),
                "retainAll() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_isUnmodifiable_set() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        assertThrows(UnsupportedOperationException.class,
                () -> deps.set(0, mock(DependencyDefinition.class)),
                "set() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_isUnmodifiable_replaceAll() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        UnaryOperator<DependencyDefinition> identity = e -> e;
        assertThrows(UnsupportedOperationException.class,
                () -> deps.replaceAll(identity),
                "replaceAll() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_isUnmodifiable_sort() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        Comparator<DependencyDefinition> cmp = (a, b) -> 0;
        assertThrows(UnsupportedOperationException.class,
                () -> deps.sort(cmp),
                "sort() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_isUnmodifiable_removeIf() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        assertThrows(UnsupportedOperationException.class,
                () -> deps.removeIf(d -> true),
                "removeIf() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_iteratorRemoveUnsupported() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        Iterator<DependencyDefinition> it = deps.iterator();
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        it.next(); // move to first element
        assertThrows(UnsupportedOperationException.class,
                it::remove,
                "Iterator.remove() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_listIteratorAddUnsupported() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        ListIterator<DependencyDefinition> li = deps.listIterator();
        assertThrows(UnsupportedOperationException.class,
                () -> li.add(mock(DependencyDefinition.class)),
                "ListIterator.add() must throw UnsupportedOperationException");
    }

    @Test
    void getDependencyDefinitions_listIteratorSetUnsupported() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        ListIterator<DependencyDefinition> li = deps.listIterator();
        assertTrue(li.hasNext(), "ListIterator should have at least one element");
        li.next(); // point to first element
        assertThrows(UnsupportedOperationException.class,
                () -> li.set(mock(DependencyDefinition.class)),
                "ListIterator.set() must throw UnsupportedOperationException");
    }

    /* --------------------------------------------------------------------- */
    /*  Live‑view behaviour                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    void getDependencyDefinitions_liveViewReflectsLaterAdditions() {
        // Retrieve the list before a new addition.
        List<DependencyDefinition> view = mic.getDependencyDefinitions();
        int sizeBefore = view.size();

        // Add another definition.
        DependencyDefinition newDd = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(newDd);

        // The previously retrieved view must reflect the new size.
        assertEquals(sizeBefore + 1, view.size(),
                "The unmodifiable view should be live and reflect added elements");
        assertTrue(view.contains(newDd), "The newly added element must be visible in the view");
    }

    @Test
    void getDependencyDefinitions_multipleRetrievalsShareLiveView() {
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd1);

        List<DependencyDefinition> listA = mic.getDependencyDefinitions();

        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        mic.addDependencyDefinition(dd2);

        List<DependencyDefinition> listB = mic.getDependencyDefinitions();

        assertEquals(2, listA.size(), "listA should reflect the second addition");
        assertEquals(2, listB.size(), "listB should reflect the second addition");
        assertTrue(listA.containsAll(List.of(dd1, dd2)));
        assertTrue(listB.containsAll(List.of(dd1, dd2)));
    }

    /* --------------------------------------------------------------------- */
    /*  Read‑only operations                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    void getDependencyDefinitions_readOnlyOperationsWork() {
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        DependencyDefinition dd3 = mock(DependencyDefinition.class);
        mic = new ModelInterpretationContext(mockContext); // start fresh
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        mic.addDependencyDefinition(dd3);

        List<DependencyDefinition> deps = mic.getDependencyDefinitions();

        // size & order
        assertEquals(3, deps.size());
        assertEquals(dd1, deps.get(0));
        assertEquals(dd2, deps.get(1));
        assertEquals(dd3, deps.get(2));

        // contains & indexOf
        assertTrue(deps.contains(dd2));
        assertEquals(2, deps.indexOf(dd3));

        // toArray
        Object[] arr = deps.toArray();
        assertArrayEquals(new Object[]{dd1, dd2, dd3}, arr);

        // iteration order
        List<DependencyDefinition> iterated = new ArrayList<>();
        for (DependencyDefinition d : deps) {
            iterated.add(d);
        }
        assertEquals(List.of(dd1, dd2, dd3), iterated);
    }

    /* --------------------------------------------------------------------- */
    /*  Edge cases                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void getDependencyDefinitions_emptyList_getThrowsIndexOutOfBounds() {
        // Fresh MIC with no definitions
        ModelInterpretationContext emptyMic = new ModelInterpretationContext(mockContext);
        List<DependencyDefinition> emptyList = emptyMic.getDependencyDefinitions();
        assertThrows(IndexOutOfBoundsException.class,
                () -> emptyList.get(0),
                "Calling get(0) on an empty list must throw IndexOutOfBoundsException");
    }

    @Test
    void getDependencyDefinitions_emptyList_iteratorNextThrowsNoSuchElement() {
        ModelInterpretationContext emptyMic = new ModelInterpretationContext(mockContext);
        List<DependencyDefinition> emptyList = emptyMic.getDependencyDefinitions();
        Iterator<DependencyDefinition> it = emptyList.iterator();
        assertFalse(it.hasNext(), "Iterator should report no elements");
        assertThrows(NoSuchElementException.class,
                it::next,
                "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    @Test
    void getDependencyDefinitions_nullEntryHandled() {
        ModelInterpretationContext micWithNull = new ModelInterpretationContext(mockContext);
        micWithNull.addDependencyDefinition(null);
        List<DependencyDefinition> list = micWithNull.getDependencyDefinitions();

        assertEquals(1, list.size(), "List size should be 1 when a null entry is added");
        assertNull(list.get(0), "The element at index 0 must be null");

        // Mutating attempts must still be blocked.
        assertThrows(UnsupportedOperationException.class, list::clear);
        assertThrows(UnsupportedOperationException.class,
                () -> list.add(mock(DependencyDefinition.class)));
    }

    @Test
    void getDependencyDefinitions_subListIsUnmodifiable() {
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        mic = new ModelInterpretationContext(mockContext);
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);

        List<DependencyDefinition> full = mic.getDependencyDefinitions();
        List<DependencyDefinition> sub = full.subList(0, 1);

        // read‑only checks on subList
        assertEquals(1, sub.size());
        assertEquals(dd1, sub.get(0));

        // mutating operations on subList must fail
        assertThrows(UnsupportedOperationException.class, sub::clear);
        assertThrows(UnsupportedOperationException.class,
                () -> sub.add(mock(DependencyDefinition.class)));
    }

    /* --------------------------------------------------------------------- */
    /*  Verification of initial @BeforeEach state (two elements)              */
    /* --------------------------------------------------------------------- */

    @Test
    void getDependencyDefinitions_initialSetupContainsTwoElements() {
        List<DependencyDefinition> deps = mic.getDependencyDefinitions();
        assertEquals(2, deps.size(), "The @BeforeEach setup should have added exactly two definitions");
    }
}