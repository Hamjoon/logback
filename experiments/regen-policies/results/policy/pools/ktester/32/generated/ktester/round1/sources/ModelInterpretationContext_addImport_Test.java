package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_addImport_Test {

    /**
     * The class under test.
     */
    private ModelInterpretationContext mic;

    /**
     * A mocked Logback {@link Context}. No real behavior is required for the tests.
     */
    @Mock
    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test so that the internal importMap
        // starts empty.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse between tests.
        mic = null;
    }

    /**
     * Sets the private {@code importMap} field via reflection.
     *
     * @param map the map to inject (may be {@code null})
     */
    private void injectImportMap(Map<String, String> map) {
        try {
            Field f = ModelInterpretationContext.class.getDeclaredField("importMap");
            f.setAccessible(true);
            f.set(mic, map);
        } catch (ReflectiveOperationException e) {
            fail("Failed to inject importMap via reflection", e);
        }
    }

    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// Basic case
        Arguments.of("myStem", "com.example.MyClass", 1, "com.example.MyClass"), // Null‑key case
        Arguments.of(null, "com.example.NullKeyClass", 1, "com.example.NullKeyClass"), // Null‑value case
        Arguments.of("nullValueStem", null, 1, null), // Empty‑string case
        Arguments.of("", "", 1, ""), // Whitespace case
        Arguments.of("   ", "   ", 1, "   "), // Special characters case
        Arguments.of("$pecial_Stem-1", "com.example.$pecialClass", 1, "com.example.$pecialClass"));
    }

    @Test
    @DisplayName("addImport stores a new stem‑to‑class mapping")
    void testAddImportAddsEntry() {
        // given
        String stem = "MyClass";
        String fqcn = "com.example.myapp.MyClass";
        // when
        mic.addImport(stem, fqcn);
        // then
        // The public getter should return the stored value.
        assertEquals(fqcn, mic.getImport(stem), "The import map should contain the supplied fully‑qualified class name.");
        // The copy of the map should also contain the entry.
        Map<String, String> copy = mic.getImportMapCopy();
        assertNotNull(copy, "Copy of import map must not be null.");
        assertEquals(1, copy.size(), "Copy of import map should contain exactly one entry.");
        assertEquals(fqcn, copy.get(stem), "Copy of import map should contain the correct mapping.");
    }

    @Test
    @DisplayName("addImport overrides an existing mapping for the same stem")
    void testAddImportOverridesExisting() {
        // given
        String stem = "Util";
        String firstFqcn = "com.example.UtilV1";
        String secondFqcn = "com.example.UtilV2";
        mic.addImport(stem, firstFqcn);
        assertEquals(firstFqcn, mic.getImport(stem), "Pre‑condition: first mapping must be present.");
        // when
        mic.addImport(stem, secondFqcn);
        // then
        assertEquals(secondFqcn, mic.getImport(stem), "The second call should replace the previous mapping for the same stem.");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(1, copy.size(), "Only one entry should remain after overriding.");
        assertEquals(secondFqcn, copy.get(stem), "The copy of the import map must reflect the overridden value.");
    }

    @Test
    @DisplayName("import map is initially empty")
    void testImportMapInitiallyEmpty() {
        // then
        assertTrue(mic.getImportMapCopy().isEmpty(), "A newly created ModelInterpretationContext must start with an empty import map.");
    }

    @ParameterizedTest(name = "[{index}] stem=''{0}'', fqcn=''{1}''")
    @MethodSource("normalCasesProvider")
    @DisplayName("addImport stores various valid keys/values")
    void testAddImportVariousValidInputs(String stem, String fqcn, int expectedSize, String expectedValue) {
        // when
        mic.addImport(stem, fqcn);
        // then
        assertEquals(expectedValue, mic.getImport(stem), () -> "Expected getImport to return the supplied value for stem: " + stem);
        assertEquals(expectedSize, mic.getImportMapCopy().size(), "Import map size should match expected after insertion.");
    }

    @Test
    @DisplayName("multiple distinct entries are stored correctly")
    void testAddImportMultipleDistinctEntries() {
        mic.addImport("stem1", "com.example.Class1");
        mic.addImport("stem2", "com.example.Class2");
        assertEquals("com.example.Class1", mic.getImport("stem1"));
        assertEquals("com.example.Class2", mic.getImport("stem2"));
        assertEquals(2, mic.getImportMapCopy().size(), "Import map should contain two distinct entries.");
    }

    @Test
    @DisplayName("case‑sensitive stems are treated as distinct")
    void testAddImportCaseSensitiveKeys() {
        mic.addImport("CaseSensitive", "com.example.Upper");
        mic.addImport("casesensitive", "com.example.Lower");
        assertEquals("com.example.Upper", mic.getImport("CaseSensitive"));
        assertEquals("com.example.Lower", mic.getImport("casesensitive"));
        assertEquals(2, mic.getImportMapCopy().size(), "Both case‑variant keys should exist separately.");
    }

    @Test
    @DisplayName("stress test with many distinct stems")
    void testAddImportStressManyEntries() {
        for (int i = 0; i < 10; i++) {
            mic.addImport("stem" + i, "com.example.Class" + i);
        }
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(10, copy.size(), "Import map should contain ten entries.");
        for (int i = 0; i < 10; i++) {
            assertEquals("com.example.Class" + i, mic.getImport("stem" + i), "Each stem should retrieve its correct class name.");
        }
    }

    @Test
    @DisplayName("addImport throws NullPointerException when internal map is null")
    void testAddImportWhenImportMapIsNull() {
        // simulate uninitialised map
        injectImportMap(null);
        assertThrows(NullPointerException.class, () -> mic.addImport("anyStem", "com.example.AnyClass"), "Calling addImport with a null internal map should throw NPE.");
    }

    @Test
    @DisplayName("modifying the copy returned by getImportMapCopy does not affect the original")
    void testCopyIsDefensive() {
        mic.addImport("isolatedStem", "com.example.IsolatedClass");
        Map<String, String> copy = mic.getImportMapCopy();
        copy.put("isolatedStem", "tampered");
        copy.put("newKey", "newValue");
        // Original map must stay unchanged
        assertEquals("com.example.IsolatedClass", mic.getImport("isolatedStem"), "Original mapping must remain unchanged after mutating the copy.");
        assertNull(mic.getImport("newKey"), "Original map must not contain entries added to the copy.");
    }

    @Test
    @DisplayName("addImport throws UnsupportedOperationException when importMap is unmodifiable")
    void testAddImportUnmodifiableMap() {
        Map<String, String> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        injectImportMap(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> mic.addImport("anyStem", "com.example.AnyClass"), "Adding to an unmodifiable importMap should throw UnsupportedOperationException.");
    }

    @Test
    @DisplayName("addImport throws NullPointerException for null key when using TreeMap")
    void testAddImportTreeMapNullKey() {
        // TreeMap does not allow null keys
        injectImportMap(new TreeMap<>());
        assertThrows(NullPointerException.class, () -> mic.addImport(null, "com.example.AnyClass"), "TreeMap should reject null keys with NPE.");
    }

    @Test
    @DisplayName("addImport propagates IllegalArgumentException from custom comparator")
    void testAddImportTreeMapComparatorThrows() {
        Comparator<String> badComparator = (a, b) -> {
            if (a != null && a.startsWith("bad")) {
                throw new IllegalArgumentException("Bad key");
            }
            return a.compareTo(b);
        };
        TreeMap<String, String> map = new TreeMap<>(badComparator);
        injectImportMap(map);
        assertThrows(IllegalArgumentException.class, () -> mic.addImport("badKey", "com.example.AnyClass"), "Comparator should cause IllegalArgumentException for disallowed keys.");
    }

    @Test
    @DisplayName("addImport throws NullPointerException for null value when using ConcurrentHashMap")
    void testAddImportConcurrentHashMapNullValue() {
        // ConcurrentHashMap disallows null values
        injectImportMap(new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> mic.addImport("anyStem", null), "ConcurrentHashMap should reject null values with NPE.");
    }
}
