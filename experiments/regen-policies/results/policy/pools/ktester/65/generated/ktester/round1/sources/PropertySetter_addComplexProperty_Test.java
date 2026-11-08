package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.testUtil.StatusChecker;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.status.Status;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Method;
import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_addComplexProperty_Test {

    private static Context context;

    private static StatusChecker statusChecker;

    private TestBean bean;

    private PropertySetter setter;

    // will be injected via reflection
    @Mock
    AggregationAssessor aggregationAssessorMock;

    // spy used for verification of internal calls
    private PropertySetter setterSpy;

    /**
     * Simple bean used as the target of {@link PropertySetter}.
     */
    static class TestBean {

        Object addedValue;

        // method that matches the naming convention expected by AggregationAssessor
        public void addDoor(Object o) {
            this.addedValue = o;
        }
        // no addWindow method – used to trigger the error path
    }

    /* -------------------------------------------------------------
     * Helper beans – each bean defines a single adder method that
     * matches (or deliberately mismatches) the expectations of the
     * {@link AggregationAssessor}.
     * ------------------------------------------------------------- */
    static class BeanWithObjectAdder {

        Object added;

        public void addValidName(Object o) {
            this.added = o;
        }
    }

    static class BeanWithStringAdder {

        String added;

        public void addValidName(String s) {
            this.added = s;
        }
    }

    static class BeanWithExceptionAdder {

        public void addValidName(Object o) {
            throw new IllegalArgumentException("bad argument");
        }
    }

    static class BeanWithNullAdder {

        Object added;

        public void addValidName(Object o) {
            // normal behaviour – used for null complexProperty test
            this.added = o;
        }
    }

    // but we expose a thin wrapper for readability.
    private static class StatusChecker {

        private final Context context;

        StatusChecker(Context ctx) {
            this.context = ctx;
        }

        void clear() {
            context.getStatusManager().clear();
        }

        List<Status> getStatusList() {
            return context.getStatusManager().getCopyOfStatusList();
        }

        boolean containsErrorMessage(String fragment) {
            return getStatusList().stream().filter(s -> s.getLevel() == Status.ERROR).anyMatch(s -> s.getMessage().contains(fragment));
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        context = new ContextBase();
        statusChecker = new StatusChecker(context);
        context = new ContextBase();
        statusChecker = new StatusChecker(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        bean = new TestBean();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        // clear any previous status entries before each test
        statusChecker.clear();
        // clear previous status entries
        statusChecker.clear();
    }

    /**
     * Utility: inject a mock {@link AggregationAssessor} into a {@link PropertySetter}
     * instance via reflection.
     */
    private void injectAggregationAssessor(PropertySetter ps) throws Exception {
        java.lang.reflect.Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(ps, aggregationAssessorMock);
    }

    @Test
    void addComplexProperty_usesAdderMethodWhenPresent() {
        // GIVEN a bean that defines an addDoor(Object) method
        // WHEN addComplexProperty is invoked with the matching name
        setter.addComplexProperty("door", "myDoor");
        // THEN the bean's addDoor method should have been called with the argument
        assertEquals("myDoor", bean.addedValue, "The addDoor method should have been invoked with the supplied argument");
        // AND no error should have been logged
        assertTrue(statusChecker.getStatusList().isEmpty(), "No error status should be recorded when the adder method exists");
    }

    @Test
    void addComplexProperty_logsErrorWhenAdderMethodMissing() {
        // GIVEN a bean that does NOT define an addWindow(Object) method
        // WHEN addComplexProperty is invoked with a name that cannot be resolved
        setter.addComplexProperty("window", "myWindow");
        // THEN an error status should be recorded containing the expected message fragment
        assertTrue(statusChecker.containsErrorMessage("Could not find method [addwindow]"), "An appropriate error message should be logged when the adder method cannot be found");
    }

    /* -------------------------------------------------------------
     * 1. Adder method NOT found – error should be logged
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_AdderMethodNotFound_logsError() throws Exception {
        // GIVEN a bean without any matching adder method
        BeanWithObjectAdder bean = new BeanWithObjectAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        injectAggregationAssessor(setter);
        // aggregationAssessor returns null → method not found
        when(aggregationAssessorMock.findAdderMethod("myProperty")).thenReturn(null);
        // WHEN
        setter.addComplexProperty("myProperty", new Object());
        // THEN an error containing the expected fragment is recorded
        assertTrue(statusChecker.containsErrorMessage("Could not find method [addmyProperty]"), "Expected error about missing adder method");
    }

    /* -------------------------------------------------------------
     * 2. Adder method found but sanity check fails – no invocation,
     *    no error logged.
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_AdderMethodFound_SanityCheckFails_noInvocation() throws Exception {
        // Bean expects a String, we pass an Integer → sanity check should fail
        BeanWithStringAdder bean = new BeanWithStringAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        injectAggregationAssessor(setter);
        Method adder = BeanWithStringAdder.class.getMethod("addValidName", String.class);
        when(aggregationAssessorMock.findAdderMethod("validName")).thenReturn(adder);
        // spy to verify that invokeMethodWithSingleParameterOnThisObject is NOT called
        setterSpy = Mockito.spy(setter);
        // re‑inject mock into the spy
        injectAggregationAssessor(setterSpy);
        // WHEN
        // Integer, not assignable to String
        setterSpy.addComplexProperty("validName", 12345);
        // THEN the adder method was never invoked
        verify(setterSpy, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
        // AND no error status should be recorded (sanity check fails silently)
        assertTrue(statusChecker.getStatusList().isEmpty(), "No error should be logged when sanity check fails");
    }

    /* -------------------------------------------------------------
     * 3. Adder method found & sanity check passes – method invoked once
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_AdderMethodFound_SanityCheckPasses_invokesAdder() throws Exception {
        BeanWithObjectAdder bean = new BeanWithObjectAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        injectAggregationAssessor(setter);
        Method adder = BeanWithObjectAdder.class.getMethod("addValidName", Object.class);
        when(aggregationAssessorMock.findAdderMethod("validName")).thenReturn(adder);
        // spy to verify internal invocation
        setterSpy = Mockito.spy(setter);
        injectAggregationAssessor(setterSpy);
        Object complex = new Object();
        // WHEN
        setterSpy.addComplexProperty("validName", complex);
        // THEN invokeMethodWithSingleParameterOnThisObject is called exactly once
        verify(setterSpy, times(1)).invokeMethodWithSingleParameterOnThisObject(eq(adder), eq(complex));
        // AND the bean's state reflects the call
        assertSame(complex, bean.added, "Bean should have received the complex property");
    }

    /* -------------------------------------------------------------
     * 4. Null name parameter – error logged, no further processing
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_NullNameParameter_logsError() throws Exception {
        BeanWithObjectAdder bean = new BeanWithObjectAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        injectAggregationAssessor(setter);
        // WHEN
        setter.addComplexProperty(null, new Object());
        // THEN an error about null property name is recorded
        assertTrue(statusChecker.containsErrorMessage("property name is null"), "Expected error for null property name");
    }

    /* -------------------------------------------------------------
     * 5. Empty name string – treated as missing adder method
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_EmptyNameString_logsError() throws Exception {
        BeanWithObjectAdder bean = new BeanWithObjectAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        injectAggregationAssessor(setter);
        when(aggregationAssessorMock.findAdderMethod("")).thenReturn(null);
        // WHEN
        setter.addComplexProperty("", new Object());
        // THEN error about missing method [add] is logged
        assertTrue(statusChecker.containsErrorMessage("Could not find method [add]"), "Expected error for empty property name");
    }

    /* -------------------------------------------------------------
     * 6. Null complex property – sanity passes, adder invoked with null
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_NullComplexProperty_SanityPasses_invokesAdderWithNull() throws Exception {
        BeanWithNullAdder bean = new BeanWithNullAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        injectAggregationAssessor(setter);
        Method adder = BeanWithNullAdder.class.getMethod("addValidName", Object.class);
        when(aggregationAssessorMock.findAdderMethod("validName")).thenReturn(adder);
        setterSpy = Mockito.spy(setter);
        injectAggregationAssessor(setterSpy);
        // WHEN
        setterSpy.addComplexProperty("validName", null);
        // THEN the internal reflective call receives null
        verify(setterSpy, times(1)).invokeMethodWithSingleParameterOnThisObject(eq(adder), isNull());
        // AND bean's field is set to null
        assertNull(bean.added, "Bean should have null as added value");
    }

    /* -------------------------------------------------------------
     * 7. Adder method throws RuntimeException – error logged
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_AdderMethodInvocationThrowsException_logsError() throws Exception {
        BeanWithExceptionAdder bean = new BeanWithExceptionAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        injectAggregationAssessor(setter);
        Method adder = BeanWithExceptionAdder.class.getMethod("addValidName", Object.class);
        when(aggregationAssessorMock.findAdderMethod("validName")).thenReturn(adder);
        // WHEN
        setter.addComplexProperty("validName", new Object());
        // THEN an error containing the exception message is recorded
        assertTrue(statusChecker.containsErrorMessage("bad argument"), "Expected error containing the exception message from adder");
    }

    /* -------------------------------------------------------------
     * 8. AggregationAssessor.findAdderMethod throws RuntimeException
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_AggregationAssessorFindAdderMethodThrowsRuntimeException_logsError() throws Exception {
        BeanWithObjectAdder bean = new BeanWithObjectAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        injectAggregationAssessor(setter);
        when(aggregationAssessorMock.findAdderMethod("validName")).thenThrow(new RuntimeException("finder failed"));
        // WHEN
        setter.addComplexProperty("validName", new Object());
        // THEN error containing the thrown message is logged
        assertTrue(statusChecker.containsErrorMessage("finder failed"), "Expected error about AggregationAssessor exception");
    }

    /* -------------------------------------------------------------
     * 9. Complex property wrong type – sanity fails, no invocation, no error
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_ComplexPropertyWrongType_SanityFails_noInvocation() throws Exception {
        // Bean expects String, we give Integer → sanity fails
        BeanWithStringAdder bean = new BeanWithStringAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        injectAggregationAssessor(setter);
        Method adder = BeanWithStringAdder.class.getMethod("addValidName", String.class);
        when(aggregationAssessorMock.findAdderMethod("validName")).thenReturn(adder);
        setterSpy = Mockito.spy(setter);
        injectAggregationAssessor(setterSpy);
        // WHEN
        setterSpy.addComplexProperty("validName", 12345);
        // THEN internal invoke method never called
        verify(setterSpy, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
        // AND no error status is produced
        assertTrue(statusChecker.getStatusList().isEmpty(), "No error should be logged when sanity check fails due to type mismatch");
    }

    /* -------------------------------------------------------------
     * 10. AggregationAssessor is null – NullPointerException expected
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_AggregationAssessorNull_throwsNPE() throws Exception {
        BeanWithObjectAdder bean = new BeanWithObjectAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        // construct normally, then nullify the field via reflection
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        // set aggregationAssessor to null
        java.lang.reflect.Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(setter, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> setter.addComplexProperty("validName", new Object()), "Expected NPE when aggregationAssessor is null");
    }

    /* -------------------------------------------------------------
     * 11. invokeMethodWithSingleParameterOnThisObject throws IllegalAccessException
     * ------------------------------------------------------------- */
    @Test
    void addComplexProperty_InvokeMethodThrowsIllegalAccessException_logsError() throws Exception {
        BeanWithObjectAdder bean = new BeanWithObjectAdder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, bean);
        setter.setContext(context);
        injectAggregationAssessor(setter);
        Method adder = BeanWithObjectAdder.class.getMethod("addValidName", Object.class);
        when(aggregationAssessorMock.findAdderMethod("validName")).thenReturn(adder);
        // spy and force the internal helper to throw IllegalAccessException
        setterSpy = Mockito.spy(setter);
        injectAggregationAssessor(setterSpy);
        doThrow(new IllegalAccessException("cannot access")).when(setterSpy).invokeMethodWithSingleParameterOnThisObject(eq(adder), any());
        // WHEN
        setterSpy.addComplexProperty("validName", new Object());
        // THEN an error containing the IllegalAccessException message is logged
        assertTrue(statusChecker.containsErrorMessage("cannot access"), "Expected error about IllegalAccessException");
    }
}
