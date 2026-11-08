package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getObj_Test {

    /**
     * shared test context
     */
    private static Context context;

    /**
     * object under test
     */
    private PropertySetter setter;

    /**
     * the bean that the PropertySetter wraps
     */
    private House house;

    class MyList extends ArrayList<String> {
    }

    /**
     * A very small POJO used by the default fixture.  It does not need any
     * behaviour – it merely provides a concrete class for the {@link PropertySetter}
     * constructor.
     */
    class House {
        // intentionally empty – only serves as a placeholder bean
    }

    class MyList extends ArrayList<String> {
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a minimal Context implementation sufficient for the tests
        context = new ContextBase();
    }

    @BeforeEach
    void setupBeforeEach() {
        // default fixture – a plain bean wrapped by PropertySetter
        house = new House();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        setter = new PropertySetter(cache, house);
        setter.setContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // help GC and avoid accidental cross‑test reuse
        setter = null;
        house = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        context = null;
    }

    @Test
    void getObjShouldReturnTheObjectPassedToConstructor() {
        // verify that getObj returns the exact instance supplied at construction time
        assertNotNull(setter.getObj(), "getObj() must not return null");
        assertSame(house, setter.getObj(), "getObj() should return the original bean instance");
    }

    @Test
    void getObjReturnsSameStringInstance() {
        // Arrange
        String sample = "sample";
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), sample);
        // Act & Assert
        assertSame(sample, ps.getObj(), "String instance must be returned unchanged");
    }

    @Test
    void getObjReturnsNullWhenConstructedWithNull() {
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), null);
        assertNull(ps.getObj(), "Constructor argument null should be reflected by getObj()");
    }

    @Test
    void getObjReturnsMutableObjectAndReflectsExternalModifications() {
        List<String> list = new ArrayList<>();
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), list);
        // Retrieve the internal reference and mutate it
        @SuppressWarnings("unchecked")
        List<String> returned = (List<String>) ps.getObj();
        returned.add("element");
        // The original list must see the change
        assertEquals(1, list.size(), "Modification through the returned reference must affect the internal state");
        assertEquals("element", list.get(0));
    }

    @Test
    void getObjRemainsUnchangedAfterUnrelatedPublicMethods() {
        // invoke a couple of methods that do not touch the obj field
        setter.setContext(context);
        setter.setProperty("someName", "someValue");
        // verify the reference is still the original bean
        assertSame(house, setter.getObj(), "obj reference must stay the same after unrelated calls");
    }

    @Test
    void getObjAfterAddBasicProperty() {
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), new StringBuilder("init"));
        ps.addBasicProperty("someName", "value");
        assertEquals("init", ps.getObj().toString(), "addBasicProperty must not alter the stored object");
    }

    @Test
    void getObjAfterAddComplexProperty() {
        Map<String, String> original = new HashMap<>();
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), original);
        ps.addComplexProperty("map", new HashMap<>());
        assertSame(original, ps.getObj(), "addComplexProperty must not replace the stored object");
    }

    @Test
    void getObjAfterSetComplexProperty() {
        List<Integer> original = new ArrayList<>();
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), original);
        ps.setComplexProperty("list", List.of(1, 2, 3));
        assertSame(original, ps.getObj(), "setComplexProperty must not replace the stored object");
    }

    @Test
    void getObjMultipleCallsReturnSameReference() {
        Integer dummy = Integer.valueOf(42);
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), dummy);
        Object first = ps.getObj();
        Object second = ps.getObj();
        Object third = ps.getObj();
        assertSame(first, second, "Second call must return the same reference as the first");
        assertSame(first, third, "Third call must return the same reference as the first");
    }

    @Test
    void getObjReturnsSubclassInstanceUnchanged() {
        class MyList extends ArrayList<String> {
        }
        MyList myList = new MyList();
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), myList);
        assertSame(myList, ps.getObj(), "Subclass instance must be returned unchanged");
    }

    @Test
    void getObjAfterSetContextNull() {
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), new Object());
        // explicitly set a null context
        ps.setContext(null);
        assertNotNull(ps.getObj(), "obj must stay non‑null after setting a null context");
    }

    @Test
    void getObjAfterInvalidSetPropertyCalls() {
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), new Object());
        // null name
        ps.setProperty(null, "value");
        // empty name
        ps.setProperty("", "value");
        // null value
        ps.setProperty("name", null);
        // none of the above should have touched the obj field
        assertNotNull(ps.getObj(), "obj must remain unchanged after invalid setProperty calls");
    }

    @Test
    void getObjAfterComputeAggregationType() {
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), new Object());
        // The method returns an AggregationType; we ignore the result – we only care about side‑effects
        ps.computeAggregationType("someName");
        assertNotNull(ps.getObj(), "obj must stay unchanged after computeAggregationType");
    }

    @Test
    void getObjAfterGetClassNameViaImplicitRules() {
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), new Object());
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        ps.getClassNameViaImplicitRules("name", AggregationType.AS_ONE, registry);
        assertNotNull(ps.getObj(), "obj must stay unchanged after getClassNameViaImplicitRules");
    }

    @Test
    void getObjAfterGetObjClass() {
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(context), new Object());
        Class<?> clazz = ps.getObjClass();
        assertEquals(Object.class, clazz, "getObjClass should return the class of the wrapped object");
        assertNotNull(ps.getObj(), "Calling getObjClass must not modify obj");
    }
}
