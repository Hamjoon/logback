package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_addDependencyDefinition_Test {

    @Mock
    Context // mocked Context – no real behaviour needed for this test
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void addDependencyDefinition_shouldStoreDefinitionInList() {
        // GIVEN
        Model mockModel = new Model() {
        };
        String dependee = "someDependee";
        DependencyDefinition dd = new DependencyDefinition(mockModel, dependee);
        // pre‑condition: list is empty
        List<DependencyDefinition> before = mic.getDependencyDefinitions();
        assertTrue(before.isEmpty(), "Dependency list should be empty before adding");
        // WHEN
        mic.addDependencyDefinition(dd);
        // THEN
        List<DependencyDefinition> after = mic.getDependencyDefinitions();
        assertEquals(1, after.size(), "Dependency list should contain exactly one element after adding");
        assertSame(dd, after.get(0), "The stored element must be the same instance that was added");
    }

    @Test
    void addDependencyDefinition_multipleCalls_accumulateDefinitions() {
        // GIVEN
        Model modelA = new Model() {
        };
        Model modelB = new Model() {
        };
        DependencyDefinition dd1 = new DependencyDefinition(modelA, "depA");
        DependencyDefinition dd2 = new DependencyDefinition(modelB, "depB");
        // WHEN
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        // THEN
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(2, list.size(), "Two definitions should be stored");
        assertTrue(list.contains(dd1), "First definition should be present");
        assertTrue(list.contains(dd2), "Second definition should be present");
    }

    @Test
    void addDependencyDefinition_nullArgument_storesNull() {
        // GIVEN
        // pre‑condition: list is empty
        assertTrue(mic.getDependencyDefinitions().isEmpty());
        // WHEN
        mic.addDependencyDefinition(null);
        // THEN
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size(), "List should contain one element even if it is null");
        assertNull(list.get(0), "The stored element should be null");
    }

    @Test
    @DisplayName("Initial state – dependencyDefinitionList must be empty")
    void initialState_dependencyListIsEmpty() {
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertTrue(list.isEmpty(), "A newly created ModelInterpretationContext must start with an empty dependency list");
    }

    @Test
    @DisplayName("Add a regular, non‑null DependencyDefinition")
    void addDependencyDefinition_regularInstance() {
        DependencyDefinition dd = new DependencyDefinition("name1", "typeA");
        mic.addDependencyDefinition(dd);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size(), "List size should become 1");
        assertSame(dd, list.get(0), "The stored element must be the exact instance added");
    }

    @Test
    @DisplayName("Add null – list should contain a null entry")
    void addDependencyDefinition_nullReference() {
        mic.addDependencyDefinition(null);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size(), "List size should become 1 after adding null");
        assertNull(list.get(0), "The first element must be null");
    }

    @Test
    @DisplayName("Add three distinct DependencyDefinition objects and verify order")
    void addDependencyDefinition_threeSequentialAdds() {
        DependencyDefinition d1 = new DependencyDefinition("first", "typeX");
        DependencyDefinition d2 = new DependencyDefinition("second", "typeY");
        DependencyDefinition d3 = new DependencyDefinition("third", "typeZ");
        mic.addDependencyDefinition(d1);
        mic.addDependencyDefinition(d2);
        mic.addDependencyDefinition(d3);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(3, list.size(), "List size should be 3 after three adds");
        assertSame(d1, list.get(0), "First added element should be at index 0");
        assertSame(d2, list.get(1), "Second added element should be at index 1");
        assertSame(d3, list.get(2), "Third added element should be at index 2");
    }

    @Test
    @DisplayName("Add two distinct instances with identical content – both should be stored")
    void addDependencyDefinition_duplicateContentDifferentInstances() {
        DependencyDefinition dup1 = new DependencyDefinition("dup", "typeD");
        // separate object
        DependencyDefinition dup2 = new DependencyDefinition("dup", "typeD");
        mic.addDependencyDefinition(dup1);
        mic.addDependencyDefinition(dup2);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(2, list.size(), "List size should be 2 after adding two duplicates");
        assertSame(dup1, list.get(0), "First duplicate should be at index 0");
        assertSame(dup2, list.get(1), "Second duplicate should be at index 1");
        assertNotSame(dup1, dup2, "The two stored objects must be different instances");
    }

    @Test
    @DisplayName("Add the exact same DependencyDefinition instance twice – identity preserved")
    void addDependencyDefinition_sameInstanceTwice() {
        DependencyDefinition shared = new DependencyDefinition("shared", "typeS");
        mic.addDependencyDefinition(shared);
        mic.addDependencyDefinition(shared);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(2, list.size(), "List size should be 2 after adding the same instance twice");
        assertSame(shared, list.get(0), "First entry must be the shared instance");
        assertSame(shared, list.get(1), "Second entry must also be the same shared instance");
    }

    @Test
    @DisplayName("Add a DependencyDefinition with empty name and type strings")
    void addDependencyDefinition_emptyStrings() {
        DependencyDefinition empty = new DependencyDefinition("", "");
        mic.addDependencyDefinition(empty);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size(), "List size should be 1");
        assertSame(empty, list.get(0), "Stored element must be the exact instance added");
    }

    @Test
    @DisplayName("Add a DependencyDefinition constructed with null name and null type")
    void addDependencyDefinition_nullFields() {
        DependencyDefinition nullFields = new DependencyDefinition(null, null);
        mic.addDependencyDefinition(nullFields);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size(), "List size should be 1");
        assertSame(nullFields, list.get(0), "Stored element must be the exact instance added");
    }

    @Test
    @DisplayName("Stress test – add 100 distinct DependencyDefinition objects")
    void addDependencyDefinition_hundredAddsStressTest() {
        List<DependencyDefinition> added = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            DependencyDefinition dd = new DependencyDefinition("name" + i, "type" + i);
            added.add(dd);
            mic.addDependencyDefinition(dd);
        }
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(100, list.size(), "List size should be 100 after 100 adds");
        // Verify insertion order
        for (int i = 0; i < 100; i++) {
            assertSame(added.get(i), list.get(i), "Element at index " + i + " must match the one added at that position");
        }
    }

    @Test
    @DisplayName("Add a DependencyDefinition containing Unicode characters")
    void addDependencyDefinition_unicodeContent() {
        DependencyDefinition unicode = new DependencyDefinition("unicode-ß", "类型-中文");
        mic.addDependencyDefinition(unicode);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size(), "List size should be 1");
        assertSame(unicode, list.get(0), "Stored element must be the exact instance added");
    }
}
