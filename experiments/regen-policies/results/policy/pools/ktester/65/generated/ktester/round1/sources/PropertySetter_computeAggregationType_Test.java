package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_computeAggregationType_Test {

    /**
     * Shared context for all tests.
     */
    private static Context context;

    /**
     * Shared cache – it can be reused because the tests run sequentially.
     */
    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * Instance under test – created fresh for each test method.
     */
    private PropertySetter setter;

    /**
     * Simple bean used to drive the aggregation‑type logic.
     */
    private House house;

    /**
     * The object whose properties would be inspected (not really used because we mock the assessor).
     */
    private DummyBean dummyBean;

    /**
     * Mocked collaborator that decides the aggregation type.
     */
    @Mock
    private AggregationAssessor aggregationAssessorMock;

    /**
     * A tiny bean that mimics the real objects used by Logback.
     * Only the setters required for the assertions above are present.
     */
    public static class House {

        // basic scalar property
        public void setCount(int count) {
        }

        // complex property (object)
        public void setDoor(Object door) {
        }

        // collection of complex properties
        public void setWindow(java.util.List<Object> windows) {
        }
        // additional setters could be added here if more test cases are needed
    }

    /**
     * A tiny placeholder bean – its actual setters are irrelevant for these
     * delegation tests because the {@link AggregationAssessor} is mocked.
     */
    public static class DummyBean {
        // No methods needed; the bean exists solely to satisfy the PropertySetter constructor.
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the Logback context and the description cache once.
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh bean and PropertySetter for every test method.
        house = new House();
        setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        dummyBean = new DummyBean();
        // The real constructor we have access to takes a BeanDescriptionCache and the target object.
        setter = new PropertySetter(beanDescriptionCache, dummyBean);
        setter.setContext(context);
        // Inject the mocked AggregationAssessor via reflection (the field is final, but reflection can override it).
        Field assessorField = PropertySetter.class.getDeclaredField("aggregationAssessor");
        assessorField.setAccessible(true);
        assessorField.set(setter, aggregationAssessorMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly required but keeps the
        // test class tidy.
        setter = null;
        house = null;
        setter = null;
        dummyBean = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references.
        beanDescriptionCache = null;
        context = null;
        beanDescriptionCache = null;
        context = null;
    }

    @Test
    void testComputeAggregationType_basicAndComplexProperties() {
        // -----------------------------------------------------------------
        // Basic property – a simple scalar setter (int, boolean, String …)
        // -----------------------------------------------------------------
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("count"), "The property 'count' should be recognised as a basic property");
        // The method name is case‑insensitive – the same result for 'Count'.
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("Count"), "Aggregation type lookup should be case‑insensitive");
        // -----------------------------------------------------------------
        // Complex property – a setter that takes an object (not a scalar)
        // -----------------------------------------------------------------
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, setter.computeAggregationType("door"), "The property 'door' should be recognised as a complex property");
        // -----------------------------------------------------------------
        // Collection of complex properties – a setter that takes a List
        // -----------------------------------------------------------------
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, setter.computeAggregationType("window"), "The property 'window' should be recognised as a complex‑property collection");
    }

    /**
     * Valid aggregation types – the mock returns the expected enum for each name.
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'' → {1}")
    @CsvSource({ "'simpleProperty', SIMPLE", "'listProperty', LIST", "'mapProperty', MAP", "'arrayProperty', ARRAY" })
    void computeAggregationType_validAggregationTypes(String name, AggregationType expected) {
        // Arrange
        when(aggregationAssessorMock.computeAggregationType(name)).thenReturn(expected);
        // Act
        AggregationType result = setter.computeAggregationType(name);
        // Assert
        assertEquals(expected, result, () -> "Expected aggregation type " + expected + " for property '" + name + "'");
        verify(aggregationAssessorMock, times(1)).computeAggregationType(name);
    }

    /**
     * Edge‑case handling – unknown, empty and null property names.
     */
    @Test
    void computeAggregationType_edgeCases() {
        // unknown property → NOT_FOUND
        when(aggregationAssessorMock.computeAggregationType("unknownProperty")).thenReturn(AggregationType.NOT_FOUND);
        assertEquals(AggregationType.NOT_FOUND, setter.computeAggregationType("unknownProperty"), "Unknown property should yield NOT_FOUND");
        // empty string → NOT_FOUND
        when(aggregationAssessorMock.computeAggregationType("")).thenReturn(AggregationType.NOT_FOUND);
        assertEquals(AggregationType.NOT_FOUND, setter.computeAggregationType(""), "Empty string should yield NOT_FOUND");
        // null → NullPointerException (delegated to assessor)
        when(aggregationAssessorMock.computeAggregationType(null)).thenThrow(new NullPointerException("name is null"));
        assertThrows(NullPointerException.class, () -> setter.computeAggregationType(null), "Passing null should result in NullPointerException");
    }

    /**
     * Whitespace handling – the assessor is expected **not** to trim the name.
     */
    @Test
    void computeAggregationType_whitespaceHandling() {
        when(aggregationAssessorMock.computeAggregationType("  listProperty  ")).thenReturn(AggregationType.NOT_FOUND);
        when(aggregationAssessorMock.computeAggregationType("\tmapProperty\n")).thenReturn(AggregationType.NOT_FOUND);
        assertEquals(AggregationType.NOT_FOUND, setter.computeAggregationType("  listProperty  "), "Leading/trailing spaces should not be trimmed");
        assertEquals(AggregationType.NOT_FOUND, setter.computeAggregationType("\tmapProperty\n"), "Internal whitespace characters should not match");
    }

    /**
     * Case‑sensitivity – the assessor treats names as case‑sensitive.
     */
    @Test
    void computeAggregationType_caseSensitivity() {
        when(aggregationAssessorMock.computeAggregationType("ListProperty")).thenReturn(AggregationType.NOT_FOUND);
        when(aggregationAssessorMock.computeAggregationType("ARRAYPROPERTY")).thenReturn(AggregationType.NOT_FOUND);
        assertEquals(AggregationType.NOT_FOUND, setter.computeAggregationType("ListProperty"), "Capitalised name should not match lower‑case definition");
        assertEquals(AggregationType.NOT_FOUND, setter.computeAggregationType("ARRAYPROPERTY"), "Upper‑case name should not match lower‑case definition");
    }

    /**
     * Assessor returns {@code null} – the method should propagate the null value.
     */
    @Test
    void computeAggregationType_assessorReturnsNull() {
        when(aggregationAssessorMock.computeAggregationType("nullReturnProperty")).thenReturn(null);
        assertNull(setter.computeAggregationType("nullReturnProperty"), "Null return from assessor should be propagated");
    }

    /**
     * Assessor throws a checked/unchecked exception – it must propagate unchanged.
     */
    @Test
    void computeAggregationType_assessorThrowsIllegalArgumentException() {
        when(aggregationAssessorMock.computeAggregationType("exceptionProperty")).thenThrow(new IllegalArgumentException("bad property"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setter.computeAggregationType("exceptionProperty"));
        assertEquals("bad property", ex.getMessage());
    }

    /**
     * Name containing dot notation – treated as a normal property name by the assessor.
     */
    @Test
    void computeAggregationType_dotNotation() {
        when(aggregationAssessorMock.computeAggregationType("parent.child")).thenReturn(AggregationType.NOT_FOUND);
        assertEquals(AggregationType.NOT_FOUND, setter.computeAggregationType("parent.child"), "Dot‑notation names are not direct properties and should yield NOT_FOUND");
    }

    /**
     * PropertySetter constructed with a {@code null} aggregationAssessor – any call should NPE.
     */
    @Test
    void computeAggregationType_nullAggregationAssessor() throws Exception {
        // Create a fresh setter and then null‑out the assessor via reflection.
        PropertySetter setterWithNullAssessor = new PropertySetter(beanDescriptionCache, dummyBean);
        setterWithNullAssessor.setContext(context);
        Field assessorField = PropertySetter.class.getDeclaredField("aggregationAssessor");
        assessorField.setAccessible(true);
        assessorField.set(setterWithNullAssessor, null);
        assertThrows(NullPointerException.class, () -> setterWithNullAssessor.computeAggregationType("anyProperty"), "When aggregationAssessor is null, computeAggregationType should throw NPE");
    }

    /**
     * Assessor throws a generic {@link RuntimeException} – should propagate.
     */
    @Test
    void computeAggregationType_assessorThrowsRuntimeException() {
        when(aggregationAssessorMock.computeAggregationType("runtimeErrorProperty")).thenThrow(new RuntimeException("runtime failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> setter.computeAggregationType("runtimeErrorProperty"));
        assertEquals("runtime failure", ex.getMessage());
    }

    /**
     * Assessor throws an {@link Error} (e.g., {@link AssertionError}) – should propagate.
     */
    @Test
    void computeAggregationType_assessorThrowsError() {
        when(aggregationAssessorMock.computeAggregationType("errorProperty")).thenThrow(new AssertionError("assertion failed"));
        AssertionError err = assertThrows(AssertionError.class, () -> setter.computeAggregationType("errorProperty"));
        assertEquals("assertion failed", err.getMessage());
    }
}
