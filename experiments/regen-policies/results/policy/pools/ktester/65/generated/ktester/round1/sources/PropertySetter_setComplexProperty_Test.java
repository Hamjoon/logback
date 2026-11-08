package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.testUtil.StatusChecker;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.status.StatusChecker;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class PropertySetter_setComplexProperty_Test {

    private ContextBase context;

    private StatusChecker checker;

    private BeanDescriptionCache beanDescriptionCache;

    /**
     * Simple bean with a proper complex‑property setter.
     */
    public static class House {

        private Door door;

        // used for type‑mismatch test
        private Window window;

        // used for exception‑throwing test
        private FaultyDoor faultyDoor;

        public void setDoor(Door door) {
            this.door = door;
        }

        public void setWindow(Window window) {
            this.window = window;
        }

        public void setFaultyDoor(FaultyDoor faultyDoor) {
            // simulate a runtime problem inside the setter
            throw new IllegalStateException("setter failure");
        }

        public Door getDoor() {
            return door;
        }

        public Window getWindow() {
            return window;
        }

        public FaultyDoor getFaultyDoor() {
            return faultyDoor;
        }
    }

    /**
     * Simple complex property class.
     */
    public static class Door {

        private final String name = "front";

        public String getName() {
            return name;
        }
    }

    /**
     * Another complex property class – used to provoke a type mismatch.
     */
    public static class Window {
    }

    /**
     * Complex property whose setter deliberately throws an exception.
     */
    public static class FaultyDoor {
    }

    /**
     * Bean with a normal public setter for an object reference.
     */
    public static class ObjectPropertyBean {

        private Object value;

        public void setObject(Object value) {
            this.value = value;
        }

        public Object getObject() {
            return value;
        }
    }

    /**
     * Bean with a primitive‑type setter (int).
     */
    public static class PrimitivePropertyBean {

        private int number;

        public void setInt(int number) {
            this.number = number;
        }

        public int getInt() {
            return number;
        }
    }

    /**
     * Bean with a private setter – reflective invocation will fail.
     */
    public static class PrivateSetterBean {

        private Object secret;

        @SuppressWarnings("unused")
        private void setSecret(Object secret) {
            this.secret = secret;
        }

        public Object getSecret() {
            return secret;
        }
    }

    /**
     * Bean whose setter takes no arguments – invoking it with an argument
     *  triggers an {@link IllegalArgumentException}.
     */
    public static class ZeroParamBean {

        private boolean called = false;

        public void setZero() {
            called = true;
        }

        public boolean isCalled() {
            return called;
        }
    }

    /**
     * Bean with a String setter – used to provoke a type‑mismatch at
     *  invocation time.
     */
    public static class StringPropertyBean {

        private String text;

        public void setString(String text) {
            this.text = text;
        }

        public String getString() {
            return text;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        context = new ContextBase();
        checker = new StatusChecker(context);
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure that tests which are not supposed to generate warnings/errors
        // indeed leave the status manager clean.
        checker.assertIsWarningOrErrorFree();
    }

    /**
     * Returns the first {@link Status} recorded in the {@link StatusManager}
     * whose level matches the supplied {@code level}.  If none is found,
     * {@code null} is returned.
     */
    private Status findFirstStatusOfLevel(int level) {
        StatusManager sm = context.getStatusManager();
        for (Status s : sm.getCopyOfStatusList()) {
            if (s.getLevel() == level) {
                return s;
            }
        }
        return null;
    }

    /**
     * Helper that injects a mock {@link AggregationAssessor} into a {@link PropertySetter}
     * instance via reflection.
     */
    private void injectAggregationAssessor(PropertySetter setter, AggregationAssessor mockAssessor) throws Exception {
        Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(setter, mockAssessor);
    }

    @Test
    void successfulComplexPropertySetting() {
        // GIVEN
        House house = new House();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        Door door = new Door();
        // WHEN
        setter.setComplexProperty("door", door);
        // THEN
        assertNotNull(house.getDoor(), "Door should have been set on the house");
        assertSame(door, house.getDoor(), "The exact Door instance should be stored");
    }

    @Test
    void missingSetterProducesWarning() {
        // GIVEN
        House house = new House();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        Door door = new Door();
        // WHEN
        setter.setComplexProperty("nonExistingProperty", door);
        // THEN
        Status status = findFirstStatusOfLevel(Status.WARN);
        assertNotNull(status, "A warning status should have been recorded");
        assertTrue(status.getMessage().contains("Not setter method for property [nonExistingProperty]"), "Warning message should mention the missing setter");
    }

    @Test
    void typeMismatchProducesWarning() {
        // GIVEN
        House house = new House();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        // The setter expects a Door, we deliberately pass a Window.
        Window window = new Window();
        // WHEN
        setter.setComplexProperty("door", window);
        // THEN
        Status status = findFirstStatusOfLevel(Status.WARN);
        assertNotNull(status, "A warning status should have been recorded for type mismatch");
        assertTrue(status.getMessage().contains("setter method for property [door]"), "Warning should reference the property name");
    }

    @Test
    void exceptionDuringInvocationProducesError() {
        // GIVEN
        House house = new House();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        FaultyDoor faultyDoor = new FaultyDoor();
        // WHEN
        setter.setComplexProperty("faultyDoor", faultyDoor);
        // THEN
        Status status = findFirstStatusOfLevel(Status.ERROR);
        assertNotNull(status, "An error status should have been recorded when the setter throws");
        assertTrue(status.getMessage().contains("Could not set component"), "Error message should indicate a failure to set the component");
        assertTrue(status.getThrowable() instanceof IllegalStateException, "The underlying throwable should be the one thrown by the setter");
    }

    @Test
    void nullPropertyNameProducesWarning() {
        // GIVEN
        House house = new House();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        Door door = new Door();
        // WHEN
        setter.setComplexProperty(null, door);
        // THEN
        Status status = findFirstStatusOfLevel(Status.WARN);
        assertNotNull(status, "A warning should be logged when property name is null");
        assertTrue(status.getMessage().contains("Not setter method for property [null]"));
    }

    @Test
    void emptyPropertyNameProducesWarning() {
        // GIVEN
        House house = new House();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        Door door = new Door();
        // WHEN
        setter.setComplexProperty("", door);
        // THEN
        Status status = findFirstStatusOfLevel(Status.WARN);
        assertNotNull(status, "A warning should be logged when property name is empty");
        assertTrue(status.getMessage().contains("Not setter method for property []"));
    }

    @Test
    void nullComplexPropertyObjectSetterInvokesSetterWithNull() throws Exception {
        // GIVEN
        ObjectPropertyBean bean = new ObjectPropertyBean();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        // WHEN
        setter.setComplexProperty("object", null);
        // THEN
        assertNull(bean.getObject(), "Setter should have been invoked with a null argument");
    }

    @Test
    void nullComplexPropertyPrimitiveSetterLogsWarning() throws Exception {
        // GIVEN
        PrimitivePropertyBean bean = new PrimitivePropertyBean();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        // WHEN
        setter.setComplexProperty("int", null);
        // THEN
        Status status = findFirstStatusOfLevel(Status.WARN);
        assertNotNull(status, "A warning should be logged when null is supplied for a primitive setter");
        assertTrue(status.getMessage().contains("setter method for property [int]"));
    }

    @Test
    void privateSetterInvocationExceptionLoggedAsError() throws Exception {
        // GIVEN
        PrivateSetterBean bean = new PrivateSetterBean();
        PropertySetter realSetter = new PropertySetter(beanDescriptionCache, bean);
        realSetter.setContext(context);
        // Spy the setter so we can verify that the reflective call fails.
        PropertySetter spySetter = Mockito.spy(realSetter);
        // Force the private method to be invoked – the real implementation will
        // attempt to call the private method and raise IllegalAccessException.
        // No extra stubbing is required.
        // WHEN
        spySetter.setComplexProperty("secret", new Object());
        // THEN
        Status status = findFirstStatusOfLevel(Status.ERROR);
        assertNotNull(status, "An error should be logged when the private setter cannot be accessed");
        assertTrue(status.getMessage().contains("Could not set component"));
        assertTrue(status.getThrowable() instanceof IllegalAccessException, "Underlying throwable must be IllegalAccessException");
    }

    @Test
    void aggregationAssessorThrowsRuntimeExceptionPropagates() throws Exception {
        // GIVEN
        House house = new House();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findSetterMethod(anyString())).thenThrow(new RuntimeException("boom"));
        injectAggregationAssessor(setter, mockAssessor);
        // WHEN / THEN
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> setter.setComplexProperty("anyProperty", new Object()), "RuntimeException from AggregationAssessor should propagate");
        assertEquals("boom", thrown.getMessage());
    }

    @Test
    void zeroParameterSetterInvocationProducesError() throws Exception {
        // GIVEN
        ZeroParamBean bean = new ZeroParamBean();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        // WHEN
        setter.setComplexProperty("zero", new Object());
        // THEN
        Status status = findFirstStatusOfLevel(Status.ERROR);
        assertNotNull(status, "An error should be logged when a zero‑arg setter is invoked with an argument");
        assertTrue(status.getMessage().contains("Could not set component"));
        assertTrue(status.getThrowable() instanceof IllegalArgumentException, "Underlying throwable must be IllegalArgumentException");
        assertFalse(bean.isCalled(), "The zero‑arg setter must not have been executed");
    }

    @Test
    void mismatchedParameterTypeProducesError() throws Exception {
        // GIVEN
        StringPropertyBean bean = new StringPropertyBean();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        // WHEN
        // Integer cannot be assigned to String
        setter.setComplexProperty("string", 12345);
        // THEN
        Status status = findFirstStatusOfLevel(Status.ERROR);
        assertNotNull(status, "An error should be logged when the supplied type does not match the setter");
        assertTrue(status.getMessage().contains("Could not set component"));
        assertTrue(status.getThrowable() instanceof IllegalArgumentException, "Underlying throwable must be IllegalArgumentException");
    }
}
