package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.api.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getRelevantMethod_Test {

    private static BeanDescriptionCache beanDescriptionCache;

    private PropertySetter setter;

    private TestBean bean;

    /**
     * Simple bean used only for the purpose of these tests.
     */
    static class TestBean {

        // used for AS_COMPLEX_PROPERTY_COLLECTION
        public void addDoor(String door) {
            /* no‑op */
        }

        // used for AS_COMPLEX_PROPERTY
        public void setWindow(String window) {
            /* no‑op */
        }

        // a method that should *not* be selected for the above aggregation types
        public void setName(String name) {
            /* no‑op */
        }

        // extra methods for delegation tests
        public void addCustom(String custom) {
            /* no‑op */
        }

        public void setCustom(String custom) {
            /* no‑op */
        }
    }

    /**
     * Subclass that overrides the private lookup methods so we can verify delegation.
     */
    static class DelegatingPropertySetter extends PropertySetter {

        private final Method adderToReturn;

        private final Method setterToReturn;

        private final RuntimeException exceptionToThrowFromAdder;

        private final RuntimeException exceptionToThrowFromSetter;

        DelegatingPropertySetter(BeanDescriptionCache cache, Object bean, Method adderToReturn, Method setterToReturn) {
            super(cache, bean);
            this.adderToReturn = adderToReturn;
            this.setterToReturn = setterToReturn;
            this.exceptionToThrowFromAdder = null;
            this.exceptionToThrowFromSetter = null;
        }

        DelegatingPropertySetter(BeanDescriptionCache cache, Object bean, RuntimeException adderException, RuntimeException setterException, boolean dummy) {
            super(cache, bean);
            this.adderToReturn = null;
            this.setterToReturn = null;
            this.exceptionToThrowFromAdder = adderException;
            this.exceptionToThrowFromSetter = setterException;
        }

        @Override
        Method findAdderMethod(String name) {
            if (exceptionToThrowFromAdder != null) {
                throw exceptionToThrowFromAdder;
            }
            return adderToReturn;
        }

        @Override
        Method findSetterMethod(String name) {
            if (exceptionToThrowFromSetter != null) {
                throw exceptionToThrowFromSetter;
            }
            return setterToReturn;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        beanDescriptionCache = new BeanDescriptionCache(new ContextBase());
        bean = new TestBean();
        // The constructor used in the production code is (BeanDescriptionCache, Object)
        setter = new PropertySetter(beanDescriptionCache, bean);
    }

    @Test
    void getRelevantMethod_returnsAdderForComplexPropertyCollection() throws Exception {
        // GIVEN a property name that matches an "addXxx" method and the proper aggregation type
        String propertyName = "door";
        // WHEN
        Method method = setter.getRelevantMethod(propertyName, AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
        // THEN
        assertNotNull(method, "Method should not be null for a valid adder");
        assertEquals("addDoor", method.getName());
        // verify the parameter type matches the declared method signature
        assertEquals(1, method.getParameterCount());
        assertEquals(String.class, method.getParameterTypes()[0]);
    }

    @Test
    void getRelevantMethod_returnsSetterForComplexProperty() throws Exception {
        // GIVEN a property name that matches a "setXxx" method and the proper aggregation type
        String propertyName = "window";
        // WHEN
        Method method = setter.getRelevantMethod(propertyName, AggregationType.AS_COMPLEX_PROPERTY);
        // THEN
        assertNotNull(method, "Method should not be null for a valid setter");
        assertEquals("setWindow", method.getName());
        assertEquals(1, method.getParameterCount());
        assertEquals(String.class, method.getParameterTypes()[0]);
    }

    @Test
    void getRelevantMethod_throwsWhenAggregationTypeNotSupported() {
        // GIVEN an aggregation type that the method does not handle
        String propertyName = "name";
        // not allowed in getRelevantMethod
        AggregationType unsupported = AggregationType.AS_BASIC_PROPERTY;
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> setter.getRelevantMethod(propertyName, unsupported));
        assertTrue(ex.getMessage().contains(unsupported.name()));
    }

    @ParameterizedTest(name = "[{index}] name={0}, type={1}")
    @CsvSource({ "door, AS_COMPLEX_PROPERTY_COLLECTION", "window, AS_COMPLEX_PROPERTY" })
    void getRelevantMethod_validAggregationTypes(String propertyName, AggregationType type) throws Exception {
        // WHEN
        Method method = setter.getRelevantMethod(propertyName, type);
        // THEN
        assertNotNull(method, "Method should be returned for a valid combination");
        if (type == AggregationType.AS_COMPLEX_PROPERTY_COLLECTION) {
            assertTrue(method.getName().startsWith("add"), "Adder method name should start with 'add'");
        } else {
            assertTrue(method.getName().startsWith("set"), "Setter method name should start with 'set'");
        }
        assertEquals(1, method.getParameterCount(), "Returned method must have exactly one parameter");
    }

    @Test
    void getRelevantMethod_throwsWhenAggregationTypeIsNull() {
        // GIVEN a null aggregation type
        String propertyName = "bar";
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> setter.getRelevantMethod(propertyName, null));
        assertTrue(ex.getMessage().contains("null"));
    }

    @Test
    void getRelevantMethod_delegatesToOverriddenFindAdderMethod() throws Exception {
        // Prepare a known Method object to be returned by the overridden findAdderMethod
        Method expected = TestBean.class.getMethod("addCustom", String.class);
        DelegatingPropertySetter delegatingSetter = new DelegatingPropertySetter(beanDescriptionCache, bean, expected, null);
        // WHEN
        Method actual = delegatingSetter.getRelevantMethod("custom", AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
        // THEN
        assertSame(expected, actual, "The method returned by getRelevantMethod must be exactly the one supplied by findAdderMethod");
    }

    @Test
    void getRelevantMethod_delegatesToOverriddenFindSetterMethod() throws Exception {
        // Prepare a known Method object to be returned by the overridden findSetterMethod
        Method expected = TestBean.class.getMethod("setCustom", String.class);
        DelegatingPropertySetter delegatingSetter = new DelegatingPropertySetter(beanDescriptionCache, bean, null, expected);
        // WHEN
        Method actual = delegatingSetter.getRelevantMethod("custom", AggregationType.AS_COMPLEX_PROPERTY);
        // THEN
        assertSame(expected, actual, "The method returned by getRelevantMethod must be exactly the one supplied by findSetterMethod");
    }

    @Test
    void getRelevantMethod_propagatesExceptionFromFindAdderMethod() {
        RuntimeException adderException = new IllegalStateException("adder error");
        DelegatingPropertySetter delegatingSetter = new DelegatingPropertySetter(beanDescriptionCache, bean, adderException, null, true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> delegatingSetter.getRelevantMethod("dummy", AggregationType.AS_COMPLEX_PROPERTY_COLLECTION));
        assertEquals("adder error", ex.getMessage());
    }

    @Test
    void getRelevantMethod_propagatesExceptionFromFindSetterMethod() {
        RuntimeException setterException = new IllegalStateException("setter error");
        DelegatingPropertySetter delegatingSetter = new DelegatingPropertySetter(beanDescriptionCache, bean, null, setterException, true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> delegatingSetter.getRelevantMethod("dummy", AggregationType.AS_COMPLEX_PROPERTY));
        assertEquals("setter error", ex.getMessage());
    }

    @Test
    void getRelevantMethod_nullPropertyName_delegatesToAdder_andPropagatesException() {
        // The default implementation of findAdderMethod will try to locate a method
        // named "add" + capitalized name; passing null should cause a NullPointerException.
        assertThrows(NullPointerException.class, () -> setter.getRelevantMethod(null, AggregationType.AS_COMPLEX_PROPERTY_COLLECTION));
    }

    @Test
    void getRelevantMethod_nullPropertyName_delegatesToSetter_andPropagatesException() {
        // Same expectation for the setter side.
        assertThrows(NullPointerException.class, () -> setter.getRelevantMethod(null, AggregationType.AS_COMPLEX_PROPERTY));
    }
}
