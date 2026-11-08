package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_addImport_Test {

    @Mock
    Context // mocked Context – no real behaviour needed
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // create a fresh MIC for every test so that the internal importMap is empty
        mic = new ModelInterpretationContext(mockContext);
    }

    private static Stream<Arguments> provideNormalAndEdgeCases() {
        return Stream.of(// group: addImport functionality
        Arguments.of("list", "java.util.List", "Adds a new import entry", 1, "java.util.List"), Arguments.of("list", "java.util.ArrayList", "Overwrites an existing import for the same stem", 1, "java.util.ArrayList"), Arguments.of(null, "java.lang.String", "Allows null stem (key)", 1, "java.lang.String"), Arguments.of("string", null, "Allows null fqcn (value)", 2, null), // group: addImport edge cases
        Arguments.of("", "java.lang.Object", "Empty string as stem", 3, "java.lang.Object"), Arguments.of("emptyValue", "", "Empty string as fqcn", 4, ""), Arguments.of("", "", "Both stem and fqcn empty", 5, ""), // idempotent insertion – size stays 5 (no new key)
        Arguments.of("list", "java.util.List", "Insert same stem‑fqcn pair again – size unchanged", 5, "java.util.List"), Arguments.of("Map", "java.util.Map", "Case‑sensitive distinct key (capital M)", 6, "java.util.Map"), Arguments.of("map", "java.util.HashMap", "Case‑sensitive distinct key (lowercase m)", 7, "java.util.HashMap"), // retrieval of non‑existing stem – map size unchanged
        Arguments.of("nonExistingStem", "some.FakeClass", "Add a new stem then query a different unknown stem", 8, "some.FakeClass"));
    }

    /**
     * Helper that uses reflection to replace the private {@code importMap} field.
     *
     * @param newMap the map instance to set (may be {@code null})
     */
    private void setImportMapViaReflection(Map<String, String> newMap) throws Exception {
        Field field = ModelInterpretationContext.class.getDeclaredField("importMap");
        field.setAccessible(true);
        field.set(mic, newMap);
    }

    @Test
    void addImport_placesEntryIntoImportMap() {
        // given
        String stem = "MyClass";
        String fqcn = "com.example.MyClass";
        // when
        mic.addImport(stem, fqcn);
        // then
        assertEquals(fqcn, mic.getImport(stem), "The fully‑qualified class name should be stored under the given stem");
        // also verify that the copy returned by getImportMapCopy contains the entry
        assertTrue(mic.getImportMapCopy().containsKey(stem), "The import map copy should contain the newly added stem");
        assertEquals(fqcn, mic.getImportMapCopy().get(stem));
    }

    @Test
    void addImport_overwritesExistingStem() {
        // given
        String stem = "Service";
        String firstFqcn = "com.example.ServiceV1";
        String secondFqcn = "com.example.ServiceV2";
        mic.addImport(stem, firstFqcn);
        assertEquals(firstFqcn, mic.getImport(stem));
        // when – add a second mapping for the same stem
        mic.addImport(stem, secondFqcn);
        // then – the later value replaces the earlier one
        assertEquals(secondFqcn, mic.getImport(stem), "Adding an import with an existing stem should replace the previous mapping");
    }

    @Test
    void addImport_allowsNullStem() {
        // given
        String stem = null;
        String fqcn = "com.example.NullStem";
        // when – HashMap permits a null key, so the method should not throw
        mic.addImport(stem, fqcn);
        // then – the null key should be present in the copy of the map
        assertTrue(mic.getImportMapCopy().containsKey(null), "Import map should contain a null key when addImport is called with null stem");
        assertEquals(fqcn, mic.getImport(null));
    }

    /* --------------------------------------------------------------
     *  Parameterised tests for normal and edge‑case behaviour
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: stem=''{0}'', fqcn=''{1}'' → {2}")
    @MethodSource("provideNormalAndEdgeCases")
    void addImport_variousScenarios(String stem, String fqcn, String expectedDescription, int expectedSizeAfter, String expectedValueAfter) {
        // when
        mic.addImport(stem, fqcn);
        // then – value can be retrieved via getImport()
        assertEquals(expectedValueAfter, mic.getImport(stem), "Retrieved value should match the one supplied to addImport");
        // and – the size of the defensive copy reflects the expected size
        assertEquals(expectedSizeAfter, mic.getImportMapCopy().size(), "Import map size should be as expected after insertion");
    }

    /* --------------------------------------------------------------
     *  Bulk insertion test
     * -------------------------------------------------------------- */
    @Test
    void addImport_bulkInsertionAndLookup() {
        // given – five distinct imports
        Map<String, String> data = new LinkedHashMap<>();
        data.put("List", "java.util.List");
        data.put("Set", "java.util.Set");
        data.put("Map", "java.util.Map");
        data.put("Queue", "java.util.Queue");
        data.put("Deque", "java.util.Deque");
        // when – add them sequentially
        data.forEach(mic::addImport);
        // then – map size matches number of distinct stems
        assertEquals(data.size(), mic.getImportMapCopy().size(), "Import map size should equal number of unique stems");
        // and – each stem resolves to its expected fqcn
        data.forEach((stem, fqcn) -> assertEquals(fqcn, mic.getImport(stem), () -> "Stem '" + stem + "' should map to '" + fqcn + "'"));
    }

    /* --------------------------------------------------------------
     *  Defensive copy test for getImportMapCopy()
     * -------------------------------------------------------------- */
    @Test
    void getImportMapCopy_returnsDefensiveCopy() {
        // given – add a single entry
        mic.addImport("A", "alpha.A");
        // when – obtain a copy and mutate it
        Map<String, String> copy = mic.getImportMapCopy();
        // modify the copy only
        copy.put("B", "beta.B");
        // then – internal map must stay unchanged
        assertNull(mic.getImport("B"), "Modifying the returned map must not affect the internal importMap");
        assertEquals(1, mic.getImportMapCopy().size(), "Internal importMap size should remain unchanged after external modification");
    }

    /* --------------------------------------------------------------
     *  Exception scenarios – manipulate internal importMap via reflection
     * -------------------------------------------------------------- */
    @Test
    void addImport_throws_NullPointerException_whenImportMapIsNull() throws Exception {
        // given – set the private field importMap to null using reflection
        setImportMapViaReflection(null);
        // when / then – invoking addImport should raise NPE
        assertThrows(NullPointerException.class, () -> mic.addImport("anyStem", "any.FQCN"), "addImport should throw NullPointerException when internal importMap is null");
    }

    @Test
    void addImport_throws_UnsupportedOperationException_whenImportMapIsUnmodifiable() throws Exception {
        // given – replace importMap with an unmodifiable map
        Map<String, String> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        setImportMapViaReflection(unmodifiable);
        // when / then – invoking addImport should raise UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> mic.addImport("stem", "fqcn"), "addImport should throw UnsupportedOperationException when internal importMap is unmodifiable");
    }
}
