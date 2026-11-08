package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getImport_Test{

    @Mock
    Context mockContext;               // mocked dependency required by the constructor

    ModelInterpretationContext mic;    // instance under test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getImport_whenStemIsNull_returnsNull() {
        // given
        String stem = null;

        // when
        String result = mic.getImport(stem);

        // then
        assertNull(result, "getImport(null) should return null");
    }

    @Test
    void getImport_whenStemNotInImportMap_returnsStemItself() {
        // given
        String stem = "com.example.Foo";

        // (no entry is added to the import map)

        // when
        String result = mic.getImport(stem);

        // then
        assertEquals(stem, result,
                "When the stem is not present in the import map, getImport should return the original stem");
    }

    @Test
    void getImport_whenStemIsMapped_returnsMappedValue() {
        // given
        String stem = "Foo";
        String fqcn = "com.example.FooImpl";

        // Populate the import map via the public API.
        mic.addImport(stem, fqcn);

        // when
        String result = mic.getImport(stem);

        // then
        assertEquals(fqcn, result,
                "When the stem is present in the import map, getImport should return the mapped fully‑qualified class name");
    }

    @Test
    void getImport_whenStemIsMapped_andStemIsDifferentCase_returnsMappedValue() {
        // given
        String stem = "Bar";
        String fqcn = "com.example.BarImpl";

        mic.addImport(stem, fqcn);

        // when
        String result = mic.getImport("Bar"); // exact match – case‑sensitive as per Map semantics

        // then
        assertEquals(fqcn, result);
    }

    @Test
    void getImport_whenStemIsEmptyString_returnsEmptyString() {
        // given
        String stem = "";

        // when
        String result = mic.getImport(stem);

        // then
        assertEquals(stem, result,
                "An empty string that is not present in the import map should be returned unchanged");
    }
}