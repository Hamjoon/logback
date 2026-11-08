package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.testUtil.StatusChecker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.StringUtil;
import org.mockito.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_addBasicProperty_Test {

    private static Context context;

    private static StatusChecker statusChecker;

    private DoorHolder doorHolder;

    private PropertySetter setter;

    /**
     * Simple bean used for the “door” happy‑path tests.
     */
    static class DoorHolder {

        private final List<String> doors = new ArrayList<>();

        public void addDoor(String door) {
            doors.add(door);
        }

        public List<String> getDoors() {
            return doors;
        }
    }

    /**
     * Bean with an Integer adder – used for conversion tests.
     */
    static class IntegerHolder {

        private final List<Integer> ints = new ArrayList<>();

        public void addIntProperty(Integer i) {
            ints.add(i);
        }

        public List<Integer> getInts() {
            return ints;
        }
    }

    /**
     * Bean with a Boolean adder – used for boolean conversion tests.
     */
    static class BooleanHolder {

        private final List<Boolean> bools = new ArrayList<>();

        public void addBooleanProp(Boolean b) {
            bools.add(b);
        }

        public List<Boolean> getBools() {
            return bools;
        }
    }

    /**
     * Bean with a String adder – used for empty‑string test.
     */
    static class StringHolder {

        private final List<String> strings = new ArrayList<>();

        public void addStringProp(String s) {
            strings.add(s);
        }

        public List<String> getStrings() {
            return strings;
        }
    }

    /**
     * Bean with a multi‑parameter adder – used for sanity‑check failure.
     */
    static class MultiParamHolder {

        public void addMultiParam(String a, String b) {
            // never called
        }
    }

    /**
     * Bean with a zero‑parameter adder – used for sanity‑check failure.
     */
    static class NoParamHolder {

        public void addNoParam() {
            // never called
        }
    }

    /**
     * Bean with a private adder – used for IllegalAccessException test.
     */
    static class PrivateAdderHolder {

        @SuppressWarnings("unused")
        private void addPrivateAdder(String s) {
            // intentionally private
        }
    }

    class CustomHolder {

        public void addCustomObject(Object o) {
            // should never be called
        }
    }

    class CustomHolder {

        public void addCustomObject(Object o) {
            // should never be called
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a shared Context for the whole test suite
        context = new ContextBase();
        statusChecker = new StatusChecker(context);
        context = new ContextBase();
        statusChecker = new StatusChecker(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh bean and PropertySetter for each test
        doorHolder = new DoorHolder();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, doorHolder);
        setter.setContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear any status messages that may have been added during a test
        statusChecker.clear();
        statusChecker.clear();
    }

    private static void injectAggregationAssessor(PropertySetter setter, AggregationAssessor mockAssessor) throws Exception {
        Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(setter, mockAssessor);
    }

    @Test
    void addBasicPropertyAddsElementToCollection() {
        // Act
        setter.addBasicProperty("door", "front");
        // Assert – the adder method should have been invoked once
        List<String> doors = doorHolder.getDoors();
        assertEquals(1, doors.size(), "Exactly one door should have been added");
        assertEquals("front", doors.get(0));
    }

    @Test
    void addBasicPropertyIgnoresNullValue() {
        // Pre‑condition: collection is empty
        assertTrue(doorHolder.getDoors().isEmpty());
        // Act – pass a null value
        setter.addBasicProperty("door", null);
        // Assert – collection must stay unchanged
        assertTrue(doorHolder.getDoors().isEmpty(), "Null value must not trigger the adder method");
    }

    @Test
    void addBasicPropertyReportsErrorWhenNoAdderFound() {
        // The bean does NOT have an adder for “window”, so an error should be logged.
        setter.addBasicProperty("window", "kitchen");
        // Verify that an error status entry was created.
        assertTrue(statusChecker.containsErrorMessage("No adder for property [Window]"), "Expected an error about missing adder for property 'Window'");
    }

    @Test
    @DisplayName("Null value handling – method returns early")
    void nullValueHandling() throws Exception {
        // Arrange – any bean works, we use DoorHolder
        DoorHolder bean = new DoorHolder();
        PropertySetter setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        // Act
        setter.addBasicProperty("anyProperty", null);
        // Assert – no door added and no error logged
        assertTrue(bean.getDoors().isEmpty(), "No adder should be invoked");
        assertTrue(statusChecker.getStatusList().stream().noneMatch(s -> s.getLevel() == Status.ERROR), "No error should be logged");
    }

    @Test
    @DisplayName("Missing adder method – error is logged")
    void missingAdderMethod() throws Exception {
        DoorHolder bean = new DoorHolder();
        PropertySetter setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        // Act – property name that has no corresponding adder
        setter.addBasicProperty("window", "kitchen");
        // Assert – error message about missing adder
        assertTrue(statusChecker.containsErrorMessage("No adder for property [Window]"), "Expected missing‑adder error");
        assertTrue(bean.getDoors().isEmpty(), "No adder should have been called");
    }

    @Test
    @DisplayName("Conversion failure – integer parsing error is reported")
    void conversionFailure() throws Exception {
        IntegerHolder bean = new IntegerHolder();
        PropertySetter setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        // Act – non‑numeric string for an Integer adder
        setter.addBasicProperty("intProperty", "notANumber");
        // Assert – conversion error logged, collection unchanged
        assertTrue(statusChecker.containsErrorMessage("Conversion to type [class java.lang.Integer] failed."), "Expected conversion‑failure error");
        assertTrue(bean.getInts().isEmpty(), "Adder must not be invoked on conversion failure");
    }

    @Test
    @DisplayName("Conversion returns null – adder is not invoked")
    void conversionReturnsNull() throws Exception {
        // We simulate a null‑return by mocking the static converter via a spy on
        // PropertySetter and stubbing the private helper that performs conversion.
        // Since the real converter is static and not easily mockable, we use a
        // custom bean whose adder expects a type that the converter cannot handle
        // and therefore returns null (e.g., a custom class with no converter).
        class CustomHolder {

            public void addCustomObject(Object o) {
                // should never be called
            }
        }
        CustomHolder bean = new CustomHolder();
        PropertySetter real = new PropertySetter(new BeanDescriptionCache(context), bean);
        real.setContext(context);
        PropertySetter setter = spy(real);
        // Mock AggregationAssessor to return the correct adder method
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = CustomHolder.class.getMethod("addCustomObject", Object.class);
        when(mockAssessor.findAdderMethod("CustomObject")).thenReturn(adder);
        injectAggregationAssessor(setter, mockAssessor);
        // Stub the conversion to return null
        doReturn(null).when(setter).invokeMethodWithSingleParameterOnThisObject(any(), any());
        // Act
        setter.addBasicProperty("customObject", "nullValue");
        // Assert – no error logged and invokeMethod... not called (verified by
        // the fact that we stubbed it to do nothing)
        assertTrue(statusChecker.getStatusList().stream().noneMatch(s -> s.getLevel() == Status.ERROR), "No error should be logged");
        // verify that invokeMethodWithSingleParameterOnThisObject was never called
        verify(setter, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
    }

    @Test
    @DisplayName("Successful conversion and invocation – integer property")
    void successfulConversionAndInvocation() throws Exception {
        IntegerHolder bean = new IntegerHolder();
        PropertySetter setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        setter.addBasicProperty("intProperty", "42");
        assertTrue(bean.getInts().size() == 1 && bean.getInts().get(0) == 42, "Integer value should be added to the collection");
        assertTrue(statusChecker.getStatusList().stream().noneMatch(s -> s.getLevel() == Status.ERROR), "No error should be logged");
    }

    @Test
    @DisplayName("Null property name handling – NullPointerException expected")
    void nullPropertyNameHandling() throws Exception {
        DoorHolder bean = new DoorHolder();
        PropertySetter setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        // The method should throw NPE when name is null because
        // StringUtil.capitalizeFirstLetter(null) throws NPE.
        assertThrows(NullPointerException.class, () -> setter.addBasicProperty(null, "anyValue"));
        // No error status should be added because the exception aborts execution.
        assertTrue(statusChecker.getStatusList().isEmpty(), "No status should be recorded when NPE is thrown");
    }

    @Test
    @DisplayName("Empty property name handling – missing adder error")
    void emptyPropertyNameHandling() throws Exception {
        DoorHolder bean = new DoorHolder();
        PropertySetter setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        setter.addBasicProperty("", "value");
        assertTrue(statusChecker.containsErrorMessage("No adder for property []"), "Expected missing‑adder error for empty property name");
        assertTrue(bean.getDoors().isEmpty(), "No adder should have been invoked");
    }

    @Test
    @DisplayName("Whitespace property name handling – missing adder error")
    void whitespacePropertyNameHandling() throws Exception {
        DoorHolder bean = new DoorHolder();
        PropertySetter setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        setter.addBasicProperty("  spacedProperty", "123");
        assertTrue(statusChecker.containsErrorMessage("No adder for property [  spacedProperty]"), "Expected missing‑adder error for whitespace‑containing name");
        assertTrue(bean.getDoors().isEmpty(), "No adder should have been invoked");
    }

    @Test
    @DisplayName("Adder method with invalid signature (multiple parameters) – sanity check error")
    void adderWithMultipleParameters() throws Exception {
        MultiParamHolder bean = new MultiParamHolder();
        PropertySetter real = new PropertySetter(new BeanDescriptionCache(context), bean);
        real.setContext(context);
        PropertySetter setter = spy(real);
        // Mock AggregationAssessor to return the multi‑parameter method
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = MultiParamHolder.class.getMethod("addMultiParam", String.class, String.class);
        when(mockAssessor.findAdderMethod("MultiParam")).thenReturn(adder);
        injectAggregationAssessor(setter, mockAssessor);
        // Act
        setter.addBasicProperty("multiParam", "value");
        // Assert – sanity‑check should have logged an error and no invocation should happen
        assertTrue(statusChecker.getStatusList().stream().anyMatch(s -> s.getLevel() == Status.ERROR && s.getMessage().contains("does not have a single parameter")), "Expected sanity‑check error for multi‑parameter adder");
        verify(setter, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
    }

    @Test
    @DisplayName("Adder method invocation throws exception – error is logged")
    void adderInvocationThrowsException() throws Exception {
        IntegerHolder bean = new IntegerHolder();
        PropertySetter real = new PropertySetter(new BeanDescriptionCache(context), bean);
        real.setContext(context);
        PropertySetter setter = spy(real);
        // Mock AggregationAssessor to return the correct adder
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = IntegerHolder.class.getMethod("addIntProperty", Integer.class);
        when(mockAssessor.findAdderMethod("IntProperty")).thenReturn(adder);
        injectAggregationAssessor(setter, mockAssessor);
        // Force invokeMethod... to throw RuntimeException
        doThrow(new RuntimeException("reflective failure")).when(setter).invokeMethodWithSingleParameterOnThisObject(eq(adder), any());
        // Act
        setter.addBasicProperty("intProperty", "42");
        // Assert – error status contains the exception message
        assertTrue(statusChecker.getStatusList().stream().anyMatch(s -> s.getLevel() == Status.ERROR && s.getMessage().contains("reflective failure")), "Expected error logged for reflective invocation failure");
        // No value should have been added
        assertTrue(bean.getInts().isEmpty(), "Adder should not have succeeded");
    }

    @Test
    @DisplayName("Successful conversion of empty string to String property")
    void emptyStringToStringProperty() throws Exception {
        StringHolder bean = new StringHolder();
        PropertySetter setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        setter.addBasicProperty("stringProp", "");
        assertEquals(1, bean.getStrings().size(), "String adder should be invoked once");
        assertEquals("", bean.getStrings().get(0), "Empty string must be stored");
        assertTrue(statusChecker.getStatusList().stream().noneMatch(s -> s.getLevel() == Status.ERROR), "No error expected");
    }

    @Test
    @DisplayName("Boolean property conversion (true) – adder invoked")
    void booleanPropertyConversionTrue() throws Exception {
        BooleanHolder bean = new BooleanHolder();
        PropertySetter setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        setter.addBasicProperty("booleanProp", "true");
        assertEquals(1, bean.getBools().size(), "Boolean adder should be called");
        assertTrue(bean.getBools().get(0), "Value must be Boolean.TRUE");
        assertTrue(statusChecker.getStatusList().stream().noneMatch(s -> s.getLevel() == Status.ERROR), "No error expected");
    }

    @Test
    @DisplayName("Boolean property conversion (invalid value) – conversion error")
    void booleanPropertyConversionInvalid() throws Exception {
        BooleanHolder bean = new BooleanHolder();
        PropertySetter setter = new PropertySetter(new BeanDescriptionCache(context), bean);
        setter.setContext(context);
        setter.addBasicProperty("booleanProp", "notABool");
        assertTrue(statusChecker.containsErrorMessage("Conversion to type [class java.lang.Boolean] failed."), "Expected conversion‑failure error for invalid boolean");
        assertTrue(bean.getBools().isEmpty(), "Adder must not be invoked on conversion failure");
    }

    @Test
    @DisplayName("Adder method with no parameters – sanity‑check error")
    void adderWithNoParameters() throws Exception {
        NoParamHolder bean = new NoParamHolder();
        PropertySetter real = new PropertySetter(new BeanDescriptionCache(context), bean);
        real.setContext(context);
        PropertySetter setter = spy(real);
        // Mock AggregationAssessor to return the zero‑arg method
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = NoParamHolder.class.getMethod("addNoParam");
        when(mockAssessor.findAdderMethod("NoParamAdder")).thenReturn(adder);
        injectAggregationAssessor(setter, mockAssessor);
        // Act
        setter.addBasicProperty("noParamAdder", "anyValue");
        // Assert – sanity‑check error logged, no invocation
        assertTrue(statusChecker.getStatusList().stream().anyMatch(s -> s.getLevel() == Status.ERROR && s.getMessage().contains("does not have a single parameter")), "Expected error for zero‑parameter adder");
        verify(setter, never()).invokeMethodWithSingleParameterOnThisObject(any(), any());
    }

    @Test
    @DisplayName("Adder method not accessible (private) – IllegalAccessException logged")
    void privateAdderInvocation() throws Exception {
        PrivateAdderHolder bean = new PrivateAdderHolder();
        PropertySetter real = new PropertySetter(new BeanDescriptionCache(context), bean);
        real.setContext(context);
        PropertySetter setter = spy(real);
        // Mock AggregationAssessor to return the private method
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = PrivateAdderHolder.class.getDeclaredMethod("addPrivateAdder", String.class);
        when(mockAssessor.findAdderMethod("PrivateAdder")).thenReturn(adder);
        injectAggregationAssessor(setter, mockAssessor);
        // Force the reflective call to throw IllegalAccessException
        doThrow(new IllegalAccessException("cannot access")).when(setter).invokeMethodWithSingleParameterOnThisObject(eq(adder), any());
        // Act
        setter.addBasicProperty("privateAdder", "42");
        // Assert – error status contains the IllegalAccessException message
        assertTrue(statusChecker.getStatusList().stream().anyMatch(s -> s.getLevel() == Status.ERROR && s.getMessage().contains("cannot access")), "Expected IllegalAccessException to be logged");
    }

    @Test
    @DisplayName("AggregationAssessor throws exception during adder lookup")
    void aggregationAssessorThrowsDuringLookup() throws Exception {
        DoorHolder bean = new DoorHolder();
        PropertySetter real = new PropertySetter(new BeanDescriptionCache(context), bean);
        real.setContext(context);
        PropertySetter setter = spy(real);
        // Mock assessor to throw RuntimeException
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        when(mockAssessor.findAdderMethod(anyString())).thenThrow(new RuntimeException("lookup failure"));
        injectAggregationAssessor(setter, mockAssessor);
        // Act
        setter.addBasicProperty("exceptionDuringLookup", "value");
        // Assert – error status contains the lookup failure message
        assertTrue(statusChecker.getStatusList().stream().anyMatch(s -> s.getLevel() == Status.ERROR && s.getMessage().contains("lookup failure")), "Expected error logged for assessor exception");
        assertTrue(bean.getDoors().isEmpty(), "No adder should have been invoked");
    }

    @Test
    @DisplayName("Sanity check throws unexpected exception")
    void sanityCheckThrowsException() throws Exception {
        DoorHolder bean = new DoorHolder();
        PropertySetter real = new PropertySetter(new BeanDescriptionCache(context), bean);
        real.setContext(context);
        PropertySetter setter = spy(real);
        // Mock assessor to return a valid adder method
        AggregationAssessor mockAssessor = mock(AggregationAssessor.class);
        Method adder = DoorHolder.class.getMethod("addDoor", String.class);
        when(mockAssessor.findAdderMethod("Door")).thenReturn(adder);
        injectAggregationAssessor(setter, mockAssessor);
        // Stub the private sanity‑check method to throw NPE
        doThrow(new NullPointerException("sanity failure")).when(setter).isSanityCheckSuccessful(eq("Door"), eq(adder), any(), any());
        // Act
        setter.addBasicProperty("door", "front");
        // Assert – error logged and no invocation performed
        assertTrue(statusChecker.getStatusList().stream().anyMatch(s -> s.getLevel() == Status.ERROR && s.getMessage().contains("sanity failure")), "Expected error from sanity‑check exception");
        assertTrue(bean.getDoors().isEmpty(), "Adder must not have been called");
    }
}
