package ru.defea.oneblockultima.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockCustomBreakable extends Block
{
    public static final IntegerProperty ORIGINAL_META = IntegerProperty.create("original_meta", 0, 15);

    private Block emulated;

    public BlockCustomBreakable(String registryName)
    {
        super(Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(50.0F, 2000.0F));
        this.registerDefaultState(this.defaultBlockState().setValue(ORIGINAL_META, 0));
    }

    public void setEmulated(Block block)
    {
        this.emulated = block;
    }

    @Nullable
    public Block getEmulated()
    {
        return this.emulated;
    }

    @Nonnull
    @Override
    public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
            @Nullable BlockState queryState, @Nullable BlockPos queryPos)
    {
        if (this.emulated != null && (queryState == null || queryState.getBlock() != this))
        {
            return this.emulated.defaultBlockState();
        }
        return super.getAppearance(state, level, pos, side, queryState, queryPos);
    }

    @Nullable
    public BlockState getEmulatedState(BlockState state)
    {
        if (this.emulated == null)
        {
            return null;
        }
        return this.emulated.defaultBlockState();
    }

    public static String buildVariantString(BlockState state)
    {
        StringBuilder sb = new StringBuilder();
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getBlock().getStateDefinition().getProperties())
        {
            if (sb.length() != 0)
            {
                sb.append(',');
            }
            sb.append(property.getName()).append('=').append(propertyValueName(property, state));
        }
        if (sb.length() == 0)
        {
            sb.append("normal");
        }
        return sb.toString();
    }

    private static <T extends Comparable<T>> String propertyValueName(net.minecraft.world.level.block.state.properties.Property<T> property, BlockState state)
    {
        return property.getName(state.getValue(property));
    }

    @Nonnull
    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state)
    {
        if (this.emulated == null)
        {
            return super.getCloneItemStack(world, pos, state);
        }
        BlockState emuState = this.getEmulatedState(state);
        return this.emulated.getCloneItemStack(world, pos, emuState == null ? this.emulated.defaultBlockState() : emuState);
    }

    @Nonnull
    @Override
    public List<ItemStack> getDrops(@Nonnull BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder)
    {
        List<ItemStack> drops = new java.util.ArrayList<>();
        if (this.emulated != null)
        {
            Item item = this.emulated.asItem();
            if (item != Items.AIR)
            {
                ItemStack stack = new ItemStack(item, 1);
                stack.setDamageValue(state.getValue(ORIGINAL_META));
                drops.add(stack);
            }
        }
        return drops;
    }

    @Nonnull
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(ORIGINAL_META);
    }
}
