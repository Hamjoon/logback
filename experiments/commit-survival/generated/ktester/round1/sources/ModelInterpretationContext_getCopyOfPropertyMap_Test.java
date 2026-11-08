package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getCopyOfPropertyMap_Test {

    private ModelInterpretationContext mic;

    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock Context – the real implementation is not required for this test
        mockContext = mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse
        mic = null;
        mockContext = null;
    }

    @Test
    void getCopyOfPropertyMap_returnsEmptyMapWhenNoPropertiesAdded() {
        // GIVEN: a fresh ModelInterpretationContext with no substitution properties
        // WHEN
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        // THEN
        assertNotNull(copy, "The returned map should never be null");
        assertTrue(copy.isEmpty(), "The map should be empty when no properties have been added");
    }

    @Test
    void getCopyOfPropertyMap_returnsMapContainingAllProperties() {
        // GIVEN
        mic.addSubstitutionProperty("host", "localhost");
        mic.addSubstitutionProperty("port", "8080");
        // WHEN
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        // THEN
        assertEquals(2, copy.size(), "The copy should contain exactly the added entries");
        assertEquals("localhost", copy.get("host"));
        assertEquals("8080", copy.get("port"));
    }

    @Test
    void getCopyOfPropertyMap_returnsASeparateInstance() {
        // GIVEN
        mic.addSubstitutionProperty("key", "value");
        // WHEN
        Map<String, String> firstCopy = mic.getCopyOfPropertyMap();
        // Mutate the returned map
        firstCopy.put("newKey", "newValue");
        // Retrieve a second copy to verify internal state unchanged
        Map<String, String> secondCopy = mic.getCopyOfPropertyMap();
        // THEN
        // The internal propertiesMap should not contain the mutation performed on the first copy
        assertFalse(secondCopy.containsKey("newKey"), "Modifying the returned map must not affect the internal properties map");
        // Ensure the original entry is still present
        assertEquals("value", secondCopy.get("key"));
        // Also verify that the two copies are distinct objects
        assertNotSame(firstCopy, secondCopy, "Each call should return a new map instance");
    }

    /**
     * Verify that the map returned by {@code getCopyOfPropertyMap()} is a
     * different instance than the internal {@code propertiesMap}.
     */
    @Test
    void getCopyOfPropertyMap_isDifferentInstanceFromInternalMap() throws Exception {
        // GIVEN
        mic.addSubstitutionProperty("alpha", "beta");
        // WHEN
        Map<String, String> returnedMap = mic.getCopyOfPropertyMap();
        // Obtain the internal map via reflection (it is protected)
        Field field = ModelInterpretationContext.class.getDeclaredField("propertiesMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> internalMap = (Map<String, String>) field.get(mic);
        // THEN
        assertNotSame(returnedMap, internalMap, "Returned map must be a different object from the internal map");
        // Also ensure they contain the same data at this point
        assertEquals(internalMap, returnedMap, "Returned map should contain the same entries as internal map");
    }

    /**
     * Ensure that a copy taken before a later internal mutation does not
     * magically acquire the new entry.
     */
    @Test
    void getCopyOfPropertyMap_copyRemainsUnchangedAfterInternalMutation() throws Exception {
        // GIVEN
        mic.addSubstitutionProperty("key1", "value1");
        Map<String, String> copyBefore = mic.getCopyOfPropertyMap();
        // WHEN: mutate internal state by adding another property
        mic.addSubstitutionProperty("key2", "value2");
        Map<String, String> copyAfter = mic.getCopyOfPropertyMap();
        // THEN
        assertEquals(1, copyBefore.size(), "First copy should contain only the first entry");
        assertEquals("value1", copyBefore.get("key1"));
        assertFalse(copyBefore.containsKey("key2"), "First copy must not contain the later added entry");
        assertEquals(2, copyAfter.size(), "Second copy should reflect the new internal state");
        assertEquals("value2", copyAfter.get("key2"));
    }

    /**
     * Modifying the returned map must not affect the internal map, and the
     * returned map must stay mutable.
     */
    @Test
    void getCopyOfPropertyMap_modifyingReturnedMapDoesNotAffectInternalMap() throws Exception {
        // GIVEN
        mic.addSubstitutionProperty("orig", "value");
        // WHEN
        Map<String, String> returned = mic.getCopyOfPropertyMap();
        // mutate the copy
        returned.put("temp", "123");
        // Access internal map via reflection
        Field field = ModelInterpretationContext.class.getDeclaredField("propertiesMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> internal = (Map<String, String>) field.get(mic);
        // THEN
        assertTrue(returned.containsKey("temp"), "Returned map should contain the newly added entry");
        assertEquals("123", returned.get("temp"));
        assertFalse(internal.containsKey("temp"), "Internal propertiesMap must not be affected by mutations on the returned map");
    }

    /**
     * Verify that the method can handle a relatively large number of entries.
     */
    @Test
    void getCopyOfPropertyMap_handlesHundredEntries() {
        // GIVEN 100 distinct properties
        for (int i = 0; i < 100; i++) {
            mic.addSubstitutionProperty("k" + i, "v" + i);
        }
        // WHEN
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        // THEN
        assertEquals(100, copy.size(), "The copy should contain exactly 100 entries");
        for (int i = 0; i < 100; i++) {
            assertEquals("v" + i, copy.get("k" + i), "Entry k" + i + " should map to v" + i);
        }
    }

    /**
     * Simulate a broken internal state where {@code propertiesMap} is {@code null}
     * and verify that a {@link NullPointerException} is thrown.
     */
    @Test
    void getCopyOfPropertyMap_throwsNPEWhenInternalMapIsNull() throws Exception {
        // GIVEN: force the protected field to null via reflection
        Field field = ModelInterpretationContext.class.getDeclaredField("propertiesMap");
        field.setAccessible(true);
        field.set(mic, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mic.getCopyOfPropertyMap(), "Calling getCopyOfPropertyMap() when internal propertiesMap is null should throw NPE");
    }
}
