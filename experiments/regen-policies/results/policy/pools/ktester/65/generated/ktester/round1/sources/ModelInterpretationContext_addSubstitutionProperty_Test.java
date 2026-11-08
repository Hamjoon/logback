package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_addSubstitutionProperty_Test {

    @Mock
    Context // a mock Context is sufficient – the MIC does not use it directly
    mockContext;

    // the object under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Build a fresh MIC for every test to avoid cross‑test contamination.
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void addsNewSubstitutionProperty() {
        // GIVEN
        String key = "myKey";
        String value = "myValue";
        // WHEN
        mic.addSubstitutionProperty(key, value);
        // THEN
        Map<String, String> map = mic.getCopyOfPropertyMap();
        assertTrue(map.containsKey(key), "Property map should contain the newly added key");
        assertEquals(value, map.get(key), "The value associated with the key should match the one supplied");
    }

    @Test
    void overridesExistingSubstitutionProperty() {
        // GIVEN
        String key = "duplicateKey";
        String firstValue = "first";
        String secondValue = "second";
        mic.addSubstitutionProperty(key, firstValue);
        assertEquals(firstValue, mic.getCopyOfPropertyMap().get(key), "Initial insertion must succeed");
        // WHEN – add the same key again with a different value
        mic.addSubstitutionProperty(key, secondValue);
        // THEN – the map should reflect the latest value
        Map<String, String> map = mic.getCopyOfPropertyMap();
        assertEquals(1, map.size(), "Map size should remain 1 after overriding the same key");
        assertEquals(secondValue, map.get(key), "The value should be updated to the most recent one");
    }

    @Test
    void throwsExceptionWhenKeyIsNull() {
        // GIVEN
        String value = "someValue";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mic.addSubstitutionProperty(null, value), "VariableSubstitutionsHelper should reject a null key");
    }

    @Test
    void throwsExceptionWhenValueIsNull() {
        // GIVEN
        String key = "someKey";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mic.addSubstitutionProperty(key, null), "VariableSubstitutionsHelper should reject a null value");
    }

    @Test
    void throwsNullPointerExceptionWhenHelperIsNull() throws Exception {
        // Simulate a mis‑configured MIC where the helper was never initialised.
        Field helperField = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        helperField.setAccessible(true);
        helperField.set(mic, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mic.addSubstitutionProperty("anyKey", "anyValue"), "Calling addSubstitutionProperty with a null helper should result in NPE");
    }

    @Test
    void acceptsEmptyStringKey() {
        String key = "";
        String value = "emptyKeyValue";
        mic.addSubstitutionProperty(key, value);
        assertEquals(value, mic.subst("${}"), "Empty key should be stored and substitution of ${} must return the value");
    }

    @Test
    void acceptsEmptyStringValue() {
        String key = "emptyValueKey";
        String value = "";
        mic.addSubstitutionProperty(key, value);
        assertEquals("", mic.subst("${emptyValueKey}"), "Empty value should be stored and substitution must return an empty string");
    }

    @Test
    void overwritesDuplicateKey() {
        String key = "duplicateKey";
        mic.addSubstitutionProperty(key, "firstValue");
        assertEquals("firstValue", mic.subst("${duplicateKey}"));
        mic.addSubstitutionProperty(key, "secondValue");
        assertEquals("secondValue", mic.subst("${duplicateKey}"), "Second insertion must overwrite the first one");
    }

    @Test
    void acceptsKeyContainingSpaces() {
        String key = "spaced key";
        String value = "valueWithSpaces";
        mic.addSubstitutionProperty(key, value);
        assertEquals(value, mic.subst("${spaced key}"), "Key with spaces should be stored and substituted correctly");
    }

    @Test
    void preservesUnicodeCharacters() {
        String key = "unicodeKeyßµ";
        String value = "unicodeValueéø";
        mic.addSubstitutionProperty(key, value);
        assertEquals(value, mic.subst("${unicodeKeyßµ}"), "Unicode characters in key/value must be preserved");
    }

    @Test
    void storesSpecialCharactersUnchanged() {
        String key = "specialCharsKey!@#$%^&*()";
        String value = "specialCharsValue<>?/:;\"'";
        mic.addSubstitutionProperty(key, value);
        assertEquals(value, mic.subst("${specialCharsKey!@#$%^&*()}"), "Special characters should be stored without alteration");
    }

    @Test
    void handlesVeryLargeValue() {
        String key = "largeKey";
        // 10 000 characters 'X'
        StringBuilder sb = new StringBuilder(10_000);
        for (int i = 0; i < 10_000; i++) {
            sb.append('X');
        }
        String largeValue = sb.toString();
        mic.addSubstitutionProperty(key, largeValue);
        assertEquals(largeValue, mic.subst("${largeKey}"), "Large value must be stored and returned unchanged");
    }

    @Test
    void storesValueContainingPlaceholderVerbatim() {
        String key = "nestedRef";
        String value = "${innerKey}";
        mic.addSubstitutionProperty(key, value);
        assertEquals(value, mic.subst("${nestedRef}"), "Value containing a placeholder must be stored verbatim (no recursive resolution)");
    }

    @Test
    void acceptsStringsWithEmbeddedNullCharacter() {
        // Note: Java strings can contain '\0' without termination.
        String key = "nullCharKey\u0000test";
        String value = "valueWithNullChar\u0000inside";
        mic.addSubstitutionProperty(key, value);
        // The placeholder syntax uses the exact key string, including the null character.
        assertEquals(value, mic.subst("${nullCharKey\u0000test}"), "Embedded null characters should be preserved in both key and value");
    }

    @Test
    void propertyMapReflectsAllInsertedEntries() {
        mic.addSubstitutionProperty("k1", "v1");
        mic.addSubstitutionProperty("k2", "v2");
        mic.addSubstitutionProperty("k3", "v3");
        Map<String, String> map = mic.getCopyOfPropertyMap();
        assertEquals(3, map.size(), "Map should contain three entries");
        assertEquals("v1", map.get("k1"));
        assertEquals("v2", map.get("k2"));
        assertEquals("v3", map.get("k3"));
    }
}
