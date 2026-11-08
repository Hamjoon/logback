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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_addDependencyDefinition_Test {

    @Mock
    Context // mock of the required Context dependency
    mockContext;

    // the object under test
    ModelInterpretationContext mic;

    /**
     * Subclass used for the “subclass instance” test case.
     */
    private static class MyDependencyDefinition extends DependencyDefinition {

        MyDependencyDefinition(Model model, String dependee) {
            super(model, dependee);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test case
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Creates a minimal {@link Model} instance – the concrete behaviour is irrelevant for these tests.
     */
    private static Model dummyModel() {
        return new Model() {
        };
    }

    /**
     * Factory for a normal DependencyDefinition.
     */
    private static DependencyDefinition dd(String dependee) {
        return new DependencyDefinition(dummyModel(), dependee);
    }

    /**
     * Provides a stream of 100 distinct DependencyDefinition objects for the bulk‑add test.
     */
    private static Stream<List<DependencyDefinition>> bulkAddProvider() {
        List<DependencyDefinition> list = new ArrayList<>(100);
        IntStream.range(0, 100).forEach(i -> list.add(new DependencyDefinition(dummyModel(), "dependencyDefinition" + i)));
        return Stream.of(list);
    }

    @Test
    void addDependencyDefinition_shouldStoreSingleDefinition() {
        // given
        Model mockModel = new Model() {
        };
        String dependee = "someDependee";
        DependencyDefinition dd = new DependencyDefinition(mockModel, dependee);
        // when
        mic.addDependencyDefinition(dd);
        // then
        List<DependencyDefinition> stored = mic.getDependencyDefinitions();
        assertEquals(1, stored.size(), "Exactly one DependencyDefinition should be stored");
        assertSame(dd, stored.get(0), "The stored DependencyDefinition should be the one that was added");
    }

    @Test
    void addDependencyDefinition_shouldAccumulateMultipleDefinitions() {
        // given
        Model mockModel1 = new Model() {
        };
        Model mockModel2 = new Model() {
        };
        DependencyDefinition dd1 = new DependencyDefinition(mockModel1, "first");
        DependencyDefinition dd2 = new DependencyDefinition(mockModel2, "second");
        // when
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        // then
        List<DependencyDefinition> stored = mic.getDependencyDefinitions();
        assertEquals(2, stored.size(), "Two DependencyDefinitions should be stored");
        assertTrue(stored.contains(dd1), "First definition should be present");
        assertTrue(stored.contains(dd2), "Second definition should be present");
    }

    @Test
    @DisplayName("Initial state – dependencyDefinitionList must be empty")
    void addDependencyDefinition_initialState() {
        // when – nothing has been added yet
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        // then
        assertNotNull(list, "The returned list should never be null");
        assertTrue(list.isEmpty(), "A newly created ModelInterpretationContext must start with an empty dependencyDefinitionList");
    }

    @Test
    @DisplayName("Add a non‑null DependencyDefinition – list size becomes 1 and contains the element")
    void addDependencyDefinition_singleNonNull() {
        DependencyDefinition dependencyDefinition1 = dd("dep1");
        mic.addDependencyDefinition(dependencyDefinition1);
        List<DependencyDefinition> stored = mic.getDependencyDefinitions();
        assertEquals(1, stored.size(), "List size should be 1 after adding a single definition");
        assertSame(dependencyDefinition1, stored.get(0), "The stored element must be the exact instance that was added");
    }

    @Test
    @DisplayName("Add a second DependencyDefinition – order is preserved")
    void addDependencyDefinition_multiplePreserveOrder() {
        DependencyDefinition dd1 = dd("first");
        DependencyDefinition dd2 = dd("second");
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        List<DependencyDefinition> stored = mic.getDependencyDefinitions();
        assertEquals(2, stored.size(), "List size should reflect two additions");
        assertEquals(dd1, stored.get(0), "First element must be the first added definition");
        assertEquals(dd2, stored.get(1), "Second element must be the second added definition");
    }

    @Test
    @DisplayName("Add a null DependencyDefinition – null is accepted and stored")
    void addDependencyDefinition_acceptsNull() {
        mic.addDependencyDefinition(null);
        List<DependencyDefinition> stored = mic.getDependencyDefinitions();
        assertEquals(1, stored.size(), "List size should increase even when null is added");
        assertNull(stored.get(0), "The last element must be null");
    }

    @Test
    @DisplayName("Add a duplicate DependencyDefinition – duplicates are stored")
    void addDependencyDefinition_duplicateReference() {
        DependencyDefinition dd1 = dd("dup");
        DependencyDefinition dd2 = dd("other");
        // same reference as dd1
        DependencyDefinition ddDuplicate = dd1;
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        mic.addDependencyDefinition(null);
        mic.addDependencyDefinition(ddDuplicate);
        List<DependencyDefinition> stored = mic.getDependencyDefinitions();
        assertEquals(4, stored.size(), "List size should reflect all four additions");
        assertEquals(Arrays.asList(dd1, dd2, null, ddDuplicate), stored, "List must contain the elements in insertion order, including duplicates and null");
    }

    @ParameterizedTest(name = "Bulk add of {0} DependencyDefinitions")
    @MethodSource("bulkAddProvider")
    @DisplayName("Bulk add – 100 DependencyDefinitions are stored in order")
    void addDependencyDefinition_bulkAdd(List<DependencyDefinition> bulk) {
        // Add each definition sequentially
        bulk.forEach(mic::addDependencyDefinition);
        List<DependencyDefinition> stored = mic.getDependencyDefinitions();
        assertEquals(100, stored.size(), "After bulk addition the list size must be 100");
        assertEquals(bulk, stored, "The stored list must preserve the exact insertion order");
    }

    @Test
    @DisplayName("Add a subclass instance of DependencyDefinition – accepted and stored")
    void addDependencyDefinition_subclassInstance() {
        MyDependencyDefinition myDD = new MyDependencyDefinition(dummyModel(), "myDep");
        mic.addDependencyDefinition(myDD);
        List<DependencyDefinition> stored = mic.getDependencyDefinitions();
        assertEquals(1, stored.size(), "List size should be 1 after adding subclass instance");
        assertSame(myDD, stored.get(0), "Stored element must be the exact subclass instance that was added");
    }

    @Test
    @DisplayName("Reference equality – retrieved element is the same object reference")
    void addDependencyDefinition_referenceEquality() {
        DependencyDefinition ref = dd("ref");
        mic.addDependencyDefinition(ref);
        DependencyDefinition retrieved = mic.getDependencyDefinitions().get(0);
        assertSame(ref, retrieved, "The object retrieved from the list must be the same reference that was added");
    }

    @Test
    @DisplayName("Multiple adds after retrieval – list continues to reflect all elements")
    void addDependencyDefinition_multipleAddsAfterRetrieval() {
        DependencyDefinition ddA = dd("A");
        DependencyDefinition ddB = dd("B");
        mic.addDependencyDefinition(ddA);
        // Retrieve the list after the first addition
        List<DependencyDefinition> interim = mic.getDependencyDefinitions();
        assertEquals(1, interim.size(), "Interim list should contain the first element");
        // Add a second definition
        mic.addDependencyDefinition(ddB);
        List<DependencyDefinition> finalList = mic.getDependencyDefinitions();
        assertEquals(2, finalList.size(), "Final list must contain both elements");
        assertEquals(ddA, finalList.get(0), "First element must still be the first added definition");
        assertEquals(ddB, finalList.get(1), "Second element must be the second added definition");
    }
}
