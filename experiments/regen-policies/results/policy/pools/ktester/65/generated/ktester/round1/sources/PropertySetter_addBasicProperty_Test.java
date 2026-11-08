package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.testUtil.StatusChecker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.util.StringUtil;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import java.lang.reflect.Method;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_addBasicProperty_Test {

    private static Context context;

    private PropertySetter setter;

    private SimpleBean bean;

    private StatusChecker statusChecker;

    /**
     * Simple bean used for the tests.
     */
    static class SimpleBean {

        private final List<String> items = new ArrayList<>();

        /**
         * adder method that will be discovered by PropertySetter
         */
        public void addItem(String s) {
            items.add(s);
        }

        /**
         * adder that expects an Integer – useful for conversion‑failure test
         */
        public void addNumber(Integer i) {
            // no‑op for the test, we only care that the method is invoked
        }

        public List<String> getItems() {
            return items;
        }
    }

    class NumberBean {

        public void addNumber(Integer i) {
            // no‑op
        }
    }

    /**
     * Bean with a Boolean adder used for the Boolean‑conversion test.
     */
    static class BooleanBean {

        private final List<Boolean> flags = new ArrayList<>();

        public void addEnabled(Boolean b) {
            flags.add(b);
        }

        public List<Boolean> getFlags() {
            return flags;
        }
    }

    /**
     * Bean with a two‑parameter adder to trigger a sanity‑check failure.
     */
    static class ComplexAdderBean {

        public void addComplex(String a, String b) {
            // no‑op
        }
    }

    /* --------------------------------------------------------------------- */
    private static class StatusCheckerExtension extends StatusChecker {

        StatusCheckerExtension(Context ctx) {
            super(ctx);
        }

        boolean containsErrorMessage() {
            return !getStatusList().isEmpty();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a single Context instance is enough for all tests
        context = new ContextBase();
    }

    @BeforeEach
    void setupBeforeEach() {
        bean = new SimpleBean();
        // PropertySetter constructor that matches the original test framework
        setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        statusChecker = new StatusChecker(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear any status messages so that each test starts clean
        statusChecker.clear();
    }

    /**
     * Utility to inject a mock {@link AggregationAssessor} into the
     * {@link PropertySetter} instance via reflection.
     */
    private void injectAggregationAssessor(AggregationAssessor mockAssessor) throws Exception {
        java.lang.reflect.Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(setter, mockAssessor);
    }

    @Test
    void addBasicPropertyInvokesAdder() {
        // "item" → capitalized to "Item" → looks for method addItem(String)
        setter.addBasicProperty("item", "apple");
        List<String> items = bean.getItems();
        assertEquals(1, items.size(), "Adder should have been called once");
        assertEquals("apple", items.get(0), "Added value should be stored");
    }

    @Test
    void addBasicPropertyIgnoresNullValue() {
        setter.addBasicProperty("item", null);
        assertTrue(bean.getItems().isEmpty(), "No value should be added when the string is null");
    }

    @Test
    void addBasicPropertyWhenNoAdderExistsLogsError() {
        // Use a bean that does NOT have an appropriate adder method
        Object dummy = new Object();
        PropertySetter dummySetter = new PropertySetter(new BeanDescriptionCache(context), dummy);
        dummySetter.setContext(context);
        dummySetter.addBasicProperty("nonexistent", "value");
        // The StatusChecker should have captured an error about the missing adder
        assertTrue(statusChecker.containsMessage("No adder for property [Nonexistent]"), "Expected an error status when no adder method is found");
    }

    @Test
    void addBasicPropertyConversionFailureLogsError() {
        // The bean has an adder that expects Integer, we pass a non‑numeric string
        // Replace the bean with one that only has addNumber(Integer)
        class NumberBean {

            public void addNumber(Integer i) {
                // no‑op
            }
        }
        NumberBean numberBean = new NumberBean();
        PropertySetter numberSetter = new PropertySetter(new BeanDescriptionCache(context), numberBean);
        numberSetter.setContext(context);
        numberSetter.addBasicProperty("number", "not-a-number");
        // Expect an error about conversion failure
        assertTrue(statusChecker.containsMessage("Conversion to type [class java.lang.Integer] failed"), "Expected a conversion‑failure error status");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullValueIsIgnored() throws Exception {
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        injectAggregationAssessor(mockAssessor);
        setter.addBasicProperty("someProperty", null);
        // No lookup, conversion or invocation should happen
        verifyNoInteractions(mockAssessor);
        assertTrue(bean.getItems().isEmpty(), "Bean must stay untouched");
        assertFalse(statusChecker.containsErrorMessage(), "No error should be logged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void noAdderMethodLogsError() throws Exception {
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findAdderMethod("MissingProperty")).thenReturn(null);
        injectAggregationAssessor(mockAssessor);
        setter.addBasicProperty("missingProperty", "anyValue");
        assertTrue(statusChecker.containsMessage("No adder for property [MissingProperty]"), "Expected an error status when adder is missing");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void conversionExceptionIsLogged() throws Exception {
        // Bean that only has addNumber(Integer)
        SimpleBean numberBean = new SimpleBean() {

            public void addNumber(Integer i) {
            }
        };
        setter = new PropertySetter(new BeanDescriptionCache(context), numberBean);
        setter.setContext(context);
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = SimpleBean.class.getMethod("addNumber", Integer.class);
        when(mockAssessor.findAdderMethod("IntProperty")).thenReturn(adder);
        injectAggregationAssessor(mockAssessor);
        try (MockedStatic<StringToObjectConverter> mockedConverter = mockStatic(StringToObjectConverter.class)) {
            mockedConverter.when(() -> StringToObjectConverter.convertArg(any(), eq("notANumber"), eq(Integer.class))).thenThrow(new NumberFormatException("bad number"));
            setter.addBasicProperty("intProperty", "notANumber");
        }
        assertTrue(statusChecker.containsMessage("Conversion to type [class java.lang.Integer] failed"), "Conversion failure must be logged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void conversionReturnsNullDoesNotInvoke() throws Exception {
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = SimpleBean.class.getMethod("addItem", String.class);
        when(mockAssessor.findAdderMethod("CustomProperty")).thenReturn(adder);
        injectAggregationAssessor(mockAssessor);
        // Stub conversion to return null
        try (MockedStatic<StringToObjectConverter> mockedConverter = mockStatic(StringToObjectConverter.class)) {
            mockedConverter.when(() -> StringToObjectConverter.convertArg(any(), eq("nullValue"), eq(String.class))).thenReturn(null);
            // Spy on setter to verify that invokeMethod... is NOT called
            PropertySetter spySetter = spy(setter);
            spySetter.addBasicProperty("customProperty", "nullValue");
            verify(spySetter, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void successfulConversionInvokesAdder() throws Exception {
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = SimpleBean.class.getMethod("addItem", String.class);
        when(mockAssessor.findAdderMethod("ListItem")).thenReturn(adder);
        injectAggregationAssessor(mockAssessor);
        try (MockedStatic<StringToObjectConverter> mockedConverter = mockStatic(StringToObjectConverter.class)) {
            // conversion returns a non‑null object (the actual value is irrelevant)
            mockedConverter.when(() -> StringToObjectConverter.convertArg(any(), eq("42"), eq(String.class))).thenReturn("42");
            PropertySetter spySetter = spy(setter);
            spySetter.addBasicProperty("listItem", "42");
            // Verify that the reflective invocation helper is called exactly once
            verify(spySetter, times(1)).invokeMethodWithSingleParameterOnThisObject(eq(adder), eq("42"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nameIsCapitalizedBeforeLookup() throws Exception {
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = SimpleBean.class.getMethod("addItem", String.class);
        // Expect lookup with capitalized name "MyProperty"
        when(mockAssessor.findAdderMethod("MyProperty")).thenReturn(adder);
        injectAggregationAssessor(mockAssessor);
        try (MockedStatic<StringToObjectConverter> mockedConverter = mockStatic(StringToObjectConverter.class)) {
            mockedConverter.when(() -> StringToObjectConverter.convertArg(any(), eq("123"), eq(String.class))).thenReturn("123");
            PropertySetter spySetter = spy(setter);
            spySetter.addBasicProperty("myProperty", "123");
            verify(mockAssessor, times(1)).findAdderMethod("MyProperty");
            verify(spySetter, times(1)).invokeMethodWithSingleParameterOnThisObject(eq(adder), eq("123"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void emptyPropertyNameLogsError() throws Exception {
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        // capitalizeFirstLetter(\"\") returns \"\"
        when(mockAssessor.findAdderMethod("")).thenReturn(null);
        injectAggregationAssessor(mockAssessor);
        setter.addBasicProperty("", "someValue");
        assertTrue(statusChecker.containsMessage("No adder for property []"), "Missing adder for empty name must be logged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void booleanConversionSuccessInvokesAdder() throws Exception {
        BooleanBean boolBean = new BooleanBean();
        setter = new PropertySetter(new BeanDescriptionCache(context), boolBean);
        setter.setContext(context);
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = BooleanBean.class.getMethod("addEnabled", Boolean.class);
        when(mockAssessor.findAdderMethod("Enabled")).thenReturn(adder);
        injectAggregationAssessor(mockAssessor);
        try (MockedStatic<StringToObjectConverter> mockedConverter = mockStatic(StringToObjectConverter.class)) {
            mockedConverter.when(() -> StringToObjectConverter.convertArg(any(), eq("true"), eq(Boolean.class))).thenReturn(Boolean.TRUE);
            PropertySetter spySetter = spy(setter);
            spySetter.addBasicProperty("enabled", "true");
            verify(spySetter, times(1)).invokeMethodWithSingleParameterOnThisObject(eq(adder), eq("true"));
            assertEquals(1, boolBean.getFlags().size());
            assertTrue(boolBean.getFlags().get(0));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void invocationExceptionIsPropagated() throws Exception {
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = SimpleBean.class.getMethod("addItem", String.class);
        when(mockAssessor.findAdderMethod("ListItem")).thenReturn(adder);
        injectAggregationAssessor(mockAssessor);
        try (MockedStatic<StringToObjectConverter> mockedConverter = mockStatic(StringToObjectConverter.class)) {
            mockedConverter.when(() -> StringToObjectConverter.convertArg(any(), eq("99"), eq(String.class))).thenReturn("99");
            PropertySetter spySetter = spy(setter);
            doThrow(new RuntimeException("boom")).when(spySetter).invokeMethodWithSingleParameterOnThisObject(any(), any());
            RuntimeException ex = assertThrows(RuntimeException.class, () -> spySetter.addBasicProperty("listItem", "99"));
            assertEquals("boom", ex.getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sanityCheckFailureLogsErrorButContinues() throws Exception {
        ComplexAdderBean complexBean = new ComplexAdderBean();
        setter = new PropertySetter(new BeanDescriptionCache(context), complexBean);
        setter.setContext(context);
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = ComplexAdderBean.class.getMethod("addComplex", String.class, String.class);
        when(mockAssessor.findAdderMethod("Complex")).thenReturn(adder);
        injectAggregationAssessor(mockAssessor);
        try (MockedStatic<StringToObjectConverter> mockedConverter = mockStatic(StringToObjectConverter.class)) {
            // conversion returns a dummy non‑null object; the actual type is irrelevant
            mockedConverter.when(() -> StringToObjectConverter.convertArg(any(), eq("someValue"), any())).thenReturn("someValue");
            PropertySetter spySetter = spy(setter);
            spySetter.addBasicProperty("complex", "someValue");
            // The sanity‑check logs an error – we only verify that the message appears
            assertTrue(statusChecker.containsMessage("sanity"), "Sanity‑check error should be logged");
            // Despite the sanity‑check failure, invocation must still happen
            verify(spySetter, times(1)).invokeMethodWithSingleParameterOnThisObject(eq(adder), eq("someValue"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullPropertyNameIsHandledGracefully() throws Exception {
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        // findAdderMethod(null) -> null
        when(mockAssessor.findAdderMethod(null)).thenReturn(null);
        injectAggregationAssessor(mockAssessor);
        setter.addBasicProperty(null, "value");
        assertTrue(statusChecker.containsMessage("No adder for property [null]"), "Null property name should result in a missing‑adder error");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findAdderMethodExceptionBubblesUp() throws Exception {
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findAdderMethod("AnyProperty")).thenThrow(new RuntimeException("finder boom"));
        injectAggregationAssessor(mockAssessor);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> setter.addBasicProperty("anyProperty", "someValue"));
        assertEquals("finder boom", ex.getMessage());
    }
}
