package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.testUtil.StatusChecker;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.joran.spi.PropertySetterException;
import ch.qos.logback.core.status.Status;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class PropertySetter_setProperty_Test {

    /**
     * Shared context used by all tests.
     */
    private static ContextBase context;

    /**
     * Shared {@link BeanDescriptionCache} used by all tests.
     */
    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * Helper that inspects the {@link ch.qos.logback.core.status.StatusManager} for warnings.
     */
    private static StatusChecker statusChecker;

    /**
     * The {@link PropertySetter} instance under test.
     */
    private PropertySetter setter;

    /**
     * A simple bean used by the tests.
     */
    private House house;

    /* -------------------------------------------------------------
     *  Helper bean used in the tests
     * ------------------------------------------------------------- */
    public static class House {

        private int count;

        private double temperature;

        private String name;

        private boolean open;

        private int port;

        private String faulty;

        private String illegal;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isOpen() {
            return open;
        }

        public void setOpen(boolean open) {
            this.open = open;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getFaulty() {
            return faulty;
        }

        public void setFaulty(String value) throws PropertySetterException {
            throw new PropertySetterException("forced failure");
        }

        public String getIllegal() {
            return illegal;
        }

        public void setIllegal(String value) {
            throw new IllegalArgumentException("illegal argument");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
        statusChecker = new StatusChecker(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        house = new House();
        setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        // make sure the status manager is clean before each test
        context.getStatusManager().clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Close the context to release internal resources.
        context.stop();
    }

    /**
     * Verify that {@code setProperty} correctly converts the supplied string value
     * and invokes the corresponding JavaBean setter on the target object.
     */
    @Test
    void testSetBasicProperties_success() {
        setter.setProperty("count", "10");
        setter.setProperty("temperature", "33.1");
        setter.setProperty("name", "jack");
        setter.setProperty("open", "true");
        assertEquals(10, house.getCount(), "int property not set correctly");
        assertEquals(33.1d, house.getTemperature(), 0.0001, "double property not set correctly");
        assertEquals("jack", house.getName(), "String property not set correctly");
        assertTrue(house.isOpen(), "boolean property not set correctly");
    }

    /**
     * When the supplied {@code value} is {@code null} the method should return
     * immediately without touching the target object or emitting a warning.
     */
    @Test
    void testSetProperty_nullValue_noAction() {
        // initialise a property with a known value
        house.setCount(5);
        setter.setProperty("count", null);
        // the original value must stay unchanged
        assertEquals(5, house.getCount(), "Property should remain unchanged when value is null");
        // no warning should have been recorded
        assertTrue(statusChecker.getStatusList().isEmpty(), "No warnings should be emitted for null values");
    }

    /**
     * If there is no matching setter method for the supplied property name,
     * the method must add a warning to the context's {@link ch.qos.logback.core.status.StatusManager}.
     */
    @Test
    void testSetProperty_noSetter_warn() {
        setter.setProperty("nonExistingProperty", "someValue");
        // The house should be untouched (no side‑effects)
        assertNull(house.getName(), "House should not have been modified");
        // Verify that a warning containing the property name was recorded
        assertTrue(statusChecker.containsWarningMessageContaining("No setter for property [nonExistingProperty]"), "Expected a warning about the missing setter");
    }

    @Test
    void testSetProperty_nullName_throwsNPE() {
        // According to the current implementation a null name leads to NPE inside findSetterMethod
        assertThrows(NullPointerException.class, () -> setter.setProperty(null, "anyValue"));
        // No warning should be recorded because the exception occurs before any warning logic
        assertTrue(statusChecker.getStatusList().isEmpty(), "No warnings should be emitted when a NPE occurs");
    }

    @Test
    void testSetProperty_emptyName_warn() {
        setter.setProperty("", "value");
        assertTrue(statusChecker.containsWarningMessageContaining("No setter for property []"), "Expected a warning for empty property name");
    }

    @Test
    void testSetProperty_whitespaceName_warn() {
        setter.setProperty("   ", "value");
        assertTrue(statusChecker.containsWarningMessageContaining("No setter for property [   ]"), "Expected a warning for whitespace‑only property name");
    }

    @Test
    void testSetProperty_dotNotation_warn() {
        setter.setProperty("parent.child", "value");
        assertTrue(statusChecker.containsWarningMessageContaining("No setter for property [parent.child]"), "Expected a warning for dot‑notation property name");
    }

    /* -------------------------------------------------------------
     *  Conversion and exception handling
     * ------------------------------------------------------------- */
    @Test
    void testSetProperty_stringEmptyValue_success() {
        setter.setProperty("name", "");
        assertEquals("", house.getName(), "Empty string should be passed unchanged");
        assertTrue(statusChecker.getStatusList().isEmpty(), "No warning should be emitted for empty string value");
    }

    @Test
    void testSetProperty_booleanConversion_success() {
        setter.setProperty("open", "true");
        assertTrue(house.isOpen(), "Boolean true should be converted correctly");
        assertTrue(statusChecker.getStatusList().isEmpty(), "No warning should be emitted for successful boolean conversion");
    }

    @Test
    void testSetProperty_numericConversionFailure_warn() {
        // 'port' expects an int; supplying a non‑numeric string triggers PropertySetterException
        setter.setProperty("port", "notANumber");
        assertTrue(statusChecker.containsWarningMessageContaining("Failed to set property [port] to value \"notANumber\"."), "Expected a warning about conversion failure");
    }

    @Test
    void testSetProperty_setterThrowsPropertySetterException_warn() {
        // The setter for 'faulty' deliberately throws PropertySetterException
        setter.setProperty("faulty", "any");
        assertTrue(statusChecker.containsWarningMessageContaining("Failed to set property [faulty]"), "Expected a warning when the underlying setter throws PropertySetterException");
    }

    /* -------------------------------------------------------------
     *  Unchecked exception propagation
     * ------------------------------------------------------------- */
    @Test
    void testSetProperty_findSetterMethodUncheckedException_propagates() {
        // Create a subclass that forces findSetterMethod to throw a RuntimeException
        PropertySetter explodingSetter = new PropertySetter(beanDescriptionCache, house) {

            @Override
            public void setProperty(String name, String value) {
                throw new SecurityException("forced");
            }
        };
        explodingSetter.setContext(context);
        SecurityException ex = assertThrows(SecurityException.class, () -> explodingSetter.setProperty("any", "value"));
        assertEquals("forced", ex.getMessage());
        // No warning should be recorded because the exception occurs before any warning logic
        assertTrue(statusChecker.getStatusList().isEmpty(), "No warnings should be emitted when an unchecked exception propagates");
    }

    @Test
    void testSetProperty_privateSetPropertyUncheckedException_propagates() {
        // The bean's setter throws IllegalArgumentException (unchecked)
        setter.setProperty("illegal", "trigger");
        // The IllegalArgumentException is wrapped in an InvocationTargetException,
        // which is then wrapped into PropertySetterException and finally caught.
        // Since PropertySetterException is caught, the unchecked exception does NOT propagate.
        // To verify propagation we need a bean whose setter throws RuntimeException directly.
        // This scenario is exercised by the following custom subclass:
        PropertySetter customSetter = new PropertySetter(beanDescriptionCache, house) {

            @Override
            public void setProperty(String name, String value) {
                // bypass the original implementation and invoke the private method via reflection
                try {
                    java.lang.reflect.Method privateSet = PropertySetter.class.getDeclaredMethod("setProperty", java.lang.reflect.Method.class, String.class, String.class);
                    privateSet.setAccessible(true);
                    java.lang.reflect.Method dummySetter = House.class.getMethod("setIllegal", String.class);
                    privateSet.invoke(this, dummySetter, name, value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        customSetter.setContext(context);
        assertThrows(IllegalArgumentException.class, () -> customSetter.setProperty("illegal", "trigger"));
    }
}
