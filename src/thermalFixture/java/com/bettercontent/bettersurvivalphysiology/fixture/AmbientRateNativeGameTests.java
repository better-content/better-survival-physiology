package com.bettercontent.bettersurvivalphysiology.fixture;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.climate.AmbientRateSeam.AmbientRateModifier;
import com.mojang.authlib.GameProfile;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.Temperature.Trait;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Nonshipping, short native thermal calibration. No clock acceleration or BODY/CORE/RATE setter. */
@GameTestHolder(SystemicSalienceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AmbientRateNativeGameTests {
    private record Reading(int tick, double world, double freeze, double burn, double body,
                           double rateInput, double rateOutput) {}

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty", timeoutTicks = 440)
    public static void harmfulBiomeDriftIsSlowInBothDirections(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        List<PlayerFixture> players = new ArrayList<>();
        try {
            // GameTest structures are placed near min build height; expose players to sky-level climate.
            BlockPos center = h.absolutePos(new BlockPos(0, 140, 0));
            PlayerFixture hot = new PlayerFixture(h, "thermal-hot", center.offset(-96, 0, 0), "minecraft:desert");
            players.add(hot);
            PlayerFixture cold = new PlayerFixture(h, "thermal-cold", center.offset(96, 0, 0), "minecraft:snowy_plains");
            players.add(cold);
            h.runAfterDelay(110, () -> {
                try {
                    Reading hotStart = read(h, hot.player, true);
                    Reading coldStart = read(h, cold.player, false);
                    h.assertTrue(Math.abs(hotStart.body) < 3D && Math.abs(coldStart.body) < 3D,
                            "body moved too far before the native harmful exposure sample");
                    h.runAfterDelay(130, () -> {
                        try {
                            Reading hotAmbient = check(h, hot.player, hotStart, true);
                            Reading coldAmbient = check(h, cold.player, coldStart, false);
                            // The fixture's two block emissions are deliberately bounded and
                            // source-owned. A weak local source must not toggle back to native RATE.
                            hot.player.level().setBlock(hot.player.blockPosition().east(2),
                                    Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                            cold.player.level().setBlock(cold.player.blockPosition().east(2),
                                    Blocks.PACKED_ICE.defaultBlockState(), 3);
                            h.runAfterDelay(110, () -> {
                                try {
                                    checkLocal(h, hot.player, hotAmbient, true);
                                    checkLocal(h, cold.player, coldAmbient, false);
                                    h.succeed();
                                } finally { players.forEach(PlayerFixture::close); }
                            });
                        } catch (Throwable failure) {
                            players.forEach(PlayerFixture::close);
                            throw failure;
                        }
                    });
                } catch (Throwable failure) {
                    players.forEach(PlayerFixture::close);
                    throw failure;
                }
            });
        } catch (Throwable failure) {
            players.forEach(PlayerFixture::close);
            throw failure;
        }
    }

    private static Reading read(GameTestHelper h, ServerPlayer player, boolean hot) {
        List<TempModifier> own = Temperature.getModifiers(player, Trait.RATE).stream()
                .filter(AmbientRateModifier.class::isInstance).toList();
        h.assertTrue(own.size() == 1, "expected one installed ambient RATE, got " + own.size());
        AmbientRateModifier rate = (AmbientRateModifier) own.get(0);
        Reading result = new Reading(player.tickCount, Temperature.get(player, Trait.WORLD),
                Temperature.get(player, Trait.FREEZING_POINT), Temperature.get(player, Trait.BURNING_POINT),
                Temperature.get(player, Trait.BODY), rate.getLastInput(Trait.RATE), rate.getLastOutput(Trait.RATE));
        String biome = player.level().getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.location().toString()).orElse("unknown");
        var biomeData = ConfigSettings.BIOME_TEMPS.get(player.level().registryAccess());
        System.out.printf(Locale.ROOT, "BSP_THERMAL_PROBE side=%s tick=%d pos=%s biome=%s centerBiomeTemp=%.6f world=%.6f bounds=[%.6f,%.6f] body=%.6f rateInput=%.8f rateOutput=%.8f%n",
                hot ? "hot" : "cold", player.tickCount, player.blockPosition(), biome,
                WorldHelper.getBiomeTemperature(player.level(), player.level().getBiome(player.blockPosition())),
                result.world, result.freeze, result.burn, result.body, result.rateInput, result.rateOutput);
        h.assertTrue(biome.equals(hot ? "minecraft:desert" : "minecraft:snowy_plains"), "biome fixture changed player location unexpectedly");
        h.assertTrue(biomeData.get(player.level().getBiome(player.blockPosition())) != null,
                "fixture-only biome override was not loaded");
        h.assertTrue(hot ? result.world > result.burn : result.world < result.freeze,
                "biome fixture was not harmful; no valid native rate comparison");
        h.assertTrue(Double.isFinite(result.rateInput) && Double.isFinite(result.rateOutput)
                && (hot ? result.rateInput > 0 : result.rateInput < 0), "native RATE did not follow harmful WORLD exposure");
        return result;
    }

    private static Reading check(GameTestHelper h, ServerPlayer player, Reading before, boolean hot) {
        Reading after = read(h, player, hot);
        double nativeRate = Math.abs(after.rateInput);
        double scaledRate = Math.abs(after.rateOutput);
        double targetPerTick = 100D / 72_000D;
        double bodyPerTick = Math.abs(after.body - before.body) / (after.tick - before.tick);
        double ratio = scaledRate / nativeRate;
        System.out.printf(Locale.ROOT, "BSP_THERMAL_RESULT side=%s tick=%d ratio=%.8f bodyPerTick=%.8f targetPerTick=%.8f%n",
                hot ? "hot" : "cold", player.tickCount, ratio, bodyPerTick, targetPerTick);
        h.assertTrue(hot ? after.body > before.body : after.body < before.body, "body drift is not harmful");
        h.assertTrue(nativeRate > targetPerTick * 4, "native rate is too small to distinguish attribution");
        h.assertTrue(scaledRate >= targetPerTick * 0.5 && scaledRate <= targetPerTick * 1.5,
                "ambient rate did not approach the 72,000-tick damage-onset target");
        h.assertTrue(bodyPerTick >= targetPerTick * 0.35 && bodyPerTick <= targetPerTick * 2.0,
                "actual body trajectory did not follow the scaled RATE over this short window");
        return after;
    }

    private static void checkLocal(GameTestHelper h, ServerPlayer player, Reading ambient, boolean hot) {
        Reading withSource = read(h, player, hot);
        System.out.printf(Locale.ROOT, "BSP_THERMAL_LOCAL side=%s ambientWorld=%.6f sourceWorld=%.6f ambientRate=%.8f sourceRate=%.8f nativeRate=%.8f%n",
                hot ? "hot" : "cold", ambient.world, withSource.world,
                ambient.rateOutput, withSource.rateOutput, withSource.rateInput);
        h.assertTrue(hot ? withSource.world > ambient.world + 0.01D
                         : withSource.world < ambient.world - 0.01D,
                "fixture block did not produce a measurable local WORLD emission");
        h.assertTrue(Math.abs(withSource.rateOutput) > Math.abs(ambient.rateOutput) * 2D,
                "local source did not increase harmful RATE smoothly above ambient pace");
        h.assertTrue(Math.abs(withSource.rateOutput) < Math.abs(withSource.rateInput) * 0.9D,
                "weak local source switched harmful RATE back to full native speed");
        h.assertTrue(hot ? withSource.body > ambient.body : withSource.body < ambient.body,
                "body did not follow the source-adjusted harmful rate");
    }

    private static final class PlayerFixture implements AutoCloseable {
        final ServerPlayer player;
        final Connection connection;
        final EmbeddedChannel channel;
        PlayerFixture(GameTestHelper h, String name, BlockPos pos, String biome) {
            var server = h.getLevel().getServer();
            player = new ServerPlayer(server, h.getLevel(), new GameProfile(UUID.randomUUID(), name));
            connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            // Cold Sweat samples a biome grid, not just the player's block. Tile
            // a bounded area with vanilla fillbiome; one giant command exceeds
            // the vanilla 32,768-block limit and smaller patches are flaky.
            int changed = 0;
            for (int dx = -60; dx <= 60; dx += 41) for (int dz = -60; dz <= 60; dz += 41) {
                var command = String.format(Locale.ROOT, "fillbiome %d %d %d %d %d %d %s",
                        pos.getX() + dx, pos.getY()-4, pos.getZ() + dz,
                        pos.getX() + Math.min(60, dx+40), pos.getY()+4,
                        pos.getZ() + Math.min(60, dz+40), biome);
                changed += server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withPermission(4), command);
            }
            h.assertTrue(changed > 0, "fixture biome commands changed no cells: " + biome);
            server.getPlayerList().placeNewPlayer(connection, player);
            server.getConnection().getConnections().add(connection);
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            player.setNoGravity(true);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20);
        }
        @Override public void close() {
            player.server.getConnection().getConnections().remove(connection);
            if (player.server.getPlayerList().getPlayers().contains(player)) player.server.getPlayerList().remove(player);
            player.discard();
            channel.finishAndReleaseAll();
        }
    }
}
