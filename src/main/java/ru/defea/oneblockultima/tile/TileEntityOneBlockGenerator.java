package ru.defea.oneblockultima.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

import static net.minecraft.nbt.Tag.TAG_COMPOUND;
import static net.minecraft.nbt.Tag.TAG_LIST;
import static net.minecraft.nbt.Tag.TAG_STRING;
import static ru.defea.oneblockultima.Constants.NBT_OBU_GENERATED;

public class TileEntityOneBlockGenerator extends BlockEntity
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

    public TileEntityOneBlockGenerator(BlockPos pos, BlockState state)
    {
        super(ModTileEntities.ONE_BLOCK_GENERATOR.get(), pos, state);
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if (getLevel() != null && !getLevel().isClientSide())
        {
            ACTIVE_GENERATORS.add(this);
        }
    }

    @Override
    public void setRemoved()
    {
        ACTIVE_GENERATORS.remove(this);
        super.setRemoved();
    }

    public static Set<TileEntityOneBlockGenerator> getActiveGenerators()
    {
        return ACTIVE_GENERATORS;
    }

    private static final int BARRIER_KEEPALIVE_INTERVAL = 4;
    private static final int REGENERATION_INTERVAL = 20;

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityOneBlockGenerator generator)
    {
        int tick = (int) (level.getGameTime() % Math.max(1, BARRIER_KEEPALIVE_INTERVAL * REGENERATION_INTERVAL));
        if (tick % BARRIER_KEEPALIVE_INTERVAL == 0)
        {
            ru.defea.oneblockultima.block.BlockOneBlockGenerator.ensureFluidBarrier(level, pos);
        }

        if (tick % REGENERATION_INTERVAL == 0)
        {
            generator.tickInvites();
            if (level.isEmptyBlock(pos.above()))
            {
                generator.tryGenerateBlock();
            }
        }
    }

    public boolean canProcessNonPlayerBreak(long worldTick)
    {
        boolean active = isNonPlayerBreakCooldownActive(worldTick);
        boolean canProcess = !active;
        OneBlockUltima.logDebug("[BreakDebug] Non-player cooldown check for generator {}: active={}, lastTick={}, currentTick={}, canProcess={}", getBlockPos(), nonPlayerBreakCooldownActive, lastNonPlayerBreakTick, worldTick, canProcess);
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
        if (getLevel() != null && !getLevel().isClientSide())
        {
            int cooldownTicks = Math.max(1, ModSettings.get().getNonPlayerBreakCooldownTicks());
            getLevel().scheduleTick(getBlockPos(), ModBlocks.ONE_BLOCK_GENERATOR, cooldownTicks);
            OneBlockUltima.logDebug("[BreakDebug] Scheduled delayed generation for generator {} in {} ticks", getBlockPos(), cooldownTicks);
        }
        setChanged();
    }

    public void tryGenerateBlock()
    {
        try
        {
            tryGenerateBlockInternal();
        }
        catch (Throwable t)
        {
            OneBlockUltima.getLogger().error("[Generator] EXCEPTION during tryGenerateBlock at {}", getBlockPos(), t);
        }
    }

    private void tryGenerateBlockInternal()
    {
        if (getLevel() != null && !getLevel().isClientSide() && isNonPlayerBreakCooldownActive(getLevel().getGameTime()))
        {
            OneBlockUltima.logDebug("[BreakDebug] Skipping generation for generator {} because non-player cooldown is active", getBlockPos());
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
                OneBlockUltima.logDebug("[Generator] No set found even after trying default");
                return;
            }
        }

        int level = resolveGenerationLevel();
        OneBlockUltima.logDebug("[Generator] Resolved level: {} for setId: {}", level, selectedSetId);
        if (level <= 0)
        {
            OneBlockUltima.logDebug("[Generator] Level is {}, resetting to default set", level);
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
            OneBlockUltima.logDebug("[Generator] Level definition is null for level={}", level);
            return;
        }

        // Case block generation: place a physical case block in the world instead of dropping the item
        ModSettings caseSettings = ModSettings.get();
        if (set.hasCaseEntries()
                && caseSettings.getCaseDropPercent() > 0.0D
                && getLevel().random.nextDouble() * 100.0D < caseSettings.getCaseDropPercent())
        {
            BlockPos casePos = getBlockPos().above();
            if (BlockUtil.canReplaceForGeneration(getLevel(), casePos))
            {
                GeneratedBlockRegistry caseRegistry = GeneratedBlockRegistry.get(getLevel());
                if (caseRegistry.isGenerated(casePos))
                {
                    caseRegistry.remove(casePos);
                }
                getLevel().setBlock(casePos, ModBlocks.CASE_BLOCK.defaultBlockState(), 3);
                caseRegistry.markGenerated(casePos, getBlockPos(), selectedSetId, 0, level, "oneblockultima:case_block", 0);
                OneBlockUltima.logDebug("[Generator] Placed case block at {}", casePos);
                if (getLevel() != null && !getLevel().isClientSide())
                {
                    nonPlayerBreakCooldownActive = false;
                    lastNonPlayerBreakTick = Long.MIN_VALUE;
                }
                generationRetryDepth = 0;
                return;
            }
        }

        BlockSetConfig.BlockEntryDefinition entry = pickGenerationEntry(levelDefinition);
        if (entry == null)
        {
            OneBlockUltima.logDebug("[Generator] pickRandom returned null");
            return;
        }

        OneBlockUltima.logDebug("[Generator] Trying to generate entry registry={} meta={} chance={}", entry.registry, entry.meta, entry.getChance());
        BlockState state = BlockUtil.toState(entry);
        OneBlockUltima.logDebug("[Generator] BlockUtil.toState returned {}", state == null ? "null" : state.getBlock().getDescriptionId());
        if (state == null)
        {
            OneBlockUltima.logDebug("[Generator] Attempting item->block fallback for registry={}", entry.registry);
            try
            {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.registry));
                OneBlockUltima.logDebug("[Generator] Fallback item lookup for registry={} -> item={}", entry.registry, item == null ? "null" : item.getDescriptionId());
                Block resolvedBlock = null;
                if (item instanceof BlockItem)
                {
                    resolvedBlock = ((BlockItem) item).getBlock();
                }
                else if (item != null)
                {
                    try {
                        resolvedBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.registry));
                    } catch (Exception ignored) { }

                    if (resolvedBlock == null)
                    {
                        try { resolvedBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.registry + "s")); } catch (Exception ignored) { }
                    }
                }

                if (resolvedBlock != null)
                {
                    try { state = resolvedBlock.defaultBlockState(); } catch (Exception ex) { state = resolvedBlock.defaultBlockState(); }
                    OneBlockUltima.logDebug("[Generator] Fallback resolved block={}", state.getBlock().getDescriptionId());
                }
            }
            catch (Exception ex)
            {
                OneBlockUltima.getLogger().error("[Generator] Exception during item->block fallback for {}", entry.registry, ex);
            }

            if (state == null)
            {
                OneBlockUltima.logDebug("[Generator] Could not resolve placeable block for registry={}", entry.registry);
                return;
            }
        }

        BlockPos targetPos = getBlockPos().above();

        // Instead of spawning an item, try to place the block
        if (state.getBlock() == Blocks.AIR)
        {
            OneBlockUltima.logDebug("[Generator] State is null or AIR for registry={}, trying to place as block anyway", entry.registry);

            // Try to find the block through various ways
            Block resolvedBlock = null;

            // 1. Try via ItemBlock
            try {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.registry));
                if (item instanceof BlockItem) {
                    resolvedBlock = ((BlockItem) item).getBlock();
                    OneBlockUltima.logDebug("[Generator] Found block via ItemBlock: {}", resolvedBlock.getDescriptionId());
                }
            } catch (Exception ex) {
                OneBlockUltima.getLogger().error("[Generator] Error getting ItemBlock", ex);
            }

            // 2. If not found, try via BlockUtil (with updated Forestry handling)
            if (resolvedBlock == null || resolvedBlock == Blocks.AIR) {
                BlockState tempState = BlockUtil.toState(entry);
                Block tempBlock = tempState != null ? tempState.getBlock() : null;
                if (tempBlock != null && tempBlock != Blocks.AIR) {
                    resolvedBlock = tempBlock;
                    OneBlockUltima.logDebug("[Generator] Found block via BlockUtil: {}", resolvedBlock.getDescriptionId());
                }
            }

            // 3. If still not found, try a direct registry lookup
            if (resolvedBlock == null || resolvedBlock == Blocks.AIR) {
                try {
                    resolvedBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.registry));
                    OneBlockUltima.logDebug("[Generator] Found block via direct registry lookup: {}", resolvedBlock == null ? "null" : resolvedBlock.getDescriptionId());
                } catch (Exception ex) {
                    OneBlockUltima.getLogger().error("[Generator] Error in direct registry lookup", ex);
                }
            }

            // If a block was found - place it
            if (resolvedBlock != null && resolvedBlock != Blocks.AIR) {
                try {
                    BlockState newState = resolvedBlock.defaultBlockState();

                    if (newState.getBlock() != Blocks.AIR) {
                        OneBlockUltima.logDebug("[Generator] Placing block: {} at {}", newState.getBlock().getDescriptionId(), targetPos);

                        // Place the block with NBT (add obuGenerated)
                        CompoundTag genNbt = ensureObuGenerated(entry.nbtTags);
                        BlockUtil.placeBlockWithNBT(getLevel(), targetPos, newState, genNbt);

                        // Mark as generated
                        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(getLevel());
registry.markGenerated(targetPos, getBlockPos(), selectedSetId, (int) Math.round(BlockPriceConfig.get().getPrice(entry.registry)), level, BuiltInRegistries.BLOCK.getKey(newState.getBlock()).toString(), entry.meta);

                        generationRetryDepth = 0;
                        return; // Block placed successfully
                    }
                } catch (Exception ex) {
                    OneBlockUltima.getLogger().error("[Generator] Failed to place block", ex);
                }
            }

            // If nothing worked - spawn as an item (fallback)
            OneBlockUltima.logDebugWarn("[Generator] Could not place as block, spawning as item fallback for {}", entry.registry);
            try {
                ResourceLocation entryRl = ResourceLocation.parse(entry.registry);
                net.minecraft.world.level.block.Block entryBlock = BuiltInRegistries.BLOCK.get(entryRl);
                ItemStack itemStack;
                if (entryBlock != net.minecraft.world.level.block.Blocks.AIR) {
                    itemStack = ModBlocks.itemStackFor(entryBlock, entry.meta);
                } else {
                    Item item = BuiltInRegistries.ITEM.get(entryRl);
                    itemStack = ModBlocks.stackFromItemAndMeta(item, entry.meta);
                }
                if (!itemStack.isEmpty()) {
                    CompoundTag obuNbt = entry.nbtTags != null ? entry.nbtTags.copy() : new CompoundTag();
                    obuNbt.putBoolean(NBT_OBU_GENERATED, true);
                    itemStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(obuNbt));

                    ItemEntity entityItem = new ItemEntity(
                            getLevel(), targetPos.getX(), targetPos.getY(), targetPos.getZ(), itemStack
                    );
                    getLevel().addFreshEntity(entityItem);

                    GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(getLevel());
registry.markGenerated(targetPos, getBlockPos(), selectedSetId, (int) Math.round(BlockPriceConfig.get().getPrice(entry.registry)), level, BuiltInRegistries.BLOCK.getKey(entryBlock).toString(), entry.meta);
                }
            } catch (Exception ex) {
                OneBlockUltima.getLogger().error("[Generator] Failed to spawn item fallback", ex);
            }

            generationRetryDepth++;
            if (generationRetryDepth >= MAX_GENERATION_RETRIES)
            {
                OneBlockUltima.logDebugWarn("[Generator] Generation retry limit reached after {} consecutive AIR/fallback results, abandoning this cycle", MAX_GENERATION_RETRIES);
                generationRetryDepth = 0;
                return;
            }

            OneBlockUltima.logDebug("[Generator] Retrying generation after item spawn (retry {}/{})", generationRetryDepth, MAX_GENERATION_RETRIES);
            tryGenerateBlock();
            generationRetryDepth = 0;

            return;
        }

        OneBlockUltima.logDebug("[Generator] Target position={}, current block={}", targetPos, getLevel().getBlockState(targetPos).getBlock().getDescriptionId());
        if (!BlockUtil.canReplaceForGeneration(getLevel(), targetPos))
        {
            OneBlockUltima.logDebug("[Generator] Cannot replace target position={}", targetPos);
            return;
        }

        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(getLevel());
        if (registry.isGenerated(targetPos))
        {
            registry.remove(targetPos);
        }

        OneBlockUltima.logDebug("[Generator] Placing state={} at {}", state.getBlock().getDescriptionId(), targetPos);
        if (entry.nbtTags != null && !entry.nbtTags.isEmpty())
        {
            OneBlockUltima.logDebug("[Generator] Placing block with NBT tags at {}: {}", targetPos, entry.nbtTags);
        }
        // Place the block and apply NBT tags simultaneously (add obuGenerated)
        CompoundTag genNbt2 = ensureObuGenerated(entry.nbtTags);
        BlockUtil.placeBlockWithNBT(getLevel(), targetPos, state, genNbt2);
        OneBlockUltima.logDebug("[Generator] After place block at {}, now={}", targetPos, getLevel().getBlockState(targetPos).getBlock().getDescriptionId());
        registry.markGenerated(targetPos, getBlockPos(), selectedSetId, (int) Math.round(BlockPriceConfig.get().getPrice(entry.registry)), level, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), entry.meta);
        if (getLevel() != null && !getLevel().isClientSide())
        {
            nonPlayerBreakCooldownActive = false;
            lastNonPlayerBreakTick = Long.MIN_VALUE;
            OneBlockUltima.logDebug("[BreakDebug] Cleared non-player cooldown for generator {} after successful generation", getBlockPos());
        }
        generationRetryDepth = 0;
    }

    private BlockSetConfig.SetLevelDefinition cachedWeightedLevel;
    private int cachedDisableMask = -1;
    private BlockSetConfig.BlockEntryDefinition[] cachedWeightedEntries;
    private int cachedWeightedChance = -1;
    private static final int MAX_GENERATION_RETRIES = 8;
    private int generationRetryDepth = 0;

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

        int roll = getLevel().random.nextInt(cachedWeightedChance);
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

    private static CompoundTag ensureObuGenerated(CompoundTag nbtTags)
    {
        CompoundTag result = nbtTags != null ? nbtTags.copy() : new CompoundTag();
        result.putBoolean(NBT_OBU_GENERATED, true);
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
        setChanged();
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
        setChanged();
    }

    public boolean isDisableFluidGeneration()
    {
        return disableFluidGeneration;
    }

    public void setDisableMobGeneration(boolean disableMobGeneration)
    {
        this.disableMobGeneration = disableMobGeneration;
        setChanged();
    }

    public boolean isDisableMobGeneration()
    {
        return disableMobGeneration;
    }

    public void setDisableChestGeneration(boolean disableChestGeneration)
    {
        this.disableChestGeneration = disableChestGeneration;
        setChanged();
    }

    public boolean isDisableChestGeneration()
    {
        return disableChestGeneration;
    }

    public void setDisableSaplingGeneration(boolean disableSaplingGeneration)
    {
        this.disableSaplingGeneration = disableSaplingGeneration;
        setChanged();
    }

    public boolean isDisableSaplingGeneration()
    {
        return disableSaplingGeneration;
    }

    public void setSelectedSetId(String selectedSetId)
    {
        OneBlockUltima.logDebug("TileEntity setSelectedSetId: {} at pos {}", selectedSetId, getBlockPos());
        this.selectedSetId = selectedSetId;
        setChanged();

        if (getLevel() != null && !getLevel().isClientSide())
        {
            BlockState state = getLevel().getBlockState(getBlockPos());
            getLevel().sendBlockUpdated(getBlockPos(), state, state, 3);
            OneBlockUltima.logDebug("TileEntity notifyBlockUpdate sent");
        }
    }

    public boolean hasAccess(Player player)
    {
        return player != null && hasAccess(player.getUUID());
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

    public boolean isOwner(Player player)
    {
        return player != null && ownerId != null && ownerId.equals(player.getUUID());
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

        if (getLevel() == null || getLevel().isClientSide())
        {
            return false;
        }

        for (TileEntityOneBlockGenerator otherGenerator : ACTIVE_GENERATORS)
        {
            if (otherGenerator == this)
            {
                continue;
            }

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
        setChanged();
    }

    public void addMember(UUID memberId)
    {
        if (memberId == null || memberIds.contains(memberId))
        {
            return;
        }

        memberIds.add(memberId);
        setChanged();
    }

    public void addPendingInvite(UUID targetPlayerId, UUID senderPlayerId, int ticks)
    {
        if (targetPlayerId == null || senderPlayerId == null)
        {
            return;
        }

        pendingInvites.removeIf(invite -> invite.targetPlayerId.equals(targetPlayerId));

        pendingInvites.add(new PendingInvite(targetPlayerId, senderPlayerId, ticks));
        setChanged();
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

                if (getLevel() != null && !getLevel().isClientSide())
                {
                    for (TileEntityOneBlockGenerator otherGenerator : ACTIVE_GENERATORS)
                    {
                        if (otherGenerator == this)
                        {
                            continue;
                        }

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

                setChanged();
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
                setChanged();
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
        setChanged();
    }

    public void clearOwnershipAndMembers()
    {
        ownerId = null;
        memberIds.clear();
        placedByPlayer = false;
        setChanged();
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
        setChanged();
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag compound, net.minecraft.core.HolderLookup.Provider provider)
    {
        super.saveAdditional(compound, provider);
        compound.putString("selectedSetId", getSelectedSetId());
        compound.putBoolean("disableFluidGeneration", disableFluidGeneration);
        compound.putBoolean("disableMobGeneration", disableMobGeneration);
        compound.putBoolean("disableChestGeneration", disableChestGeneration);
        compound.putBoolean("disableSaplingGeneration", disableSaplingGeneration);
        if (ownerId != null)
        {
            compound.putUUID("ownerId", ownerId);
        }
        compound.putBoolean("placedByPlayer", placedByPlayer);
        compound.putLong("lastNonPlayerBreakTick", lastNonPlayerBreakTick);

        ListTag levelsTag = new ListTag();
        for (Map.Entry<String, Integer> entry : setLevels.entrySet())
        {
            if (entry.getKey() == null || entry.getValue() == null)
            {
                continue;
            }

            CompoundTag levelTag = new CompoundTag();
            levelTag.putString("setId", entry.getKey());
            levelTag.putInt("level", entry.getValue());
            levelsTag.add(levelTag);
        }
        compound.put("setLevels", levelsTag);

        ListTag membersTag = new ListTag();
        for (UUID memberId : memberIds)
        {
            if (memberId != null)
            {
                membersTag.add(StringTag.valueOf(memberId.toString()));
            }
        }
        compound.put("memberIds", membersTag);

        compound.put("pendingInvites", getInvitesTag());
    }

    private ListTag getInvitesTag() {
        ListTag invitesTag = new ListTag();
        for (PendingInvite invite : pendingInvites)
        {
            if (invite == null || invite.targetPlayerId == null || invite.senderPlayerId == null)
            {
                continue;
            }

            CompoundTag inviteTag = new CompoundTag();
            inviteTag.putUUID("targetPlayerId", invite.targetPlayerId);
            inviteTag.putUUID("senderPlayerId", invite.senderPlayerId);
            inviteTag.putInt("ticksLeft", invite.ticksLeft);
            invitesTag.add(inviteTag);
        }
        return invitesTag;
    }

    @Override
    public void loadAdditional(@Nonnull CompoundTag compound, net.minecraft.core.HolderLookup.Provider provider)
    {
        super.loadAdditional(compound, provider);
        selectedSetId = compound.getString("selectedSetId");
        disableFluidGeneration = compound.getBoolean("disableFluidGeneration");
        disableMobGeneration = compound.getBoolean("disableMobGeneration");
        disableChestGeneration = compound.getBoolean("disableChestGeneration");
        disableSaplingGeneration = compound.getBoolean("disableSaplingGeneration");
        if (compound.hasUUID("ownerId"))
        {
            ownerId = compound.getUUID("ownerId");
        }
        placedByPlayer = compound.getBoolean("placedByPlayer");
        lastNonPlayerBreakTick = compound.contains("lastNonPlayerBreakTick") ? compound.getLong("lastNonPlayerBreakTick") : Long.MIN_VALUE;

        setLevels.clear();
        if (compound.contains("setLevels", TAG_LIST))
        {
            ListTag levelsTag = compound.getList("setLevels", TAG_COMPOUND);
            for (int i = 0; i < levelsTag.size(); i++)
            {
                CompoundTag levelTag = levelsTag.getCompound(i);
                if (levelTag.contains("setId") && levelTag.contains("level"))
                {
                    setLevels.put(levelTag.getString("setId"), levelTag.getInt("level"));
                }
            }
        }

        memberIds.clear();
        if (compound.contains("memberIds", TAG_LIST))
        {
            ListTag membersTag = compound.getList("memberIds", TAG_STRING);
            for (int i = 0; i < membersTag.size(); i++)
            {
                try
                {
                    memberIds.add(UUID.fromString(membersTag.getString(i)));
                }
                catch (IllegalArgumentException ignored)
                {
                }
            }
        }

        pendingInvites.clear();
        if (compound.contains("pendingInvites", TAG_LIST))
        {
            ListTag invitesTag = compound.getList("pendingInvites", TAG_COMPOUND);
            for (int i = 0; i < invitesTag.size(); i++)
            {
                CompoundTag inviteTag = invitesTag.getCompound(i);
                if (inviteTag.hasUUID("targetPlayerId") && inviteTag.hasUUID("senderPlayerId"))
                {
                    pendingInvites.add(new PendingInvite(
                            inviteTag.getUUID("targetPlayerId"),
                            inviteTag.getUUID("senderPlayerId"),
                            inviteTag.getInt("ticksLeft")
                    ));
                }
            }
        }
    }

    @Nonnull
    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider)
    {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
