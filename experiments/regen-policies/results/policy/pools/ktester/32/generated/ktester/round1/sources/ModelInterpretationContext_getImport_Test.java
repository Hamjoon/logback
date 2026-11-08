package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getImport_Test {

    @Mock
    Context // Mockito will provide a mock Context
    mockContext;

    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Helper that uses reflection to set the private {@code importMap} field.
     *
     * @param value the value to assign (may be {@code null})
     */
    private void setImportMap(Object value) throws Exception {
        Field field = ModelInterpretationContext.class.getDeclaredField("importMap");
        field.setAccessible(true);
        field.set(mic, value);
    }

    @Test
    void getImport_whenStemIsNull_returnsNull() {
        // Given
        String stem = null;
        // When
        String result = mic.getImport(stem);
        // Then
        assertNull(result, "getImport(null) should return null");
    }

    @Test
    void getImport_whenStemNotInImportMap_returnsStemItself() {
        // Given
        String stem = "com.example.Foo";
        // (importMap is empty by default)
        // When
        String result = mic.getImport(stem);
        // Then
        assertEquals(stem, result, "When the stem is not present in the import map, the original stem should be returned");
    }

    @Test
    void getImport_whenStemInImportMap_returnsMappedValue() {
        // Given
        String stem = "Foo";
        String fqcn = "com.example.FooImpl";
        mic.addImport(stem, fqcn);
        // When
        String result = mic.getImport(stem);
        // Then
        assertEquals(fqcn, result, "When the stem is present in the import map, the mapped fully‑qualified class name should be returned");
    }

    @Test
    void copyConstructor_copiesImportMapCorrectly() {
        // Given: original MIC with an import entry
        String stem = "Bar";
        String fqcn = "com.example.BarImpl";
        mic.addImport(stem, fqcn);
        // When: create a copy
        ModelInterpretationContext copyMic = new ModelInterpretationContext(mic);
        // Then: the copy should resolve the same import without affecting the original
        assertEquals(fqcn, copyMic.getImport(stem), "Copy constructor should duplicate the import map so that the copy resolves the same import");
        // Also ensure that modifications to the copy do not affect the original
        copyMic.addImport("Baz", "com.example.BazImpl");
        assertNull(mic.getImport("Baz"), "Changes to the copy's import map must not affect the original instance");
    }

    @Test
    @DisplayName("getImport throws NullPointerException when internal importMap is null and stem is non‑null")
    void getImport_whenImportMapIsNull_andStemNotNull_throwsNPE() throws Exception {
        // Given: force the internal importMap to null
        setImportMap(null);
        String stem = "anyStem";
        // When / Then
        assertThrows(NullPointerException.class, () -> mic.getImport(stem), "With a null importMap, calling getImport with a non‑null stem should throw NPE");
    }

    @Test
    @DisplayName("getImport returns null when internal importMap is null and stem is null")
    void getImport_whenImportMapIsNull_andStemNull_returnsNull() throws Exception {
        // Given: force the internal importMap to null
        setImportMap(null);
        String stem = null;
        // When
        String result = mic.getImport(stem);
        // Then
        assertNull(result, "Even if importMap is null, a null stem should cause getImport to return null without throwing");
    }

    @Test
    @DisplayName("getImport throws ClassCastException when importMap contains a non‑String value")
    void getImport_whenImportMapContainsNonStringValue_throwsClassCastException() throws Exception {
        // Given: a map that contains an Integer instead of a String for the key "badStem"
        Map rawMap = new HashMap<>();
        // deliberately break the generic type
        rawMap.put("badStem", Integer.valueOf(42));
        setImportMap(rawMap);
        // When / Then
        assertThrows(ClassCastException.class, () -> mic.getImport("badStem"), "If the importMap holds a non‑String value, getImport should propagate a ClassCastException");
    }
}
