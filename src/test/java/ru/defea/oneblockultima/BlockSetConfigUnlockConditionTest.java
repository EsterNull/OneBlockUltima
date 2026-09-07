package ru.defea.oneblockultima;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import ru.defea.oneblockultima.block.BlockCustomBreakable;
import ru.defea.oneblockultima.block.BlockFluidBarrier;
import ru.defea.oneblockultima.block.BlockOneBlockGenerator;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.capability.OneBlockPlayerData;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.testutil.TestBootstrap;
import ru.defea.oneblockultima.util.BlockUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class BlockSetConfigUnlockConditionTest
{
    private static final double DELTA = 0.0001;

    @BeforeClass
    public static void initMinecraftBootstrap()
    {
        TestBootstrap.prepare();
        TestBootstrap.unfreezeBlockRegistry();
        if (ModBlocks.ONE_BLOCK_GENERATOR == null)
        {
            ModBlocks.ONE_BLOCK_GENERATOR = new BlockOneBlockGenerator();
        }
        if (ModBlocks.FLUID_BARRIER == null)
        {
            ModBlocks.FLUID_BARRIER = new BlockFluidBarrier();
        }
        if (ModBlocks.CUSTOM_BREAKABLE_POOL.isEmpty())
        {
            for (int i = 0; i < 3; i++)
            {
                ModBlocks.CUSTOM_BREAKABLE_POOL.add(new BlockCustomBreakable("test_breakable_" + i));
            }
        }
    }

    @Test
    public void reloadFallsBackToDefaultSetsWhenConfigFileContainsNoSets() throws Exception
    {
        Path tempDir = Files.createTempDirectory("oneblockultima-test");
        File configDir = tempDir.toFile();
        File configFile = new File(configDir, "oneblockultima/blocksets.json");
        //noinspection ResultOfMethodCallIgnored
        configFile.getParentFile().mkdirs();
        Files.write(configFile.toPath(), "{\"sets\":[],\"settings\":{}}".getBytes(StandardCharsets.UTF_8));

        BlockSetConfig.load(configDir);

        assertNotNull(BlockSetConfig.get());
        assertFalse(BlockSetConfig.get().getSets().isEmpty());
        assertEquals("classic", BlockSetConfig.get().getDefaultSetId());
    }

    @Test
    public void unlockConditionsCanRequireBrokenBlocksOrSetLevels()
    {
        BlockSetConfig.BlockSetDefinition set = new BlockSetConfig.BlockSetDefinition();
        set.unlockConditions = new BlockSetConfig.UnlockConditionGroup();
        set.unlockConditions.mode = "any";
        set.unlockConditions.conditions.add(new BlockSetConfig.UnlockConditionDefinition());
        set.unlockConditions.conditions.get(0).type = "broken_blocks_total";
        set.unlockConditions.conditions.get(0).count = 100;

        OneBlockPlayerData data = new OneBlockPlayerData();
        assertFalse(set.hasUnlockRequirementsMet(data));

        data.addBrokenBlocks("classic", 100);
        assertTrue(set.hasUnlockRequirementsMet(data));
    }

    @Test
    public void clonedPlayerDataKeepsCurrencyAndProgress()
    {
        OneBlockPlayerData source = new OneBlockPlayerData();
        source.setCurrency(120);
        source.addBrokenBlocks("classic", 7);
        source.getSetLevels().put("classic", 2);

        OneBlockPlayerData target = new OneBlockPlayerData();
        target.copyFrom(source);

        assertEquals(120, target.getCurrency(), DELTA);
        assertEquals(7, target.getBrokenBlocksCount());
        assertEquals(7, target.getBrokenBlocksCount("classic"));
        assertEquals(2, target.getSetLevel("classic"));
    }

    @Test
    public void replacesUnbreakableBlocksWhenPlacedAboveGenerator()
    {
        BlockState barrier = BlockUtil.getReplacementStateForGeneratorPlacement(Blocks.BARRIER.defaultBlockState(), ModBlocks.ONE_BLOCK_GENERATOR.defaultBlockState());
        assertTrue("barrier must be substituted", barrier.getBlock() instanceof BlockCustomBreakable);
        assertEquals(Blocks.BARRIER, ((BlockCustomBreakable) barrier.getBlock()).getEmulated());

        BlockState bedrock = BlockUtil.getReplacementStateForGeneratorPlacement(Blocks.BEDROCK.defaultBlockState(), ModBlocks.ONE_BLOCK_GENERATOR.defaultBlockState());
        assertTrue("bedrock must be substituted", bedrock.getBlock() instanceof BlockCustomBreakable);
        assertEquals(Blocks.BEDROCK, ((BlockCustomBreakable) bedrock.getBlock()).getEmulated());

        BlockState frame = BlockUtil.getReplacementStateForGeneratorPlacement(Blocks.END_PORTAL_FRAME.defaultBlockState(), ModBlocks.ONE_BLOCK_GENERATOR.defaultBlockState());
        assertTrue("end portal frame must be substituted", frame.getBlock() instanceof BlockCustomBreakable);
        assertEquals(Blocks.END_PORTAL_FRAME, ((BlockCustomBreakable) frame.getBlock()).getEmulated());
    }

    @Test
    public void keepsBreakableBlocksWhenPlacedAboveGenerator()
    {
        assertSame(Blocks.STONE.defaultBlockState(),
                BlockUtil.getReplacementStateForGeneratorPlacement(Blocks.STONE.defaultBlockState(), ModBlocks.ONE_BLOCK_GENERATOR.defaultBlockState()));
    }

    @Test
    public void keepsUnbreakableBlocksWhenNotPlacedAboveGenerator()
    {
        assertSame(Blocks.BARRIER.defaultBlockState(),
                BlockUtil.getReplacementStateForGeneratorPlacement(Blocks.BARRIER.defaultBlockState(), Blocks.STONE.defaultBlockState()));
    }

    @Test
    public void customBreakableIsBreakableAndDropsOriginalBlock()
    {
        BlockState state = BlockUtil.getReplacementStateForGeneratorPlacement(Blocks.BARRIER.defaultBlockState(), ModBlocks.ONE_BLOCK_GENERATOR.defaultBlockState());

        assertTrue("substitute must be breakable", state.getDestroySpeed(null, null) >= 0.0F);
        ServerLevel level = Mockito.mock(ServerLevel.class);
        List<ItemStack> drops = ((BlockCustomBreakable) state.getBlock()).getDrops(state, new LootParams.Builder(level));
        assertEquals(1, drops.size());
        assertEquals(Blocks.BARRIER.asItem(), drops.get(0).getItem());
    }

    @Test
    public void fluidBarrierIsTransparentToExplosions()
    {
        float resistance = ModBlocks.FLUID_BARRIER.getExplosionResistance(null, null, null, null);
        assertEquals("FluidBarrier must not absorb explosion rays", 0.0F, resistance, DELTA);
    }

    @Test
    public void fluidBarrierIsAirLikeForPlacement()
    {
        // ItemFlintAndSteel/ItemFireball require world.isAirBlock to place fire
        assertTrue("FluidBarrier must be treated as air so fire can be placed on its slot",
                ModBlocks.FLUID_BARRIER.defaultBlockState().isAir());
    }

    @Test
    public void fluidBarrierBlocksLiquidsWithEmptyCollisionContext()
    {
        // Liquids (FlowingFluid.canPassThroughWall) and pathfinding query collisions with
        // CollisionContext.empty() (no entity) and must see a full cube so fluids cannot pass.
        VoxelShape shape = ModBlocks.FLUID_BARRIER.defaultBlockState()
                .getCollisionShape(null, BlockPos.ZERO, CollisionContext.empty());
        assertFalse("FluidBarrier must be a full cube for liquids/pathfinding", shape.isEmpty());
        assertEquals("FluidBarrier must cover the full block for liquids",
                Shapes.block().bounds(), shape.bounds());
    }

    @Test
    public void fluidBarrierLetsLivingEntitiesPassThrough()
    {
        Player player = Mockito.mock(Player.class);
        VoxelShape shape = ModBlocks.FLUID_BARRIER.defaultBlockState()
                .getCollisionShape(null, BlockPos.ZERO, CollisionContext.of(player));
        assertTrue("FluidBarrier must not block living entities", shape.isEmpty());
    }

    @Test
    public void fluidBarrierIsTransparentToRaycast()
    {
        // The barrier must not intercept LMB/RMB raycasts: clicks pass through it to the block
        // behind (the generated block above the generator / the generator itself).
        VoxelShape shape = ModBlocks.FLUID_BARRIER.defaultBlockState()
                .getShape(null, BlockPos.ZERO, CollisionContext.empty());
        assertTrue("FluidBarrier must be transparent to sight/raycast (LMB/PKM)", shape.isEmpty());
    }

    @Test
    public void fluidBarrierIsTransparentToInteraction()
    {
        VoxelShape shape = ModBlocks.FLUID_BARRIER.defaultBlockState()
                .getInteractionShape(null, BlockPos.ZERO);
        assertTrue("FluidBarrier must not be an interaction target", shape.isEmpty());
    }

    @Test
    public void fluidBarrierConsumesRightClick()
    {
        Player player = Mockito.mock(Player.class);
        InteractionResult result = ModBlocks.FLUID_BARRIER.useWithoutItem(
                ModBlocks.FLUID_BARRIER.defaultBlockState(), null, BlockPos.ZERO, player,
                new BlockHitResult(new Vec3(0.5D, 0.5D, 0.5D), Direction.UP, BlockPos.ZERO, false));
        assertEquals("FluidBarrier must consume the click so the generator menu cannot open through it",
                InteractionResult.SUCCESS, result);
    }
}