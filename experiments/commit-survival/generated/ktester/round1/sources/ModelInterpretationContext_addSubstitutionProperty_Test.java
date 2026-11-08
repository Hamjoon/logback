package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_addSubstitutionProperty_Test {

    private ModelInterpretationContext mic;

    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a simple mock Context – the concrete implementation is not needed
        mockContext = org.mockito.Mockito.mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        mic = null;
        mockContext = null;
    }

    private static Stream<Arguments> provideKeyValueScenarios() {
        return Stream.of(// empty key, non‑empty value
        Arguments.of("", "someValue", "someValue"), // non‑empty key, empty value
        Arguments.of("emptyValue", "", ""), // non‑empty key, whitespace‑only value (trim -> empty)
        Arguments.of("blank", "    \t   ", ""));
    }

    @Test
    void whenKeyIsNull_thenPropertyIsNotAdded() {
        // Given
        String key = null;
        String value = "someValue";
        // When
        mic.addSubstitutionProperty(key, value);
        // Then
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertTrue(props.isEmpty(), "Properties map should remain empty when key is null");
    }

    @Test
    void whenValueIsNull_thenPropertyIsNotAdded() {
        // Given
        String key = "someKey";
        String value = null;
        // When
        mic.addSubstitutionProperty(key, value);
        // Then
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertTrue(props.isEmpty(), "Properties map should remain empty when value is null");
    }

    @Test
    void whenBothKeyAndValueAreNull_thenPropertyIsNotAdded() {
        // When
        mic.addSubstitutionProperty(null, null);
        // Then
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertTrue(props.isEmpty(), "Properties map should remain empty when both arguments are null");
    }

    @Test
    void whenValueHasLeadingAndTrailingSpaces_thenItIsTrimmedBeforeStoring() {
        // Given
        String key = "trimKey";
        String valueWithSpaces = "   trimmedValue   ";
        // When
        mic.addSubstitutionProperty(key, valueWithSpaces);
        // Then
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertEquals(1, props.size(), "Exactly one property should be stored");
        assertEquals("trimmedValue", props.get(key), "The stored value should be trimmed");
    }

    @Test
    void whenKeyAndValueAreValid_thenPropertyIsStoredUnchanged() {
        // Given
        String key = "simpleKey";
        String value = "simpleValue";
        // When
        mic.addSubstitutionProperty(key, value);
        // Then
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertEquals(1, props.size(), "Exactly one property should be stored");
        assertEquals(value, props.get(key), "The stored value should match the supplied value");
    }

    /**
     * Overwrites an existing entry with the same key.
     */
    @Test
    void whenKeyAlreadyExists_thenValueIsOverwritten() {
        // First insertion
        mic.addSubstitutionProperty("host", "192.168.0.1");
        // Overwrite with a new value
        mic.addSubstitutionProperty("host", "127.0.0.1");
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertEquals(1, props.size(), "Only one entry should exist after overwriting");
        assertEquals("127.0.0.1", props.get("host"), "The value should be replaced by the new one");
    }

    /**
     * Empty string as a key is accepted and stored verbatim.
     */
    @Test
    void whenKeyIsEmptyString_thenEntryIsStored() {
        mic.addSubstitutionProperty("", "someValue");
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertEquals(1, props.size(), "Map should contain exactly one entry");
        assertEquals("someValue", props.get(""), "Empty string key should map to the supplied value");
    }

    /**
     * Empty string as a value is stored after trimming (trim of empty string is still empty).
     */
    @Test
    void whenValueIsEmptyString_thenEntryIsStoredAsEmpty() {
        mic.addSubstitutionProperty("emptyValue", "");
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertEquals(1, props.size(), "Map should contain exactly one entry");
        assertEquals("", props.get("emptyValue"), "The stored value should be an empty string");
    }

    /**
     * A value consisting only of whitespace is trimmed to an empty string and stored.
     */
    @Test
    void whenValueIsOnlyWhitespace_thenStoredAsEmptyString() {
        mic.addSubstitutionProperty("blank", "    \t   ");
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertEquals(1, props.size(), "Map should contain exactly one entry");
        assertEquals("", props.get("blank"), "Whitespace‑only value should be trimmed to empty string");
    }

    /**
     * Keys are *not* trimmed – they are stored exactly as supplied, even with surrounding spaces.
     */
    @Test
    void whenKeyHasSurroundingSpaces_thenKeyIsStoredUntrimmed() {
        mic.addSubstitutionProperty("  spacedKey  ", "value");
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertEquals(1, props.size(), "Map should contain exactly one entry");
        assertTrue(props.containsKey("  spacedKey  "), "Key with spaces should be present unchanged");
        assertEquals("value", props.get("  spacedKey  "), "Associated value should be stored correctly");
    }

    /**
     * Simulate a scenario where the internal {@code propertiesMap} field is {@code null}.
     * The method should throw a {@link NullPointerException} when it attempts to {@code put}.
     */
    @Test
    void whenPropertiesMapIsNull_thenNullPointerExceptionIsThrown() throws Exception {
        // Use reflection to set the protected field to null
        Field propMapField = ModelInterpretationContext.class.getDeclaredField("propertiesMap");
        propMapField.setAccessible(true);
        propMapField.set(mic, null);
        // Now invoke the method with valid arguments – we expect NPE
        assertThrows(NullPointerException.class, () -> mic.addSubstitutionProperty("someKey", "someValue"), "Calling addSubstitutionProperty with a null internal map should throw NPE");
    }

    /**
     * Parameterized test covering empty key/value and whitespace‑only value scenarios.
     */
    @ParameterizedTest(name = "{index} => key=''{0}'', rawValue=''{1}'', expectedValue=''{2}''")
    @MethodSource("provideKeyValueScenarios")
    void whenVariousKeyValueScenarios_thenMapReflectsExpected(String key, String rawValue, String expectedValue) {
        mic.addSubstitutionProperty(key, rawValue);
        Map<String, String> props = mic.getCopyOfPropertyMap();
        assertEquals(1, props.size(), "Map should contain exactly one entry");
        assertEquals(expectedValue, props.get(key), "Stored value should match expected after trimming");
    }
}
