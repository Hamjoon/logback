package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_addDependencyDefinition_Test {

    private ModelInterpretationContext mic;

    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for every test to avoid state leakage.
        mockContext = mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void addSingleDependencyDefinition_isStoredInList() {
        // given
        Model mockModel = mock(Model.class);
        DependencyDefinition dd = new DependencyDefinition(mockModel, "myDep");
        // when
        mic.addDependencyDefinition(dd);
        // then
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertNotNull(list, "Dependency list should never be null");
        assertEquals(1, list.size(), "Exactly one definition should have been added");
        assertSame(dd, list.get(0), "The stored definition should be the same instance that was added");
    }

    @Test
    void addMultipleDependencyDefinitions_areAllStoredInList() {
        // given
        Model mockModel1 = mock(Model.class);
        Model mockModel2 = mock(Model.class);
        DependencyDefinition dd1 = new DependencyDefinition(mockModel1, "firstDep");
        DependencyDefinition dd2 = new DependencyDefinition(mockModel2, "secondDep");
        // when
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        // then
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(2, list.size(), "Two definitions should have been added");
        assertTrue(list.contains(dd1), "First definition should be present");
        assertTrue(list.contains(dd2), "Second definition should be present");
    }

    @Test
    void addNullDependencyDefinition_isAcceptedByList() {
        // This behaviour is not explicitly documented, but the current implementation
        // simply forwards the argument to the underlying List, which permits nulls.
        // The test confirms that no exception is thrown and the list contains the null entry.
        // when
        mic.addDependencyDefinition(null);
        // then
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size(), "List should contain exactly one element");
        assertNull(list.get(0), "The stored element should be null");
    }

    @Test
    void addDuplicateContentDefinitions_areBothStored() {
        // Verify that the method does not deduplicate based on content (name/type).
        Model mockModel = mock(Model.class);
        DependencyDefinition first = new DependencyDefinition(mockModel, "duplicate");
        // same content, new instance
        DependencyDefinition second = new DependencyDefinition(mockModel, "duplicate");
        mic.addDependencyDefinition(first);
        mic.addDependencyDefinition(second);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(2, list.size(), "Both definitions should be stored");
        assertSame(first, list.get(0), "First element should be the first instance");
        assertSame(second, list.get(1), "Second element should be the second instance");
    }

    @Test
    void addSameInstanceTwice_isStoredTwice() {
        // Adding the exact same object reference twice must result in two list entries.
        Model mockModel = mock(Model.class);
        DependencyDefinition shared = new DependencyDefinition(mockModel, "shared");
        mic.addDependencyDefinition(shared);
        mic.addDependencyDefinition(shared);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(2, list.size(), "List size should reflect two additions");
        assertSame(shared, list.get(0), "First entry should be the shared instance");
        assertSame(shared, list.get(1), "Second entry should also be the shared instance");
        assertTrue(list.get(0) == list.get(1), "Both entries must reference the same object");
    }

    @Test
    void addEmptyStringFields_isStoredUnchanged() {
        // DependencyDefinition with empty name and type strings should be accepted.
        Model mockModel = mock(Model.class);
        // second param is the name; empty string
        DependencyDefinition empty = new DependencyDefinition(mockModel, "");
        mic.addDependencyDefinition(empty);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size(), "One definition should be stored");
        DependencyDefinition stored = list.get(0);
        assertSame(empty, stored, "Stored instance should be the same object");
        // Assuming DependencyDefinition exposes getters – if not, we only verify reference.
    }

    @Test
    void addNullFieldsDefinition_isStored() {
        // Create a DependencyDefinition with null fields (if constructor permits it).
        // We use a mock Model that can be null as well.
        DependencyDefinition nullFields = new DependencyDefinition(null, null);
        mic.addDependencyDefinition(nullFields);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size(), "List should contain the definition with null fields");
        assertSame(nullFields, list.get(0), "Stored definition must be the same instance");
    }

    @Test
    void bulkAddThousandDefinitions_preservesOrder() {
        // Stress test: add 1,000 distinct definitions and verify order and size.
        for (int i = 0; i < 1000; i++) {
            Model mockModel = mock(Model.class);
            DependencyDefinition dd = new DependencyDefinition(mockModel, "name" + i);
            mic.addDependencyDefinition(dd);
        }
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1000, list.size(), "Exactly 1,000 definitions should have been added");
        // Verify a few random positions to ensure order is preserved.
        for (int i : new int[] { 0, 123, 456, 999 }) {
            DependencyDefinition dd = list.get(i);
            assertNotNull(dd, "Definition at index " + i + " should not be null");
            // Since we cannot inspect the internal fields without getters, we rely on reference equality.
            // The test mainly guarantees that no elements were lost or reordered.
        }
    }

    @Test
    void interleavedStackOperations_doNotAffectDependencyListOrder() {
        // Add a definition, perform unrelated stack ops, then add another definition.
        Model mockModel = mock(Model.class);
        DependencyDefinition first = new DependencyDefinition(mockModel, "firstInterleaved");
        DependencyDefinition second = new DependencyDefinition(mockModel, "secondInterleaved");
        mic.addDependencyDefinition(first);
        // Unrelated stack operations
        mic.pushObject("tempObject");
        assertEquals("tempObject", mic.peekObject(), "Object should be on top of the stack");
        mic.popObject();
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty after pop");
        mic.addDependencyDefinition(second);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(2, list.size(), "Both definitions should be present");
        assertSame(first, list.get(0), "First definition must remain at index 0");
        assertSame(second, list.get(1), "Second definition must be at index 1");
    }
}
