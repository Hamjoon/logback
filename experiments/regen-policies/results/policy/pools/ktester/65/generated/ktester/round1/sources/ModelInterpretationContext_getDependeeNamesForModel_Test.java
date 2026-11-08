package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getDependeeNamesForModel_Test {

    private ModelInterpretationContext mic;

    @Mock
    private Context // mock of the Logback Context
    mockContext;

    @Mock
    private Model // first model used in the tests
    modelA;

    @Mock
    private Model // second model used in the tests
    modelB;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test case.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern tidy.
        mic = null;
    }

    private static Stream<Arguments> emptyAndNoMatchProvider() {
        // The mock models are injected by JUnit; we use placeholders and let the test method use the class fields.
        return Stream.of(Arguments.of("The dependencyDefinitionList is empty, therefore the method should return an empty list regardless of the supplied model.", // will be replaced with modelA inside the test (null indicates use of modelA)
        null, Collections.emptyList()), Arguments.of("A single DependencyDefinition whose depender is a different Model instance (modelB).", // placeholder – test method will use modelA
        null, Collections.emptyList()), Arguments.of("The supplied model is null but there is no DependencyDefinition with a null depender.", null, Collections.emptyList()));
    }

    /**
     * When the MIC contains no {@link DependencyDefinition} entries,
     * the method must return an empty list.
     */
    @Test
    void returnsEmptyListWhenNoDependenciesDefined() {
        // GIVEN – mic is freshly created and therefore has an empty dependency list.
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(modelA);
        // THEN
        assertNotNull(result, "Result list should never be null");
        assertTrue(result.isEmpty(), "Result list should be empty when no dependencies are defined");
    }

    /**
     * The method should return the names of all dependee models that are
     * associated with the supplied depender model.
     */
    @Test
    void returnsOnlyMatchingDependeeNames() {
        // GIVEN
        DependencyDefinition ddForA = mock(DependencyDefinition.class);
        when(ddForA.getDepender()).thenReturn(modelA);
        when(ddForA.getDependee()).thenReturn("dependee-of-A");
        DependencyDefinition ddForB = mock(DependencyDefinition.class);
        when(ddForB.getDepender()).thenReturn(modelB);
        when(ddForB.getDependee()).thenReturn("dependee-of-B");
        // Add both definitions to the MIC.
        mic.addDependencyDefinition(ddForA);
        mic.addDependencyDefinition(ddForB);
        // WHEN
        List<String> resultForA = mic.getDependeeNamesForModel(modelA);
        List<String> resultForB = mic.getDependeeNamesForModel(modelB);
        // THEN
        assertEquals(1, resultForA.size(), "Exactly one dependee should be returned for modelA");
        assertEquals("dependee-of-A", resultForA.get(0), "Returned dependee name for modelA is incorrect");
        assertEquals(1, resultForB.size(), "Exactly one dependee should be returned for modelB");
        assertEquals("dependee-of-B", resultForB.get(0), "Returned dependee name for modelB is incorrect");
    }

    /**
     * When several {@link DependencyDefinition}s refer to the same depender,
     * all matching dependee names must be returned in insertion order.
     */
    @Test
    void returnsAllMatchingDependeeNamesInOrder() {
        // GIVEN
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        when(dd1.getDepender()).thenReturn(modelA);
        when(dd1.getDependee()).thenReturn("first");
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        when(dd2.getDepender()).thenReturn(modelA);
        when(dd2.getDependee()).thenReturn("second");
        DependencyDefinition dd3 = mock(DependencyDefinition.class);
        when(dd3.getDepender()).thenReturn(modelA);
        when(dd3.getDependee()).thenReturn("third");
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        mic.addDependencyDefinition(dd3);
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(modelA);
        // THEN
        assertEquals(3, result.size(), "All three dependee names should be returned");
        assertEquals(List.of("first", "second", "third"), result, "Dependee names should be returned in the order they were added");
    }

    /**
     * No match scenario: a DependencyDefinition exists but its depender is a different
     * Model instance. The method must return an empty list.
     */
    @Test
    void returnsEmptyWhenDependerDoesNotMatch() {
        // GIVEN
        DependencyDefinition dd = mock(DependencyDefinition.class);
        // different model
        when(dd.getDepender()).thenReturn(modelB);
        when(dd.getDependee()).thenReturn("someDep");
        mic.addDependencyDefinition(dd);
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(modelA);
        // THEN
        assertTrue(result.isEmpty(), "Result should be empty when no depender matches the supplied model");
    }

    /**
     * Null model supplied while there is no null‑depender entry – expect empty list.
     */
    @Test
    void nullModelWithoutNullDependerReturnsEmpty() {
        // GIVEN – dependency list is empty (fresh MIC)
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(null);
        // THEN
        assertTrue(result.isEmpty(), "Result should be empty when model is null and no null depender exists");
    }

    /**
     * Null depender matching: a DependencyDefinition whose depender is {@code null}
     * must be selected when the supplied model is {@code null}.
     */
    @Test
    void matchesNullDependerWhenModelIsNull() {
        // GIVEN
        DependencyDefinition dd = mock(DependencyDefinition.class);
        when(dd.getDepender()).thenReturn(null);
        when(dd.getDependee()).thenReturn("globalDep");
        mic.addDependencyDefinition(dd);
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(null);
        // THEN
        assertEquals(1, result.size(), "Exactly one entry should be returned for null model");
        assertEquals("globalDep", result.get(0), "Returned dependee name does not match expected value");
    }

    /**
     * Duplicate dependee names are allowed – the method must preserve both entries.
     */
    @Test
    void returnsDuplicateDependeeNamesPreservingDuplicates() {
        // GIVEN
        DependencyDefinition dd1 = mock(DependencyDefinition.class);
        when(dd1.getDepender()).thenReturn(modelA);
        when(dd1.getDependee()).thenReturn("common");
        DependencyDefinition dd2 = mock(DependencyDefinition.class);
        when(dd2.getDepender()).thenReturn(modelA);
        when(dd2.getDependee()).thenReturn("common");
        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(modelA);
        // THEN
        assertEquals(2, result.size(), "Both duplicate entries should be present");
        assertEquals(List.of("common", "common"), result, "Duplicate entries must be preserved in order");
    }

    /**
     * Reference equality vs logical equality: even if two Model mocks have the same
     * behaviour, the method uses {@code ==}. Therefore a different instance does not match.
     */
    @Test
    void usesReferenceEqualityForModelComparison() {
        // GIVEN
        DependencyDefinition dd = mock(DependencyDefinition.class);
        // modelB is a different mock instance
        when(dd.getDepender()).thenReturn(modelB);
        when(dd.getDependee()).thenReturn("someDep");
        mic.addDependencyDefinition(dd);
        // WHEN
        // modelA != modelB
        List<String> result = mic.getDependeeNamesForModel(modelA);
        // THEN
        assertTrue(result.isEmpty(), "Result should be empty because the comparison is by reference (==)");
    }

    /**
     * Null dependee handling – the method should add the {@code null} value to the result list.
     */
    @Test
    void includesNullDependeeInResult() {
        // GIVEN
        DependencyDefinition dd = mock(DependencyDefinition.class);
        when(dd.getDepender()).thenReturn(modelA);
        // explicit null
        when(dd.getDependee()).thenReturn(null);
        mic.addDependencyDefinition(dd);
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(modelA);
        // THEN
        assertEquals(1, result.size(), "Result should contain exactly one element");
        assertNull(result.get(0), "The element should be null as per the DependencyDefinition");
    }

    /**
     * Large dependency list – 20 entries alternating between two models.
     * Only the even‑indexed entries (dep0, dep2, …) should be returned for modelX.
     */
    @Test
    void handlesLargeDependencyListAndPreservesOrder() {
        // GIVEN
        Model modelX = mock(Model.class);
        Model otherModel = mock(Model.class);
        // Populate 20 DependencyDefinitions
        for (int i = 0; i < 20; i++) {
            DependencyDefinition dd = mock(DependencyDefinition.class);
            Model depender = (i % 2 == 0) ? modelX : otherModel;
            when(dd.getDepender()).thenReturn(depender);
            when(dd.getDependee()).thenReturn("dep" + i);
            mic.addDependencyDefinition(dd);
        }
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(modelX);
        // THEN
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 20; i += 2) {
            expected.add("dep" + i);
        }
        assertEquals(expected, result, "Only the even‑indexed dependee names should be returned for modelX");
    }

    /**
     * Internal list contains a {@code null} element – iteration should throw
     * {@link NullPointerException} when the method attempts to dereference it.
     */
    @Test
    void throwsNPEWhenDependencyDefinitionListContainsNullElement() throws Exception {
        // GIVEN – add a normal definition first
        DependencyDefinition validDd = mock(DependencyDefinition.class);
        when(validDd.getDepender()).thenReturn(modelA);
        when(validDd.getDependee()).thenReturn("valid");
        mic.addDependencyDefinition(validDd);
        // Use reflection to inject a null element into the list
        Field listField = ModelInterpretationContext.class.getDeclaredField("dependencyDefinitionList");
        listField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<DependencyDefinition> list = (List<DependencyDefinition>) listField.get(mic);
        // now the list contains a null entry
        list.add(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mic.getDependeeNamesForModel(modelA), "Method should throw NPE when encountering a null DependencyDefinition");
    }

    /**
     * The internal {@code dependencyDefinitionList} itself is {@code null}.
     * The enhanced for‑loop should throw {@link NullPointerException}.
     */
    @Test
    void throwsNPEWhenDependencyDefinitionListIsNull() throws Exception {
        // GIVEN – set the internal list to null via reflection
        Field listField = ModelInterpretationContext.class.getDeclaredField("dependencyDefinitionList");
        listField.setAccessible(true);
        listField.set(mic, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mic.getDependeeNamesForModel(modelA), "Method should throw NPE when the internal dependencyDefinitionList is null");
    }

    /**
     * Parameterised test covering the "Empty and No‑Match Scenarios" group.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("emptyAndNoMatchProvider")
    void emptyAndNoMatchScenarios(String description, Model suppliedModel, List<String> expected) {
        // GIVEN – for the second case we need a non‑matching DependencyDefinition
        if ("A single DependencyDefinition whose depender is a different Model instance (modelB).".equals(description)) {
            DependencyDefinition dd = mock(DependencyDefinition.class);
            when(dd.getDepender()).thenReturn(modelB);
            when(dd.getDependee()).thenReturn("irrelevant");
            mic.addDependencyDefinition(dd);
        }
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(suppliedModel);
        // THEN
        assertEquals(expected, result, description);
    }
}
