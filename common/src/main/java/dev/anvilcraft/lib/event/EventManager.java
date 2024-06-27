package dev.anvilcraft.lib.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class EventManager {
    public static final EventManager EVENT_BUS = new EventManager(null);
    private static final Logger LOGGER = LoggerFactory.getLogger("AnvilLib-Event");
    @Nullable
    private final EventManager parent;
    private final Map<Class<?>, List<EventListener<?>>> eventListener = Collections.synchronizedMap(new HashMap<>());

    public EventManager(@Nullable EventManager parent) {
        this.parent = parent;
    }

    public EventManager() {
        this(EventManager.EVENT_BUS);
    }

    /**
     * 注册侦听器
     *
     * @param object 侦听器类
     */
    public void register(@NotNull Object object) {
        if (this.parent != null) this.parent.register(object);
        for (Method method : object.getClass().getMethods()) {
            if (method.getParameterCount() != 1) continue;
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            if (null == annotation) continue;
            Class<?> event = method.getParameterTypes()[0];
            Consumer<?> trigger = (obj) -> {
                try {
                    method.invoke(object, obj);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            };
            List<EventListener<?>> triggers = eventListener.getOrDefault(event, Collections.synchronizedList(new ArrayList<>()));
            triggers.add(new EventListener<>(trigger, annotation.priority()));
            triggers.sort(EventListener::compare);
            eventListener.putIfAbsent(event, triggers);
        }
    }

    /**
     * 侦听事件
     *
     * @param event    事件
     * @param trigger  处理器
     * @param priority 优先级
     * @param <E>      事件
     */
    public <E> void listen(Class<E> event, Consumer<E> trigger, int priority) {
        if (this.parent != null) this.parent.listen(event, trigger, priority);
        List<EventListener<?>> triggers = eventListener.getOrDefault(event, Collections.synchronizedList(new ArrayList<>()));
        triggers.add(new EventListener<>(trigger, priority));
        triggers.sort(EventListener::compare);
        eventListener.putIfAbsent(event, triggers);
    }

    /**
     * 侦听事件
     *
     * @param event   事件
     * @param trigger 处理器
     * @param <E>     事件
     */
    public <E> void listen(Class<E> event, Consumer<E> trigger) {
        this.listen(event, trigger, 100);
    }

    /**
     * 发布事件
     *
     * @param event 事件
     * @param <E>   事件类型
     */
    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    public <E> void post(@NotNull E event) {
        List<Consumer<?>> triggers = new ArrayList<>();
        eventListener.entrySet().stream().filter((k) -> event.getClass().isAssignableFrom(k.getKey())).map(Map.Entry::getValue).forEach(triggers::addAll);
        for (Consumer<?> trigger : triggers) {
            ((Consumer<E>) trigger).accept(event);
            if (event instanceof Cancelable cancelable && cancelable.isCanceled()) return;
        }
    }

    /**
     * 发布事件
     *
     * @param event    事件
     * @param callback 事件回调
     * @param <E>      事件类型
     */
    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    public <E> void post(@NotNull E event, Consumer<E> callback) {
        List<Consumer<?>> triggers = new ArrayList<>();
        eventListener.entrySet().stream().filter((k) -> event.getClass().isAssignableFrom(k.getKey())).map(Map.Entry::getValue).forEach(triggers::addAll);
        for (Consumer<?> trigger : triggers) {
            ((Consumer<E>) trigger).accept(event);
            if (event instanceof Cancelable cancelable && cancelable.isCanceled()) return;
        }
        callback.accept(event);
    }
}
