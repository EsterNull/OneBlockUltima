package ru.defea.oneblockultima.loot;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.LootModifier;
import ru.defea.oneblockultima.Constants;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.event.ModEvents;
import ru.defea.oneblockultima.util.CaseUtil;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OneBlockDropModifier extends LootModifier
{
    public static final MapCodec<OneBlockDropModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, OneBlockDropModifier::new)
    );

    public OneBlockDropModifier(LootItemCondition[] conditions)
    {
        super(conditions);
        OneBlockUltima.getLogger().info("[DropMod] OneBlockDropModifier instance created (loaded from datapack)");
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec()
    {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        LootContext lootContext = context;
        OneBlockUltima.logDebug("[DropMod] doApply called for {} at {} (generatedLoot={})",
                lootContext.getQueriedLootTableId(), lootContext.getParam(LootContextParams.ORIGIN), generatedLoot.stream().filter(s -> !s.isEmpty()).count());
        Vec3 origin = lootContext.getParam(LootContextParams.ORIGIN);
        if (origin == null)
        {
            OneBlockUltima.logDebug("[DropMod] origin == null, skipping");
            return generatedLoot;
        }

        BlockPos pos = BlockPos.containing(origin);
        GeneratedBlockRegistry.GeneratedBlockEntry entry = ModEvents.consumePendingMobSpawnEntry(pos);
        if (entry == null)
        {
            OneBlockUltima.logDebug("[DropMod] no pending entry at {}", pos);
            return generatedLoot;
        }

        Level level = context.getLevel();
        UUID breakerId = ModEvents.consumeBreakerId(pos);

        Entity contextEntity = context.hasParam(LootContextParams.THIS_ENTITY) ? context.getParam(LootContextParams.THIS_ENTITY) : null;
        Player player = breakerId != null ? level.getPlayerByUUID(breakerId)
                : (contextEntity instanceof Player ? (Player) contextEntity : null);

        if (player == null)
        {
            OneBlockUltima.logDebug("[DropMod] player is null for {} (breakerId={}, entity={})", pos, breakerId, contextEntity);
            return generatedLoot;
        }

        BlockState state = lootContext.getParam(LootContextParams.BLOCK_STATE);
        OneBlockUltima.logDebug("[DropMod] processing generated break at {} set={} level={} block={} stateBlock={}",
                pos, entry.setId, entry.level, entry.blockRegistry,
                state == null ? "null" : state.getBlock().getDescriptionId());

        boolean isPlayerRePlaced = entry.generatorPos != null && entry.generatorPos.equals(pos);
        if (isPlayerRePlaced)
        {
            for (ItemStack drop : generatedLoot)
            {
                if (!drop.isEmpty())
                {
                    markGenerated(drop);
                }
            }
            return generatedLoot;
        }

        boolean isCaseBlock = state != null && state.getBlock() == ModBlocks.CASE_BLOCK;

        if (isCaseBlock)
        {
            OneBlockUltima.logDebug("[DropMod] case block at {} — case item granted directly in onBlockBreak (set {})", pos, entry.setId);
        }

        ObjectArrayList<ItemStack> result = new ObjectArrayList<>(generatedLoot);
        boolean hasRealDrops = result.stream().anyMatch(drop -> !drop.isEmpty());

        if (!hasRealDrops)
        {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.blockRegistry));
            if (!isCaseBlock && block != Blocks.AIR)
            {
                if (isShouldDropBlock(block, player, result))
                {
                    ItemStack blockStack = new ItemStack(block.asItem(), 1);
                    result.add(blockStack);
                }
            }
        }

        for (ItemStack drop : result)
        {
            if (drop.isEmpty())
            {
                continue;
            }
            markGenerated(drop);
            if (!player.getInventory().add(drop))
            {
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + Constants.BLOCK_CENTER_OFFSET,
                        pos.getY() + Constants.BLOCK_CENTER_OFFSET,
                        pos.getZ() + Constants.BLOCK_CENTER_OFFSET, drop));
            }
        }

        return new ObjectArrayList<>();
    }

    private static boolean isShouldDropBlock(Block block, Player player, List<ItemStack> drops)
    {
        boolean shouldDropBlock = true;

        ItemStack heldItem = player.getMainHandItem();
        boolean hasShears = !heldItem.isEmpty() && heldItem.getItem() == net.minecraft.world.item.Items.SHEARS;

        if (block == Blocks.TALL_GRASS || block == Blocks.DEAD_BUSH)
        {
            if (!hasShears)
            {
                shouldDropBlock = false;
            }
        }

        if (block instanceof net.minecraft.world.level.block.LeavesBlock)
        {
            if (!hasShears)
            {
                boolean hasSapling = false;
                for (ItemStack drop : drops)
                {
                    if (!drop.isEmpty()
                            && drop.getItem() instanceof net.minecraft.world.item.BlockItem bi
                            && bi.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock)
                    {
                        hasSapling = true;
                        break;
                    }
                }
                if (!hasSapling)
                {
                    shouldDropBlock = false;
                }
            }
        }

        if (block == Blocks.COBWEB)
        {
            if (!hasShears)
            {
                shouldDropBlock = false;
            }
        }
        return shouldDropBlock;
    }

    private static void markGenerated(ItemStack drop)
    {
        net.minecraft.world.item.component.CustomData existing = drop.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        net.minecraft.nbt.CompoundTag tag = existing != null ? existing.copyTag() : new net.minecraft.nbt.CompoundTag();
        tag.putBoolean(Constants.NBT_OBU_GENERATED, true);
        drop.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        OneBlockUltima.logDebug("[DropMod] marked generated: {} x{}", drop.getItem().getDescriptionId(), drop.getCount());
    }
}
