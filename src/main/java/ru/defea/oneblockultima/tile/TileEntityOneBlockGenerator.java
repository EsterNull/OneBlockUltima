package ru.defea.oneblockultima.tile;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.util.BlockUtil;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    public void onLoad()
    {
        super.onLoad();
        if (world != null && !world.isRemote)
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
        OneBlockUltima.getLogger().info("[BreakDebug] Non-player cooldown check for generator {}: active={}, lastTick={}, currentTick={}, canProcess={}", pos, nonPlayerBreakCooldownActive, lastNonPlayerBreakTick, worldTick, canProcess);
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
        if (world != null && !world.isRemote)
        {
            int cooldownTicks = Math.max(1, ModSettings.get().getNonPlayerBreakCooldownTicks());
            world.scheduleUpdate(pos, ModBlocks.ONE_BLOCK_GENERATOR, cooldownTicks);
            OneBlockUltima.getLogger().info("[BreakDebug] Scheduled delayed generation for generator {} in {} ticks", pos, cooldownTicks);
        }
        markDirty();
    }

    public void tryGenerateBlock()
    {
        if (world != null && !world.isRemote && isNonPlayerBreakCooldownActive(world.getTotalWorldTime()))
        {
            OneBlockUltima.getLogger().info("[BreakDebug] Skipping generation for generator {} because non-player cooldown is active", pos);
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

        BlockSetConfig.SetLevelDefinition levelDefinition = set.getLevelClamped(level);
        if (levelDefinition == null)
        {
            OneBlockUltima.getLogger().info("[Generator] Level definition is null for level={}", level);
            return;
        }

        // Case block generation: place a physical case block in the world instead of dropping the item
        ModSettings caseSettings = ModSettings.get();
        if (set.hasCaseEntries()
                && caseSettings.getCaseDropPercent() > 0.0D
                && world.rand.nextDouble() * 100.0D < caseSettings.getCaseDropPercent())
        {
            BlockPos casePos = pos.up();
            if (BlockUtil.canReplaceForGeneration(world, casePos))
            {
                GeneratedBlockRegistry caseRegistry = GeneratedBlockRegistry.get(world);
                if (caseRegistry.isGenerated(casePos))
                {
                    caseRegistry.remove(casePos);
                }
                world.setBlockState(casePos, ModBlocks.CASE_BLOCK.getDefaultState(), 3);
                caseRegistry.markGenerated(casePos, pos, selectedSetId, 0, level, "oneblockultima:case_block", 0);
                OneBlockUltima.getLogger().info("[Generator] Placed case block at {}", casePos);
                if (world != null && !world.isRemote)
                {
                    nonPlayerBreakCooldownActive = false;
                    lastNonPlayerBreakTick = Long.MIN_VALUE;
                }
                return;
            }
        }

        BlockSetConfig.BlockEntryDefinition entry = pickGenerationEntry(levelDefinition);
        if (entry == null)
        {
            OneBlockUltima.getLogger().info("[Generator] pickRandom returned null");
            return;
        }

        OneBlockUltima.getLogger().info("[Generator] Trying to generate entry registry={} meta={} chance={}", entry.registry, entry.meta, entry.getChance());
        IBlockState state = BlockUtil.toState(entry);
        OneBlockUltima.getLogger().info("[Generator] BlockUtil.toState returned {}", state == null ? "null" : state.getBlock().getRegistryName());
        if (state == null)
        {
            OneBlockUltima.getLogger().info("[Generator] Attempting item->block fallback for registry={}", entry.registry);
        }
            if (state == null)
            {
                // Try to resolve entry as an item that corresponds to a placeable block (carrots, wheat, reeds etc.)
                try
                {
                    Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation(entry.registry));
                    OneBlockUltima.getLogger().info("[Generator] Fallback item lookup for registry={} -> item={}", entry.registry, item == null ? "null" : item.getRegistryName());
                    Block resolvedBlock = null;
                    if (item instanceof ItemBlock)
                    {
                        resolvedBlock = ((ItemBlock) item).getBlock();
                    }
                    else if (item != null)
                    {
                        try {
                            resolvedBlock = ForgeRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation(entry.registry));
                        } catch (Exception ignored) { }

                        if (resolvedBlock == null)
                        {
                            try { resolvedBlock = ForgeRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation(entry.registry + "s")); } catch (Exception ignored) { }
                        }
                    }

                    if (resolvedBlock != null)
                    {
                        try { state = resolvedBlock.getStateFromMeta(entry.meta); } catch (Exception ex) { state = resolvedBlock.getDefaultState(); }
                        OneBlockUltima.getLogger().info("[Generator] Fallback resolved block={}", state.getBlock().getRegistryName());
                    }
                }
                catch (Exception ex)
                {
                    OneBlockUltima.getLogger().error("[Generator] Exception during item->block fallback for {}", entry.registry, ex);
                }

                if (state == null)
                {
                    OneBlockUltima.getLogger().info("[Generator] Could not resolve placeable block for registry={}", entry.registry);
                    return;
                }
            }

        BlockPos targetPos = pos.up();

        // Instead of spawning an item, try to place the block
        if (state.getBlock() == Blocks.AIR)
        {
            OneBlockUltima.getLogger().info("[Generator] State is null or AIR for registry={}, trying to place as block anyway", entry.registry);

            // Try to find the block through various ways
            Block resolvedBlock = null;

            // 1. Try via ItemBlock
            try {
                Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation(entry.registry));
                if (item instanceof ItemBlock) {
                    resolvedBlock = ((ItemBlock) item).getBlock();
                    OneBlockUltima.getLogger().info("[Generator] Found block via ItemBlock: {}", resolvedBlock.getRegistryName());
                }
            } catch (Exception ex) {
                OneBlockUltima.getLogger().error("[Generator] Error getting ItemBlock", ex);
            }

            // 2. If not found, try via BlockUtil (with updated Forestry handling)
            if (resolvedBlock == null || resolvedBlock == Blocks.AIR) {
                Block tempBlock = BlockUtil.toState(entry) != null ? Objects.requireNonNull(BlockUtil.toState(entry)).getBlock() : null;
                if (tempBlock != null && tempBlock != Blocks.AIR) {
                    resolvedBlock = tempBlock;
                    OneBlockUltima.getLogger().info("[Generator] Found block via BlockUtil: {}", resolvedBlock.getRegistryName());
                }
            }

            // 3. If still not found, try a direct registry lookup
            if (resolvedBlock == null || resolvedBlock == Blocks.AIR) {
                try {
                    resolvedBlock = ForgeRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation(entry.registry));
                    OneBlockUltima.getLogger().info("[Generator] Found block via direct registry lookup: {}", resolvedBlock == null ? "null" : resolvedBlock.getRegistryName());
                } catch (Exception ex) {
                    OneBlockUltima.getLogger().error("[Generator] Error in direct registry lookup", ex);
                }
            }

            // If a block was found - place it
            if (resolvedBlock != null && resolvedBlock != Blocks.AIR) {
                try {
                    // Get the block state
                    IBlockState newState;
                    try {
                        newState = resolvedBlock.getStateFromMeta(entry.meta);
                        if (newState.getBlock() == Blocks.AIR) {
                            newState = resolvedBlock.getDefaultState();
                        }
                    } catch (Exception ex) {
                        newState = resolvedBlock.getDefaultState();
                    }

                    if (newState.getBlock() != Blocks.AIR) {
                        OneBlockUltima.getLogger().info("[Generator] Placing block: {} at {}", newState.getBlock().getRegistryName(), targetPos);

                        // Place the block with NBT (add obuGenerated)
                        NBTTagCompound genNbt = ensureObuGenerated(entry.nbtTags);
                        BlockUtil.placeBlockWithNBT(world, targetPos, newState, genNbt);

                        // Mark as generated
                        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
                        registry.markGenerated(targetPos, pos, selectedSetId, (int) Math.round(BlockPriceConfig.get().getPrice(entry.registry)), level, entry.registry, entry.meta);

                        return; // Block placed successfully
                    }
                } catch (Exception ex) {
                    OneBlockUltima.getLogger().error("[Generator] Failed to place block", ex);
                }
            }

            // If nothing worked - spawn as an item (fallback)
            OneBlockUltima.getLogger().warn("[Generator] Could not place as block, spawning as item fallback for {}", entry.registry);
            try {
                Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation(entry.registry));
                if (item != null) {
                    net.minecraft.item.ItemStack itemStack = new net.minecraft.item.ItemStack(item, 1, entry.meta);
                    if (entry.nbtTags != null && !entry.nbtTags.hasNoTags()) {
                        itemStack.setTagCompound(entry.nbtTags.copy());
                    }
                    // Add obuGenerated to the fallback item
                    if (itemStack.getTagCompound() == null) {
                        itemStack.setTagCompound(new NBTTagCompound());
                    }
                    itemStack.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);

                    net.minecraft.entity.item.EntityItem entityItem = new net.minecraft.entity.item.EntityItem(
                            world, targetPos.getX(), targetPos.getY(), targetPos.getZ(), itemStack
                    );
                    world.spawnEntity(entityItem);

                    GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
                    registry.markGenerated(targetPos, pos, selectedSetId, (int) Math.round(BlockPriceConfig.get().getPrice(entry.registry)), level, entry.registry, entry.meta);
                }
            } catch (Exception ex) {
                OneBlockUltima.getLogger().error("[Generator] Failed to spawn item fallback", ex);
            }

            OneBlockUltima.getLogger().info("[Generator] Retrying generation after item spawn");
            tryGenerateBlock();

            return;
        }

        OneBlockUltima.getLogger().info("[Generator] Target position={}, current block={}", targetPos, world.getBlockState(targetPos).getBlock().getRegistryName());
        if (!BlockUtil.canReplaceForGeneration(world, targetPos))
        {
            OneBlockUltima.getLogger().info("[Generator] Cannot replace target position={}", targetPos);
            return;
        }

        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
        if (registry.isGenerated(targetPos))
        {
            registry.remove(targetPos);
        }

        OneBlockUltima.getLogger().info("[Generator] Placing state={} at {}", state.getBlock().getRegistryName(), targetPos);
        if (entry.nbtTags != null && !entry.nbtTags.hasNoTags())
        {
            OneBlockUltima.getLogger().info("[Generator] Placing block with NBT tags at {}: {}", targetPos, entry.nbtTags);
        }
        // Place the block and apply NBT tags simultaneously (add obuGenerated)
        NBTTagCompound genNbt2 = ensureObuGenerated(entry.nbtTags);
        BlockUtil.placeBlockWithNBT(world, targetPos, state, genNbt2);
        OneBlockUltima.getLogger().info("[Generator] After place block at {}, now={}", targetPos, world.getBlockState(targetPos).getBlock().getRegistryName());
        registry.markGenerated(targetPos, pos, selectedSetId, (int) Math.round(BlockPriceConfig.get().getPrice(entry.registry)), level, entry.registry, entry.meta);
        if (world != null && !world.isRemote)
        {
            nonPlayerBreakCooldownActive = false;
            lastNonPlayerBreakTick = Long.MIN_VALUE;
            OneBlockUltima.getLogger().info("[BreakDebug] Cleared non-player cooldown for generator {} after successful generation", pos);
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

        int roll = world.rand.nextInt(cachedWeightedChance);
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
        NBTTagCompound result = nbtTags != null ? nbtTags.copy() : new NBTTagCompound();
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
        OneBlockUltima.getLogger().debug("TileEntity setSelectedSetId: {} at pos {}", selectedSetId, pos);
        this.selectedSetId = selectedSetId;
        markDirty();

        if (world != null && !world.isRemote)
        {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            OneBlockUltima.getLogger().debug("TileEntity notifyBlockUpdate sent");
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

        if (world == null || world.isRemote)
        {
            return false;
        }

        for (TileEntity otherTile : world.loadedTileEntityList)
        {
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

                if (world != null && !world.isRemote)
                {
                    for (TileEntity otherTile : world.loadedTileEntityList)
                    {
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
    @Nonnull
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setString("selectedSetId", getSelectedSetId());
        compound.setBoolean("disableFluidGeneration", disableFluidGeneration);
        compound.setBoolean("disableMobGeneration", disableMobGeneration);
        compound.setBoolean("disableChestGeneration", disableChestGeneration);
        compound.setBoolean("disableSaplingGeneration", disableSaplingGeneration);
        if (ownerId != null)
        {
            compound.setUniqueId("ownerId", ownerId);
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
        return compound;
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
            inviteTag.setUniqueId("targetPlayerId", invite.targetPlayerId);
            inviteTag.setUniqueId("senderPlayerId", invite.senderPlayerId);
            inviteTag.setInteger("ticksLeft", invite.ticksLeft);
            invitesTag.appendTag(inviteTag);
        }
        return invitesTag;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        selectedSetId = compound.getString("selectedSetId");
        disableFluidGeneration = compound.getBoolean("disableFluidGeneration");
        disableMobGeneration = compound.getBoolean("disableMobGeneration");
        disableChestGeneration = compound.getBoolean("disableChestGeneration");
        disableSaplingGeneration = compound.getBoolean("disableSaplingGeneration");
        if (compound.hasUniqueId("ownerId"))
        {
            ownerId = compound.getUniqueId("ownerId");
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
                if (inviteTag.hasUniqueId("targetPlayerId") && inviteTag.hasUniqueId("senderPlayerId"))
                {
                    pendingInvites.add(new PendingInvite(
                            inviteTag.getUniqueId("targetPlayerId"),
                            inviteTag.getUniqueId("senderPlayerId"),
                            inviteTag.getInteger("ticksLeft")
                    ));
                }
            }
        }
    }

    @Override
    @Nonnull
    public NBTTagCompound getUpdateTag()
    {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(@Nonnull NetworkManager net, SPacketUpdateTileEntity pkt)
    {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(@Nonnull NBTTagCompound tag)
    {
        readFromNBT(tag);
    }
}