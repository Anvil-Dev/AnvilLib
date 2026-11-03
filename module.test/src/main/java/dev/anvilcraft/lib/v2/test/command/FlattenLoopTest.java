package dev.anvilcraft.lib.v2.test.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.lib.v2.util.FlattenLoop;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link FlattenLoop} 的游戏内回归。
 *
 * <p>逐项验证四个不变量：产出顺序必须是最靠前的维度位于最外层、元素数量必须等于各维尺寸之积、
 * 任一维度为空时不得产出任何元素、数组重载与列表重载必须给出相同结果。
 * 覆盖 2 到 7 维的全部重载；任一项失败即抛出断言错误并把失败项逐条回显到命令输出。
 */
public final class FlattenLoopTest {
    private static final Logger LOGGER = LogUtils.getLogger();

    private FlattenLoopTest() {
    }

    /**
     * 执行全部断言并返回失败项，供命令与 GameTest 共用同一套判定。
     */
    public static Result check() {
        List<String> failures = new ArrayList<>();
        int checks = 0;
        checks += FlattenLoopTest.checkOrder(failures);
        checks += FlattenLoopTest.checkCardinality(failures);
        checks += FlattenLoopTest.checkEmptyDimensions(failures);
        checks += FlattenLoopTest.checkOverloads(failures);
        return new Result(checks, failures);
    }

