package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.util.AggregationAssessor;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

// kept for consistency with the template; not used here
@ExtendWith(MockitoExtension.class)
class PropertySetter_computeAggregationType_Test {

    /**
     * Shared Logback {@link Context} used by the {@link BeanDescriptionCache}.
     */
    private static Context context;

    /**
     * Shared cache – it is cheap to reuse it across tests because it does not
     * retain mutable state that would affect the outcome of {@code computeAggregationType}.
     */
    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * Minimal bean with a handful of getters/setters that the {@link PropertySetter}
     * can introspect.  The actual implementation is not important for the
     * aggregation test – only the method signatures matter.
     */
    @SuppressWarnings("unused")
    private static class House {

        private int count;

        private double temperature;

        private String name;

        private boolean open;

        private FileSize fs;

        private FilterReply filterReply;

        private HouseColor houseColor;

        private Charset charset;

        // basic properties -------------------------------------------------
        public void setCount(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setOpen(boolean open) {
            this.open = open;
        }

        public boolean isOpen() {
            return open;
        }

        public void setFs(FileSize fs) {
            this.fs = fs;
        }

        public FileSize getFs() {
            return fs;
        }

        public void setFilterReply(FilterReply filterReply) {
            this.filterReply = filterReply;
        }

        public FilterReply getFilterReply() {
            return filterReply;
        }

        public void setHouseColor(HouseColor houseColor) {
            this.houseColor = houseColor;
        }

        public HouseColor getHouseColor() {
            return houseColor;
        }

        public void setCharset(Charset charset) {
            this.charset = charset;
        }

        public Charset getCharset() {
            return charset;
        }

        // complex property -------------------------------------------------
        public void setDoor(Door door) {
            /* no‑op for the test */
        }

        public Door getDoor() {
            return null;
        }

        // collection property ----------------------------------------------
        public void addWindow(Window window) {
            /* no‑op for the test */
        }

        public void addAdjective(String adjective) {
            /* no‑op for the test */
        }
    }

    /**
     * Simple placeholder types – they do not contain logic; they only exist
     * so that the reflection‑based discovery performed by {@code PropertySetter}
     * can find the corresponding setters.
     */
    private static class Door {
    }

    private static class Window {
    }

    private static class FileSize {

        private long size;

        public void setSize(long size) {
            this.size = size;
        }

        public long getSize() {
            return size;
        }
    }

    /**
     * Bean that implements a generic interface, thereby generating bridge
     * methods at compile time.
     */
    private static class Orange implements CitrusInterface<String> {

        @Override
        public void setPrecarpPropertyName(String value) {
            /* no‑op */
        }

        @Override
        public void setPrefixPropertyName(String value) {
            /* no‑op */
        }
    }

    /**
     * Generic interface used to provoke bridge methods in {@link Orange}.
     */
    private interface CitrusInterface<T> {

        void setPrecarpPropertyName(T value);

        void setPrefixPropertyName(T value);
    }

    @BeforeAll
    static void setupBeforeAll() {
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to release.
        context = null;
        beanDescriptionCache = null;
    }

    private static Stream<Arguments> delegationCases() {
        return Stream.of(Arguments.of("basic", AggregationType.AS_BASIC_PROPERTY), Arguments.of("complex", AggregationType.AS_COMPLEX_PROPERTY), Arguments.of("nested", AggregationType.AS_NESTED_COMPONENT), Arguments.of("unknown", AggregationType.NOT_FOUND), // null argument – mock returns null
        Arguments.of(null, null));
    }

    private static Stream<Arguments> edgeCaseCases() {
        return Stream.of(Arguments.of("", AggregationType.NOT_FOUND), Arguments.of("   ", AggregationType.NOT_FOUND), Arguments.of("BaSiC", AggregationType.NOT_FOUND), Arguments.of("basic ", AggregationType.NOT_FOUND), // The special case where the assessor is stubbed to return null.
        Arguments.of("nullReturn", null));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null aggregationAssessor – handled specially in the test method.
        Arguments.of("basic", NullPointerException.class), // IllegalArgumentException for illegal characters.
        Arguments.of("invalid!name", IllegalArgumentException.class), // Mock throws NPE when name itself is null.
        Arguments.of(null, NullPointerException.class), // Unchecked runtime exception.
        Arguments.of("triggerRuntime", IllegalStateException.class), // Checked exception wrapped in RuntimeException – the mock throws it directly.
        Arguments.of("throwsPropertySetterException", PropertySetterException.class));
    }

    /**
     * ----------------------------------------------------------------
     *  Helper – inject (or clear) the private {@code aggregationAssessor} field
     *  via reflection.  This keeps the test code independent from the internal
     *  constructor logic.
     * ----------------------------------------------------------------
     */
    private static void injectAggregationAssessor(PropertySetter setter, AggregationAssessor assessor) throws Exception {
        Field field = PropertySetter.class.getDeclaredField("aggregationAssessor");
        field.setAccessible(true);
        field.set(setter, assessor);
    }

