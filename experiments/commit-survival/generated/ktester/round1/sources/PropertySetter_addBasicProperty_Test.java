package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.util.AggregationType;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class PropertySetter_addBasicProperty_Test {

    /**
     * Shared Logback context – required by BeanDescriptionCache.
     */
    private static Context context;

    /**
     * Cache used by every PropertySetter instance in this test suite.
     */
    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * The bean on which the adder will be invoked.
     */
    private TestBean bean;

    /**
     * The PropertySetter under test – created as a Mockito spy so that we can verify error handling.
     */
    private PropertySetter setter;

    /* --------------------------------------------------------------------- */
    static class TestBean {

        private final List<String> doors = new ArrayList<>();

        // basic adder used by the original tests
        public void addDoor(String door) {
            doors.add(door);
        }

        public List<String> getDoors() {
            return doors;
        }

        // ----- additional adder methods for the extended test suite -----
        public void addThreshold(Integer value) {
            /* no‑op, just existence */
        }

        public void addOptionalValue(String value) {
            /* no‑op */
        }

        public void addPort(Integer value) {
            /* no‑op */
        }

        public void addSize(String size) {
            /* no‑op */
        }

        public void addColor(String color) {
            /* no‑op */
        }

        public void addMaxConnections(int max) {
            /* no‑op */
        }

        // adder with zero parameters – used to provoke ArrayIndexOutOfBoundsException
        public void addEmptyAdder() {
            /* no‑op */
        }

        // adder that will be invoked but we force the invocation to fail
        public void addFailingAdder(Object o) {
            /* no‑op */
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        bean = new TestBean();
        // create a real instance first, then spy it – we need the real reflection logic
        PropertySetter realSetter = new PropertySetter(beanDescriptionCache, bean);
        realSetter.setContext(context);
        setter = Mockito.spy(realSetter);
    }

    @Test
    void addBasicProperty_successfulAddsElement() {
        // Act
        setter.addBasicProperty("door", "front");
        // Assert – the bean’s collection must contain the added value
        List<String> doors = bean.getDoors();
        assertEquals(1, doors.size(), "Exactly one door should have been added");
        assertEquals("front", doors.get(0), "The added door must match the supplied string");
        // No error should have been logged
        verify(setter, never()).addError(anyString());
    }

    @Test
    void addBasicProperty_nullValueIsIgnored() {
        // Act – passing a null value must be a no‑op
        setter.addBasicProperty("door", null);
        // Assert – bean stays untouched and no error is reported
        assertTrue(bean.getDoors().isEmpty(), "No doors should be added when value is null");
        verify(setter, never()).addError(anyString());
    }

    @Test
    void addBasicProperty_noAdderMethodLogsError() {
        // Act – property name that does not map to any “addXxx” method
        setter.addBasicProperty("window", "big");
        // Assert – the bean must not have been modified
        assertTrue(bean.getDoors().isEmpty(), "Bean should not be modified when adder is missing");
        // Verify that an error was recorded. The exact message format is defined in the
        // implementation (see PropertySetter.addBasicProperty), we only check that it
        // contains the expected fragment.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(setter, times(1)).addError(captor.capture());
        String loggedMessage = captor.getValue();
        assertTrue(loggedMessage.contains("No adder for property [Window]"), "Error message should mention the missing adder");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addBasicProperty_successfulConversionInvokesAdder() {
        try (MockedStatic<StringToObjectConverter> mocked = mockStatic(StringToObjectConverter.class)) {
            // simulate successful conversion to Integer
            mocked.when(() -> StringToObjectConverter.convertArg(setter, "10", Integer.class)).thenReturn(10);
            // invoke the method – adder is addThreshold(Integer)
            setter.addBasicProperty("threshold", "10");
            // verify that the adder was invoked with the original string value
            verify(setter, times(1)).invokeMethodWithSingleParameterOnThisObject(argThat((Method m) -> m.getName().equals("addThreshold")), eq("10"));
            verify(setter, never()).addError(anyString());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addBasicProperty_conversionReturnsNullDoesNotInvokeAdder() {
        try (MockedStatic<StringToObjectConverter> mocked = mockStatic(StringToObjectConverter.class)) {
            mocked.when(() -> StringToObjectConverter.convertArg(setter, "", String.class)).thenReturn(// empty string leads to null
            null);
            setter.addBasicProperty("optionalValue", "");
            verify(setter, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
            verify(setter, never()).addError(anyString());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addBasicProperty_conversionThrowsExceptionLogsError() {
        try (MockedStatic<StringToObjectConverter> mocked = mockStatic(StringToObjectConverter.class)) {
            RuntimeException conversionEx = new RuntimeException("bad number");
            mocked.when(() -> StringToObjectConverter.convertArg(setter, "notANumber", Integer.class)).thenThrow(conversionEx);
            setter.addBasicProperty("port", "notANumber");
            // error must be logged with the expected fragment
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(setter, times(1)).addError(captor.capture(), eq(conversionEx));
            String msg = captor.getValue();
            assertTrue(msg.contains("Conversion to type [class java.lang.Integer] failed"), "Error message should mention conversion failure");
            verify(setter, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addBasicProperty_nameCapitalizationLowerCase() {
        try (MockedStatic<StringToObjectConverter> mocked = mockStatic(StringToObjectConverter.class)) {
            mocked.when(() -> StringToObjectConverter.convertArg(setter, "large", String.class)).thenReturn("large");
            setter.addBasicProperty("size", "large");
            verify(setter, times(1)).invokeMethodWithSingleParameterOnThisObject(argThat((Method m) -> m.getName().equals("addSize")), eq("large"));
        }
    }

    @Test
    void addBasicProperty_nameCapitalizationAlreadyUpperCase() {
        try (MockedStatic<StringToObjectConverter> mocked = mockStatic(StringToObjectConverter.class)) {
            mocked.when(() -> StringToObjectConverter.convertArg(setter, "red", String.class)).thenReturn("red");
            setter.addBasicProperty("Color", "red");
            verify(setter, times(1)).invokeMethodWithSingleParameterOnThisObject(argThat((Method m) -> m.getName().equals("addColor")), eq("red"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addBasicProperty_primitiveConversionInvokesAdder() {
        try (MockedStatic<StringToObjectConverter> mocked = mockStatic(StringToObjectConverter.class)) {
            mocked.when(() -> StringToObjectConverter.convertArg(setter, "100", int.class)).thenReturn(// autoboxed Integer is fine
            100);
            setter.addBasicProperty("maxConnections", "100");
            verify(setter, times(1)).invokeMethodWithSingleParameterOnThisObject(argThat((Method m) -> m.getName().equals("addMaxConnections")), eq("100"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addBasicProperty_nullNameThrowsNPE() {
        assertThrows(NullPointerException.class, () -> setter.addBasicProperty(null, "someValue"), "Passing a null property name must result in NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addBasicProperty_adderWithNoParametersThrowsAIOOBE() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> setter.addBasicProperty("emptyAdder", "irrelevant"), "Zero‑parameter adder should cause ArrayIndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addBasicProperty_adderInvocationThrowsExceptionPropagates() {
        // stub conversion to succeed
        try (MockedStatic<StringToObjectConverter> mocked = mockStatic(StringToObjectConverter.class)) {
            mocked.when(() -> StringToObjectConverter.convertArg(setter, "valid", Object.class)).thenReturn(new Object());
            // make the spy throw when the protected helper is called
            doThrow(new IllegalArgumentException("adder failure")).when(setter).invokeMethodWithSingleParameterOnThisObject(argThat((Method m) -> m.getName().equals("addFailingAdder")), eq("valid"));
            // the exception should propagate out of addBasicProperty
            assertThrows(IllegalArgumentException.class, () -> setter.addBasicProperty("failingAdder", "valid"), "Exception from adder invocation must propagate");
        }
    }
}
