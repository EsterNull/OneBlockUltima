package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.client.ModModels;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Ломаемая замена любого неразрушаемого блока (hardness &lt; 0).
 * Каждый зарегистрированный экземпляр "привязан" к одному оригинальному блоку ({@link #setEmulated(Block)});
 * meta оригинального блока хранится в свойстве {@link #ORIGINAL_META}.
 */
public class BlockCustomBreakable extends Block
{
    public static final PropertyInteger ORIGINAL_META = PropertyInteger.create("original_meta", 0, 15);

    private Block emulated;

    public BlockCustomBreakable(String registryName)
    {
        super(Material.ROCK);
        this.setDefaultState(this.blockState.getBaseState().withProperty(ORIGINAL_META, 0));
        this.setHardness(50.0F);
        this.setResistance(2000.0F);
        this.setUnlocalizedName("custom_breakable");
        this.setRegistryName(OneBlockUltima.MODID, registryName);
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

    @Nullable
    public IBlockState getEmulatedState(IBlockState state)
    {
        if (this.emulated == null)
        {
            return null;
        }
        try
        {
            return this.emulated.getStateFromMeta(state.getValue(ORIGINAL_META));
        }
        catch (Exception ex)
        {
            return this.emulated.getDefaultState();
        }
    }

    public static String buildVariantString(IBlockState state)
    {
        StringBuilder sb = new StringBuilder();
        for (IProperty<?> property : state.getPropertyKeys())
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

    private static <T extends Comparable<T>> String propertyValueName(IProperty<T> property, IBlockState state)
    {
        return property.getName(state.getValue(property));
    }

    @Override
    @Nonnull
    public ItemStack getPickBlock(@Nonnull IBlockState state, @Nonnull RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player)
    {
        if (this.emulated == null)
        {
            return super.getPickBlock(state, target, world, pos, player);
        }
        IBlockState emuState = this.getEmulatedState(state);
        return this.emulated.getPickBlock(emuState == null ? this.emulated.getDefaultState() : emuState, target, world, pos, player);
    }

    @Override
    public boolean canHarvestBlock(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player)
    {
        return true;
    }

    @Override
    public float getExplosionResistance(@Nonnull Entity exploder)
    {
        return Blocks.OBSIDIAN.getExplosionResistance(exploder);
    }

    @Override
    @Nonnull
    public List<ItemStack> getDrops(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState state, int fortune)
    {
        List<ItemStack> drops = new java.util.ArrayList<>();
        if (this.emulated != null)
        {
            Item item = Item.getItemFromBlock(this.emulated);
            if (item != Items.AIR)
            {
                drops.add(new ItemStack(item, 1, state.getValue(ORIGINAL_META)));
            }
        }
        return drops;
    }

    @Override
    @Nonnull
    public Item getItemDropped(@Nonnull IBlockState state, @Nonnull Random rand, int fortune)
    {
        return this.emulated == null ? Items.AIR : Item.getItemFromBlock(this.emulated);
    }

    @Override
    public int quantityDropped(@Nonnull Random random)
    {
        return this.emulated == null ? 0 : 1;
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return state.getValue(ORIGINAL_META);
    }

    @Override
    public int getHarvestLevel(@Nonnull IBlockState state)
    {
        return 3;
    }

    @Override
    public String getHarvestTool(@Nonnull IBlockState state)
    {
        return "pickaxe";
    }

    @Override
    public boolean isToolEffective(@Nonnull String tool, @Nonnull IBlockState state)
    {
        return "pickaxe".equals(tool);
    }

    @Override
    @Nonnull
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(ORIGINAL_META, Math.max(0, Math.min(15, meta)));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(ORIGINAL_META);
    }

    @Override
    @Nonnull
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, ORIGINAL_META);
    }

    @Override
    @Nonnull
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer()
    {
        if (this.emulated != null)
        {
            if (this.emulated.getBlockLayer() != BlockRenderLayer.SOLID)
            {
                return this.emulated.getBlockLayer();
            }
            if (ModModels.hasNoBlockstateModel(this.emulated))
            {
                return BlockRenderLayer.TRANSLUCENT;
            }
        }
        return super.getBlockLayer();
    }

    @Override
    public boolean isOpaqueCube(@Nonnull IBlockState state)
    {
        if (this.emulated != null)
        {
            return this.emulated.isOpaqueCube(this.emulated.getDefaultState());
        }
        return super.isOpaqueCube(state);
    }
}