    /**
     * Verifies that {@code computeAggregationType} returns the correct
     * {@link AggregationType} for a representative set of property names.
     *
     * <p>The expected values are taken from Logback's own test suite
     * (<code>PropertySetterTest.testCanAggregateComponent</code>).</p>
     */
    @Test
    void testAggregationTypeForHouseProperties() {
        // Arrange – a simple bean that mimics the one used in Logback's own tests.
        House house = new House();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        // Act & Assert
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, setter.computeAggregationType("door"), "door → complex property");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("count"), "count → basic property");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("Count"), "Count (capitalised) → basic property");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("name"), "name → basic property");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("Name"), "Name (capitalised) → basic property");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("Duration"), "Duration → basic property");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("fs"), "fs (file size) → basic property");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("open"), "open → basic property");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("Open"), "Open (capitalised) → basic property");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, setter.computeAggregationType("Window"), "Window → complex collection");
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, setter.computeAggregationType("adjective"), "adjective → basic collection");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("filterReply"), "filterReply → basic property (enum)");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("houseColor"), "houseColor → basic property (enum)");
    }

    /**
     * Ensures that bridge methods (generated by the compiler for generic
     * interfaces) are ignored by the aggregation logic – they must be treated
     * as ordinary basic properties.
     *
     * <p>This mirrors the behaviour verified in Logback's
     * {@code PropertySetterTest.bridgeMethodsShouldBeIgnored}.</p>
     */
    @Test
    void testBridgeMethodsAreIgnored() {
        // Arrange – a bean that implements a generic interface, thus exposing
        // bridge methods.
        Orange orange = new Orange();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, orange);
        setter.setContext(context);
        // Act & Assert
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType(Citrus.PRECARP_PROPERTY_NAME), "bridge method name should be treated as basic property");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType(Citrus.PREFIX_PROPERTY_NAME), "another bridge method name should be treated as basic property");
    }

    /**
     * ----------------------------------------------------------------
     *  Delegation tests – verify that {@code computeAggregationType} simply
     *  forwards the call to the injected {@link AggregationAssessor} and
     *  returns whatever the assessor provides.
     * ----------------------------------------------------------------
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'' → expected={1}")
    @MethodSource("delegationCases")
    void testComputeAggregationTypeDelegates(String name, AggregationType expected) throws Exception {
        // Arrange – mock AggregationAssessor
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.computeAggregationType(name)).thenReturn(expected);
        // Create a PropertySetter (the real constructor needs a bean; we can
        // pass a dummy Object) and inject the mock via reflection.
        PropertySetter setter = new PropertySetter(beanDescriptionCache, new Object());
        injectAggregationAssessor(setter, mockAssessor);
        setter.setContext(context);
        // Act
        AggregationType result = setter.computeAggregationType(name);
        // Assert
        assertEquals(expected, result, () -> "Delegation failed for name='" + name + "'. Expected " + expected);
        verify(mockAssessor, times(1)).computeAggregationType(name);
    }

    /**
     * ----------------------------------------------------------------
     *  Edge‑case tests – ensure that the method behaves correctly for empty,
     *  whitespace‑only or otherwise malformed names when the assessor is
     *  stubbed to return {@link AggregationType#NOT_FOUND} or {@code null}.
     * ----------------------------------------------------------------
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'' → expected={1}")
    @MethodSource("edgeCaseCases")
    void testComputeAggregationTypeEdgeCases(String name, AggregationType expected) throws Exception {
        // Arrange – mock that always returns the supplied expected value.
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.computeAggregationType(name)).thenReturn(expected);
        PropertySetter setter = new PropertySetter(beanDescriptionCache, new Object());
        injectAggregationAssessor(setter, mockAssessor);
        setter.setContext(context);
        // Act
        AggregationType result = setter.computeAggregationType(name);
        // Assert
        assertEquals(expected, result, () -> "Edge case failed for name='" + name + "'. Expected " + expected);
        verify(mockAssessor, times(1)).computeAggregationType(name);
    }

    /**
     * ----------------------------------------------------------------
     *  Exception handling tests – verify that any exception thrown by the
     *  {@link AggregationAssessor} (or a missing assessor) propagates unchanged.
     * ----------------------------------------------------------------
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'' → throws {1}")
    @MethodSource("exceptionCases")
    void testComputeAggregationTypeExceptionPropagation(String name, Class<? extends Throwable> exceptionClass) throws Exception {
        PropertySetter setter = new PropertySetter(beanDescriptionCache, new Object());
        // Special case: null aggregationAssessor → NPE without any stubbing.
        if (exceptionClass == NullPointerException.class && name.equals("basic")) {
            // Force the private field to null.
            injectAggregationAssessor(setter, null);
            setter.setContext(context);
            assertThrows(NullPointerException.class, () -> setter.computeAggregationType(name), "Expected NullPointerException when aggregationAssessor is null");
            return;
        }
        // Normal case: mock assessor throws the required exception.
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.computeAggregationType(name)).thenThrow(exceptionClass);
        injectAggregationAssessor(setter, mockAssessor);
        setter.setContext(context);
        assertThrows(exceptionClass, () -> setter.computeAggregationType(name), () -> "Expected exception " + exceptionClass.getSimpleName() + " for name='" + name + "'");
        verify(mockAssessor, times(1)).computeAggregationType(name);
    }
}
