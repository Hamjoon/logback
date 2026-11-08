package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.ArrayList;
import java.util.stream.Stream;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_hasDependers_Test {

    private ModelInterpretationContext mic;

    private Context mockContext;

    private static class ArgumentsHolder {

        final String dependeeName;

        final boolean expected;

        final List<DependencyDefinition> definitions;

        ArgumentsHolder(String dependeeName, boolean expected, List<DependencyDefinition> definitions) {
            this.dependeeName = dependeeName;
            this.expected = expected;
            this.definitions = definitions;
        }

        @Override
        public String toString() {
            return "name='" + dependeeName + "', expected=" + expected;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a simple mock Context – the concrete behaviour of Context is not required
        mockContext = org.mockito.Mockito.mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    private static Stream<ArgumentsHolder> normalCasesProvider() throws Exception {
        ???;
        data.add(new ArgumentsHolder("serviceA", false, List.of()));
        data.add(new ArgumentsHolder("serviceB", true, List.of(createDD("serviceB"))));
        data.add(new ArgumentsHolder("serviceC", true, List.of(createDD("x1"), createDD("serviceC"), createDD("x2"))));
        data.add(new ArgumentsHolder("ServiceA", false, List.of(createDD("serviceA"))));
        data.add(new ArgumentsHolder("dupService", true, List.of(createDD("dupService"), createDD("dupService"))));
        List<DependencyDefinition> largeList = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            largeList.add(createDD("service_" + i));
        }
        largeList.add(createDD("targetService"));
        data.add(new ArgumentsHolder("targetService", true, largeList));
        data.add(new ArgumentsHolder("unknown", false, List.of()));
        return data.stream();
    }

    /**
     * The method under test creates an {@link IllegalArgumentException}
     * when the supplied name is {@code null} or blank, but it does **not**
     * throw it. Therefore the method should simply return {@code false}
     * for such inputs.
     */
    @Test
    @DisplayName("hasDependers returns false for null or blank dependee name")
    void returnsFalseForNullOrBlank() {
        assertFalse(mic.hasDependers(null), "null name should yield false");
        assertFalse(mic.hasDependers("   "), "blank name should yield false");
    }

    /**
     * When a {@link DependencyDefinition} whose {@code dependee} field matches
     * the argument is present in {@code dependencyDefinitionList},
     * {@code hasDependers} must return {@code true}.
     */
    @Test
    @DisplayName("hasDependers returns true when a matching DependencyDefinition exists")
    void returnsTrueWhenMatchingDependerExists() throws Exception {
        // Create a mock DependencyDefinition – we only need the public field `dependee`
        DependencyDefinition dd = org.mockito.Mockito.mock(DependencyDefinition.class);
        // Use reflection to set the package‑private (or public) field `dependee`
        Field dependeeField = DependencyDefinition.class.getDeclaredField("dependee");
        dependeeField.setAccessible(true);
        dependeeField.set(dd, "myAppender");
        // Add the mock definition to the MIC's internal list
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        list.add(dd);
        assertTrue(mic.hasDependers("myAppender"), "Existing dependee name should make hasDependers return true");
    }

    /**
     * If the list does not contain a matching {@code dependee},
     * the method must return {@code false}.
     */
    @Test
    @DisplayName("hasDependers returns false when no matching DependencyDefinition exists")
    void returnsFalseWhenNoMatch() throws Exception {
        // Add a definition with a different name
        DependencyDefinition dd = org.mockito.Mockito.mock(DependencyDefinition.class);
        Field dependeeField = DependencyDefinition.class.getDeclaredField("dependee");
        dependeeField.setAccessible(true);
        dependeeField.set(dd, "otherAppender");
        mic.getDependencyDefinitions().add(dd);
        assertFalse(mic.hasDependers("myAppender"), "Non‑matching name should make hasDependers return false");
    }

    @Test
    @DisplayName("hasDependers returns false for null, blank or empty dependee name")
    void returnsFalseForNullBlankOrEmpty() {
        assertFalse(mic.hasDependers(null), "null name should yield false");
        assertFalse(mic.hasDependers("   "), "blank name should yield false");
        assertFalse(mic.hasDependers(""), "empty string should yield false");
    }

    @ParameterizedTest(name = "[{index}] name=''{0}'' expected={1}")
    @MethodSource("normalCasesProvider")
    @DisplayName("hasDependers normal matching / non‑matching scenarios")
    void normalScenarios(ArgumentsHolder holder) {
        List<DependencyDefinition> internalList = mic.getDependencyDefinitions();
        internalList.clear();
        internalList.addAll(holder.definitions);
        assertEquals(holder.expected, mic.hasDependers(holder.dependeeName), "Unexpected result for name='" + holder.dependeeName + "'");
    }

    @Test
    @DisplayName("hasDependers propagates NullPointerException when a DependencyDefinition has a null dependee")
    void throwsNPEWhenDependeeIsNull() throws Exception {
        DependencyDefinition dd = DependencyDefinition.class.getDeclaredConstructor().newInstance();
        mic.getDependencyDefinitions().add(dd);
        assertThrows(NullPointerException.class, () -> mic.hasDependers("anyService"), "Expected NPE because dd.dependee is null");
    }

    @Test
    @DisplayName("hasDependers propagates NullPointerException when the list contains a null element")
    void throwsNPEWhenListContainsNullElement() {
        mic.getDependencyDefinitions().add(null);
        assertThrows(NullPointerException.class, () -> mic.hasDependers("anyService"), "Expected NPE because list element is null");
    }

    @Test
    @DisplayName("hasDependers propagates NullPointerException when dependencyDefinitionList itself is null")
    void throwsNPEWhenDependencyDefinitionListIsNull() throws Exception {
        Field listField = ModelInterpretationContext.class.getDeclaredField("dependencyDefinitionList");
        listField.setAccessible(true);
        listField.set(mic, null);
        assertThrows(NullPointerException.class, () -> mic.hasDependers("anyService"), "Expected NPE because dependencyDefinitionList is null");
    }
}
