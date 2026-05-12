package slimeknights.tconstruct.port1211.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Phase 0 smoke test that proves the gameTestServer run wiring is healthy end-to-end:
 * NeoForge discovers the {@link GameTestHolder}, the {@code tconstruct} namespace is enabled,
 * the structure template loads, and the test exits successfully. Real coverage replaces this
 * as Phase 1+ ports actual subsystems.
 */
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SmokeTest {

    static final String NAMESPACE = "tconstruct";

    private SmokeTest() {
    }

    @GameTest(template = "smoke_test")
    public static void smokeTest(GameTestHelper helper) {
        helper.succeed();
    }
}
