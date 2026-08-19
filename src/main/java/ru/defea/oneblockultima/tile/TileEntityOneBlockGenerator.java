package ru.defea.oneblockultima.tile;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.util.BlockUtil;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;

import java.util.*;

import static ru.defea.oneblockultima.Constants.NBT_OBU_GENERATED;

public class TileEntityOneBlockGenerator extends TileEntity
{
    private static final Set<TileEntityOneBlockGenerator> ACTIVE_GENERATORS = Collections.synchronizedSet(new HashSet<>());

    private String selectedSetId;
    private UUID ownerId;
    private final List<UUID> memberIds = new ArrayList<>();
    private final List<PendingInvite> pendingInvites = new ArrayList<>();
    private boolean placedByPlayer = false;
    private boolean disableFluidGeneration = false;
    private boolean disableMobGeneration = false;
    private boolean disableChestGeneration = false;
    private boolean disableSaplingGeneration = false;
    private final Map<String, Integer> setLevels = new HashMap<>();
    private long lastNonPlayerBreakTick = Long.MIN_VALUE;
    private boolean nonPlayerBreakCooldownActive = false;

    public TileEntityOneBlockGenerator()
    {
    }

    @Override
    public void validate()
    {
        super.validate();
        if (worldObj != null && !worldObj.isRemote)
        {
            ACTIVE_GENERATORS.add(this);
        }
    }

    @Override
    public void onChunkUnload()
    {
        ACTIVE_GENERATORS.remove(this);
        super.onChunkUnload();
    }

    @Override
    public void invalidate()
    {
        ACTIVE_GENERATORS.remove(this);
        super.invalidate();
    }

    public static Set<TileEntityOneBlockGenerator> getActiveGenerators()
    {
        return ACTIVE_GENERATORS;
    }

    public boolean canProcessNonPlayerBreak(long worldTick)
    {
        boolean active = isNonPlayerBreakCooldownActive(worldTick);
        boolean canProcess = !active;
        OneBlockUltima.getLogger().info("[BreakDebug] Non-player cooldown check for generator at ({}, {}, {}): active={}, lastTick={}, currentTick={}, canProcess={}", xCoord, yCoord, zCoord, nonPlayerBreakCooldownActive, lastNonPlayerBreakTick, worldTick, canProcess);
        return canProcess;
    }

    public boolean isNonPlayerBreakCooldownActive(long worldTick)
    {
        if (lastNonPlayerBreakTick == Long.MIN_VALUE)
        {
            nonPlayerBreakCooldownActive = false;
            return false;
        }

        boolean active = worldTick - lastNonPlayerBreakTick < Math.max(0, ModSettings.get().getNonPlayerBreakCooldownTicks());
        if (!active)
        {
            nonPlayerBreakCooldownActive = false;
            lastNonPlayerBreakTick = Long.MIN_VALUE;
        }
        return active;
    }

    public void markNonPlayerBreak(long worldTick)
    {
        lastNonPlayerBreakTick = worldTick;
        nonPlayerBreakCooldownActive = true;
        if (worldObj != null && !worldObj.isRemote)
        {
            int cooldownTicks = Math.max(1, ModSettings.get().getNonPlayerBreakCooldownTicks());
            worldObj.scheduleBlockUpdate(xCoord, yCoord, zCoord, ModBlocks.ONE_BLOCK_GENERATOR, cooldownTicks);
            OneBlockUltima.getLogger().info("[BreakDebug] Scheduled delayed generation for generator at ({}, {}, {}) in {} ticks", xCoord, yCoord, zCoord, cooldownTicks);
        }
        markDirty();
    }

    /**
     * Regenerates the slot block even when a liquid currently occupies the slot.
     * Called when fluid-generation toggles or the selected set change, so a fluid
     * sitting in the slot is replaced right away instead of waiting for a break.
     */
    public void tryRegenerateSlotBlock()
    {
        if (worldObj == null || worldObj.isRemote)
        {
            return;
        }

        int targetX = xCoord;
        int targetY = yCoord + 1;
        int targetZ = zCoord;
        Block current = worldObj.getBlock(targetX, targetY, targetZ);
        if (current != null && current != Blocks.air && !current.getMaterial().isLiquid())
        {
            return;
        }

        tryGenerateBlock();
    }

