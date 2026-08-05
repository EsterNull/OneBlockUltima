package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import java.util.UUID;

import static org.junit.Assert.*;

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
        Bootstrap.register();
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(
            TileEntityOneBlockGenerator.class, "oneblockultima:generator");
    }

    private TileEntityOneBlockGenerator newGenerator()
    {
        return new TileEntityOneBlockGenerator();
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
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("selectedSetId", "nether");
        nbt.setBoolean("disableFluidGeneration", true);
        nbt.setBoolean("disableMobGeneration", true);
        nbt.setBoolean("disableChestGeneration", false);
        nbt.setBoolean("disableSaplingGeneration", true);
        nbt.setUniqueId("ownerId", OWNER);
        nbt.setBoolean("placedByPlayer", true);

        net.minecraft.nbt.NBTTagList levelsTag = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound levelTag = new NBTTagCompound();
        levelTag.setString("setId", "nether");
        levelTag.setInteger("level", 2);
        levelsTag.appendTag(levelTag);
        nbt.setTag("setLevels", levelsTag);

        net.minecraft.nbt.NBTTagList membersTag = new net.minecraft.nbt.NBTTagList();
        membersTag.appendTag(new net.minecraft.nbt.NBTTagString(MEMBER.toString()));
        nbt.setTag("memberIds", membersTag);

        TileEntityOneBlockGenerator loaded = newGenerator();
        loaded.readFromNBT(nbt);

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

        NBTTagCompound nbt = gen.writeToNBT(new NBTTagCompound());

        TileEntityOneBlockGenerator loaded = newGenerator();
        loaded.readFromNBT(nbt);
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
        NBTTagCompound nbt = gen.writeToNBT(new NBTTagCompound());
        TileEntityOneBlockGenerator loaded = newGenerator();
        loaded.readFromNBT(nbt);
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
        NBTTagCompound nbt = gen.writeToNBT(new NBTTagCompound());
        TileEntityOneBlockGenerator loaded = newGenerator();
        loaded.readFromNBT(nbt);
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
}
