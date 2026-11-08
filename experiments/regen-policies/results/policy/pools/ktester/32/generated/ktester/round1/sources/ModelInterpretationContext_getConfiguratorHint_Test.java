package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getConfiguratorHint_Test {

    private Context mockContext;

    /**
     * Simple user‑defined class to test reference equality.
     */
    static class CustomHint {

        final int id = 42;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock Context for each test
        mockContext = mock(Context.class);
    }

    @Test
    void getConfiguratorHint_returnsNull_whenNotProvidedInConstructor() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        // when
        Object result = mic.getConfiguratorHint();
        // then
        assertNull(result, "Configurator hint should be null when not supplied");
    }

    @Test
    void getConfiguratorHint_returnsProvidedObject_whenSuppliedInConstructor() {
        // given
        Object expectedHint = new Object();
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext, expectedHint);
        // when
        Object result = mic.getConfiguratorHint();
        // then
        assertSame(expectedHint, result, "Configurator hint should be the same object supplied to the constructor");
    }

    @Test
    void getConfiguratorHint_reflectsValueSetViaSetter() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        Object expectedHint = "myHint";
        // when
        mic.setConfiguratorHint(expectedHint);
        Object result = mic.getConfiguratorHint();
        // then
        assertEquals(expectedHint, result, "Configurator hint should reflect the value set by setConfiguratorHint");
    }

    @Test
    void getConfiguratorHint_copiesHintFromOtherMic_usingCopyConstructor() {
        // given
        Object originalHint = Integer.valueOf(42);
        ModelInterpretationContext original = new ModelInterpretationContext(mockContext, originalHint);
        // when
        ModelInterpretationContext copy = new ModelInterpretationContext(original);
        Object result = copy.getConfiguratorHint();
        // then
        assertSame(originalHint, result, "Copy constructor should preserve the configurator hint from the original instance");
    }

    @Test
    void getConfiguratorHint_returnsStringHint_setViaSetter() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        String hint = "simple-hint";
        // when
        mic.setConfiguratorHint(hint);
        // then
        assertSame(hint, mic.getConfiguratorHint(), "Getter should return the exact String instance that was set");
    }

    @Test
    void getConfiguratorHint_returnsIntegerHint_setViaSetter() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        Integer hint = Integer.valueOf(12345);
        // when
        mic.setConfiguratorHint(hint);
        // then
        assertSame(hint, mic.getConfiguratorHint(), "Getter should return the same Integer object that was set");
    }

    @Test
    void getConfiguratorHint_returnsMutableStringBuilder_andReflectsModification() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        StringBuilder hint = new StringBuilder("mutable");
        mic.setConfiguratorHint(hint);
        // when: modify the original mutable object
        hint.append("-modified");
        // then
        assertSame(hint, mic.getConfiguratorHint(), "Getter should return the same mutable reference");
        assertEquals("mutable-modified", mic.getConfiguratorHint().toString(), "Modification of the mutable hint must be visible through the getter");
    }

    @Test
    void getConfiguratorHint_overwritesPreviousHint() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        mic.setConfiguratorHint("first-hint");
        // when
        mic.setConfiguratorHint("second-hint");
        // then
        assertEquals("second-hint", mic.getConfiguratorHint(), "The most recent hint should overwrite the previous one");
    }

    @Test
    void getConfiguratorHint_explicitNullClearsPreviousHint() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        mic.setConfiguratorHint("non-null-hint");
        // when
        mic.setConfiguratorHint(null);
        // then
        assertNull(mic.getConfiguratorHint(), "Explicitly setting null should clear the configurator hint");
    }

    @Test
    void getConfiguratorHint_returnsCustomObjectReference() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        CustomHint customHint = new CustomHint();
        mic.setConfiguratorHint(customHint);
        // then
        assertSame(customHint, mic.getConfiguratorHint(), "Getter must return the exact custom object reference supplied");
    }

    @Test
    void getConfiguratorHint_isUnaffectedByObjectStackOperations() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        String hint = "stack-hint";
        mic.setConfiguratorHint(hint);
        // Perform unrelated stack manipulations
        mic.pushObject("foo");
        mic.pushObject(123);
        // removes 123
        mic.popObject();
        // removes "foo"
        mic.popObject();
        // then
        assertEquals(hint, mic.getConfiguratorHint(), "Stack operations must not alter the stored configurator hint");
    }

    @Test
    void getConfiguratorHint_returnsMutableList_andReflectsModification() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        List<String> hintList = new ArrayList<>();
        hintList.add("initial");
        mic.setConfiguratorHint(hintList);
        // when: modify the list after it has been set as the hint
        hintList.add("added");
        // then
        @SuppressWarnings("unchecked")
        List<String> retrieved = (List<String>) mic.getConfiguratorHint();
        assertSame(hintList, retrieved, "Getter should return the same list instance");
        assertEquals(List.of("initial", "added"), retrieved, "Modifications to the mutable collection must be visible through the getter");
    }
}
