package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

// kept from the template; not strictly required here
@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getImportMapCopy_Test {

    private ModelInterpretationContext mic;

    private ContextBase context;

    @BeforeEach
    void setupBeforeEach() {
        // Create a real Context implementation (ContextBase) – no mocking required
        context = new ContextBase();
        mic = new ModelInterpretationContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure each test starts with a clean state
        mic = null;
        context = null;
    }

    @Test
    void getImportMapCopy_returnsCopyWithSameEntries() {
        // given – populate the internal import map via the public API
        mic.addImport("stemA", "com.example.Foo");
        mic.addImport("stemB", "org.sample.Bar");
        // when
        Map<String, String> copy = mic.getImportMapCopy();
        // then – the copy contains exactly the same entries
        assertEquals(2, copy.size(), "Copy should contain two entries");
        assertEquals("com.example.Foo", copy.get("stemA"));
        assertEquals("org.sample.Bar", copy.get("stemB"));
        // and – it is a different object (i.e., a true copy)
        assertNotSame(mic.getImportMapCopy(), copy, "Each call should return a new Map instance");
    }

    @Test
    void modifyingReturnedMapDoesNotAffectOriginalImportMap() {
        // given
        mic.addImport("stemX", "net.test.X");
        Map<String, String> copyBefore = mic.getImportMapCopy();
        // when – mutate the returned map
        copyBefore.put("newStem", "net.test.New");
        // then – the internal importMap inside ModelInterpretationContext stays unchanged
        Map<String, String> copyAfter = mic.getImportMapCopy();
        assertFalse(copyAfter.containsKey("newStem"), "Original importMap must not be affected by changes to the returned copy");
        assertEquals("net.test.X", copyAfter.get("stemX"));
    }

    @Test
    void copyIsIndependentEvenWhenOriginalIsModifiedLater() {
        // given
        mic.addImport("initialStem", "initial.Value");
        Map<String, String> firstCopy = mic.getImportMapCopy();
        // when – modify the internal import map via the public API after the copy was taken
        mic.addImport("addedLater", "later.Value");
        // then – the earlier copy must stay unchanged
        assertEquals(1, firstCopy.size(), "First copy should still contain only the entry that existed at copy time");
        assertTrue(firstCopy.containsKey("initialStem"));
        assertFalse(firstCopy.containsKey("addedLater"));
    }

    @Test
    @DisplayName("Empty internal importMap returns an empty copy")
    void getImportMapCopy_whenEmpty_returnsEmptyMap() {
        // No imports added → internal importMap should be empty (or null but not used yet)
        Map<String, String> copy = mic.getImportMapCopy();
        assertTrue(copy.isEmpty(), "Copy of an empty importMap must be empty");
        assertEquals(0, copy.size());
    }

    @Test
    @DisplayName("Normal case – several entries added via addImport()")
    void getImportMapCopy_normalEntries() {
        mic.addImport("java.util.List", "java.util.ArrayList");
        mic.addImport("java.util.Map", "java.util.HashMap");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(2, copy.size());
        assertEquals("java.util.ArrayList", copy.get("java.util.List"));
        assertEquals("java.util.HashMap", copy.get("java.util.Map"));
    }

    @Test
    @DisplayName("Duplicate key – latest value overwrites previous one")
    void getImportMapCopy_duplicateKeyOverwrites() {
        mic.addImport("java.util.List", "java.util.ArrayList");
        // Overwrite same stem with a different fully qualified class name
        mic.addImport("java.util.List", "java.util.LinkedList");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(1, copy.size());
        assertEquals("java.util.LinkedList", copy.get("java.util.List"), "The map must contain the latest mapping for a duplicate key");
    }

    @Test
    @DisplayName("Returned map is a true copy – modifying it does not affect the original")
    void getImportMapCopy_modifyingReturnedMapDoesNotAffectOriginal() throws Exception {
        mic.addImport("stemX", "net.test.X");
        Map<String, String> copyBefore = mic.getImportMapCopy();
        // Mutate the returned map
        copyBefore.put("newStem", "net.test.New");
        // Use reflection to read the internal map directly
        Field f = ModelInterpretationContext.class.getDeclaredField("importMap");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> internalMap = (Map<String, String>) f.get(mic);
        assertFalse(internalMap.containsKey("newStem"), "Internal importMap must not be affected by changes to the returned copy");
        assertEquals("net.test.X", internalMap.get("stemX"));
    }

    @Test
    @DisplayName("Snapshot semantics – copy remains unchanged after internal mutation")
    void getImportMapCopy_snapshotAfterInternalMutation() {
        mic.addImport("initialStem", "initial.Value");
        Map<String, String> firstCopy = mic.getImportMapCopy();
        // Mutate internal map after the copy was taken
        mic.addImport("addedLater", "later.Value");
        // The earlier copy must stay unchanged
        assertEquals(1, firstCopy.size());
        assertTrue(firstCopy.containsKey("initialStem"));
        assertFalse(firstCopy.containsKey("addedLater"));
    }

    @Test
    @DisplayName("Distinct instances on successive calls")
    void getImportMapCopy_distinctInstances() {
        mic.addImport("a", "A");
        Map<String, String> first = mic.getImportMapCopy();
        Map<String, String> second = mic.getImportMapCopy();
        assertNotSame(first, second, "Each call must return a new Map instance");
        // Mutate first and ensure second does not see the change
        first.put("extra", "E");
        assertFalse(second.containsKey("extra"));
    }

    @Test
    @DisplayName("Large import map (100 entries) is copied correctly")
    void getImportMapCopy_largeMap() {
        for (int i = 0; i < 100; i++) {
            mic.addImport("key" + i, "value" + i);
        }
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(100, copy.size(), "Copy should contain exactly 100 entries");
        for (int i = 0; i < 100; i++) {
            assertEquals("value" + i, copy.get("key" + i));
        }
    }

    @Test
    @DisplayName("Internal importMap containing a null value is copied without NPE")
    void getImportMapCopy_nullValueInMap() throws Exception {
        // Prepare a map with a null value via reflection
        Field f = ModelInterpretationContext.class.getDeclaredField("importMap");
        f.setAccessible(true);
        Map<String, String> customMap = new HashMap<>();
        customMap.put("com.example.NullValue", null);
        f.set(mic, customMap);
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(1, copy.size());
        assertTrue(copy.containsKey("com.example.NullValue"));
        assertNull(copy.get("com.example.NullValue"), "The null value must be present in the copied map");
    }

    @Test
    @DisplayName("Null internal importMap leads to NullPointerException")
    void getImportMapCopy_internalMapNull_throwsNPE() throws Exception {
        // Force the private field to null
        Field f = ModelInterpretationContext.class.getDeclaredField("importMap");
        f.setAccessible(true);
        f.set(mic, null);
        assertThrows(NullPointerException.class, () -> mic.getImportMapCopy(), "Calling getImportMapCopy() when importMap is null should throw NPE");
    }

    @Test
    @DisplayName("Faulty internal map that throws ConcurrentModificationException is propagated")
    void getImportMapCopy_faultyMap_propagatesException() throws Exception {
        // Create a Map whose entrySet iterator throws CME
        Map<String, String> buggyMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                return new AbstractSet<>() {

                    @Override
                    public Iterator<Entry<String, String>> iterator() {
                        return new Iterator<>() {

                            @Override
                            public boolean hasNext() {
                                return false;
                            }

                            @Override
                            public Entry<String, String> next() {
                                throw new ConcurrentModificationException("Simulated CME");
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return 0;
                    }
                };
            }
        };
        // Inject the buggy map via reflection
        Field f = ModelInterpretationContext.class.getDeclaredField("importMap");
        f.setAccessible(true);
        f.set(mic, buggyMap);
        assertThrows(ConcurrentModificationException.class, () -> mic.getImportMapCopy(), "Exception from the internal map should propagate");
    }
}