    public static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Result result = FlattenLoopTest.check();
        if (result.failures().isEmpty()) {
            FlattenLoopTest.success(source, "PASS " + result.checks() + " checks");
            LOGGER.info("FLATTEN_LOOP_TEST ALL PASS: {} checks", result.checks());
            return result.checks();
        }
        FlattenLoopTest.failure(source, result.failures().size() + " of " + result.checks() + " checks failed");
        for (String failure : result.failures()) {
            FlattenLoopTest.failure(source, failure);
            LOGGER.error("FLATTEN_LOOP_TEST FAILED: {}", failure);
        }
        return 0;
    }

    /**
     * 一次断言的结论，{@code failures} 为空表示全部通过。
     */
    public record Result(int checks, List<String> failures) {
    }

    private static int checkOrder(List<String> failures) {
        int checks = 0;
        List<String> actual2 = new ArrayList<>();
        for (FlattenLoop.Objs2<String, String> pair : FlattenLoop.loop(List.of("a", "b"), List.of("1", "2"))) {
            actual2.add(pair.t() + pair.u());
        }
        checks++;
        if (!actual2.equals(List.of("a1", "a2", "b1", "b2"))) {
            failures.add("2 维顺序应为 a1,a2,b1,b2，实际为 " + actual2);
        }

        List<String> actual3 = new ArrayList<>();
        for (FlattenLoop.Objs3<String, String, String> triple : FlattenLoop.loop(
            List.of("a", "b"), List.of("1", "2"), List.of("x", "y")
        )) {
            actual3.add(triple.t() + triple.u() + triple.v());
        }
        checks++;
        if (!actual3.equals(List.of("a1x", "a1y", "a2x", "a2y", "b1x", "b1y", "b2x", "b2y"))) {
            failures.add("3 维顺序应为最靠前的维度在最外层，实际为 " + actual3);
        }

        List<String> actual4 = new ArrayList<>();
        for (FlattenLoop.Objs4<String, String, String, String> quad : FlattenLoop.loop(
            List.of("a", "b"), List.of("1"), List.of("x"), List.of("p", "q")
        )) {
            actual4.add(quad.t() + quad.u() + quad.v() + quad.w());
        }
        checks++;
        if (!actual4.equals(List.of("a1xp", "a1xq", "b1xp", "b1xq"))) {
            failures.add("4 维顺序应为 a1xp,a1xq,b1xp,b1xq，实际为 " + actual4);
        }
        return checks;
    }

    private static int checkCardinality(List<String> failures) {
        int checks = 0;
        List<String> one = List.of("a");
        List<String> two = List.of("a", "b");
        List<String> three = List.of("a", "b", "c");

        checks++;
        int count5 = 0;
        for (FlattenLoop.Objs5<String, String, String, String, String> ignored
            : FlattenLoop.loop(one, two, three, one, two)) {
            count5++;
        }
        if (count5 != 12) {
            failures.add("5 维元素数量应为 12，实际为 " + count5);
        }

        checks++;
        int count6 = 0;
        for (FlattenLoop.Objs6<String, String, String, String, String, String> ignored
            : FlattenLoop.loop(one, two, three, one, two, three)) {
            count6++;
        }
        if (count6 != 36) {
            failures.add("6 维元素数量应为 36，实际为 " + count6);
        }

        checks++;
        int count7 = 0;
        for (FlattenLoop.Objs7<String, String, String, String, String, String, String> ignored
            : FlattenLoop.loop(one, two, three, one, two, three, two)) {
            count7++;
        }
        if (count7 != 72) {
            failures.add("7 维元素数量应为 72，实际为 " + count7);
        }
        return checks;
    }

    private static int checkEmptyDimensions(List<String> failures) {
        int checks = 0;
        List<String> empty = List.of();
        List<String> one = List.of("a");

        checks++;
        if (FlattenLoop.loop(empty, one).iterator().hasNext()) {
            failures.add("2 维中任一维为空时不得产出元素");
        }

        checks++;
        if (FlattenLoop.loop(one, empty, one).iterator().hasNext()) {
            failures.add("3 维中任一维为空时不得产出元素");
        }

        checks++;
        if (FlattenLoop.loop(new String[0], new String[]{"a"}).iterator().hasNext()) {
            failures.add("数组重载在空维度时不得产出元素");
        }

        checks++;
        if (FlattenLoop.loop(one, one, one, one, one, one, empty).iterator().hasNext()) {
            failures.add("7 维中任一维为空时不得产出元素");
        }
        return checks;
    }

    private static int checkOverloads(List<String> failures) {
        int checks = 0;
        String[] arrayA = {"a", "b"};
        String[] arrayB = {"1", "2"};
        String[] arrayC = {"x", "y"};

        checks++;
        List<String> lists = new ArrayList<>();
        for (FlattenLoop.Objs2<String, String> pair : FlattenLoop.loop(Arrays.asList(arrayA), Arrays.asList(arrayB))) {
            lists.add(pair.t() + pair.u());
        }
        List<String> arrays = new ArrayList<>();
        for (FlattenLoop.Objs2<String, String> pair : FlattenLoop.loop(arrayA, arrayB)) {
            arrays.add(pair.t() + pair.u());
        }
        if (!lists.equals(arrays)) {
            failures.add("2 维数组重载与列表重载结果不一致：数组=" + arrays + "，列表=" + lists);
        }

        checks++;
        List<String> listTriples = new ArrayList<>();
        for (FlattenLoop.Objs3<String, String, String> triple
            : FlattenLoop.loop(Arrays.asList(arrayA), Arrays.asList(arrayB), Arrays.asList(arrayC))) {
            listTriples.add(triple.t() + triple.u() + triple.v());
        }
        List<String> arrayTriples = new ArrayList<>();
        for (FlattenLoop.Objs3<String, String, String> triple : FlattenLoop.loop(arrayA, arrayB, arrayC)) {
            arrayTriples.add(triple.t() + triple.u() + triple.v());
        }
        if (!listTriples.equals(arrayTriples)) {
            failures.add("3 维数组重载与列表重载结果不一致：数组=" + arrayTriples + "，列表=" + listTriples);
        }

        checks++;
        List<String> expectedTriples = List.of("a1x", "a1y", "a2x", "a2y", "b1x", "b1y", "b2x", "b2y");
        if (!listTriples.equals(expectedTriples)) {
            failures.add("3 维顺序应是第 3 维变化最快，期望 " + expectedTriples + "，实际为 " + listTriples);
        }

        checks++;
        List<String> arraySevens = new ArrayList<>();
        for (FlattenLoop.Objs7<String, String, String, String, String, String, String> seven
            : FlattenLoop.loop(arrayA, arrayB, arrayC, arrayA, arrayB, arrayC, arrayB)) {
            arraySevens.add(String.join("", seven.t(), seven.u(), seven.v(), seven.w(), seven.x(), seven.y(), seven.z()));
        }
        List<String> expectedFirstTwo = List.of("a1xa1x1", "a1xa1x2");
        if (arraySevens.size() != 128 || !arraySevens.subList(0, 2).equals(expectedFirstTwo)) {
            failures.add("7 维数组重载应产出 128 项且前两项为 " + expectedFirstTwo + "，实际为 "
                + arraySevens.subList(0, Math.min(2, arraySevens.size())) + "，共 " + arraySevens.size() + " 项");
        }
        return checks;
    }

    private static void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[FlattenLoop] " + message), false);
    }

    private static void failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal("[FlattenLoop] " + message));
    }
}
