package dev.anvilcraft.lib.v2.recipe.util;

/**
 * 优先级接口，用于定义具有优先级的对象
 * 实现该接口的类可以通过 getPriority 方法获取优先级值
 * 优先级值越小表示优先级越高
 */
public interface IPrioritized extends Comparable<IPrioritized> {
    /**
     * 获取优先级值，默认为1
     *
     * @return 优先级值
     */
    default int priority() {
        return 1;
    }

    /**
     * 比较两个优先级对象
     *
     * @param o 要比较的对象
     * @return 比较结果
     */
    default int compareTo(IPrioritized o) {
        if (this.equals(o)) return 0;
        int compared = Integer.compare(this.priority(), o.priority());
        return compared == 0 ? 1 : -compared;
    }
}
