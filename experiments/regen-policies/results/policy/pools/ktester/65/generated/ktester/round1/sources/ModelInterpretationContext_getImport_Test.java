package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getImport_Test {

    @Mock
    Context // mocked Context required by the constructor
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor only needs a Context; the mock is sufficient.
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Provides a stream of test arguments:
     * <ul>
     *   <li>stem – the argument passed to {@code getImport}</li>
     *   <li>expected – the expected return value</li>
     *   <li>mapSetup – a {@link Consumer} that prepares the internal importMap for the case</li>
     *   <li>description – a readable description used as the test name</li>
     * </ul>
     */
    private static Stream<Arguments> provideGetImportCases() {
        return Stream.of(// stem not present – should return the original stem
        arguments("unknownStem", "unknownStem", (Consumer<ModelInterpretationContext>) ctx -> {
        }, /*no map entries*/
        "Stem not present in import map returns original stem"), // regular mapping
        arguments("myStem", "com.example.MyClass", (Consumer<ModelInterpretationContext>) ctx -> {
            Map<String, String> m = new HashMap<>();
            m.put("myStem", "com.example.MyClass");
            setImportMap(ctx, m);
        }, "Stem present with normal fqcn mapping"), // empty string stem without mapping
        arguments("", "", (Consumer<ModelInterpretationContext>) ctx -> {
        }, /*no map entries*/
        "Empty string stem without mapping returns empty string"), // mapping whose value is null – should behave as if missing
        arguments("nullValueStem", "nullValueStem", (Consumer<ModelInterpretationContext>) ctx -> {
            Map<String, String> m = new HashMap<>();
            m.put("nullValueStem", null);
            setImportMap(ctx, m);
        }, "Mapping with null value returns original stem"), // mapping whose value is empty string – should return the empty string
        arguments("emptyValueStem", "", (Consumer<ModelInterpretationContext>) ctx -> {
            Map<String, String> m = new HashMap<>();
            m.put("emptyValueStem", "");
            setImportMap(ctx, m);
        }, "Mapping with empty string value returns empty string"), // mapping identical to the stem
        arguments("sameAsStem", "sameAsStem", (Consumer<ModelInterpretationContext>) ctx -> {
            Map<String, String> m = new HashMap<>();
            m.put("sameAsStem", "sameAsStem");
            setImportMap(ctx, m);
        }, "Stem maps to identical value (still retrieved from map)"), // case‑sensitivity check
        arguments("caseSensitive", "caseSensitive", (Consumer<ModelInterpretationContext>) ctx -> {
            Map<String, String> m = new HashMap<>();
            m.put("CaseSensitive", "com.example.CaseSensitiveClass");
            setImportMap(ctx, m);
        }, "Lookup is case‑sensitive; mismatched case returns original stem"), // large map with many entries
        arguments("largeKey", "com.example.LargeKeyClass", (Consumer<ModelInterpretationContext>) ctx -> {
            Map<String, String> m = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                m.put("key" + i, "value" + i);
            }
            m.put("largeKey", "com.example.LargeKeyClass");
            setImportMap(ctx, m);
        }, "Method correctly finds entry in a large map"));
    }

    /**
     * Replaces the {@code importMap} field of the given context with the supplied map.
     *
     * @param ctx the {@link ModelInterpretationContext} instance to modify
     * @param map the map to set as the internal importMap
     */
    private static void setImportMap(ModelInterpretationContext ctx, Map<String, String> map) {
        try {
            Field f = ModelInterpretationContext.class.getDeclaredField("importMap");
            f.setAccessible(true);
            f.set(ctx, map);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set importMap via reflection", e);
        }
    }

    @Test
    @DisplayName("getImport returns null when stem is null")
    void getImportReturnsNullForNullStem() {
        // given – nothing to set up, just a fresh mic instance
        // when
        String result = mic.getImport(null);
        // then
        assertNull(result, "Expected null when the stem argument is null");
    }

    @Test
    @DisplayName("getImport returns the original stem when no import mapping exists")
    void getImportReturnsStemWhenNotMapped() {
        // given
        String stem = "com.example.MyClass";
        // No import added for this stem
        // when
        String result = mic.getImport(stem);
        // then
        assertEquals(stem, result, "When the stem is not present in the import map the method should return the stem unchanged");
    }

    @Test
    @DisplayName("getImport returns the fully‑qualified class name when a mapping exists")
    void getImportReturnsMappedValue() {
        // given
        String stem = "MyClass";
        String fqcn = "com.example.MyClass";
        // public API to populate the import map
        mic.addImport(stem, fqcn);
        // when
        String result = mic.getImport(stem);
        // then
        assertEquals(fqcn, result, "When the stem is present in the import map the method should return the mapped value");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("provideGetImportCases")
    void testGetImportVarious(String stem, String expected, Consumer<ModelInterpretationContext> mapSetup, String description) {
        // Arrange – prepare the internal importMap as required for the case
        mapSetup.accept(mic);
        // Act
        String result = mic.getImport(stem);
        // Assert
        assertEquals(expected, result, description);
    }

    @Test
    @DisplayName("getImport throws NullPointerException when internal importMap is null")
    void getImportThrowsWhenImportMapIsNull() throws Exception {
        // Force the protected importMap field to be null via reflection
        Field f = ModelInterpretationContext.class.getDeclaredField("importMap");
        f.setAccessible(true);
        f.set(mic, null);
        // Expect NPE because the method dereferences importMap without a null‑check
        assertThrows(NullPointerException.class, () -> mic.getImport("anyStem"), "When importMap is null, getImport should throw NullPointerException");
    }
}