    public void tryGenerateBlock()
    {
        if (worldObj != null && !worldObj.isRemote && isNonPlayerBreakCooldownActive(worldObj.getTotalWorldTime()))
        {
            OneBlockUltima.getLogger().info("[BreakDebug] Skipping generation for generator at ({}, {}, {}) because non-player cooldown is active", xCoord, yCoord, zCoord);
            return;
        }

        if (selectedSetId == null || selectedSetId.isEmpty())
        {
            selectedSetId = BlockSetConfig.get().getDefaultSetId();
        }

        BlockSetConfig.BlockSetDefinition set = BlockSetConfig.get().getSet(selectedSetId);
        if (set == null)
        {
            selectedSetId = BlockSetConfig.get().getDefaultSetId();
            set = BlockSetConfig.get().getSet(selectedSetId);
            if (set == null)
            {
                OneBlockUltima.getLogger().info("[Generator] No set found even after trying default");
                return;
            }
        }

        int level = resolveGenerationLevel();
        OneBlockUltima.getLogger().info("[Generator] Resolved level: {} for setId: {}", level, selectedSetId);
        if (level <= 0)
        {
            OneBlockUltima.getLogger().info("[Generator] Level is {}, resetting to default set", level);
            selectedSetId = BlockSetConfig.get().getDefaultSetId();
            set = BlockSetConfig.get().getSet(selectedSetId);
            if (set == null)
            {
                return;
            }
            level = resolveGenerationLevel();
        }

        BlockSetConfig.SetLevelDefinition levelDefinition = set.getLevel(level);
        if (levelDefinition == null)
        {
            OneBlockUltima.getLogger().info("[Generator] Level definition is null for level={}", level);
            return;
        }

        BlockSetConfig.BlockEntryDefinition entry = pickGenerationEntry(levelDefinition);
        if (entry == null)
        {
            OneBlockUltima.getLogger().info("[Generator] pickRandom returned null");
            return;
        }

        OneBlockUltima.getLogger().info("[Generator] Trying to generate entry registry={} meta={} chance={}", entry.registry, entry.meta, entry.getChance());
        Block block = BlockUtil.resolveBlock(entry);
        OneBlockUltima.getLogger().info("[Generator] BlockUtil.resolveBlock returned {}", BlockUtil.getRegistryString(block));
        if (block == null)
        {
            OneBlockUltima.getLogger().info("[Generator] Attempting item->block fallback for registry={}", entry.registry);
        }
            if (block == null)
            {
                // Try to resolve entry as an item that corresponds to a placeable block (carrots, wheat, reeds etc.)
                try
                {
                    Item item = (Item) Item.itemRegistry.getObject(entry.registry);
                    OneBlockUltima.getLogger().info("[Generator] Fallback item lookup for registry={} -> item={}", entry.registry, BlockUtil.getRegistryString(item));
                    Block resolvedBlock = null;
                    if (item instanceof ItemBlock)
                    {
                        resolvedBlock = ((ItemBlock) item).blockInstance;
                    }
                    else if (item != null)
                    {
                        try
                        {
                            resolvedBlock = (Block) Block.blockRegistry.getObject(entry.registry);
                        } catch (Exception ignored) { }

                        if (resolvedBlock == null)
                        {
                            try { resolvedBlock = (Block) Block.blockRegistry.getObject(entry.registry + "s"); } catch (Exception ignored) { }
                        }

                        if (resolvedBlock != null)
                        {
                            block = resolvedBlock;
                            OneBlockUltima.getLogger().info("[Generator] Fallback resolved block={}", BlockUtil.getRegistryString(block));
                        }
                    }
                }
                catch (Exception ex)
                {
                    OneBlockUltima.getLogger().error("[Generator] Exception during item->block fallback for {}", entry.registry, ex);
                }

                if (block == null)
                {
                    OneBlockUltima.getLogger().info("[Generator] Could not resolve placeable block for registry={}", entry.registry);
                    return;
                }
            }

        int targetX = xCoord;
        int targetY = yCoord + 1;
        int targetZ = zCoord;

        // Instead of spawning an item, try to place the block
        if (block == Blocks.air)
        {
            OneBlockUltima.getLogger().info("[Generator] Block is AIR for registry={}, trying to place as block anyway", entry.registry);

            // Try to find the block through various ways
            Block resolvedBlock = null;

            // 1. Try via ItemBlock
            try {
                Item item = (Item) Item.itemRegistry.getObject(entry.registry);
                if (item instanceof ItemBlock) {
                    resolvedBlock = ((ItemBlock) item).blockInstance;
                    OneBlockUltima.getLogger().info("[Generator] Found block via ItemBlock: {}", BlockUtil.getRegistryString(resolvedBlock));
                }
            } catch (Exception ex) {
                OneBlockUltima.getLogger().error("[Generator] Error getting ItemBlock", ex);
            }

            // 2. If not found, try via BlockUtil (with updated Forestry handling)
            if (resolvedBlock == null || resolvedBlock == Blocks.air) {
                Block tempBlock = BlockUtil.resolveBlock(entry);
                if (tempBlock != null && tempBlock != Blocks.air) {
                    resolvedBlock = tempBlock;
                    OneBlockUltima.getLogger().info("[Generator] Found block via BlockUtil: {}", BlockUtil.getRegistryString(resolvedBlock));
                }
            }

            // 3. If still not found, try a direct registry lookup
            if (resolvedBlock == null || resolvedBlock == Blocks.air) {
                try {
                    resolvedBlock = (Block) Block.blockRegistry.getObject(entry.registry);
                    OneBlockUltima.getLogger().info("[Generator] Found block via direct registry lookup: {}", BlockUtil.getRegistryString(resolvedBlock));
                } catch (Exception ex) {
                    OneBlockUltima.getLogger().error("[Generator] Error in direct registry lookup", ex);
                }
            }

            // If a block was found - place it
            if (resolvedBlock != null && resolvedBlock != Blocks.air) {
                try {
                    int placeMeta = BlockUtil.resolveMeta(entry, resolvedBlock);
                    if (resolvedBlock != Blocks.air) {
                        OneBlockUltima.getLogger().info("[Generator] Placing block: {} at ({}, {}, {})", BlockUtil.getRegistryString(resolvedBlock), targetX, targetY, targetZ);

                        // Place the block with NBT (add obuGenerated)
                        NBTTagCompound genNbt = ensureObuGenerated(entry.nbtTags);
                        BlockUtil.placeBlockWithNBT(worldObj, targetX, targetY, targetZ, resolvedBlock, placeMeta, genNbt);

                        // Mark as generated
                        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(worldObj);
                        registry.markGenerated(targetX, targetY, targetZ, xCoord, yCoord, zCoord, selectedSetId, (int) Math.round(BlockPriceConfig.get().getPrice(entry.registry)), level, entry.registry, entry.meta);

                        return; // Block placed successfully
                    }
                } catch (Exception ex) {
                    OneBlockUltima.getLogger().error("[Generator] Failed to place block", ex);
                }
            }

            // If nothing worked - spawn as an item (fallback)
            OneBlockUltima.getLogger().warn("[Generator] Could not place as block, spawning as item fallback for {}", entry.registry);
            try {
                Item item = (Item) Item.itemRegistry.getObject(entry.registry);
                if (item != null) {
                    ItemStack itemStack = new ItemStack(item, 1, entry.meta);
                    if (entry.nbtTags != null && !entry.nbtTags.hasNoTags()) {
                        itemStack.setTagCompound((NBTTagCompound) entry.nbtTags.copy());
                    }
                    // Add obuGenerated to the fallback item
                    if (itemStack.getTagCompound() == null) {
                        itemStack.setTagCompound(new NBTTagCompound());
                    }
                    itemStack.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);

                    net.minecraft.entity.item.EntityItem entityItem = new net.minecraft.entity.item.EntityItem(
                            worldObj, targetX + 0.5D, targetY + 0.5D, targetZ + 0.5D, itemStack
                    );
                    worldObj.spawnEntityInWorld(entityItem);

                    GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(worldObj);
                    registry.markGenerated(targetX, targetY, targetZ, xCoord, yCoord, zCoord, selectedSetId, (int) Math.round(BlockPriceConfig.get().getPrice(entry.registry)), level, entry.registry, entry.meta);
                }
            } catch (Exception ex) {
                OneBlockUltima.getLogger().error("[Generator] Failed to spawn item fallback", ex);
            }

            OneBlockUltima.getLogger().info("[Generator] Retrying generation after item spawn");
            tryGenerateBlock();

            return;
        }

