package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_addImport_Test {

    private ModelInterpretationContext mic;

    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Context is an interface in Logback; a mock is sufficient for construction.
        mockContext = org.mockito.Mockito.mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        mic = null;
        mockContext = null;
    }

    private static Stream<Arguments> basicAddImportProvider() {
        return Stream.of(// Basic case
        Arguments.of("myClass", "com.example.MyClass", "com.example.MyClass"), // Overwrite case
        Arguments.of("myClass", "com.example.MyClassV2", "com.example.MyClassV2"), // Null‑key case
        Arguments.of(null, "com.example.NullStem", "com.example.NullStem"), // Null‑value case
        Arguments.of("nullValue", null, null), // Empty‑string case
        Arguments.of("", "", ""));
    }

    /* --------------------------------------------------------------
     *  Helper method to inject a custom importMap via reflection.
     * -------------------------------------------------------------- */
    private void setImportMap(Map<String, String> map) throws Exception {
        Field importMapField = ModelInterpretationContext.class.getDeclaredField("importMap");
        importMapField.setAccessible(true);
        importMapField.set(mic, map);
    }

    @Test
    void shouldAddSingleImport() {
        // given
        String stem = "MyClass";
        String fqcn = "com.example.MyClass";
        // when
        mic.addImport(stem, fqcn);
        // then
        assertEquals(fqcn, mic.getImport(stem), "getImport should return the fully qualified class name");
        Map<String, String> copy = mic.getImportMapCopy();
        assertTrue(copy.containsKey(stem), "Import map copy should contain the added stem");
        assertEquals(fqcn, copy.get(stem), "Import map copy should map stem to the correct FQCN");
    }

    @Test
    void shouldAddMultipleImports() {
        // given
        String stem1 = "A";
        String fqcn1 = "pkg.A";
        String stem2 = "B";
        String fqcn2 = "pkg.sub.B";
        // when
        mic.addImport(stem1, fqcn1);
        mic.addImport(stem2, fqcn2);
        // then
        assertEquals(fqcn1, mic.getImport(stem1));
        assertEquals(fqcn2, mic.getImport(stem2));
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(2, copy.size(), "Import map should contain exactly two entries");
        assertEquals(fqcn1, copy.get(stem1));
        assertEquals(fqcn2, copy.get(stem2));
    }

    @Test
    void shouldOverwriteExistingStem() {
        // given
        String stem = "Duplicate";
        String firstFqcn = "first.pkg.Duplicate";
        String secondFqcn = "second.pkg.Duplicate";
        // when
        mic.addImport(stem, firstFqcn);
        // overwrite
        mic.addImport(stem, secondFqcn);
        // then
        assertEquals(secondFqcn, mic.getImport(stem), "The later call should replace the previous mapping");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(1, copy.size(), "Only one entry should exist after overwriting");
        assertEquals(secondFqcn, copy.get(stem));
    }

    @Test
    void shouldAcceptNullStemAndFqcn() {
        // given
        String nullStem = null;
        String nullFqcn = null;
        // when
        mic.addImport(nullStem, nullFqcn);
        // then
        // HashMap permits null keys and values; the public accessor should reflect that.
        assertNull(mic.getImport(nullStem), "Retrieving a null stem should return null");
        Map<String, String> copy = mic.getImportMapCopy();
        assertTrue(copy.containsKey(nullStem), "Import map copy should contain a null key");
        assertNull(copy.get(nullStem), "The value associated with the null key should be null");
    }

    /* --------------------------------------------------------------
     *  Parameterised tests for the basic scenarios (group: addImport)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} ⇒ stem=''{0}'', fqcn=''{1}''")
    @MethodSource("basicAddImportProvider")
    void shouldAddImportBasic(String stem, String fqcn, String expected) {
        // when
        mic.addImport(stem, fqcn);
        // then
        assertEquals(expected, mic.getImport(stem), () -> "getImport should return the expected value for stem '" + stem + "'");
        Map<String, String> copy = mic.getImportMapCopy();
        assertTrue(copy.containsKey(stem), "Import map copy should contain the added stem");
        assertEquals(expected, copy.get(stem), "Import map copy should map stem to the correct FQCN");
    }

    /* --------------------------------------------------------------
     *  Extended scenarios (group: addImport_extended)
     * -------------------------------------------------------------- */
    @Test
    void shouldReturnNullForNonExistentStem() {
        assertNull(mic.getImport("nonExistentStem"), "Lookup of a stem that was never added should return null");
    }

    @Test
    void shouldOverwriteNullKey() {
        // first mapping with null key
        mic.addImport(null, "com.example.FirstNullKey");
        // second mapping with same null key, different value
        mic.addImport(null, "com.example.SecondNullKey");
        assertEquals("com.example.SecondNullKey", mic.getImport(null), "Second insertion with a null key should overwrite the first value");
    }

    @Test
    void shouldLeaveMapSizeUnchangedWhenAddingIdenticalEntry() {
        mic.addImport("duplicateStem", "com.example.DupClass");
        int sizeBefore = mic.getImportMapCopy().size();
        // add the same entry again
        mic.addImport("duplicateStem", "com.example.DupClass");
        int sizeAfter = mic.getImportMapCopy().size();
        assertEquals(sizeBefore, sizeAfter, "Adding an identical key/value pair should not change the map size");
    }

    @Test
    void shouldPreserveExistingEntriesWhenAddingNewImport() {
        // pre‑populate the map with two entries
        mic.addImport("existing1", "com.example.Existing1");
        mic.addImport("existing2", "com.example.Existing2");
        // add a new, independent entry
        mic.addImport("uniqueStem1", "com.example.Unique1");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(3, copy.size(), "Map should contain three entries after adding a new one");
        assertEquals("com.example.Existing1", copy.get("existing1"));
        assertEquals("com.example.Existing2", copy.get("existing2"));
        assertEquals("com.example.Unique1", copy.get("uniqueStem1"));
    }

    /* --------------------------------------------------------------
     *  Exception scenarios (group: addImport_exceptions)
     * -------------------------------------------------------------- */
    @Test
    void shouldThrowNPEWhenImportMapIsNull() throws Exception {
        setImportMap(null);
        assertThrows(NullPointerException.class, () -> mic.addImport("anyStem", "com.example.AnyClass"), "Calling addImport with a null internal map should throw NullPointerException");
    }

    @Test
    void shouldThrowUnsupportedOperationExceptionWhenImportMapIsUnmodifiable() throws Exception {
        Map<String, String> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        setImportMap(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> mic.addImport("anyStem", "com.example.AnyClass"), "Calling addImport on an unmodifiable map should throw UnsupportedOperationException");
    }

    @Test
    void shouldThrowNPEWhenUsingConcurrentHashMapWithNullKey() throws Exception {
        ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
        setImportMap(concurrentMap);
        assertThrows(NullPointerException.class, () -> mic.addImport(null, "com.example.AnyClass"), "ConcurrentHashMap does not allow null keys – should throw NullPointerException");
    }

    @Test
    void shouldThrowNPEWhenUsingConcurrentHashMapWithNullValue() throws Exception {
        ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
        setImportMap(concurrentMap);
        assertThrows(NullPointerException.class, () -> mic.addImport("anyStem", null), "ConcurrentHashMap does not allow null values – should throw NullPointerException");
    }
}
