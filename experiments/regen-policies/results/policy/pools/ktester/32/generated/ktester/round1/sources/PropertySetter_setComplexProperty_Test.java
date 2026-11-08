package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.testUtil.StatusChecker;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_setComplexProperty_Test {

    /**
     * Shared JUnit test context.
     */
    private static Context context;

    /**
     * Helper that inspects the {@link Context}'s status manager.
     */
    private StatusChecker statusChecker;

    /**
     * The bean on which the setter will act.
     */
    private ParentBean parentBean;

    /**
     * Instance under test.
     */
    private PropertySetter propertySetter;

    /**
     * Simple bean that contains a complex property named {@code child}.
     */
    public static class ParentBean {

        private ChildBean child;

        public void setChild(ChildBean child) {
            this.child = child;
        }

        public ChildBean getChild() {
            return child;
        }
    }

    /**
     * Simple bean used as the complex property value.
     */
    public static class ChildBean {

        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a single Context instance is enough for all tests
        context = new ContextBase();
    }

    @BeforeEach
    void setupBeforeEach() {
        // a fresh bean for every test method
        parentBean = new ParentBean();
        // create the PropertySetter that will operate on the bean (real behaviour)
        propertySetter = new PropertySetter(new BeanDescriptionCache(context), parentBean);
        propertySetter.setContext(context);
        // initialise a fresh StatusChecker to verify warnings / errors
        statusChecker = new StatusChecker(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear any status messages that may have been produced
        for (Status s : context.getStatusManager().getCopyOfStatusList()) {
            context.getStatusManager().remove(s);
        }
    }

    /**
     * Sets a (possibly final) field via reflection.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            var f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify that a matching setter method is invoked and the bean's state is updated.
     */
    @Test
    void setComplexProperty_successfulInvocation() {
        ChildBean child = new ChildBean();
        child.setValue("expected");
        // The property name must match the JavaBeans setter name (setChild)
        propertySetter.setComplexProperty("child", child);
        assertSame(child, parentBean.getChild(), "The child bean should have been set on the parent bean");
        assertEquals("expected", parentBean.getChild().getValue(), "The child bean's internal state must be preserved");
        // No warnings or errors should have been recorded
        statusChecker.assertIsWarningOrErrorFree();
    }

    /**
     * When there is no corresponding setter method, {@code setComplexProperty}
     * should emit a warning but otherwise leave the bean unchanged.
     */
    @Test
    void setComplexProperty_noSetterFoundEmitsWarning() {
        Object dummy = new Object();
        // Property name that does NOT correspond to any setter on ParentBean
        propertySetter.setComplexProperty("nonExisting", dummy);
        // Bean must stay untouched
        assertNull(parentBean.getChild(), "No setter should have been called, child must stay null");
        // Verify that a warning containing the property name was logged
        boolean warningFound = statusChecker.getStatusList().stream().anyMatch(s -> s.getLevel() == Status.WARN && s.getMessage().contains("nonExisting"));
        assertTrue(warningFound, "A warning about the missing setter should have been recorded");
    }

    /**
     * If the supplied value's type does not match the setter's parameter type,
     * the sanity check fails and the method returns without invoking the setter.
     */
    @Test
    void setComplexProperty_typeMismatchDoesNotInvokeSetter() {
        // The setter expects a {@link ChildBean}, we deliberately pass a plain String
        propertySetter.setComplexProperty("child", "I am not a ChildBean");
        // The bean must remain unchanged
        assertNull(parentBean.getChild(), "Setter should not have been invoked due to type mismatch");
        // A warning about the sanity check failure is expected
        boolean warningFound = statusChecker.getStatusList().stream().anyMatch(s -> s.getLevel() == Status.WARN && s.getMessage().contains("child"));
        assertTrue(warningFound, "A warning about the sanity check failure should have been recorded");
    }

    /**
     * Scenario: AggregationAssessor cannot locate a setter (returns null).
     * Expected: a warning is logged and the method returns early.
     */
    @Test
    void setComplexProperty_setterMethodNotFound_logsWarning() throws Exception {
        // Mock AggregationAssessor to return null for any property name
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findSetterMethod("nonExistentProperty")).thenReturn(null);
        // Build a PropertySetter and inject the mock assessor
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), new ParentBean());
        ps.setContext(context);
        setField(ps, "aggregationAssessor", mockAssessor);
        // Execute
        ps.setComplexProperty("nonExistentProperty", new Object());
        // Verify warning contains the expected text
        List<Status> statuses = statusChecker.getStatusList();
        assertTrue(statuses.stream().anyMatch(s -> s.getLevel() == Status.WARN && s.getMessage().contains("Not setter method for property [nonExistentProperty]")), "Expected warning about missing setter");
    }

    /**
     * Scenario: The internal sanity check returns {@code false}.
     * Expected: no warning, no error, and the setter is never invoked.
     */
    @Test
    void setComplexProperty_sanityCheckFails_noInvocation() throws Exception {
        // Real method for the setter we will pretend exists
        Method setter = ParentBean.class.getMethod("setChild", ChildBean.class);
        // Mock AggregationAssessor to return the setter method
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findSetterMethod("child")).thenReturn(setter);
        // Spy the PropertySetter to stub isSanityCheckSuccessful
        PropertySetter ps = spy(new PropertySetter(new BeanDescriptionCache(context), new ParentBean()));
        ps.setContext(context);
        setField(ps, "aggregationAssessor", mockAssessor);
        doReturn(false).when(ps).isSanityCheckSuccessful(eq("child"), eq(setter), any(), any());
        // Execute
        ps.setComplexProperty("child", new ChildBean());
        // Verify that invokeMethodWithSingleParameterOnThisObject was never called
        verify(ps, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
        // No warnings or errors should be present
        statusChecker.assertIsWarningOrErrorFree();
    }

    /**
     * Scenario: Successful property setting (setter found, sanity check passes).
     * Expected: invokeMethodWithSingleParameterOnThisObject is called exactly once,
     * and no warnings or errors are logged.
     */
    @Test
    void setComplexProperty_successfulInvocation_viaMock() throws Exception {
        Method setter = ParentBean.class.getMethod("setChild", ChildBean.class);
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findSetterMethod("child")).thenReturn(setter);
        PropertySetter ps = spy(new PropertySetter(new BeanDescriptionCache(context), new ParentBean()));
        ps.setContext(context);
        setField(ps, "aggregationAssessor", mockAssessor);
        doReturn(true).when(ps).isSanityCheckSuccessful(eq("child"), eq(setter), any(), any());
        ChildBean child = new ChildBean();
        child.setValue("ok");
        ps.setComplexProperty("child", child);
        // Verify that the reflective invocation helper was called once with the correct args
        verify(ps, times(1)).invokeMethodWithSingleParameterOnThisObject(eq(setter), eq(child));
        // No status messages should be recorded
        statusChecker.assertIsWarningOrErrorFree();
    }

    /**
     * Scenario: The reflective invocation throws a generic exception.
     * Expected: the exception is caught and logged as an error; no warning is emitted.
     */
    @Test
    void setComplexProperty_invocationThrowsException_logsError() throws Exception {
        Method setter = ParentBean.class.getMethod("setChild", ChildBean.class);
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findSetterMethod("child")).thenReturn(setter);
        PropertySetter ps = spy(new PropertySetter(new BeanDescriptionCache(context), new ParentBean()));
        ps.setContext(context);
        setField(ps, "aggregationAssessor", mockAssessor);
        doReturn(true).when(ps).isSanityCheckSuccessful(eq("child"), eq(setter), any(), any());
        // Force invokeMethodWithSingleParameterOnThisObject to throw an exception
        doThrow(new RuntimeException("boom")).when(ps).invokeMethodWithSingleParameterOnThisObject(eq(setter), any());
        ps.setComplexProperty("child", new ChildBean());
        // Verify that an error status containing the expected text was added
        List<Status> errors = statusChecker.getStatusList().stream().filter(s -> s.getLevel() == Status.ERROR).toList();
        assertFalse(errors.isEmpty(), "An error status should have been recorded");
        assertTrue(errors.get(0).getMessage().contains("Could not set component"), "Error message should contain the expected prefix");
    }

    /**
     * Scenario: Property name passed to {@code setComplexProperty} is {@code null}.
     * Expected: AggregationAssessor returns {@code null}, a warning is logged,
     * and the method returns without further action.
     */
    @Test
    void setComplexProperty_nullPropertyName_logsWarning() throws Exception {
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findSetterMethod(null)).thenReturn(null);
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), new ParentBean());
        ps.setContext(context);
        setField(ps, "aggregationAssessor", mockAssessor);
        ps.setComplexProperty(null, new ChildBean());
        boolean warningFound = statusChecker.getStatusList().stream().anyMatch(s -> s.getLevel() == Status.WARN && s.getMessage().contains("null"));
        assertTrue(warningFound, "A warning about a null property name should have been recorded");
    }

    /**
     * Scenario: {@code aggregationAssessor} field is {@code null}.
     * Expected: a {@link NullPointerException} is thrown before any logging occurs.
     */
    @Test
    void setComplexProperty_nullAggregationAssessor_throwsNPE() throws Exception {
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), new ParentBean());
        ps.setContext(context);
        // Force the aggregationAssessor to be null
        setField(ps, "aggregationAssessor", null);
        assertThrows(NullPointerException.class, () -> ps.setComplexProperty("any", new Object()), "Calling setComplexProperty with a null aggregationAssessor should throw NPE");
    }

    /**
     * Scenario: The target object ({@code obj}) is {@code null}.
     * Expected: a {@link NullPointerException} is thrown while building the warning message.
     */
    @Test
    void setComplexProperty_nullTargetObject_throwsNPEInWarningPath() throws Exception {
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), new ParentBean());
        ps.setContext(context);
        // Set the internal obj field to null
        setField(ps, "obj", null);
        // Ensure aggregationAssessor returns null to trigger the warning path
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findSetterMethod("nonExisting")).thenReturn(null);
        setField(ps, "aggregationAssessor", mockAssessor);
        assertThrows(NullPointerException.class, () -> ps.setComplexProperty("nonExisting", new Object()), "A NPE should be thrown when obj is null while constructing the warning message");
    }

    /**
     * Scenario: {@code isSanityCheckSuccessful} throws an unchecked exception.
     * Expected: the exception propagates; no warning or error is logged.
     */
    @Test
    void setComplexProperty_isSanityCheckThrowsUnchecked_exceptionBubbles() throws Exception {
        Method setter = ParentBean.class.getMethod("setChild", ChildBean.class);
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findSetterMethod("child")).thenReturn(setter);
        PropertySetter ps = spy(new PropertySetter(new BeanDescriptionCache(context), new ParentBean()));
        ps.setContext(context);
        setField(ps, "aggregationAssessor", mockAssessor);
        doThrow(new IllegalStateException("sanity failure")).when(ps).isSanityCheckSuccessful(eq("child"), eq(setter), any(), any());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ps.setComplexProperty("child", new ChildBean()), "Unchecked exception from sanity check should propagate");
        assertEquals("sanity failure", ex.getMessage());
        // Ensure no status messages were recorded
        assertTrue(statusChecker.getStatusList().isEmpty(), "No warnings or errors should have been logged");
    }

    /**
     * Scenario: The reflective call throws an {@link InvocationTargetException}.
     * Expected: the exception is caught and logged as an error (no warning).
     */
    @Test
    void setComplexProperty_invocationTargetException_loggedAsError() throws Exception {
        Method setter = ParentBean.class.getMethod("setChild", ChildBean.class);
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findSetterMethod("child")).thenReturn(setter);
        PropertySetter ps = spy(new PropertySetter(new BeanDescriptionCache(context), new ParentBean()));
        ps.setContext(context);
        setField(ps, "aggregationAssessor", mockAssessor);
        doReturn(true).when(ps).isSanityCheckSuccessful(eq("child"), eq(setter), any(), any());
        // Simulate InvocationTargetException wrapping a cause
        InvocationTargetException ite = new InvocationTargetException(new IllegalArgumentException("bad argument"));
        doThrow(ite).when(ps).invokeMethodWithSingleParameterOnThisObject(eq(setter), any());
        ps.setComplexProperty("child", new ChildBean());
        // Verify an error status containing the wrapped cause message
        List<Status> errors = statusChecker.getStatusList().stream().filter(s -> s.getLevel() == Status.ERROR).toList();
        assertFalse(errors.isEmpty(), "An error status should have been recorded");
        assertTrue(errors.get(0).getMessage().contains("Could not set component"), "Error message should contain the expected prefix");
        assertTrue(errors.get(0).getThrowable().getCause() instanceof IllegalArgumentException, "The original cause should be attached to the status");
    }
}
