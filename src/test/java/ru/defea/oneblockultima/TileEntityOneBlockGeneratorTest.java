package ru.defea.oneblockultima;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.testutil.TestBootstrap;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Core behavior of {@link TileEntityOneBlockGenerator}: selected set / level bookkeeping,
 * toggle flags, ownership/membership, invites, non-player-break cooldown, NBT round-trip and
 * the static active-generator set iterated by {@code ModEvents.onWorldTick}.
 *
 * <p>The tile entity is constructed headless via
 * {@link TestBootstrap#registerOneBlockTileEntity()}; NBT uses
 * {@code saveAdditional(new CompoundTag(), RegistryAccess.EMPTY)} /
 * {@code loadAdditional(compound, RegistryAccess.EMPTY)} (1.21 inherits these from
 * {@link net.minecraft.world.level.block.entity.BlockEntity}).
 *
 * <p>Forge 1.21 has no {@code onChunkUnload()} on {@code BlockEntity} and the port removed the
 * 1.12 {@code invalidate()} façade, so the active-set lifecycle is only exercised through the
 * port's real invalidation path: {@code setRemoved()}.
 */
public class TileEntityOneBlockGeneratorTest
{
    private static final UUID OWNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MEMBER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID STRANGER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SENDER = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID INVITEE = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @BeforeClass
    public static void setUp()
    {
        TestBootstrap.registerOneBlockTileEntity();
    }

    private TileEntityOneBlockGenerator newGenerator()
    {
        return new TileEntityOneBlockGenerator(BlockPos.ZERO, ModBlocks.ONE_BLOCK_GENERATOR.defaultBlockState());
    }

    @After
    public void clearActiveGenerators()
    {
        TileEntityOneBlockGenerator.getActiveGenerators().clear();
    }

    // 1
    @Test
    public void defaultSetIdFallsBackToConfig()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        String expected = BlockSetConfig.get().getDefaultSetId();
        assertEquals(expected, gen.getSelectedSetId());
    }

    // 2
    @Test
    public void setSelectedSetIdChangesId()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setSelectedSetId("nether");
        assertEquals("nether", gen.getSelectedSetId());
    }

    // 3
    @Test
    public void setSelectedSetIdWithNullUsesDefault()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setSelectedSetId("nether");
        gen.setSelectedSetId(null);
        String expected = BlockSetConfig.get().getDefaultSetId();
        assertEquals(expected, gen.getSelectedSetId());
    }

    // 4
    @Test
    public void setSelectedSetIdWithEmptyUsesDefault()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setSelectedSetId("nether");
        gen.setSelectedSetId("");
        String expected = BlockSetConfig.get().getDefaultSetId();
        assertEquals(expected, gen.getSelectedSetId());
    }

    // 5
    @Test
    public void upgradeSetIncreasesLevel()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        String defaultId = BlockSetConfig.get().getDefaultSetId();
        assertEquals(1, gen.getSetLevel(defaultId));
        assertTrue(gen.upgradeSet(defaultId, 0, 10));
        assertEquals(2, gen.getSetLevel(defaultId));
    }

    // 6
    @Test
    public void upgradeSetFailsAtMaxLevel()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        String defaultId = BlockSetConfig.get().getDefaultSetId();
        assertEquals(1, gen.getSetLevel(defaultId));
        assertFalse(gen.upgradeSet(defaultId, 0, 1));
    }

    // 7
    @Test
    public void upgradeSetFailsWithNegativeCost()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        String defaultId = BlockSetConfig.get().getDefaultSetId();
        assertFalse(gen.upgradeSet(defaultId, -1, 10));
    }

    // 8
    @Test
    public void upgradeSetFailsWithNullSetId()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        assertFalse(gen.upgradeSet(null, 0, 10));
    }

    // 9
    @Test
    public void toggleFlagsDefaultFalse()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        assertFalse(gen.isDisableFluidGeneration());
        assertFalse(gen.isDisableMobGeneration());
        assertFalse(gen.isDisableChestGeneration());
        assertFalse(gen.isDisableSaplingGeneration());
    }

    // 10
    @Test
    public void toggleFlagsCanBeSet()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setDisableFluidGeneration(true);
        gen.setDisableMobGeneration(true);
        gen.setDisableChestGeneration(true);
        gen.setDisableSaplingGeneration(true);
        assertTrue(gen.isDisableFluidGeneration());
        assertTrue(gen.isDisableMobGeneration());
        assertTrue(gen.isDisableChestGeneration());
        assertTrue(gen.isDisableSaplingGeneration());
    }

    // 11
    @Test
    public void hasAccessForOwner()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        assertTrue(gen.hasAccess(OWNER));
    }

    // 12
    @Test
    public void hasAccessForMember()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addMember(MEMBER);
        assertTrue(gen.hasAccess(MEMBER));
    }

    // 13
    @Test
    public void hasAccessRejectsUnknown()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        assertFalse(gen.hasAccess(STRANGER));
    }

    // 14
    @Test
    public void hasAccessRejectsNull()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        assertFalse(gen.hasAccess((UUID) null));
    }

    // 15
    @Test
    public void isFreeWhenNoOwnerNoMembers()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        assertTrue(gen.isFree());
    }

    // 16
    @Test
    public void isNotFreeAfterOwnerSet()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        assertFalse(gen.isFree());
    }

    // 17
    @Test
    public void isNotFreeAfterMemberAdded()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addMember(MEMBER);
        assertFalse(gen.isFree());
    }

    // 18
    @Test
    public void setOwnerIdClearsMembers()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addMember(MEMBER);
        gen.setOwnerId(OWNER);
        assertFalse(gen.hasAccess(MEMBER));
        gen.setOwnerId(STRANGER);
        assertFalse(gen.hasAccess(MEMBER));
    }

    // 19
    @Test
    public void removeAccessRemovesOwner()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.removeAccess(OWNER);
        assertTrue(gen.isFree());
    }

    // 20
    @Test
    public void removeAccessRemovesMember()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addMember(MEMBER);
        gen.removeAccess(MEMBER);
        assertFalse(gen.hasAccess(MEMBER));
    }

    // 21
    @Test
    public void removeAccessNullIsNoOp()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.removeAccess(null);
        assertTrue(gen.hasAccess(OWNER));
    }

    // 22
    @Test
    public void clearOwnershipAndMembersResets()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addMember(MEMBER);
        gen.setPlacedByPlayer(true);
        gen.clearOwnershipAndMembers();
        assertTrue(gen.isFree());
        assertFalse(gen.isPlacedByPlayer());
    }

    // 23
    @Test
    public void addPendingInviteAddsInvite()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.addPendingInvite(INVITEE, SENDER, 1200);
        assertEquals(1, gen.getPendingInvites().size());
    }

    // 24
    @Test
    public void addPendingInviteReplacesExistingForSameTarget()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.addPendingInvite(INVITEE, SENDER, 1200);
        gen.addPendingInvite(INVITEE, SENDER, 2400);
        assertEquals(1, gen.getPendingInvites().size());
    }

    // 25
    @Test
    public void acceptInviteAddsMemberAndReturnsTrue()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addPendingInvite(INVITEE, SENDER, 1200);
        assertTrue(gen.acceptInvite(INVITEE));
        assertTrue(gen.hasAccess(INVITEE));
        assertEquals(0, gen.getPendingInvites().size());
    }

    // 26
    @Test
    public void acceptInviteReturnsFalseWhenNoInvite()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        assertFalse(gen.acceptInvite(INVITEE));
    }

    // 27
    @Test
    public void declineInviteRemovesAndReturnsTrue()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addPendingInvite(INVITEE, SENDER, 1200);
        assertTrue(gen.declineInvite(INVITEE));
        assertEquals(0, gen.getPendingInvites().size());
    }

    // 27a
    @Test
    public void getMemberCountCountsOwnerAndMembers()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        assertEquals(0, gen.getMemberCount());
        gen.setOwnerId(OWNER);
        assertEquals(1, gen.getMemberCount());
        gen.addMember(MEMBER);
        assertEquals(2, gen.getMemberCount());
        gen.addMember(INVITEE);
        assertEquals(3, gen.getMemberCount());
    }

    // 27b
    @Test
    public void memberLimitZeroMeansNoLimit()
    {
        ModSettings.get().setMaxGeneratorMembers(0);
        try
        {
            TileEntityOneBlockGenerator gen = newGenerator();
            gen.setOwnerId(OWNER);
            gen.addMember(MEMBER);
            assertFalse(gen.isMemberLimitReached());
        }
        finally
        {
            ModSettings.get().setMaxGeneratorMembers(0);
        }
    }

    // 27c
    @Test
    public void memberLimitReachedWhenCountEqualsLimit()
    {
        ModSettings.get().setMaxGeneratorMembers(2);
        try
        {
            TileEntityOneBlockGenerator gen = newGenerator();
            gen.setOwnerId(OWNER);
            gen.addMember(MEMBER);
            assertTrue(gen.isMemberLimitReached());
        }
        finally
        {
            ModSettings.get().setMaxGeneratorMembers(0);
        }
    }

    // 27d
    @Test
    public void acceptInviteRejectedWhenMemberLimitReached()
    {
        ModSettings.get().setMaxGeneratorMembers(2);
        try
        {
            TileEntityOneBlockGenerator gen = newGenerator();
            gen.setOwnerId(OWNER);
            gen.addMember(MEMBER);
            gen.addPendingInvite(INVITEE, SENDER, 1200);
            assertFalse(gen.acceptInvite(INVITEE));
            assertFalse(gen.hasAccess(INVITEE));
        }
        finally
        {
            ModSettings.get().setMaxGeneratorMembers(0);
        }
    }

    // 27e
    @Test
    public void acceptInviteAllowedWithinMemberLimit()
    {
        ModSettings.get().setMaxGeneratorMembers(3);
        try
        {
            TileEntityOneBlockGenerator gen = newGenerator();
            gen.setOwnerId(OWNER);
            gen.addMember(MEMBER);
            gen.addPendingInvite(INVITEE, SENDER, 1200);
            assertTrue(gen.acceptInvite(INVITEE));
            assertTrue(gen.hasAccess(INVITEE));
        }
        finally
        {
            ModSettings.get().setMaxGeneratorMembers(0);
        }
    }

    // 28
    @Test
    public void declineInviteReturnsFalseWhenNoInvite()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        assertFalse(gen.declineInvite(INVITEE));
    }

    // 29
    @Test
    public void tickInvitesRemovesExpired()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addPendingInvite(INVITEE, SENDER, 1);
        gen.tickInvites();
        assertEquals(0, gen.getPendingInvites().size());
    }

    // 30
    @Test
    public void tickInvitesDecrementsTicks()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addPendingInvite(INVITEE, SENDER, 10);
        gen.tickInvites();
        assertEquals(1, gen.getPendingInvites().size());
        assertEquals(9, gen.getPendingInvites().get(0).ticksLeft);
    }

    // 31
    @Test
    public void nonPlayerCooldownBasicBehavior()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.markNonPlayerBreak(100);
        assertTrue(gen.isNonPlayerBreakCooldownActive(100));
        assertFalse(gen.isNonPlayerBreakCooldownActive(120));
    }

    // 32
    @Test
    public void nbtReadFromCompound()
    {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("selectedSetId", "nether");
        nbt.putBoolean("disableFluidGeneration", true);
        nbt.putBoolean("disableMobGeneration", true);
        nbt.putBoolean("disableChestGeneration", false);
        nbt.putBoolean("disableSaplingGeneration", true);
        nbt.putUUID("ownerId", OWNER);
        nbt.putBoolean("placedByPlayer", true);

        net.minecraft.nbt.ListTag levelsTag = new net.minecraft.nbt.ListTag();
        CompoundTag levelTag = new CompoundTag();
        levelTag.putString("setId", "nether");
        levelTag.putInt("level", 2);
        levelsTag.add(levelTag);
        nbt.put("setLevels", levelsTag);

        net.minecraft.nbt.ListTag membersTag = new net.minecraft.nbt.ListTag();
        membersTag.add(net.minecraft.nbt.StringTag.valueOf(MEMBER.toString()));
        nbt.put("memberIds", membersTag);

        TileEntityOneBlockGenerator loaded = newGenerator();
        loaded.loadAdditional(nbt, RegistryAccess.EMPTY);

        assertEquals("nether", loaded.getSelectedSetId());
        assertTrue(loaded.isDisableFluidGeneration());
        assertTrue(loaded.isDisableMobGeneration());
        assertFalse(loaded.isDisableChestGeneration());
        assertTrue(loaded.isDisableSaplingGeneration());
        assertEquals(OWNER, loaded.getOwnerId());
        assertTrue(loaded.isPlacedByPlayer());
        assertFalse(loaded.isFree());
        assertTrue(loaded.hasAccess(MEMBER));
        assertEquals(2, loaded.getSetLevel("nether"));
    }

    @Test
    public void nbtWriteAndReadRoundtrip()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);

        CompoundTag nbt = new CompoundTag();
        gen.saveAdditional(nbt, RegistryAccess.EMPTY);
        TileEntityOneBlockGenerator loaded = newGenerator();
        loaded.loadAdditional(nbt, RegistryAccess.EMPTY);
        assertEquals(OWNER, loaded.getOwnerId());
    }

    @Test
    public void toggleFlagsAreIndependent()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setDisableFluidGeneration(true);
        assertFalse(gen.isDisableMobGeneration());
        assertFalse(gen.isDisableChestGeneration());
        assertFalse(gen.isDisableSaplingGeneration());

        gen.setDisableMobGeneration(true);
        assertTrue(gen.isDisableFluidGeneration());
        assertTrue(gen.isDisableMobGeneration());
        assertFalse(gen.isDisableChestGeneration());

        gen.setDisableFluidGeneration(false);
        assertFalse(gen.isDisableFluidGeneration());
        assertTrue(gen.isDisableMobGeneration());
    }

    @Test
    public void toggleFluidOnlyNbtRoundTrip()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setDisableFluidGeneration(true);
        CompoundTag nbt = new CompoundTag();
        gen.saveAdditional(nbt, RegistryAccess.EMPTY);
        TileEntityOneBlockGenerator loaded = newGenerator();
        loaded.loadAdditional(nbt, RegistryAccess.EMPTY);
        assertTrue(loaded.isDisableFluidGeneration());
        assertFalse(loaded.isDisableMobGeneration());
        assertFalse(loaded.isDisableChestGeneration());
        assertFalse(loaded.isDisableSaplingGeneration());
    }

    @Test
    public void toggleMobAndSaplingNbtRoundTrip()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setDisableMobGeneration(true);
        gen.setDisableSaplingGeneration(true);
        CompoundTag nbt = new CompoundTag();
        gen.saveAdditional(nbt, RegistryAccess.EMPTY);
        TileEntityOneBlockGenerator loaded = newGenerator();
        loaded.loadAdditional(nbt, RegistryAccess.EMPTY);
        assertFalse(loaded.isDisableFluidGeneration());
        assertTrue(loaded.isDisableMobGeneration());
        assertFalse(loaded.isDisableChestGeneration());
        assertTrue(loaded.isDisableSaplingGeneration());
    }

    @Test
    public void upgradeSetOnNonDefaultSetWorks()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.upgradeSet("nether", 0, 10);
        assertEquals(1, gen.getSetLevel("nether"));
    }

    @Test
    public void nonPlayerCooldownExpiresAfterCooldownPeriod()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.markNonPlayerBreak(100);
        assertTrue(gen.isNonPlayerBreakCooldownActive(110));
        assertFalse(gen.isNonPlayerBreakCooldownActive(121));
    }

    @Test
    public void nonPlayerCooldownExactlyAtBoundary()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.markNonPlayerBreak(100);
        assertTrue(gen.isNonPlayerBreakCooldownActive(100));
    }

    @Test
    public void memberAccessRevokedAfterOwnerChange()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addMember(MEMBER);
        assertTrue(gen.hasAccess(MEMBER));

        gen.setOwnerId(STRANGER);
        assertFalse(gen.hasAccess(MEMBER));
        assertFalse(gen.hasAccess(OWNER));
        assertTrue(gen.hasAccess(STRANGER));
    }

    @Test
    public void addMultipleMembersAndRemoveOne()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        gen.setOwnerId(OWNER);
        gen.addMember(MEMBER);
        gen.addMember(INVITEE);

        gen.removeAccess(MEMBER);
        assertFalse(gen.hasAccess(MEMBER));
        assertTrue(gen.hasAccess(INVITEE));
        assertTrue(gen.hasAccess(OWNER));
    }

    // ------------------------------------------------------------------
    // active-generator set (the set iterated by ModEvents.onWorldTick)
    // ------------------------------------------------------------------

    @Test
    public void activeGeneratorsSetStartsEmpty()
    {
        assertTrue(TileEntityOneBlockGenerator.getActiveGenerators().isEmpty());
    }

    @Test
    public void getActiveGeneratorsReturnsSharedSet()
    {
        assertSame("getActiveGenerators must expose the single static set",
                TileEntityOneBlockGenerator.getActiveGenerators(),
                TileEntityOneBlockGenerator.getActiveGenerators());
    }

    @Test
    public void setRemovedUnregistersGeneratorFromActiveSet()
    {
        TileEntityOneBlockGenerator gen = newGenerator();
        addToActiveSet(gen);

        assertTrue("the generator must be tracked until it is invalidated",
                TileEntityOneBlockGenerator.getActiveGenerators().contains(gen));

        gen.setRemoved();

        assertFalse("setRemoved (the port's invalidation path) must unregister the generator",
                TileEntityOneBlockGenerator.getActiveGenerators().contains(gen));
    }

    @Test
    public void setRemovedLeavesOtherGeneratorsInActiveSet()
    {
        TileEntityOneBlockGenerator a = newGenerator();
        TileEntityOneBlockGenerator b = newGenerator();
        addToActiveSet(a);
        addToActiveSet(b);

        a.setRemoved();

        Set<TileEntityOneBlockGenerator> active = TileEntityOneBlockGenerator.getActiveGenerators();
        assertFalse("the invalidated generator must be removed", active.contains(a));
        assertTrue("unrelated generators must stay tracked", active.contains(b));
    }

    private static void addToActiveSet(TileEntityOneBlockGenerator generator)
    {
        TileEntityOneBlockGenerator.getActiveGenerators().add(generator);
    }
}