        OneBlockUltima.getLogger().info("[Generator] Target position=({}, {}, {}), current block={}", targetX, targetY, targetZ, BlockUtil.getRegistryString(worldObj.getBlock(targetX, targetY, targetZ)));
        if (!BlockUtil.canReplaceForGeneration(worldObj, targetX, targetY, targetZ))
        {
            OneBlockUltima.getLogger().info("[Generator] Cannot replace target position=({}, {}, {})", targetX, targetY, targetZ);
            return;
        }

        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(worldObj);
        if (registry.isGenerated(targetX, targetY, targetZ))
        {
            registry.remove(targetX, targetY, targetZ);
        }

        OneBlockUltima.getLogger().info("[Generator] Placing block={} at ({}, {}, {})", BlockUtil.getRegistryString(block), targetX, targetY, targetZ);
        if (entry.nbtTags != null && !entry.nbtTags.hasNoTags())
        {
            OneBlockUltima.getLogger().info("[Generator] Placing block with NBT tags at ({}, {}, {}): {}", targetX, targetY, targetZ, entry.nbtTags);
        }
        // Place the block and apply NBT tags simultaneously (add obuGenerated)
        NBTTagCompound genNbt2 = ensureObuGenerated(entry.nbtTags);
        int placeMeta = BlockUtil.resolveMeta(entry, block);
        BlockUtil.placeBlockWithNBT(worldObj, targetX, targetY, targetZ, block, placeMeta, genNbt2);
        OneBlockUltima.getLogger().info("[Generator] After place block at ({}, {}, {}), now={}", targetX, targetY, targetZ, BlockUtil.getRegistryString(worldObj.getBlock(targetX, targetY, targetZ)));
        registry.markGenerated(targetX, targetY, targetZ, xCoord, yCoord, zCoord, selectedSetId, (int) Math.round(BlockPriceConfig.get().getPrice(entry.registry)), level, entry.registry, entry.meta);
        if (worldObj != null && !worldObj.isRemote)
        {
            nonPlayerBreakCooldownActive = false;
            lastNonPlayerBreakTick = Long.MIN_VALUE;
            OneBlockUltima.getLogger().info("[BreakDebug] Cleared non-player cooldown for generator at ({}, {}, {}) after successful generation", xCoord, yCoord, zCoord);
        }
    }

    private BlockSetConfig.SetLevelDefinition cachedWeightedLevel;
    private int cachedDisableMask = -1;
    private BlockSetConfig.BlockEntryDefinition[] cachedWeightedEntries;
    private int cachedWeightedChance = -1;

    private BlockSetConfig.BlockEntryDefinition pickGenerationEntry(BlockSetConfig.SetLevelDefinition levelDefinition)
    {
        if (levelDefinition == null || levelDefinition.blocks == null || levelDefinition.blocks.isEmpty())
        {
            return null;
        }

        int disableMask = (disableFluidGeneration ? 1 : 0) | (disableChestGeneration ? 2 : 0) | (disableSaplingGeneration ? 4 : 0);
        if (levelDefinition != cachedWeightedLevel || disableMask != cachedDisableMask)
        {
            List<BlockSetConfig.BlockEntryDefinition> allowed = new ArrayList<>();
            int totalChance = 0;
            for (BlockSetConfig.BlockEntryDefinition candidate : levelDefinition.blocks)
            {
                if (!isAllowedGenerationEntry(candidate))
                {
                    continue;
                }
                totalChance += candidate.getChance();
                allowed.add(candidate);
            }
            cachedWeightedLevel = levelDefinition;
            cachedDisableMask = disableMask;
            cachedWeightedChance = totalChance;
            cachedWeightedEntries = allowed.toArray(new BlockSetConfig.BlockEntryDefinition[0]);
        }

        if (cachedWeightedEntries.length == 0 || cachedWeightedChance <= 0)
        {
            return null;
        }

        int roll = worldObj.rand.nextInt(cachedWeightedChance);
        int current = 0;
        for (BlockSetConfig.BlockEntryDefinition candidate : cachedWeightedEntries)
        {
            current += candidate.getChance();
            if (roll < current)
            {
                return candidate;
            }
        }
        return cachedWeightedEntries[cachedWeightedEntries.length - 1];
    }

    private static NBTTagCompound ensureObuGenerated(NBTTagCompound nbtTags)
    {
        NBTTagCompound result = nbtTags != null ? (NBTTagCompound) nbtTags.copy() : new NBTTagCompound();
        result.setBoolean(NBT_OBU_GENERATED, true);
        return result;
    }

    private boolean isAllowedGenerationEntry(BlockSetConfig.BlockEntryDefinition entry)
    {
        if (entry == null)
        {
            return false;
        }
        if (disableFluidGeneration && entry.isFluid())
        {
            return false;
        }
        if (disableChestGeneration && entry.isChestEntry())
        {
            return false;
        }
        return !disableSaplingGeneration || !entry.isSaplingEntry();
    }

    private int resolveGenerationLevel()
    {
        return getSetLevel(selectedSetId);
    }

    public int getSetLevel(String setId)
    {
        if (setId == null)
        {
            return 0;
        }

        Integer level = setLevels.get(setId);
        if (level != null)
        {
            return level;
        }

        BlockSetConfig config = BlockSetConfig.get();
        if (config == null)
        {
            return 0;
        }

        String defaultSetId = config.getDefaultSetId();
        return setId.equals(defaultSetId) ? 1 : 0;
    }

    public boolean upgradeSet(String setId, int cost, int maxLevel)
    {
        if (setId == null || cost < 0 || maxLevel <= 0)
        {
            return false;
        }

        int currentLevel = getSetLevel(setId);
        if (currentLevel >= maxLevel)
        {
            return false;
        }

        setLevels.put(setId, currentLevel + 1);
        markDirty();
        return true;
    }

    public String getSelectedSetId()
    {
        if (selectedSetId == null || selectedSetId.isEmpty())
        {
            selectedSetId = BlockSetConfig.get().getDefaultSetId();
        }
        return selectedSetId;
    }

    public void setDisableFluidGeneration(boolean disableFluidGeneration)
    {
        this.disableFluidGeneration = disableFluidGeneration;
        markDirty();
    }

    public boolean isDisableFluidGeneration()
    {
        return disableFluidGeneration;
    }

    public void setDisableMobGeneration(boolean disableMobGeneration)
    {
        this.disableMobGeneration = disableMobGeneration;
        markDirty();
    }

    public boolean isDisableMobGeneration()
    {
        return disableMobGeneration;
    }

    public void setDisableChestGeneration(boolean disableChestGeneration)
    {
        this.disableChestGeneration = disableChestGeneration;
        markDirty();
    }

    public boolean isDisableChestGeneration()
    {
        return disableChestGeneration;
    }

    public void setDisableSaplingGeneration(boolean disableSaplingGeneration)
    {
        this.disableSaplingGeneration = disableSaplingGeneration;
        markDirty();
    }

    public boolean isDisableSaplingGeneration()
    {
        return disableSaplingGeneration;
    }

    public void setSelectedSetId(String selectedSetId)
    {
        OneBlockUltima.getLogger().debug("TileEntity setSelectedSetId: {} at pos ({}, {}, {})", selectedSetId, xCoord, yCoord, zCoord);
        this.selectedSetId = selectedSetId;
        markDirty();

        if (worldObj != null && !worldObj.isRemote)
        {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            OneBlockUltima.getLogger().debug("TileEntity markBlockForUpdate sent");
        }
    }

    public boolean hasAccess(EntityPlayer player)
    {
        return player != null && hasAccess(player.getUniqueID());
    }

    public boolean hasAccess(UUID playerId)
    {
        if (playerId == null)
        {
            return false;
        }

        if (ownerId != null && ownerId.equals(playerId))
        {
            return true;
        }

        return memberIds.contains(playerId);
    }

    public boolean isOwner(EntityPlayer player)
    {
        return player != null && ownerId != null && ownerId.equals(player.getUniqueID());
    }

    public int getMemberCount()
    {
        return (ownerId != null ? 1 : 0) + memberIds.size();
    }

    public boolean isMemberLimitReached()
    {
        int limit = ModSettings.get().getMaxGeneratorMembers();
        return limit > 0 && getMemberCount() >= limit;
    }

    public boolean isFree()
    {
        return ownerId == null && memberIds.isEmpty();
    }

    public boolean canBeClaimedBy(UUID playerId)
    {
        if (playerId == null || !isFree())
        {
            return false;
        }

        if (worldObj == null || worldObj.isRemote)
        {
            return false;
        }

        for (Object otherTileObj : worldObj.loadedTileEntityList)
        {
            TileEntity otherTile = (TileEntity) otherTileObj;
            if (otherTile == this || !(otherTile instanceof TileEntityOneBlockGenerator))
            {
                continue;
            }

            TileEntityOneBlockGenerator otherGenerator = (TileEntityOneBlockGenerator) otherTile;
            if (otherGenerator.hasAccess(playerId))
            {
                return false;
            }
        }

        return true;
    }

    public boolean tryAssignOwnerIfEligible(UUID playerId)
    {
        if (!canBeClaimedBy(playerId))
        {
            return false;
        }

        setOwnerId(playerId);
        return true;
    }

    public boolean ensureOwnership(UUID playerId)
    {
        if (playerId == null)
        {
            return false;
        }

        if (!isFree())
        {
            return true;
        }

        return tryAssignOwnerIfEligible(playerId);
    }

    public boolean assignOwnerForPlacement(UUID playerId)
    {
        if (playerId == null)
        {
            return false;
        }

        if (ownerId == null && memberIds.isEmpty())
        {
            setOwnerId(playerId);
            return true;
        }

        return ownerId != null && ownerId.equals(playerId);
    }

    public boolean isPlacedByPlayer()
    {
        return placedByPlayer;
    }

    public void setPlacedByPlayer(boolean placedByPlayer)
    {
        this.placedByPlayer = placedByPlayer;
        markDirty();
    }

    public void addMember(UUID memberId)
    {
        if (memberId == null || memberIds.contains(memberId))
        {
            return;
        }

        memberIds.add(memberId);
        markDirty();
    }

    public void addPendingInvite(UUID targetPlayerId, UUID senderPlayerId, int ticks)
    {
        if (targetPlayerId == null || senderPlayerId == null)
        {
            return;
        }

        pendingInvites.removeIf(invite -> invite.targetPlayerId.equals(targetPlayerId));

        pendingInvites.add(new PendingInvite(targetPlayerId, senderPlayerId, ticks));
        markDirty();
    }

    public boolean acceptInvite(UUID targetPlayerId)
    {
        if (targetPlayerId == null)
        {
            return false;
        }

        Iterator<PendingInvite> iterator = pendingInvites.iterator();
        while (iterator.hasNext())
        {
            PendingInvite invite = iterator.next();
            if (invite.targetPlayerId.equals(targetPlayerId))
            {
                iterator.remove();

                if (!hasAccess(targetPlayerId) && isMemberLimitReached())
                {
                    return false;
                }

                if (worldObj != null && !worldObj.isRemote)
                {
                    for (Object otherTileObj : worldObj.loadedTileEntityList)
                    {
                        TileEntity otherTile = (TileEntity) otherTileObj;
                        if (otherTile == this || !(otherTile instanceof TileEntityOneBlockGenerator))
                        {
                            continue;
                        }

                        TileEntityOneBlockGenerator otherGenerator = (TileEntityOneBlockGenerator) otherTile;
                        if (otherGenerator.hasAccess(targetPlayerId))
                        {
                            otherGenerator.removeAccess(targetPlayerId);
                        }
                    }
                }

                if (!hasAccess(targetPlayerId))
                {
                    addMember(targetPlayerId);
                }

                if (ownerId == null)
                {
                    ownerId = invite.senderPlayerId;
                }

                markDirty();
                return true;
            }
        }
        return false;
    }

    public boolean declineInvite(UUID targetPlayerId)
    {
        if (targetPlayerId == null)
        {
            return false;
        }

        Iterator<PendingInvite> iterator = pendingInvites.iterator();
        while (iterator.hasNext())
        {
            PendingInvite invite = iterator.next();
            if (invite.targetPlayerId.equals(targetPlayerId))
            {
                iterator.remove();
                markDirty();
                return true;
            }
        }
        return false;
    }

    public void tickInvites()
    {
        if (pendingInvites.isEmpty())
        {
            return;
        }

        Iterator<PendingInvite> iterator = pendingInvites.iterator();
        while (iterator.hasNext())
        {
            PendingInvite invite = iterator.next();
            invite.ticksLeft--;
            if (invite.ticksLeft <= 0)
            {
                iterator.remove();
            }
        }
    }

    public List<PendingInvite> getPendingInvites()
    {
        return pendingInvites;
    }

    public void removeAccess(UUID playerId)
    {
        if (playerId == null)
        {
            return;
        }

        if (ownerId != null && ownerId.equals(playerId))
        {
            ownerId = null;
        }

        memberIds.remove(playerId);
        if (ownerId == null && memberIds.isEmpty())
        {
            placedByPlayer = false;
        }
        markDirty();
    }

    public void clearOwnershipAndMembers()
    {
        ownerId = null;
        memberIds.clear();
        placedByPlayer = false;
        markDirty();
    }

    public static class PendingInvite
    {
        public final UUID targetPlayerId;
        public final UUID senderPlayerId;
        public int ticksLeft;

        public PendingInvite(UUID targetPlayerId, UUID senderPlayerId, int ticksLeft)
        {
            this.targetPlayerId = targetPlayerId;
            this.senderPlayerId = senderPlayerId;
            this.ticksLeft = ticksLeft;
        }
    }

    public UUID getOwnerId()
    {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId)
    {
        this.ownerId = ownerId;
        this.memberIds.clear();
        markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setString("selectedSetId", getSelectedSetId());
        compound.setBoolean("disableFluidGeneration", disableFluidGeneration);
        compound.setBoolean("disableMobGeneration", disableMobGeneration);
        compound.setBoolean("disableChestGeneration", disableChestGeneration);
        compound.setBoolean("disableSaplingGeneration", disableSaplingGeneration);
        if (ownerId != null)
        {
            compound.setString("ownerId", ownerId.toString());
        }
        compound.setBoolean("placedByPlayer", placedByPlayer);
        compound.setLong("lastNonPlayerBreakTick", lastNonPlayerBreakTick);

        NBTTagList levelsTag = new NBTTagList();
        for (Map.Entry<String, Integer> entry : setLevels.entrySet())
        {
            if (entry.getKey() == null || entry.getValue() == null)
            {
                continue;
            }

            NBTTagCompound levelTag = new NBTTagCompound();
            levelTag.setString("setId", entry.getKey());
            levelTag.setInteger("level", entry.getValue());
            levelsTag.appendTag(levelTag);
        }
        compound.setTag("setLevels", levelsTag);

        NBTTagList membersTag = new NBTTagList();
        for (UUID memberId : memberIds)
        {
            if (memberId != null)
            {
                membersTag.appendTag(new NBTTagString(memberId.toString()));
            }
        }
        compound.setTag("memberIds", membersTag);

        NBTTagList invitesTag = getInvitesTag();
        compound.setTag("pendingInvites", invitesTag);
    }

    private NBTTagList getInvitesTag() {
        NBTTagList invitesTag = new NBTTagList();
        for (PendingInvite invite : pendingInvites)
        {
            if (invite == null || invite.targetPlayerId == null || invite.senderPlayerId == null)
            {
                continue;
            }

            NBTTagCompound inviteTag = new NBTTagCompound();
            inviteTag.setString("targetPlayerId", invite.targetPlayerId.toString());
            inviteTag.setString("senderPlayerId", invite.senderPlayerId.toString());
            inviteTag.setInteger("ticksLeft", invite.ticksLeft);
            invitesTag.appendTag(inviteTag);
        }
        return invitesTag;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        selectedSetId = compound.getString("selectedSetId");
        disableFluidGeneration = compound.getBoolean("disableFluidGeneration");
        disableMobGeneration = compound.getBoolean("disableMobGeneration");
        disableChestGeneration = compound.getBoolean("disableChestGeneration");
        disableSaplingGeneration = compound.getBoolean("disableSaplingGeneration");
        if (compound.hasKey("ownerId"))
        {
            try
            {
                ownerId = UUID.fromString(compound.getString("ownerId"));
            }
            catch (IllegalArgumentException ignored)
            {
                ownerId = null;
            }
        }
        placedByPlayer = compound.getBoolean("placedByPlayer");
        lastNonPlayerBreakTick = compound.hasKey("lastNonPlayerBreakTick") ? compound.getLong("lastNonPlayerBreakTick") : Long.MIN_VALUE;

        setLevels.clear();
        if (compound.hasKey("setLevels", Constants.NBT.TAG_LIST))
        {
            NBTTagList levelsTag = compound.getTagList("setLevels", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < levelsTag.tagCount(); i++)
            {
                NBTTagCompound levelTag = levelsTag.getCompoundTagAt(i);
                if (levelTag.hasKey("setId") && levelTag.hasKey("level"))
                {
                    setLevels.put(levelTag.getString("setId"), levelTag.getInteger("level"));
                }
            }
        }

        memberIds.clear();
        if (compound.hasKey("memberIds", Constants.NBT.TAG_LIST))
        {
            NBTTagList membersTag = compound.getTagList("memberIds", Constants.NBT.TAG_STRING);
            for (int i = 0; i < membersTag.tagCount(); i++)
            {
                try
                {
                    memberIds.add(UUID.fromString(membersTag.getStringTagAt(i)));
                }
                catch (IllegalArgumentException ignored)
                {
                }
            }
        }

        pendingInvites.clear();
        if (compound.hasKey("pendingInvites", Constants.NBT.TAG_LIST))
        {
            NBTTagList invitesTag = compound.getTagList("pendingInvites", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < invitesTag.tagCount(); i++)
            {
                NBTTagCompound inviteTag = invitesTag.getCompoundTagAt(i);
                if (inviteTag.hasKey("targetPlayerId") && inviteTag.hasKey("senderPlayerId"))
                {
                    try
                    {
                        pendingInvites.add(new PendingInvite(
                                UUID.fromString(inviteTag.getString("targetPlayerId")),
                                UUID.fromString(inviteTag.getString("senderPlayerId")),
                                inviteTag.getInteger("ticksLeft")
                        ));
                    }
                    catch (IllegalArgumentException ignored)
                    {
                    }
                }
            }
        }
    }

    private NBTTagCompound getUpdateNbt()
    {
        NBTTagCompound compound = new NBTTagCompound();
        writeToNBT(compound);
        return compound;
    }

    @Override
    public Packet getDescriptionPacket()
    {
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, getUpdateNbt());
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
    {
        readFromNBT(pkt.getNbtCompound());
    }
}
