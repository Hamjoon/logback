package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.testUtil.StatusChecker;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Method;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_setProperty_Test {

    /**
     * Shared Logback context used by all tests.
     */
    private static Context context;

    /**
     * Helper that inspects the {@link StatusManager} of the context.
     */
    private static StatusChecker checker;

    /**
     * Bean description cache required by {@link PropertySetter}.
     */
    private BeanDescriptionCache beanDescriptionCache;

    /**
     * The object whose properties will be set by the {@code PropertySetter}.
     */
    private House house;

    /**
     * Instance under test.
     */
    private PropertySetter setter;

    class BadBean {

        public void setProblematic(String s) {
            throw new IllegalArgumentException("bad value");
        }
    }

    class BadBean {

        public void setProblematic(String s) {
            throw new IllegalArgumentException("bad value");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a single Context instance is sufficient for the whole suite
        context = new ContextBase();
        checker = new StatusChecker(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh objects for each test to avoid cross‑test pollution
        beanDescriptionCache = new BeanDescriptionCache(context);
        house = new House();
        setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        // clear any previous status entries
        StatusManager sm = context.getStatusManager();
        sm.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // release the static context – not strictly required but tidy
        context = null;
        checker = null;
    }

    /**
     * Uses reflection to replace the {@code aggregationAssessor} field of a
     * {@link PropertySetter} instance with a mock. This allows fine‑grained
     * control over the behaviour of {@code findSetterMethod}.
     */
    private static void setAggregationAssessor(PropertySetter target, AggregationAssessor mockAssessor) throws Exception {
        java.lang.reflect.Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(target, mockAssessor);
    }

    @Test
    void testSetPropertyWithValidSetters() {
        // basic numeric, boolean and string properties
        setter.setProperty("count", "10");
        setter.setProperty("temperature", "33.1");
        setter.setProperty("name", "jack");
        setter.setProperty("open", "true");
        assertEquals(10, house.getCount(), "count should be set to 10");
        assertEquals(33.1d, house.getTemperature(), 0.0001, "temperature should be set to 33.1");
        assertEquals("jack", house.getName(), "name should be set to 'jack'");
        assertTrue(house.isOpen(), "open flag should be true");
    }

    @Test
    void testSetPropertyIgnoresNullValue() {
        // the house starts with default values (null / 0 / false)
        // should be a no‑op
        setter.setProperty("name", null);
        // should be a no‑op
        setter.setProperty("count", null);
        // verify that nothing changed
        assertNull(house.getName(), "name must stay null when null value is supplied");
        assertEquals(0, house.getCount(), "count must stay 0 when null value is supplied");
        // also ensure no warning was emitted
        StatusManager sm = context.getStatusManager();
        assertEquals(0, sm.getCount(), "No status messages should be produced for null values");
    }

    @Test
    void testSetPropertyWhenNoSetterExistsProducesWarning() {
        // property "nonExisting" does not have a corresponding setter in House
        setter.setProperty("nonExisting", "someValue");
        // the warning is stored in the context's StatusManager
        StatusManager sm = context.getStatusManager();
        assertTrue(sm.getCount() > 0, "A warning status should have been added");
        // verify that the warning message contains the expected text
        boolean found = false;
        for (Status s : sm.getCopyOfStatusList()) {
            if (s.getMessage().contains("No setter for property [nonExisting]")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected warning about missing setter was not found");
    }

    @Test
    void testSetPropertyWhenSetterThrowsPropertySetterException() {
        // The House class contains a setter that deliberately throws an exception
        // when the supplied value cannot be parsed (e.g. setting a file size with
        // an invalid unit). We use that behaviour to trigger the catch block.
        // The method under test should log a warning but not propagate the exception.
        // "fs" is a FileSize property
        setter.setProperty("fs", "invalid size");
        // The house's FileSize should remain null (setter failed)
        assertNull(house.getFs(), "FileSize should stay null after a failed conversion");
        // A warning must have been recorded
        StatusManager sm = context.getStatusManager();
        assertTrue(sm.getCount() > 0, "A warning should be logged when PropertySetterException occurs");
        boolean contains = sm.getCopyOfStatusList().stream().anyMatch(s -> s.getMessage().contains("Failed to set property [fs]"));
        assertTrue(contains, "Expected warning about failed property setting was not found");
    }

    /**
     * Group: Null value handling
     *
     * When the supplied value is {@code null} the method must return immediately,
     * without invoking {@code aggregationAssessor.findSetterMethod} and without
     * logging any warning.
     */
    @Test
    void testNullValueCausesEarlyReturn(@Mock AggregationAssessor mockAssessor) throws Exception {
        // inject the mock assessor
        setAggregationAssessor(setter, mockAssessor);
        // invoke with a non‑null property name but a null value
        setter.setProperty("anyProperty", null);
        // verify that the assessor was never consulted
        verifyNoInteractions(mockAssessor);
        // no status messages should have been added
        assertEquals(0, context.getStatusManager().getCount(), "No warning should be emitted for a null value");
    }

    /**
     * Group: No setter found
     *
     * The assessor returns {@code null}. The method must log a warning that
     * mentions the missing setter and must not attempt to call the private
     * {@code setProperty(Method,String)} overload.
     */
    @Test
    void testNoSetterFoundLogsWarning(@Mock AggregationAssessor mockAssessor) throws Exception {
        when(mockAssessor.findSetterMethod("nonExistentProperty")).thenReturn(null);
        setAggregationAssessor(setter, mockAssessor);
        setter.setProperty("nonExistentProperty", "someValue");
        // verify that a warning was added
        StatusManager sm = context.getStatusManager();
        assertTrue(sm.getCount() > 0, "A warning status should have been added");
        // ensure the warning text contains the expected fragment
        boolean found = sm.getCopyOfStatusList().stream().anyMatch(s -> s.getMessage().contains("No setter for property [nonExistentProperty]"));
        assertTrue(found, "Expected warning about missing setter was not found");
    }

    /**
     * Group: Successful property set
     *
     * The assessor finds a valid setter; the conversion succeeds and the
     * underlying object's property is updated. No warning should be emitted.
     */
    @Test
    void testSuccessfulPropertySet(@Mock AggregationAssessor mockAssessor) throws Exception {
        // Use the real setter method from House: setCount(int)
        Method countSetter = House.class.getMethod("setCount", int.class);
        when(mockAssessor.findSetterMethod("count")).thenReturn(countSetter);
        setAggregationAssessor(setter, mockAssessor);
        setter.setProperty("count", "42");
        assertEquals(42, house.getCount(), "The count property should be set to 42");
        assertEquals(0, context.getStatusManager().getCount(), "No warning should be emitted on successful property setting");
    }

    /**
     * Group: Setter throws {@link PropertySetterException}
     *
     * The private {@code setProperty(Method,String)} throws a
     * {@link PropertySetterException} (simulated by providing a value that cannot
     * be converted to the required type). The public method must catch the
     * exception, log a warning and continue without propagating the exception.
     */
    @Test
    void testConversionFailureLeadsToWarning(@Mock AggregationAssessor mockAssessor) throws Exception {
        // House#setCount expects an int – passing a non‑numeric string triggers
        // PropertySetterException inside the private helper.
        Method countSetter = House.class.getMethod("setCount", int.class);
        when(mockAssessor.findSetterMethod("count")).thenReturn(countSetter);
        setAggregationAssessor(setter, mockAssessor);
        setter.setProperty("count", "notANumber");
        // value must remain unchanged (default 0)
        assertEquals(0, house.getCount(), "Count should stay at default after conversion failure");
        // a warning containing the property name and the offending value must be present
        StatusManager sm = context.getStatusManager();
        assertTrue(sm.getCount() > 0, "A warning should be logged when conversion fails");
        boolean contains = sm.getCopyOfStatusList().stream().anyMatch(s -> s.getMessage().contains("Failed to set property [count]"));
        assertTrue(contains, "Warning must mention the failed property");
    }

    /**
     * Group: Empty string value (non‑null)
     *
     * An empty string is a legitimate value; the method must locate the setter
     * and invoke it. For a String‑typed property the empty string should be set.
     */
    @Test
    void testEmptyStringIsHandledLikeAnyOtherValue() {
        // House#setName(String)
        setter.setProperty("name", "");
        assertEquals("", house.getName(), "The name property should be set to an empty string");
        assertEquals(0, context.getStatusManager().getCount(), "No warning should be emitted for a successful empty‑string set");
    }

    /**
     * Group: Null property name
     *
     * Passing {@code null} as the property name should result in a
     * {@link NullPointerException} (or any unchecked exception) because the
     * assessor receives a null argument.
     */
    @Test
    void testNullPropertyNameThrowsException() {
        assertThrows(NullPointerException.class, () -> setter.setProperty(null, "anyValue"), "A null property name should cause a NullPointerException");
    }

    /**
     * Group: AggregationAssessor throws unchecked exception
     *
     * The assessor is mocked to throw an {@link IllegalStateException}. The
     * public {@code setProperty} method must not catch it, allowing it to
     * propagate to the caller.
     */
    @Test
    void testUncheckedExceptionFromAssessorPropagates(@Mock AggregationAssessor mockAssessor) throws Exception {
        when(mockAssessor.findSetterMethod("someProperty")).thenThrow(new IllegalStateException("boom"));
        setAggregationAssessor(setter, mockAssessor);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> setter.setProperty("someProperty", "someValue"));
        assertEquals("boom", ex.getMessage(), "The original unchecked exception should be propagated unchanged");
    }

    /**
     * Group: setProperty overload throws unchecked exception
     *
     * The private {@code setProperty(Method,String)} overload is forced to throw
     * an {@link IllegalArgumentException}. The public method must let this
     * exception escape because it only catches {@link PropertySetterException}.
     *
     * We achieve this by using a custom bean whose setter deliberately throws
     * IllegalArgumentException.
     */
    @Test
    void testUncheckedExceptionFromSetterPropagates() {
        // custom bean with a setter that throws IllegalArgumentException
        class BadBean {

            public void setProblematic(String s) {
                throw new IllegalArgumentException("bad value");
            }
        }
        BadBean badBean = new BadBean();
        PropertySetter badSetter = new PropertySetter(beanDescriptionCache, badBean);
        badSetter.setContext(context);
        // No mocking – we rely on the real AggregationAssessor inside PropertySetter
        // which will locate the setter via reflection.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> badSetter.setProperty("problematic", "any"));
        assertEquals("bad value", ex.getMessage(), "The IllegalArgumentException from the bean's setter must propagate");
    }
}
