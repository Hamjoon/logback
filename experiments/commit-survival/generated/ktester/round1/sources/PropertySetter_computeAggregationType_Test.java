package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;
import org.mockito.Mockito;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_computeAggregationType_Test {

    /**
     * Shared test infrastructure
     */
    private static Context context;

    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * Object under test – a fresh instance for every test method
     */
    private PropertySetter setter;

    // the bean used by PropertySetter
    private House house;

    /**
     * Object under test – a fresh spy for every test method
     */
    private PropertySetter setterSpy;

    // the bean used by PropertySetter
    private DummyBean dummyBean;

    /**
     * Minimal bean class – only used to obtain {@link Method} objects.
     * The actual method bodies are irrelevant because we stub the
     * {@code computeRawAggregationType} helper.
     */
    public static class DummyBean {

        // adder signatures
        public void addBasic(String s) {
        }

        public void addComplex(Object o) {
        }

        public void addBasicColl(String s) {
        }

        public void addComplexColl(Object o) {
        }

        // setter signatures
        public void setBasic(String s) {
        }

        public void setComplex(Object o) {
        }

        public void setNotFound(String s) {
        }

        public void setBasicColl(String s) {
        }

        public void setComplexColl(Object o) {
        }
    }

    /**
     * Simple holder for the behaviour we want to inject into the spy.
     * All fields are optional – {@code null} means “do not stub / return null”.
     */
    private static class TestConfig {

        Method adderMethod;

        AggregationType adderRawType;

        Method setterMethod;

        AggregationType setterRawType;

        RuntimeException findAdderThrows;

        RuntimeException findSetterThrows;

        RuntimeException computeRawThrows;

        RuntimeException addErrorThrows;

        TestConfig withAdder(Method m, AggregationType raw) {
            this.adderMethod = m;
            this.adderRawType = raw;
            return this;
        }

        TestConfig withSetter(Method m, AggregationType raw) {
            this.setterMethod = m;
            this.setterRawType = raw;
            return this;
        }

        TestConfig withAdderThrows(RuntimeException ex) {
            this.findAdderThrows = ex;
            return this;
        }

        TestConfig withSetterThrows(RuntimeException ex) {
            this.findSetterThrows = ex;
            return this;
        }

        TestConfig withComputeRawThrows(RuntimeException ex) {
            this.computeRawThrows = ex;
            return this;
        }

        TestConfig withAddErrorThrows(RuntimeException ex) {
            this.addErrorThrows = ex;
            return this;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a real Context is cheap and gives us a working StatusManager
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // the target bean
        house = new House();
        setter = new PropertySetter(beanDescriptionCache, house);
        // required by ContextAwareBase
        setter.setContext(context);
        dummyBean = new DummyBean();
        // a real PropertySetter instance that we will spy on
        PropertySetter real = new PropertySetter(beanDescriptionCache, dummyBean);
        real.setContext(context);
        setterSpy = Mockito.spy(real);
    }

    @AfterEach
    void teardownAfterEach() {
        // reset references – helps the garbage collector and isolates tests
        setter = null;
        house = null;
        setterSpy = null;
        dummyBean = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing special to clean up – the ContextBase does not hold external resources
        context = null;
        beanDescriptionCache = null;
        context = null;
        beanDescriptionCache = null;
    }

    /**
     * Returns a {@link Method} object from {@link DummyBean} with the given name.
     */
    private static Method methodOf(String name, Class<?>... paramTypes) {
        try {
            return DummyBean.class.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No such method in DummyBean: " + name, e);
        }
    }

    /**
     * Stubs the three helper methods according to the supplied configuration.
     */
    private void configureHelpers(TestConfig cfg) {
        // findAdderMethod
        if (cfg.findAdderThrows != null) {
            doThrow(cfg.findAdderThrows).when(setterSpy).findAdderMethod(anyString());
        } else {
            doReturn(cfg.adderMethod).when(setterSpy).findAdderMethod(anyString());
        }
        // computeRawAggregationType (only invoked when an adder or setter is found)
        if (cfg.computeRawThrows != null) {
            doThrow(cfg.computeRawThrows).when(setterSpy).computeRawAggregationType(any(Method.class));
        } else {
            // the stub will be consulted with the *exact* method that was returned
            // by findAdderMethod / findSetterMethod, therefore we can use a generic
            // Answer that maps the method to the desired raw type.
            doAnswer(invocation -> {
                Method m = invocation.getArgument(0);
                if (cfg.adderMethod != null && m.equals(cfg.adderMethod)) {
                    return cfg.adderRawType;
                }
                if (cfg.setterMethod != null && m.equals(cfg.setterMethod)) {
                    return cfg.setterRawType;
                }
                // default – should never happen in the configured tests
                return AggregationType.NOT_FOUND;
            }).when(setterSpy).computeRawAggregationType(any(Method.class));
        }
        // findSetterMethod
        if (cfg.findSetterThrows != null) {
            doThrow(cfg.findSetterThrows).when(setterSpy).findSetterMethod(anyString());
        } else {
            doReturn(cfg.setterMethod).when(setterSpy).findSetterMethod(anyString());
        }
        // addError – only needed for the “unexpected raw type” branches
        if (cfg.addErrorThrows != null) {
            doThrow(cfg.addErrorThrows).when(setterSpy).addError(anyString());
        } else {
            // normal behaviour – just record the call (no side‑effect)
            doNothing().when(setterSpy).addError(anyString());
        }
    }

    /**
     * Verify that an {@code addXxx} method is recognised and the correct
     * {@link AggregationType} is returned.
     *
     * The {@code House} bean defines:
     * <ul>
     *   <li>{@code addDoor(Door)} – a complex property adder → {@code AS_COMPLEX_PROPERTY}</li>
     *   <li>{@code addWindow(Window)} – a complex collection adder → {@code AS_COMPLEX_PROPERTY_COLLECTION}</li>
     *   <li>{@code addAdjective(String)} – a basic collection adder → {@code AS_BASIC_PROPERTY_COLLECTION}</li>
     * </ul>
     */
    @Test
    void computeAggregationType_usesAdderWhenPresent() {
        // complex property adder
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, setter.computeAggregationType("door"), "adder for a complex property should map to AS_COMPLEX_PROPERTY");
        // basic property adder (the raw type is AS_BASIC_PROPERTY, which is mapped to
        // AS_BASIC_PROPERTY_COLLECTION by the switch statement)
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, setter.computeAggregationType("adjective"), "adder for a basic collection should map to AS_BASIC_PROPERTY_COLLECTION");
        // complex collection adder – the raw type is AS_COMPLEX_PROPERTY, which is
        // mapped to AS_COMPLEX_PROPERTY_COLLECTION
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, setter.computeAggregationType("Window"), "adder for a complex collection should map to AS_COMPLEX_PROPERTY_COLLECTION");
    }

    /**
     * Verify that when an {@code addXxx} method is not found, the algorithm falls
     * back to a {@code setXxx} method.
     *
     * The {@code House} bean defines simple setters for several basic properties.
     */
    @Test
    void computeAggregationType_fallsBackToSetterWhenNoAdder() {
        // basic property setters
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("count"), "setter for a basic property should map to AS_BASIC_PROPERTY");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("name"), "setter for a basic property should map to AS_BASIC_PROPERTY");
        // boolean property (setter is {@code setOpen(boolean)})
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("open"), "setter for a boolean property should map to AS_BASIC_PROPERTY");
    }

    /**
     * Verify that an unknown property name results in {@link AggregationType#NOT_FOUND}.
     */
    @Test
    void computeAggregationType_returnsNotFoundWhenNoMatch() {
        assertEquals(AggregationType.NOT_FOUND, setter.computeAggregationType("nonExistingProperty"), "unknown property name must yield NOT_FOUND");
    }

    /**
     * Verify that the method is case‑insensitive for the first character,
     * matching the behaviour of {@code capitalizeFirstLetter}.
     */
    @Test
    void computeAggregationType_isCaseInsensitiveForFirstLetter() {
        // both forms should resolve to the same setter
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("count"), "lower‑case first letter");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, setter.computeAggregationType("Count"), "upper‑case first letter");
    }

    /**
     * 1. Adder present – raw type NOT_FOUND → immediate NOT_FOUND
     */
    @Test
    void adderPresent_rawNotFound_returnsNotFound() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addBasic", String.class), AggregationType.NOT_FOUND).withSetter(null, // setter must not be consulted
        null);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("someProperty");
        assertEquals(AggregationType.NOT_FOUND, result);
        // verify that the setter was never looked up
        verify(setterSpy, never()).findSetterMethod(anyString());
    }

    /**
     * 2. Adder present – raw type AS_BASIC_PROPERTY → AS_BASIC_PROPERTY_COLLECTION
     */
    @Test
    void adderPresent_rawBasicProperty_mapsToCollection() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addBasic", String.class), AggregationType.AS_BASIC_PROPERTY).withSetter(null, null);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("basicProp");
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, result);
    }

    /**
     * 3. Adder present – raw type AS_COMPLEX_PROPERTY → AS_COMPLEX_PROPERTY_COLLECTION
     */
    @Test
    void adderPresent_rawComplexProperty_mapsToCollection() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addComplex", Object.class), AggregationType.AS_COMPLEX_PROPERTY).withSetter(null, null);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("complexProp");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, result);
    }

    /**
     * 4. Adder present – unexpected raw type AS_BASIC_PROPERTY_COLLECTION → error + NOT_FOUND
     */
    @Test
    void adderPresent_unexpectedBasicCollection_callsAddErrorAndReturnsNotFound() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addBasicColl", String.class), AggregationType.AS_BASIC_PROPERTY_COLLECTION).withSetter(null, // no setter
        null);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("unexpectedBasicColl");
        assertEquals(AggregationType.NOT_FOUND, result);
        verify(setterSpy, times(1)).addError(contains("Unexpected AggregationType"));
    }

    /**
     * 5. Adder present – unexpected raw type AS_COMPLEX_PROPERTY_COLLECTION → error + NOT_FOUND
     */
    @Test
    void adderPresent_unexpectedComplexCollection_callsAddErrorAndReturnsNotFound() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addComplexColl", Object.class), AggregationType.AS_COMPLEX_PROPERTY_COLLECTION).withSetter(null, null);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("unexpectedComplexColl");
        assertEquals(AggregationType.NOT_FOUND, result);
        verify(setterSpy, times(1)).addError(contains("Unexpected AggregationType"));
    }

    /**
     * 6. No adder, setter present → raw type AS_BASIC_PROPERTY returned unchanged
     */
    @Test
    void setterOnly_basicPropertyReturnedDirectly() {
        TestConfig cfg = new TestConfig().withAdder(null, // no adder
        null).withSetter(methodOf("setBasic", String.class), AggregationType.AS_BASIC_PROPERTY);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("setterOnly");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, result);
    }

    /**
     * 7. No adder, no setter → NOT_FOUND
     */
    @Test
    void noAdderNoSetter_returnsNotFound() {
        TestConfig cfg = new TestConfig().withAdder(null, null).withSetter(null, null);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("unknownProp");
        assertEquals(AggregationType.NOT_FOUND, result);
    }

    /**
     * 8. Unexpected adder raw type but a good setter exists → setter result
     */
    @Test
    void unexpectedAdder_thenGoodSetter_returnsSetterResult() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addBasicColl", String.class), // unexpected
        AggregationType.AS_BASIC_PROPERTY_COLLECTION).withSetter(methodOf("setBasic", String.class), AggregationType.AS_BASIC_PROPERTY);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("unexpectedWithSetterBasic");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, result);
        verify(setterSpy, times(1)).addError(contains("Unexpected AggregationType"));
    }

    /**
     * 9. Unexpected adder raw type, setter exists but returns NOT_FOUND → final NOT_FOUND
     */
    @Test
    void unexpectedAdder_thenSetterNotFound_returnsNotFound() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addComplexColl", Object.class), // unexpected
        AggregationType.AS_COMPLEX_PROPERTY_COLLECTION).withSetter(methodOf("setNotFound", String.class), AggregationType.NOT_FOUND);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("unexpectedWithSetterNotFound");
        assertEquals(AggregationType.NOT_FOUND, result);
        verify(setterSpy, times(1)).addError(contains("Unexpected AggregationType"));
    }

    /**
     * 10. Adder raw NOT_FOUND, setter also exists → NOT_FOUND (setter ignored)
     */
    @Test
    void adderRawNotFound_ignoresSetter() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addBasic", String.class), AggregationType.NOT_FOUND).withSetter(methodOf("setBasic", String.class), // should be ignored
        AggregationType.AS_BASIC_PROPERTY);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("adderNotFoundSetterExists");
        assertEquals(AggregationType.NOT_FOUND, result);
        // setter never looked up
        verify(setterSpy, never()).findSetterMethod(anyString());
    }

    /**
     * 11. Adder raw AS_BASIC_PROPERTY → collection, setter ignored
     */
    @Test
    void adderRawBasicProperty_ignoresSetter() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addBasic", String.class), AggregationType.AS_BASIC_PROPERTY).withSetter(methodOf("setBasic", String.class), // ignored
        AggregationType.AS_BASIC_PROPERTY);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("adderBasicPropSetterExists");
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, result);
        verify(setterSpy, never()).findSetterMethod(anyString());
    }

    /**
     * 12. Adder raw AS_COMPLEX_PROPERTY → collection, setter ignored
     */
    @Test
    void adderRawComplexProperty_ignoresSetter() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addComplex", Object.class), AggregationType.AS_COMPLEX_PROPERTY).withSetter(methodOf("setComplex", Object.class), // ignored
        AggregationType.AS_COMPLEX_PROPERTY);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("adderComplexPropSetterExists");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, result);
        verify(setterSpy, never()).findSetterMethod(anyString());
    }

    /**
     * 13. No adder, setter raw AS_COMPLEX_PROPERTY → returned unchanged
     */
    @Test
    void setterOnly_complexPropertyReturnedDirectly() {
        TestConfig cfg = new TestConfig().withAdder(null, null).withSetter(methodOf("setComplex", Object.class), AggregationType.AS_COMPLEX_PROPERTY);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("setterOnlyComplex");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, result);
    }

    /**
     * 14. No adder, setter raw AS_BASIC_PROPERTY_COLLECTION → returned unchanged
     */
    @Test
    void setterOnly_basicCollectionReturnedDirectly() {
        TestConfig cfg = new TestConfig().withAdder(null, null).withSetter(methodOf("setBasicColl", String.class), AggregationType.AS_BASIC_PROPERTY_COLLECTION);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("setterOnlyBasicCollection");
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, result);
    }

    /**
     * 15. No adder, setter raw AS_COMPLEX_PROPERTY_COLLECTION → returned unchanged
     */
    @Test
    void setterOnly_complexCollectionReturnedDirectly() {
        TestConfig cfg = new TestConfig().withAdder(null, null).withSetter(methodOf("setComplexColl", Object.class), AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("setterOnlyComplexCollection");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, result);
    }

    /**
     * 16. Null name argument → NullPointerException from capitalizeFirstLetter
     */
    @Test
    void nullName_throwsNullPointerException() {
        // No stubbing needed – the method will hit capitalizeFirstLetter first.
        assertThrows(NullPointerException.class, () -> setterSpy.computeAggregationType(null));
    }

    /**
     * 17. Empty string name → NOT_FOUND (no adder / setter)
     */
    @Test
    void emptyName_returnsNotFound() {
        TestConfig cfg = new TestConfig().withAdder(null, null).withSetter(null, null);
        configureHelpers(cfg);
        AggregationType result = setterSpy.computeAggregationType("");
        assertEquals(AggregationType.NOT_FOUND, result);
    }

    /**
     * 18. findAdderMethod throws RuntimeException → exception propagates
     */
    @Test
    void findAdderThrowsRuntimeException_propagates() {
        TestConfig cfg = new TestConfig().withAdderThrows(new RuntimeException("adder failure")).withSetter(null, null);
        configureHelpers(cfg);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> setterSpy.computeAggregationType("anyProperty"));
        assertEquals("adder failure", ex.getMessage());
    }

    /**
     * 19. computeRawAggregationType throws IllegalArgumentException for adder → propagate
     */
    @Test
    void computeRawThrowsIllegalArgumentException_propagates() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addBasic", String.class), // adder found
        null).withComputeRawThrows(new IllegalArgumentException("bad raw")).withSetter(null, null);
        configureHelpers(cfg);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setterSpy.computeAggregationType("anyProperty"));
        assertEquals("bad raw", ex.getMessage());
    }

    /**
     * 20. findSetterMethod throws RuntimeException → propagate
     */
    @Test
    void findSetterThrowsRuntimeException_propagates() {
        TestConfig cfg = new TestConfig().withAdder(null, null).withSetterThrows(new RuntimeException("setter failure"));
        configureHelpers(cfg);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> setterSpy.computeAggregationType("anyProperty"));
        assertEquals("setter failure", ex.getMessage());
    }

    /**
     * 21. addError throws IllegalStateException → propagate
     */
    @Test
    void addErrorThrowsIllegalStateException_propagates() {
        TestConfig cfg = new TestConfig().withAdder(methodOf("addBasicColl", String.class), // unexpected
        AggregationType.AS_BASIC_PROPERTY_COLLECTION).withAddErrorThrows(new IllegalStateException("addError failure")).withSetter(null, null);
        configureHelpers(cfg);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> setterSpy.computeAggregationType("unexpectedWithError"));
        assertEquals("addError failure", ex.getMessage());
    }

    /**
     * 22. capitalizeFirstLetter throws IllegalArgumentException for invalid identifier
     */
    @Test
    void capitalizeFirstLetterInvalidIdentifier_throwsIllegalArgumentException() {
        // No stubbing – the real implementation validates the identifier.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setterSpy.computeAggregationType("1invalid"));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));
    }
}
