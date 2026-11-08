package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getImportMapCopy_Test {

    @Mock
    private Context mockContext;

    private ModelInterpretationContext mic;

    class ThrowingMap extends AbstractMap<String, String> {

        private final Map<String, String> delegate = new HashMap<>();

        ThrowingMap() {
            delegate.put("k", "v");
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            return new Set<Entry<String, String>>() {

                @Override
                public Iterator<Entry<String, String>> iterator() {
                    return new Iterator<Entry<String, String>>() {

                        @Override
                        public boolean hasNext() {
                            throw new RuntimeException("Iterator failure");
                        }

                        @Override
                        public Entry<String, String> next() {
                            throw new RuntimeException("Iterator failure");
                        }
                    };
                }

                // The remaining Set methods are not needed for this test
                @Override
                public int size() {
                    return delegate.size();
                }

                @Override
                public boolean isEmpty() {
                    return delegate.isEmpty();
                }

                @Override
                public boolean contains(Object o) {
                    return delegate.entrySet().contains(o);
                }

                @Override
                public Object[] toArray() {
                    return delegate.entrySet().toArray();
                }

                @Override
                public <T> T[] toArray(T[] a) {
                    return delegate.entrySet().toArray(a);
                }

                @Override
                public boolean add(Entry<String, String> e) {
                    return delegate.entrySet().add(e);
                }

                @Override
                public boolean remove(Object o) {
                    return delegate.entrySet().remove(o);
                }

                @Override
                public boolean containsAll(java.util.Collection<?> c) {
                    return delegate.entrySet().containsAll(c);
                }

                @Override
                public boolean addAll(java.util.Collection<? extends Entry<String, String>> c) {
                    return delegate.entrySet().addAll(c);
                }

                @Override
                public boolean retainAll(java.util.Collection<?> c) {
                    return delegate.entrySet().retainAll(c);
                }

                @Override
                public boolean removeAll(java.util.Collection<?> c) {
                    return delegate.entrySet().removeAll(c);
                }

                @Override
                public void clear() {
                    delegate.clear();
                }
            };
        }
    }

    class ThrowingMap extends AbstractMap<String, String> {

        private final Map<String, String> delegate = new HashMap<>();

        ThrowingMap() {
            delegate.put("k", "v");
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            return new Set<Entry<String, String>>() {

                @Override
                public Iterator<Entry<String, String>> iterator() {
                    return new Iterator<Entry<String, String>>() {

                        @Override
                        public boolean hasNext() {
                            throw new RuntimeException("Iterator failure");
                        }

                        @Override
                        public Entry<String, String> next() {
                            throw new RuntimeException("Iterator failure");
                        }
                    };
                }

                // The remaining Set methods are not needed for this test
                @Override
                public int size() {
                    return delegate.size();
                }

                @Override
                public boolean isEmpty() {
                    return delegate.isEmpty();
                }

                @Override
                public boolean contains(Object o) {
                    return delegate.entrySet().contains(o);
                }

                @Override
                public Object[] toArray() {
                    return delegate.entrySet().toArray();
                }

                @Override
                public <T> T[] toArray(T[] a) {
                    return delegate.entrySet().toArray(a);
                }

                @Override
                public boolean add(Entry<String, String> e) {
                    return delegate.entrySet().add(e);
                }

                @Override
                public boolean remove(Object o) {
                    return delegate.entrySet().remove(o);
                }

                @Override
                public boolean containsAll(java.util.Collection<?> c) {
                    return delegate.entrySet().containsAll(c);
                }

                @Override
                public boolean addAll(java.util.Collection<? extends Entry<String, String>> c) {
                    return delegate.entrySet().addAll(c);
                }

                @Override
                public boolean retainAll(java.util.Collection<?> c) {
                    return delegate.entrySet().retainAll(c);
                }

                @Override
                public boolean removeAll(java.util.Collection<?> c) {
                    return delegate.entrySet().removeAll(c);
                }

                @Override
                public void clear() {
                    delegate.clear();
                }
            };
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        mic = null;
    }

    @Test
    void getImportMapCopy_returnsIndependentCopy() {
        // given
        mic.addImport("java.util", "java.util.List");
        mic.addImport("java.lang", "java.lang.String");
        // when
        Map<String, String> copy = mic.getImportMapCopy();
        // then
        assertEquals(2, copy.size(), "Copy should contain the two added imports");
        assertEquals("java.util.List", copy.get("java.util"));
        assertEquals("java.lang.String", copy.get("java.lang"));
        // when – mutate the returned map
        copy.put("org.example", "org.example.Foo");
        // then – original importMap inside mic must stay unchanged
        Map<String, String> secondCopy = mic.getImportMapCopy();
        assertEquals(2, secondCopy.size(), "Original importMap must not be affected by modifications to the returned copy");
        assertNull(secondCopy.get("org.example"), "Original importMap must not contain the newly added entry");
    }

    @Test
    void copyConstructor_createsIndependentImportMap() {
        // given – populate the original context
        mic.addImport("com.foo", "com.foo.Bar");
        // when – create a copy via the copy‑constructor
        ModelInterpretationContext micCopy = new ModelInterpretationContext(mic);
        // then – both contexts initially share the same entries
        Map<String, String> originalCopy = mic.getImportMapCopy();
        Map<String, String> copiedCopy = micCopy.getImportMapCopy();
        assertEquals(originalCopy, copiedCopy, "Copy constructor should duplicate the import map");
        // when – modify the original after copying
        mic.addImport("com.baz", "com.baz.Qux");
        // then – the copied context must remain unchanged
        Map<String, String> afterModificationCopy = micCopy.getImportMapCopy();
        assertEquals(1, afterModificationCopy.size(), "Copied context must not reflect later changes to the original");
        assertEquals("com.foo.Bar", afterModificationCopy.get("com.foo"));
        assertNull(afterModificationCopy.get("com.baz"));
    }

    /* --------------------------------------------------------------
     * Multiple calls – ensure distinct objects and independence.
     * -------------------------------------------------------------- */
    @Test
    void getImportMapCopy_multipleCallsReturnDistinctObjects() {
        mic.addImport("a", "A");
        mic.addImport("b", "B");
        Map<String, String> first = mic.getImportMapCopy();
        Map<String, String> second = mic.getImportMapCopy();
        // distinct references
        assertNotSame(first, second, "Each call should return a new HashMap instance");
        // same content at the time of each call
        assertEquals(first, second, "Both copies should contain identical entries");
    }

    @Test
    void getImportMapCopy_mutatingFirstCopyDoesNotAffectSecondOrInternalMap() {
        mic.addImport("x", "X");
        mic.addImport("y", "Y");
        Map<String, String> firstCopy = mic.getImportMapCopy();
        Map<String, String> secondCopy = mic.getImportMapCopy();
        // mutate first copy
        firstCopy.put("z", "Z");
        firstCopy.remove("x");
        // second copy must stay unchanged
        assertEquals(2, secondCopy.size(), "Second copy must remain unaffected by first copy mutation");
        assertTrue(secondCopy.containsKey("x"));
        assertFalse(secondCopy.containsKey("z"));
        // internal map must also stay unchanged
        Map<String, String> internalCopy = mic.getImportMapCopy();
        assertEquals(2, internalCopy.size(), "Internal importMap must remain unchanged");
        assertTrue(internalCopy.containsKey("x"));
        assertFalse(internalCopy.containsKey("z"));
    }

    /* --------------------------------------------------------------
     * Large import set – stress test the copy operation.
     * -------------------------------------------------------------- */
    @Test
    void getImportMapCopy_largeImportSet_isCopiedCorrectly() {
        final int COUNT = 1000;
        for (int i = 0; i < COUNT; i++) {
            mic.addImport("stem" + i, "com.example.Class" + i);
        }
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(COUNT, copy.size(), "Copy should contain all imported entries");
        // spot‑check a few entries
        assertEquals("com.example.Class0", copy.get("stem0"));
        assertEquals("com.example.Class999", copy.get("stem999"));
    }

    /* --------------------------------------------------------------
     * Null key/value handling – HashMap permits nulls.
     * -------------------------------------------------------------- */
    @Test
    void getImportMapCopy_allowsNullKeyAndValue() {
        mic.addImport(null, "com.example.NullKey");
        mic.addImport("nullValue", null);
        Map<String, String> copy = mic.getImportMapCopy();
        assertTrue(copy.containsKey(null), "Copy should contain the null key");
        assertEquals("com.example.NullKey", copy.get(null));
        assertTrue(copy.containsKey("nullValue"));
        assertNull(copy.get("nullValue"));
    }

    @Test
    void getImportMapCopy_nullEntriesDoNotThrowException() {
        // The previous test already verifies no NPE is thrown.
        // This test simply ensures the method completes without exception.
        mic.addImport(null, null);
        assertDoesNotThrow(() -> mic.getImportMapCopy(), "Method should handle null key/value without throwing");
    }

    /* --------------------------------------------------------------
     * Returned map mutability – callers can modify the snapshot.
     * -------------------------------------------------------------- */
    @Test
    void getImportMapCopy_returnedMapIsMutableAndDoesNotAffectInternalState() {
        mic.addImport("existingStem", "com.example.Existing");
        Map<String, String> copy = mic.getImportMapCopy();
        // mutable operations
        copy.put("newStem", "com.example.New");
        copy.remove("existingStem");
        // internal state unchanged
        assertEquals("com.example.Existing", mic.getImport("existingStem"));
        assertNull(mic.getImport("newStem"));
    }

    /* --------------------------------------------------------------
     * Return type verification – concrete class must be HashMap.
     * -------------------------------------------------------------- */
    @Test
    void getImportMapCopy_returnsHashMapInstance() {
        mic.addImport("a", "A");
        Map<String, String> copy = mic.getImportMapCopy();
        assertTrue(copy instanceof HashMap, "Returned map should be an instance of java.util.HashMap");
        assertEquals(HashMap.class, copy.getClass(), "Returned map's concrete class must be java.util.HashMap");
    }

    /* --------------------------------------------------------------
     * Null internal importMap – simulate via reflection.
     * -------------------------------------------------------------- */
    @Test
    void getImportMapCopy_whenInternalImportMapIsNull_throwsNullPointerException() throws Exception {
        // Force the private field 'importMap' to null
        Field importMapField = ModelInterpretationContext.class.getDeclaredField("importMap");
        importMapField.setAccessible(true);
        importMapField.set(mic, null);
        assertThrows(NullPointerException.class, () -> mic.getImportMapCopy(), "Calling getImportMapCopy with a null internal map should throw NPE");
    }

    /* --------------------------------------------------------------
     * Exception during map copy – custom map that throws on iteration.
     * -------------------------------------------------------------- */
    @Test
    void getImportMapCopy_whenCopyingThrowsRuntimeException_propagatesException() throws Exception {
        // Custom map that throws RuntimeException when its entrySet iterator is used
        class ThrowingMap extends AbstractMap<String, String> {

            private final Map<String, String> delegate = new HashMap<>();

            ThrowingMap() {
                delegate.put("k", "v");
            }

            @Override
            public Set<Entry<String, String>> entrySet() {
                return new Set<Entry<String, String>>() {

                    @Override
                    public Iterator<Entry<String, String>> iterator() {
                        return new Iterator<Entry<String, String>>() {

                            @Override
                            public boolean hasNext() {
                                throw new RuntimeException("Iterator failure");
                            }

                            @Override
                            public Entry<String, String> next() {
                                throw new RuntimeException("Iterator failure");
                            }
                        };
                    }

                    // The remaining Set methods are not needed for this test
                    @Override
                    public int size() {
                        return delegate.size();
                    }

                    @Override
                    public boolean isEmpty() {
                        return delegate.isEmpty();
                    }

                    @Override
                    public boolean contains(Object o) {
                        return delegate.entrySet().contains(o);
                    }

                    @Override
                    public Object[] toArray() {
                        return delegate.entrySet().toArray();
                    }

                    @Override
                    public <T> T[] toArray(T[] a) {
                        return delegate.entrySet().toArray(a);
                    }

                    @Override
                    public boolean add(Entry<String, String> e) {
                        return delegate.entrySet().add(e);
                    }

                    @Override
                    public boolean remove(Object o) {
                        return delegate.entrySet().remove(o);
                    }

                    @Override
                    public boolean containsAll(java.util.Collection<?> c) {
                        return delegate.entrySet().containsAll(c);
                    }

                    @Override
                    public boolean addAll(java.util.Collection<? extends Entry<String, String>> c) {
                        return delegate.entrySet().addAll(c);
                    }

                    @Override
                    public boolean retainAll(java.util.Collection<?> c) {
                        return delegate.entrySet().retainAll(c);
                    }

                    @Override
                    public boolean removeAll(java.util.Collection<?> c) {
                        return delegate.entrySet().removeAll(c);
                    }

                    @Override
                    public void clear() {
                        delegate.clear();
                    }
                };
            }
        }
        // Inject the throwing map via reflection
        Field importMapField = ModelInterpretationContext.class.getDeclaredField("importMap");
        importMapField.setAccessible(true);
        importMapField.set(mic, new ThrowingMap());
        // The copy constructor of HashMap will iterate over entrySet, causing the RuntimeException
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> mic.getImportMapCopy(), "RuntimeException from the custom map should be propagated");
        assertEquals("Iterator failure", thrown.getMessage());
    }
}
