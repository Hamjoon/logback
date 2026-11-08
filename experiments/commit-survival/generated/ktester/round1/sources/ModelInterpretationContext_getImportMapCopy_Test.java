package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getImportMapCopy_Test {

    /**
     * The class under test.
     */
    private ModelInterpretationContext mic;

    /**
     * A mocked Logback Context – required by the MIC constructor.
     */
    @Mock
    private Context mockContext;

    /**
     * A Map whose {@code entrySet().iterator()} throws a {@link RuntimeException}
     * when iteration begins. Used to verify that {@code getImportMapCopy()} does not
     * swallow unchecked exceptions from the underlying map.
     */
    private static class RuntimeExceptionMap<K, V> extends AbstractMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {

                        @Override
                        public boolean hasNext() {
                            throw new RuntimeException("iteration failure");
                        }

                        @Override
                        public Entry<K, V> next() {
                            throw new RuntimeException("iteration failure");
                        }
                    };
                }

                @Override
                public int size() {
                    // size is irrelevant; iteration will fail first.
                    return 1;
                }
            };
        }
    }

    /**
     * A Map that throws {@link ConcurrentModificationException} from its iterator.
     * This simulates a map being structurally modified while {@code new HashMap<>(source)}
     * is copying it.
     */
    private static class ConcurrentModificationMap<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> delegate = new HashMap<>();

        ConcurrentModificationMap() {
            delegate.put((K) "key", (V) "value");
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {

                        private boolean first = true;

                        @Override
                        public boolean hasNext() {
                            return first;
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (!first) {
                                throw new NoSuchElementException();
                            }
                            first = false;
                            // Simulate concurrent modification right before returning the entry.
                            delegate.put((K) "another", (V) "value2");
                            throw new ConcurrentModificationException("simulated concurrent change");
                        }
                    };
                }

                @Override
                public int size() {
                    return delegate.size();
                }
            };
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor under test only needs a Context (which we mock).
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current test scenario.
        mic = null;
    }

    /* --------------------------------------------------------------------- */
    private void setImportMap(Map<String, String> map) throws Exception {
        Field f = ModelInterpretationContext.class.getDeclaredField("importMap");
        f.setAccessible(true);
        f.set(mic, map);
    }

    /**
     * Verify that {@code getImportMapCopy()} returns an empty map when no imports have been added,
     * and that the returned map is a distinct instance from the internal map.
     */
    @Test
    void getImportMapCopy_returnsEmptyMapWhenNoImports() {
        // WHEN
        Map<String, String> copy = mic.getImportMapCopy();
        // THEN
        assertNotNull(copy, "The returned map should never be null");
        assertTrue(copy.isEmpty(), "The copy should be empty when no imports are present");
        // The copy must not be the same object as the internal map.
        // We cannot access the internal map directly, but we can assert that mutating the copy
        // does not affect subsequent calls.
        copy.put("dummy", "value");
        Map<String, String> secondCopy = mic.getImportMapCopy();
        assertFalse(secondCopy.containsKey("dummy"), "Modifying the returned map must not affect the internal import map");
    }

    /**
     * Verify that {@code getImportMapCopy()} returns a map containing all previously added imports
     * and that the returned map is a defensive copy.
     */
    @Test
    void getImportMapCopy_containsAddedImportsAndIsDefensiveCopy() {
        // GIVEN – add a couple of imports using the public API
        mic.addImport("stem1", "com.example.ClassOne");
        mic.addImport("stem2", "com.example.ClassTwo");
        // WHEN
        Map<String, String> copy = mic.getImportMapCopy();
        // THEN – the copy must contain the same entries
        assertEquals(2, copy.size(), "Copy should contain exactly the number of added imports");
        assertEquals("com.example.ClassOne", copy.get("stem1"));
        assertEquals("com.example.ClassTwo", copy.get("stem2"));
        // AND – the copy must be a distinct object (defensive copy)
        assertNotSame(copy, mic.getImportMapCopy(), "Each call should return a new map instance (defensive copy)");
        // Mutate the returned map and ensure the internal state is unchanged
        copy.put("stem3", "com.example.ClassThree");
        Map<String, String> afterMutationCopy = mic.getImportMapCopy();
        assertNull(afterMutationCopy.get("stem3"), "Changes to the returned map must not be reflected in the internal import map");
        assertEquals(2, afterMutationCopy.size(), "Internal import map size should remain unchanged after external mutation");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getImportMapCopy_nullImportMap_throwsNPE() throws Exception {
        // GIVEN: internal map is null
        setImportMap(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mic.getImportMapCopy(), "Calling getImportMapCopy() when internal importMap is null should throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getImportMapCopy_preservesNullKey() throws Exception {
        Map<String, String> internal = new HashMap<>();
        internal.put(null, "NullClass");
        setImportMap(internal);
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(1, copy.size());
        assertTrue(copy.containsKey(null));
        assertEquals("NullClass", copy.get(null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getImportMapCopy_preservesNullValue() throws Exception {
        Map<String, String> internal = new HashMap<>();
        internal.put("nullValue", null);
        setImportMap(internal);
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(1, copy.size());
        assertTrue(copy.containsKey("nullValue"));
        assertNull(copy.get("nullValue"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getImportMapCopy_originalMutationDoesNotAffectCopy() throws Exception {
        Map<String, String> internal = new HashMap<>();
        internal.put("alpha", "AlphaClass");
        internal.put("beta", "BetaClass");
        setImportMap(internal);
        Map<String, String> snapshot = mic.getImportMapCopy();
        // mutate the original internal map
        internal.put("gamma", "GammaClass");
        // the snapshot must stay unchanged
        assertEquals(2, snapshot.size());
        assertFalse(snapshot.containsKey("gamma"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getImportMapCopy_returnedMapIsMutable() throws Exception {
        // internal map empty
        setImportMap(new HashMap<>());
        Map<String, String> copy = mic.getImportMapCopy();
        // Adding a new entry must not throw
        assertDoesNotThrow(() -> copy.put("newStem", "NewClass"));
        assertEquals("NewClass", copy.get("newStem"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getImportMapCopy_unmodifiableInternalMap() throws Exception {
        Map<String, String> mutable = new HashMap<>();
        mutable.put("u1", "UnmodClass1");
        mutable.put("u2", "UnmodClass2");
        Map<String, String> unmodifiable = Collections.unmodifiableMap(mutable);
        setImportMap(unmodifiable);
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(2, copy.size());
        assertEquals("UnmodClass1", copy.get("u1"));
        assertEquals("UnmodClass2", copy.get("u2"));
        // ensure the returned map is mutable
        assertDoesNotThrow(() -> copy.put("extra", "ExtraClass"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getImportMapCopy_mapThrowsDuringIteration_propagatesException() throws Exception {
        Map<String, String> throwingMap = new RuntimeExceptionMap<>();
        setImportMap(throwingMap);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> mic.getImportMapCopy(), "Exception from the internal map during iteration must propagate");
        assertEquals("iteration failure", thrown.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getImportMapCopy_concurrentModification_throwsCME() throws Exception {
        // The map deliberately throws CME when its entrySet iterator is used.
        Map<String, String> cmeMap = new ConcurrentModificationMap<>();
        setImportMap(cmeMap);
        assertThrows(ConcurrentModificationException.class, () -> mic.getImportMapCopy(), "Concurrent modification during copy should result in CME");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getImportMapCopy_eachCallReturnsNewInstance() throws Exception {
        Map<String, String> internal = new HashMap<>();
        internal.put("a", "A");
        setImportMap(internal);
        Map<String, String> first = mic.getImportMapCopy();
        Map<String, String> second = mic.getImportMapCopy();
        assertNotSame(first, second, "Each invocation must return a distinct map instance");
    }
}
