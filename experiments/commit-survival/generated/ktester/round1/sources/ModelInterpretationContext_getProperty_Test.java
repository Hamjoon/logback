package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getProperty_Test {

    @Mock
    Context // mocked Context dependency
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor of ModelInterpretationContext expects a Context.
        // We pass the mock – it will be stored in the superclass field.
        mic = new ModelInterpretationContext(mockContext);
    }

    /* --------------------------------------------------------------------- */
    private static void setPropertiesMap(ModelInterpretationContext mic, Map<String, String> map) throws Exception {
        Field f = ModelInterpretationContext.class.getDeclaredField("propertiesMap");
        f.setAccessible(true);
        f.set(mic, map);
    }

    private static void setContext(ModelInterpretationContext mic, Context ctx) throws Exception {
        Field f = ContextAwareBase.class.getDeclaredField("context");
        f.setAccessible(true);
        f.set(mic, ctx);
    }

    private static Stream<Arguments> provideNormalCases() {
        return Stream.of(// key exists locally
        Arguments.of("appName", null, "MyApp"), // key missing locally, context supplies value
        Arguments.of("host", "localhost", "localhost"), // key missing everywhere → null
        Arguments.of("missingKey", null, null), // null key → NPE (propertiesMap.get(null) returns null, then context.getProperty(null) throws NPE)
        Arguments.of(null, null, NullPointerException.class));
    }

    private static Stream<Arguments> provideEdgeCases() {
        return Stream.of(// propertiesMap is null → delegate to context
        Arguments.of("configPath", true, false, "/etc/app/config"), // context is null but key exists locally
        Arguments.of("servicePort", false, true, "8080"), // both maps null → NPE
        Arguments.of("undefinedKey", true, true, NullPointerException.class), // local map contains key with null value → fallback to context
        Arguments.of("override", false, false, "defaultValue"), // empty string key present locally
        Arguments.of("", false, false, "emptyKeyValue"), // empty string key absent locally but present in context
        Arguments.of("", false, false, "emptyFromContext"), // key maps to empty (non‑null) string
        Arguments.of("blank", false, false, ""));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        return Stream.of(// propertiesMap null + null key → NPE before context lookup
        Arguments.of(null, true, false), // context null + non‑null map (empty) + null key → NPE after map miss
        Arguments.of(null, false, true), // map contains key with null value, context null → NPE on fallback
        Arguments.of("nullableKey", false, true));
    }

    @Test
    void getProperty_returnsValueFromLocalPropertiesMap_whenKeyExists() {
        // given – a key/value pair stored in the MIC's own properties map
        String key = "localKey";
        String expected = "localValue";
        // public API adds to propertiesMap
        mic.addSubstitutionProperty(key, expected);
        // when
        String actual = mic.getProperty(key);
        // then
        assertEquals(expected, actual, "Should return the value from the MIC's properties map");
        // verify that the context was not consulted
        verifyNoInteractions(mockContext);
    }

    @Test
    void getProperty_fallsBackToContext_whenKeyNotInLocalMap() {
        // given – a key that is NOT present in the MIC's properties map
        String key = "missingKey";
        String expected = "contextValue";
        // configure the mock Context to return a value for the missing key
        when(mockContext.getProperty(key)).thenReturn(expected);
        // when
        String actual = mic.getProperty(key);
        // then
        assertEquals(expected, actual, "Should delegate to the Context when the key is absent locally");
        // verify that the context's getProperty was indeed called once
        verify(mockContext, times(1)).getProperty(key);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: key={0} → expected={2}")
    @MethodSource("provideNormalCases")
    void getProperty_normalBehaviour(String key, String contextReturn, Object expected) throws Exception {
        // Arrange – ensure a fresh, non‑null propertiesMap
        setPropertiesMap(mic, new HashMap<>());
        // Populate local map when needed
        if ("appName".equals(key)) {
            mic.addSubstitutionProperty(key, "MyApp");
        } else if ("override".equals(key)) {
            // key present with null value – simulate by direct map manipulation
            Map<String, String> map = new HashMap<>();
            map.put(key, null);
            setPropertiesMap(mic, map);
        } else if ("".equals(key)) {
            mic.addSubstitutionProperty(key, "emptyKeyValue");
        } else if ("blank".equals(key)) {
            mic.addSubstitutionProperty(key, "");
        }
        // Mock context when a fallback is expected
        if (contextReturn != null) {
            when(mockContext.getProperty(key)).thenReturn(contextReturn);
        } else {
            when(mockContext.getProperty(key)).thenReturn(null);
        }
        // Act & Assert
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<?>) expected, () -> mic.getProperty(key));
        } else {
            String actual = mic.getProperty(key);
            assertEquals(expected, actual);
        }
        // Verify interaction only when fallback should happen
        if (!mic.getCopyOfPropertyMap().containsKey(key) || mic.getCopyOfPropertyMap().get(key) == null) {
            verify(mockContext, atLeastOnce()).getProperty(key);
        } else {
            verifyNoInteractions(mockContext);
        }
        // Reset mock for next iteration
        reset(mockContext);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: key={0} (propertiesMap null={1}, context null={2}) → expected={3}")
    @MethodSource("provideEdgeCases")
    void getProperty_edgeCases(String key, boolean propertiesMapIsNull, boolean contextIsNull, Object expected) throws Exception {
        // Arrange – possibly nullify propertiesMap
        if (propertiesMapIsNull) {
            setPropertiesMap(mic, null);
        } else {
            setPropertiesMap(mic, new HashMap<>());
        }
        // Arrange – possibly nullify the underlying Context
        if (contextIsNull) {
            setContext(mic, null);
        } else {
            setContext(mic, mockContext);
        }
        // Populate map when required and map is not null
        if (!propertiesMapIsNull && key != null) {
            if ("configPath".equals(key)) {
                // map is null, so we skip – nothing to put
            } else if ("servicePort".equals(key)) {
                mic.addSubstitutionProperty(key, "8080");
            } else if ("override".equals(key)) {
                // map contains key with null value
                Map<String, String> map = new HashMap<>();
                map.put(key, null);
                setPropertiesMap(mic, map);
            } else if ("".equals(key) && !"emptyFromContext".equals(expected)) {
                mic.addSubstitutionProperty(key, "emptyKeyValue");
            } else if ("blank".equals(key)) {
                mic.addSubstitutionProperty(key, "");
            }
        }
        // Mock context response when context is present
        if (!contextIsNull && key != null) {
            when(mockContext.getProperty(key)).thenReturn(expected instanceof String ? (String) expected : null);
        }
        // Act & Assert
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<?>) expected, () -> mic.getProperty(key));
        } else {
            String actual = mic.getProperty(key);
            assertEquals(expected, actual);
        }
        // Verify interaction only when fallback is expected
        if (!propertiesMapIsNull && mic.getCopyOfPropertyMap().containsKey(key) && mic.getCopyOfPropertyMap().get(key) != null) {
            verifyNoInteractions(mockContext);
        } else if (!contextIsNull) {
            verify(mockContext, atLeastOnce()).getProperty(key);
        }
        reset(mockContext);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: key={0} (propertiesMap null={1}, context null={2}) → NPE")
    @MethodSource("provideExceptionScenarios")
    void getProperty_exceptionScenarios(String key, boolean propertiesMapIsNull, boolean contextIsNull) throws Exception {
        // Arrange – set up null states as described
        if (propertiesMapIsNull) {
            setPropertiesMap(mic, null);
        } else {
            setPropertiesMap(mic, new HashMap<>());
        }
        if (contextIsNull) {
            setContext(mic, null);
        } else {
            setContext(mic, mockContext);
        }
        // Populate map when needed (e.g., key maps to null)
        if (!propertiesMapIsNull && key != null && !"nullableKey".equals(key)) {
            // normal map – nothing to add
        } else if (!propertiesMapIsNull && "nullableKey".equals(key)) {
            Map<String, String> map = new HashMap<>();
            map.put(key, null);
            setPropertiesMap(mic, map);
        }
        // Act & Assert – always expect NPE
        assertThrows(NullPointerException.class, () -> mic.getProperty(key));
    }
}
