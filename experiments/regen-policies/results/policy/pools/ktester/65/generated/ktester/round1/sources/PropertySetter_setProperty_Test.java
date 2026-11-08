package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class PropertySetter_setProperty_Test {

    /**
     * Mocked {@link Context} – used only to satisfy the constructor of {@link BeanDescriptionCache}.
     */
    @Mock
    Context mockContext;

    /**
     * The {@link PropertySetter} under test – we spy on it to verify protected {@code addWarn} calls.
     */
    PropertySetter setter;

    /**
     * Fresh bean instance for each test.
     */
    TestBean bean;

    /**
     * Simple bean used for the happy‑path tests.
     */
    public static class TestBean {

        private String name;

        private int count;

        private boolean active;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isActive() {
            return active;
        }
        // No setter for "missing"
    }

    /**
     * Bean whose setter deliberately throws an unchecked exception.
     */
    public static class ThrowingBean {

        public void setFaulty(String value) {
            throw new IllegalArgumentException("illegal argument");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        bean = new TestBean();
        // BeanDescriptionCache needs a real (or mocked) Context; the mock is sufficient.
        BeanDescriptionCache cache = new BeanDescriptionCache(mockContext);
        // Create a real PropertySetter and then spy on it so we can verify calls to addWarn().
        PropertySetter realSetter = new PropertySetter(cache, bean);
        // The setter must have a context to be able to add status messages – we give it the mock.
        realSetter.setContext(mockContext);
        setter = spy(realSetter);
    }

    /* --------------------------------------------------------------
     *  Helper to inject a (mock or null) AggregationAssessor via reflection.
     * -------------------------------------------------------------- */
    private static void setAggregationAssessor(PropertySetter target, AggregationAssessor assessor) throws Exception {
        Field field = PropertySetter.class.getDeclaredField("aggregationAssessor");
        field.setAccessible(true);
        field.set(target, assessor);
    }

    @Test
    void testSetPropertySuccess() {
        // Simple successful property assignments.
        setter.setProperty("name", "bob");
        setter.setProperty("count", "42");
        setter.setProperty("active", "true");
        assertEquals("bob", bean.getName(), "Name should be set via reflection");
        assertEquals(42, bean.getCount(), "Count should be converted to int and set");
        assertTrue(bean.isActive(), "Active flag should be converted to boolean and set");
        // Verify that no warning was emitted.
        verify(setter, never()).addWarn(anyString());
    }

    @Test
    void testSetPropertyNullValueIsIgnored() {
        // The method should return early when the value is null – nothing should happen.
        setter.setProperty("name", null);
        assertNull(bean.getName(), "Bean property must stay null when value is null");
        // No warning should be logged.
        verify(setter, never()).addWarn(anyString());
    }

    @Test
    void testSetPropertyNoMatchingSetterLogsWarning() {
        // "missing" has no corresponding setter in TestBean.
        setter.setProperty("missing", "whatever");
        // Verify that a warning containing the property name was added.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(setter, times(1)).addWarn(captor.capture());
        String warning = captor.getValue();
        assertTrue(warning.contains("No setter for property [missing]"), "Warning should mention the missing setter");
    }

    @Test
    void testSetPropertyConversionFailureLogsWarningWithException() {
        // Pass a non‑numeric value to an int setter – this triggers a PropertySetterException.
        setter.setProperty("count", "not-a-number");
        // Verify that a warning containing the property name and the cause was logged.
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Throwable> excCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(setter, times(1)).addWarn(msgCaptor.capture(), excCaptor.capture());
        String warningMsg = msgCaptor.getValue();
        Throwable cause = excCaptor.getValue();
        assertTrue(warningMsg.contains("Failed to set property [count]"), "Warning should mention the failure to set the property");
        assertNotNull(cause, "The cause of the warning must be present");
        assertTrue(cause instanceof PropertySetterException, "The cause should be a PropertySetterException");
    }

    @Test
    void testSetPropertyUsesAggregationAssessorToFindSetter() throws Exception {
        // This test ensures that the internal AggregationAssessor is consulted.
        // We replace the real AggregationAssessor with a mock that returns a known setter.
        // Because AggregationAssessor is created inside the constructor and is final,
        // we cannot inject a mock directly. Instead we verify the behaviour indirectly
        // by checking that a setter *is* found for a valid property name.
        Method setterMethod = TestBean.class.getMethod("setName", String.class);
        // The real AggregationAssessor should locate the same method.
        Method found = setter.aggregationAssessor.findSetterMethod("name");
        assertEquals(setterMethod, found, "AggregationAssessor should locate the correct setter method");
    }

    @Test
    void testNullValueHandling_NoInteractionWithAggregationAssessor() throws Exception {
        // Replace the real AggregationAssessor with a mock to verify it is never called.
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        setAggregationAssessor(setter, mockAssessor);
        setter.setProperty("anyProperty", null);
        // No warning and no call to findSetterMethod.
        verify(setter, never()).addWarn(anyString());
        verify(mockAssessor, never()).findSetterMethod(anyString());
    }

    @Test
    void testNoSetterFound_PropertyNameIsNull_LogsWarning() throws Exception {
        // Mock AggregationAssessor to return null when name is null.
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findSetterMethod(isNull())).thenReturn(null);
        setAggregationAssessor(setter, mockAssessor);
        setter.setProperty(null, "anyValue");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(setter, times(1)).addWarn(captor.capture());
        String warning = captor.getValue();
        assertTrue(warning.contains("No setter for property [null]"));
    }

    @Test
    void testAggregationAssessorIsNull_ThrowsNPE() throws Exception {
        // Create a setter and then nullify its aggregationAssessor field.
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(mockContext), new Object());
        ps.setContext(mockContext);
        // force NPE later
        setAggregationAssessor(ps, null);
        // The NPE should be thrown before any warning is added.
        assertThrows(NullPointerException.class, () -> ps.setProperty("someProperty", "anyValue"));
    }

    @Test
    void testUncheckedExceptionFromSetter_IsPropagated() {
        // Use a bean whose setter throws IllegalArgumentException.
        ThrowingBean throwingBean = new ThrowingBean();
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(mockContext), throwingBean);
        ps.setContext(mockContext);
        PropertySetter spyPs = spy(ps);
        // No need to mock AggregationAssessor – the real one will find the setter.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> spyPs.setProperty("faulty", "triggerException"));
        assertEquals("illegal argument", ex.getMessage());
        // The warning method should never be invoked because the catch block only handles PropertySetterException.
        verify(spyPs, never()).addWarn(anyString());
    }
}
