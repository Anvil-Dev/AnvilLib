package dev.anvilcraft.lib.v2.cube.client.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCaptureTest {
    @AfterEach
    void clearSources() throws ReflectiveOperationException { sources().clear(); }

    @Test
    void equalModelsKeepSeparateIdentityEntries() throws ReflectiveOperationException {
        Object first = new EqualModel(), second = new EqualModel();
        var firstSource = new ModelCapture.Source(List.of(), null);
        var secondSource = new ModelCapture.Source(List.of(), null);
        sources().put(first, firstSource);
        sources().put(second, secondSource);

        var snapshot = ModelCapture.take();
        assertEquals(2, snapshot.size());
        assertSame(firstSource, snapshot.get(first));
        assertSame(secondSource, snapshot.get(second));
        assertTrue(sources().isEmpty());
    }

    @Test
    void transientRuntimeModelsCanBeCollectedWithoutAnotherResourceReload() throws Exception {
        var queue = new ReferenceQueue<Object>();
        WeakReference<Object> reference = rememberTransientModel(queue);
        Object collected = null;
        for (int attempt = 0; attempt < 100 && collected == null; attempt++) {
            System.gc();
            collected = queue.remove(20);
        }
        assertNotNull(collected, "捕获表不能作为运行时临时模型的强引用根");
        assertNull(reference.get());
    }

    @Test
    void takingSourcesDetachesTheSnapshot() throws ReflectiveOperationException {
        Object first = new EqualModel(), second = new EqualModel();
        sources().put(first, new ModelCapture.Source(List.of(), null));
        var snapshot = ModelCapture.take();
        sources().put(second, new ModelCapture.Source(List.of(), null));

        assertEquals(1, snapshot.size());
        assertTrue(snapshot.containsKey(first));
        assertTrue(sources().containsKey(second));
    }

    private static WeakReference<Object> rememberTransientModel(ReferenceQueue<Object> queue)
        throws ReflectiveOperationException {
        Object model = new EqualModel();
        sources().put(model, new ModelCapture.Source(List.of(), null));
        return new WeakReference<>(model, queue);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, ModelCapture.Source> sources() throws ReflectiveOperationException {
        Field field = ModelCapture.class.getDeclaredField("SOURCES");
        field.setAccessible(true);
        return (Map<Object, ModelCapture.Source>) field.get(null);
    }

    private static final class EqualModel {
        @Override
        public boolean equals(Object other) { return other instanceof EqualModel; }
        @Override
        public int hashCode() { return 0; }
    }
}
