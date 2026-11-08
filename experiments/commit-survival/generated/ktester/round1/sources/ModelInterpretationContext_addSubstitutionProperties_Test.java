package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.util.Set;
import java.util.HashSet;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_addSubstitutionProperties_Test {

    private ModelInterpretationContext mic;

    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // A simple mock Context is sufficient – the class only stores it.
        mockContext = org.mockito.Mockito.mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void addSubstitutionProperties_nullArgument_shouldDoNothing() {
        // Given: a MIC with an empty property map
        assertNull(mic.getProperty("anyKey"), "Pre‑condition: property map must be empty");
        // When: null is passed
        mic.addSubstitutionProperties(null);
        // Then: the property map is still empty and no exception is thrown
        assertNull(mic.getProperty("anyKey"));
    }

    @Test
    void addSubstitutionProperties_multipleEntries_shouldPopulatePropertyMap() {
        // Given: a Properties instance with several key/value pairs
        Properties props = new Properties();
        props.setProperty("firstKey", "firstValue");
        props.setProperty("secondKey", "secondValue");
        props.setProperty("thirdKey", "thirdValue");
        // Ensure the MIC starts with no such properties
        assertNull(mic.getProperty("firstKey"));
        assertNull(mic.getProperty("secondKey"));
        assertNull(mic.getProperty("thirdKey"));
        // When: the method under test is invoked
        mic.addSubstitutionProperties(props);
        // Then: each property is available via getProperty()
        assertEquals("firstValue", mic.getProperty("firstKey"));
        assertEquals("secondValue", mic.getProperty("secondKey"));
        assertEquals("thirdValue", mic.getProperty("thirdKey"));
    }

    @Test
    void addSubstitutionProperties_overwritesExistingKey() {
        // Given: an existing property and a new Properties object that contains the same key
        mic.addSubstitutionProperty("dupKey", "originalValue");
        assertEquals("originalValue", mic.getProperty("dupKey"));
        Properties newProps = new Properties();
        newProps.setProperty("dupKey", "newValue");
        // When: we add the new properties
        mic.addSubstitutionProperties(newProps);
        // Then: the value is overwritten with the new one
        assertEquals("newValue", mic.getProperty("dupKey"));
    }

    @Test
    void addSubstitutionProperties_emptyProperties_shouldLeaveMapUnchanged() {
        // Given: an empty Properties instance
        Properties emptyProps = new Properties();
        // When: the method is called
        mic.addSubstitutionProperties(emptyProps);
        // Then: the internal property map should still be empty
        assertTrue(mic.getCopyOfPropertyMap().isEmpty(), "Property map must remain empty after processing an empty Properties object");
    }

    @Test
    void addSubstitutionProperties_singleEntry_shouldAddOneProperty() {
        // Given: a Properties with a single key/value pair
        Properties single = new Properties();
        single.setProperty("host", "localhost");
        // When
        mic.addSubstitutionProperties(single);
        // Then
        assertEquals("localhost", mic.getProperty("host"));
        assertEquals(1, mic.getCopyOfPropertyMap().size(), "Exactly one property should have been added");
    }

    @Test
    void addSubstitutionProperties_multipleEntriesIncludingEmptyAndNull_shouldHandleAll() {
        // This test uses a Mockito‑mocked Properties to inject a null value,
        // because java.util.Properties (a Hashtable) does not allow null values
        // via the regular API.
        // Mocked Properties
        Properties mockProps = mock(Properties.class);
        Set<Object> keys = new HashSet<>();
        keys.add("url");
        keys.add("port");
        // we will return null for this key
        keys.add("user");
        when(mockProps.keySet()).thenReturn(keys);
        when(mockProps.getProperty("url")).thenReturn("http://example.com");
        // empty string
        when(mockProps.getProperty("port")).thenReturn("");
        // explicit null
        when(mockProps.getProperty("user")).thenReturn(null);
        // When
        mic.addSubstitutionProperties(mockProps);
        // Then
        assertEquals("http://example.com", mic.getProperty("url"));
        assertEquals("", mic.getProperty("port"));
        assertNull(mic.getProperty("user"), "A null value from the Properties object should be stored as null");
    }

    @Test
    void addSubstitutionProperties_nonStringKey_shouldThrowClassCastException() {
        // Given: a Properties containing a non‑String key (Integer)
        Properties props = new Properties();
        // raw put bypasses type safety
        props.put(123, "value");
        // When / Then: iterating over the keySet will cause a ClassCastException
        assertThrows(ClassCastException.class, () -> mic.addSubstitutionProperties(props), "A non‑String key should result in ClassCastException");
    }

    @Test
    void addSubstitutionProperties_nullKey_shouldThrowNullPointerException() {
        // Create a mock Properties whose keySet contains a null entry.
        Properties mockProps = mock(Properties.class);
        Set<Object> keys = new HashSet<>();
        keys.add(null);
        when(mockProps.keySet()).thenReturn(keys);
        // getProperty(null) on a real Properties throws NPE; we let the mock behave the same.
        when(mockProps.getProperty(null)).thenThrow(new NullPointerException());
        // When / Then
        assertThrows(NullPointerException.class, () -> mic.addSubstitutionProperties(mockProps), "A null key should cause NullPointerException when getProperty(null) is invoked");
    }

    @Test
    void addSubstitutionProperties_keySetThrowsRuntimeException_shouldPropagate() {
        // Mock a Properties whose keySet() throws a RuntimeException.
        Properties mockProps = mock(Properties.class);
        when(mockProps.keySet()).thenThrow(new RuntimeException("keySet failure"));
        // When / Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> mic.addSubstitutionProperties(mockProps), "The RuntimeException from keySet() should propagate to the caller");
        assertEquals("keySet failure", thrown.getMessage());
    }
}
