package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.testUtil.StatusChecker;
import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.AggregationAssessor;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Method;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_addComplexProperty_Test {

    private static Context context;

    private static StatusChecker statusChecker;

    private BeanDescriptionCache beanDescriptionCache;

    private PropertySetter setter;

    private SimpleBean bean;

    @Mock
    private AggregationAssessor // mocked collaborator
    aggregationAssessor;

    /**
     * Simple bean with a single‑parameter public adder.
     */
    static class SimpleBean {

        private final List<Object> values = new ArrayList<>();

        public void addMyProp(Object o) {
            values.add(o);
        }

        public List<Object> getValues() {
            return values;
        }
    }

    /**
     * Bean whose adder expects two parameters (invalid for PropertySetter).
     */
    static class MultiParamBean {

        public void addMultiParam(String a, String b) {
            // never called in the tests
        }
    }

    /**
     * Bean whose adder expects a String – used to simulate a type‑mismatch.
     */
    static class TypeMismatchBean {

        public void addTypeMismatch(String s) {
            // never called in the tests
        }
    }

    /**
     * Bean whose adder throws a runtime exception when invoked.
     */
    static class ThrowsExceptionBean {

        public void addThrowsException(Object o) {
            throw new IllegalArgumentException("boom");
        }
    }

    /**
     * Bean with a private adder method – will cause IllegalAccessException.
     */
    static class PrivateAdderBean {

        @SuppressWarnings("unused")
        private void addPrivateAdder(Object o) {
            // never called – reflection will fail
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // shared Context for the whole test suite
        context = new ContextBase();
        statusChecker = new StatusChecker(context);
        context = new ContextBase();
        statusChecker = new StatusChecker(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh bean and setter for each test
        bean = new SimpleBean();
        beanDescriptionCache = new BeanDescriptionCache(context);
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        // in each test method as needed.
        beanDescriptionCache = new BeanDescriptionCache(context);
        // create a dummy bean – will be overwritten in each test
        setter = new PropertySetter(beanDescriptionCache, new SimpleBean());
        // inject the mocked AggregationAssessor via reflection
        java.lang.reflect.Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(setter, aggregationAssessor);
        // make sure the setter uses the shared Context for error logging
        setter.setContext(context);
        // clear any previous status entries
        statusChecker.clear();
    }

    @AfterEach
    void teardownAfterEach() {
        // reset the status checker so that errors from one test do not affect another
        statusChecker.clear();
        statusChecker.clear();
    }

    /* --------------------------------------------------------------------- */
    private static void injectAggregationAssessor(PropertySetter setter, AggregationAssessor mockAssessor) throws Exception {
        java.lang.reflect.Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(setter, mockAssessor);
    }

    @Test
    void addComplexProperty_usesAdderMethodWhenPresent() {
        // GIVEN a property name that matches an existing adder method (addDoor)
        String propertyName = "Door";
        String doorValue = "front";
        // WHEN addComplexProperty is invoked
        setter.addComplexProperty(propertyName, doorValue);
        // THEN the adder method should have been called and the bean state updated
        assertEquals(1, bean.getDoors().size(), "The door should have been added to the bean");
        assertEquals(doorValue, bean.getDoors().get(0), "The added door should match the supplied value");
        // AND no error should have been logged
        assertTrue(statusChecker.getStatusList().isEmpty(), "No error status should be present when the adder method exists");
    }

    @Test
    void addComplexProperty_logsErrorWhenNoAdderMethodFound() {
        // GIVEN a property name for which no adder method exists on SimpleBean
        String propertyName = "Window";
        Object dummyValue = new Object();
        // WHEN addComplexProperty is invoked
        setter.addComplexProperty(propertyName, dummyValue);
        // THEN the bean should remain unchanged
        assertTrue(bean.getDoors().isEmpty(), "Bean state must stay unchanged when no adder is found");
        // AND an error should have been recorded in the context's status manager
        assertFalse(statusChecker.getStatusList().isEmpty(), "An error status should be present when the adder method cannot be found");
        assertTrue(statusChecker.containsMessage("Could not find method [add" + propertyName + "]"), "Error message should mention the missing add method");
    }

    @Test
    @DisplayName("Happy path – adder method found, sanity check passes, method invoked")
    void addComplexProperty_happyPath() throws Exception {
        // GIVEN a bean with a public addMyProp(Object) method
        SimpleBean bean = new SimpleBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        injectAggregationAssessor(setter, aggregationAssessor);
        // the assessor returns the public method
        Method adder = SimpleBean.class.getMethod("addMyProp", Object.class);
        when(aggregationAssessor.findAdderMethod("myProp")).thenReturn(adder);
        // sanity check is forced to succeed
        PropertySetter spy = Mockito.spy(setter);
        doReturn(true).when(spy).isSanityCheckSuccessful(eq("myProp"), eq(adder), any(), any());
        // WHEN
        spy.addComplexProperty("myProp", "value");
        // THEN the adder was invoked exactly once
        verify(spy, times(1)).invokeMethodWithSingleParameterOnThisObject(eq(adder), eq("value"));
        // AND the bean state reflects the addition
        assertEquals(1, bean.getValues().size());
        assertEquals("value", bean.getValues().get(0));
        // AND no error was logged
        assertTrue(statusChecker.getStatusList().isEmpty(), "No error status should be present on the happy path");
    }

    @Test
    @DisplayName("Sanity check fails – method is discovered but not invoked")
    void addComplexProperty_sanityCheckFails() throws Exception {
        SimpleBean bean = new SimpleBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        injectAggregationAssessor(setter, aggregationAssessor);
        Method adder = SimpleBean.class.getMethod("addMyProp", Object.class);
        when(aggregationAssessor.findAdderMethod("myProp")).thenReturn(adder);
        PropertySetter spy = Mockito.spy(setter);
        doReturn(false).when(spy).isSanityCheckSuccessful(eq("myProp"), eq(adder), any(), any());
        spy.addComplexProperty("myProp", "value");
        // verify that the reflective invocation never happened
        verify(spy, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
        // bean must stay untouched
        assertTrue(bean.getValues().isEmpty());
        // no error should be logged (the method simply returns)
        assertTrue(statusChecker.getStatusList().isEmpty(), "Sanity‑check failure must not generate an error status");
    }

    @Test
    @DisplayName("No adder method found – error is logged")
    void addComplexProperty_noAdderMethod() {
        when(aggregationAssessor.findAdderMethod("unknown")).thenReturn(null);
        setter.addComplexProperty("unknown", new Object());
        // bean unchanged (the default bean is SimpleBean from setUp)
        SimpleBean bean = (SimpleBean) setter.getObj();
        assertTrue(bean.getValues().isEmpty());
        // an error status must be present and contain the expected message
        List<Status> statuses = statusChecker.getStatusList();
        assertFalse(statuses.isEmpty(), "An error status should be recorded");
        assertTrue(statusChecker.containsMessage("Could not find method [addunknown]"), "Error message must mention the missing add method");
    }

    @Test
    @DisplayName("Null property name – treated like missing method")
    void addComplexProperty_nullName() {
        when(aggregationAssessor.findAdderMethod(null)).thenReturn(null);
        setter.addComplexProperty(null, new Object());
        List<Status> statuses = statusChecker.getStatusList();
        assertFalse(statuses.isEmpty());
        assertTrue(statusChecker.containsMessage("Could not find method [addnull]"));
    }

    @Test
    @DisplayName("Null complexProperty with failing sanity check – no invocation, no error")
    void addComplexProperty_nullComplexProperty_sanityFails() throws Exception {
        SimpleBean bean = new SimpleBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        injectAggregationAssessor(setter, aggregationAssessor);
        Method adder = SimpleBean.class.getMethod("addMyProp", Object.class);
        when(aggregationAssessor.findAdderMethod("myProp")).thenReturn(adder);
        PropertySetter spy = Mockito.spy(setter);
        doReturn(false).when(spy).isSanityCheckSuccessful(eq("myProp"), eq(adder), any(), isNull());
        spy.addComplexProperty("myProp", null);
        verify(spy, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
        assertTrue(bean.getValues().isEmpty());
        assertTrue(statusChecker.getStatusList().isEmpty(), "Sanity‑check failure must not produce an error status");
    }

    @Test
    @DisplayName("Null complexProperty with passing sanity check – method invoked with null")
    void addComplexProperty_nullComplexProperty_sanityPasses() throws Exception {
        SimpleBean bean = new SimpleBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        injectAggregationAssessor(setter, aggregationAssessor);
        Method adder = SimpleBean.class.getMethod("addMyProp", Object.class);
        when(aggregationAssessor.findAdderMethod("myProp")).thenReturn(adder);
        PropertySetter spy = Mockito.spy(setter);
        doReturn(true).when(spy).isSanityCheckSuccessful(eq("myProp"), eq(adder), any(), isNull());
        spy.addComplexProperty("myProp", null);
        verify(spy, times(1)).invokeMethodWithSingleParameterOnThisObject(eq(adder), isNull());
        assertEquals(1, bean.getValues().size());
        assertNull(bean.getValues().get(0));
        assertTrue(statusChecker.getStatusList().isEmpty());
    }

    @Test
    @DisplayName("Adder method with multiple parameters – sanity check rejects")
    void addComplexProperty_adderMethodWithMultipleParameters() throws Exception {
        MultiParamBean bean = new MultiParamBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        injectAggregationAssessor(setter, aggregationAssessor);
        Method adder = MultiParamBean.class.getMethod("addMultiParam", String.class, String.class);
        when(aggregationAssessor.findAdderMethod("multiParam")).thenReturn(adder);
        PropertySetter spy = Mockito.spy(setter);
        // the real implementation would reject because param count != 1,
        // but we force the same behaviour via the stub
        doReturn(false).when(spy).isSanityCheckSuccessful(eq("multiParam"), eq(adder), any(), any());
        spy.addComplexProperty("multiParam", new Object());
        verify(spy, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
        assertTrue(statusChecker.getStatusList().isEmpty(), "Parameter‑count mismatch must not log an error, just skip");
    }

    @Test
    @DisplayName("Parameter type mismatch – sanity check fails")
    void addComplexProperty_parameterTypeMismatch() throws Exception {
        TypeMismatchBean bean = new TypeMismatchBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        injectAggregationAssessor(setter, aggregationAssessor);
        Method adder = TypeMismatchBean.class.getMethod("addTypeMismatch", String.class);
        when(aggregationAssessor.findAdderMethod("typeMismatch")).thenReturn(adder);
        PropertySetter spy = Mockito.spy(setter);
        doReturn(false).when(spy).isSanityCheckSuccessful(eq("typeMismatch"), eq(adder), any(), any());
        spy.addComplexProperty("typeMismatch", Integer.valueOf(5));
        verify(spy, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
        assertTrue(statusChecker.getStatusList().isEmpty(), "Type mismatch should not produce an error status, just abort");
    }

    @Test
    @DisplayName("Invocation throws exception – error is logged, exception not propagated")
    void addComplexProperty_invocationThrowsException() throws Exception {
        ThrowsExceptionBean bean = new ThrowsExceptionBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        injectAggregationAssessor(setter, aggregationAssessor);
        Method adder = ThrowsExceptionBean.class.getMethod("addThrowsException", Object.class);
        when(aggregationAssessor.findAdderMethod("throwsException")).thenReturn(adder);
        PropertySetter spy = Mockito.spy(setter);
        doReturn(true).when(spy).isSanityCheckSuccessful(eq("throwsException"), eq(adder), any(), any());
        // make the reflective invocation throw an IllegalArgumentException
        doThrow(new IllegalArgumentException("boom")).when(spy).invokeMethodWithSingleParameterOnThisObject(eq(adder), any());
        // method must not propagate the exception
        assertDoesNotThrow(() -> spy.addComplexProperty("throwsException", new Object()));
        // an error status containing the exception message must be recorded
        assertFalse(statusChecker.getStatusList().isEmpty(), "An error status should be logged when invocation fails");
        assertTrue(statusChecker.containsMessage("boom"));
    }

    @Test
    @DisplayName("Private adder method – IllegalAccessException is logged")
    void addComplexProperty_privateAdderMethod() throws Exception {
        PrivateAdderBean bean = new PrivateAdderBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        injectAggregationAssessor(setter, aggregationAssessor);
        Method adder = PrivateAdderBean.class.getDeclaredMethod("addPrivateAdder", Object.class);
        when(aggregationAssessor.findAdderMethod("privateAdder")).thenReturn(adder);
        PropertySetter spy = Mockito.spy(setter);
        doReturn(true).when(spy).isSanityCheckSuccessful(eq("privateAdder"), eq(adder), any(), any());
        // simulate IllegalAccessException from reflection
        doThrow(new IllegalAccessException("cannot access")).when(spy).invokeMethodWithSingleParameterOnThisObject(eq(adder), any());
        assertDoesNotThrow(() -> spy.addComplexProperty("privateAdder", new Object()));
        assertFalse(statusChecker.getStatusList().isEmpty(), "Error status must be logged for illegal‑access failures");
        assertTrue(statusChecker.containsMessage("cannot access"));
    }

    @Test
    @DisplayName("Null aggregationAssessor – NullPointerException is thrown")
    void addComplexProperty_nullAggregationAssessor() throws Exception {
        // create a setter with a real bean, then null out the assessor via reflection
        SimpleBean bean = new SimpleBean();
        PropertySetter localSetter = new PropertySetter(beanDescriptionCache, bean);
        // explicitly set the assessor field to null
        java.lang.reflect.Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(localSetter, null);
        localSetter.setContext(context);
        // NPE is expected when the method tries to dereference the null field
        assertThrows(NullPointerException.class, () -> localSetter.addComplexProperty("myProp", new Object()));
    }
}
