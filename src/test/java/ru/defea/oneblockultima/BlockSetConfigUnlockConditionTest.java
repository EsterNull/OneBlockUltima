package ru.defea.oneblockultima;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.capability.OneBlockPlayerData;
import ru.defea.oneblockultima.command.CommandAcceptGeneratorInvite;
import ru.defea.oneblockultima.command.CommandDeclineGeneratorInvite;
import ru.defea.oneblockultima.command.CommandInviteGeneratorMember;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.event.ModEvents;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.util.BlockUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class BlockSetConfigUnlockConditionTest
{
    private static final double DELTA = 0.0001;

    @BeforeClass
    public static void initMinecraftBootstrap()
    {
        Bootstrap.register();
    }

    @Test
    public void reloadFallsBackToDefaultSetsWhenConfigFileContainsNoSets() throws Exception
    {
        Path tempDir = Files.createTempDirectory("oneblockultima-test");
        File configDir = tempDir.toFile();
        File configFile = new File(configDir, "oneblockultima/blocksets.json");
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
    public void replacesBedrockAndEndPortalFrameWhenPlacedAboveGenerator()
    {
        assertEquals(ModBlocks.CUSTOM_BEDROCK.getDefaultState(),
                BlockUtil.getReplacementStateForGeneratorPlacement(Blocks.BEDROCK.getDefaultState(), ModBlocks.ONE_BLOCK_GENERATOR.getDefaultState()));

        assertEquals(ModBlocks.CUSTOM_PORTAL_FRAME.getDefaultState(),
                BlockUtil.getReplacementStateForGeneratorPlacement(Blocks.END_PORTAL_FRAME.getDefaultState(), ModBlocks.ONE_BLOCK_GENERATOR.getDefaultState()));
    }

    @Test
    public void inviteCommandsExposeExpectedMetadata()
    {
        CommandInviteGeneratorMember inviteCommand = new CommandInviteGeneratorMember();
        CommandAcceptGeneratorInvite acceptCommand = new CommandAcceptGeneratorInvite();
        CommandDeclineGeneratorInvite declineCommand = new CommandDeclineGeneratorInvite();

        assertEquals("inviteGeneratorMember", inviteCommand.getName());
        assertEquals("/inviteGeneratorMember <playerName>", inviteCommand.getUsage(null));
        assertEquals(0, inviteCommand.getRequiredPermissionLevel());

        assertEquals("acceptGeneratorInvite", acceptCommand.getName());
        assertEquals("/acceptGeneratorInvite", acceptCommand.getUsage(null));
        assertEquals(0, acceptCommand.getRequiredPermissionLevel());

        assertEquals("declineGeneratorInvite", declineCommand.getName());
        assertEquals("/declineGeneratorInvite", declineCommand.getUsage(null));
        assertEquals(0, declineCommand.getRequiredPermissionLevel());
    }

    @Test
    public void acceptingInviteAddsMemberAndClearsPendingInvite()
    {
        TileEntityOneBlockGenerator generator = new TileEntityOneBlockGenerator();
        java.util.UUID ownerId = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
        java.util.UUID invitedPlayerId = java.util.UUID.fromString("22222222-2222-2222-2222-222222222222");
        generator.setOwnerId(ownerId);
        generator.addPendingInvite(invitedPlayerId, ownerId, 1200);

        assertTrue(generator.acceptInvite(invitedPlayerId));
        assertTrue(generator.hasAccess(invitedPlayerId));
        assertFalse(generator.getPendingInvites().stream().anyMatch(invite -> invite.targetPlayerId.equals(invitedPlayerId)));
    }

    @Test
    public void freeGeneratorCannotBeClaimedByPlayerAlreadyAttachedToAnotherGenerator()
    {
        TileEntityOneBlockGenerator freeGenerator = new TileEntityOneBlockGenerator();
        TileEntityOneBlockGenerator otherGenerator = new TileEntityOneBlockGenerator();
        java.util.UUID playerId = java.util.UUID.fromString("33333333-3333-3333-3333-333333333333");
        otherGenerator.setOwnerId(playerId);

        freeGenerator.setWorld(null);
        otherGenerator.setWorld(null);

        assertFalse(freeGenerator.tryAssignOwnerIfEligible(playerId));
        assertTrue(freeGenerator.isFree());
    }

    @Test
    public void ensureOwnershipKeepsAlreadyOwnedGeneratorSelectable()
    {
        TileEntityOneBlockGenerator generator = new TileEntityOneBlockGenerator();
        java.util.UUID playerId = java.util.UUID.fromString("66666666-6666-6666-6666-666666666666");
        generator.setOwnerId(playerId);

        assertTrue(generator.ensureOwnership(playerId));
    }

    @Test
    public void duplicateAccessDeniedMessagesAreSuppressedForSameInteraction()
    {
        java.util.UUID playerId = java.util.UUID.fromString("55555555-5555-5555-5555-555555555555");
        BlockPos pos = new BlockPos(10, 64, 10);

        assertTrue(ModEvents.trySendAccessDeniedMessage(playerId, pos, 100L));
        assertFalse(ModEvents.trySendAccessDeniedMessage(playerId, pos, 100L));
        assertTrue(ModEvents.trySendAccessDeniedMessage(playerId, pos, 101L));
    }

    @Test
    public void placingGeneratorAssignsOwnerForPlacerWithoutBreakingExistingGeneratorAccess()
    {
        java.util.UUID playerId = java.util.UUID.fromString("44444444-4444-4444-4444-444444444444");
        TileEntityOneBlockGenerator oldGenerator = new TileEntityOneBlockGenerator();
        oldGenerator.setOwnerId(playerId);

        TileEntityOneBlockGenerator newGenerator = new TileEntityOneBlockGenerator();
        assertTrue(newGenerator.assignOwnerForPlacement(playerId));
        assertTrue(newGenerator.hasAccess(playerId));
        assertTrue(oldGenerator.hasAccess(playerId));
    }
}
