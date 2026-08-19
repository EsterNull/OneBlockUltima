package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Random;

public class BlockCustomBreakable extends Block
{
    private Block emulated;

    public BlockCustomBreakable(String registryName)
    {
        super(Material.rock);
        this.setHardness(50.0F);
        this.setResistance(2000.0F);
        this.setUnlocalizedName(registryName);
        this.setTextureName("minecraft:stone");
    }

    public void setEmulated(Block block)
    {
        this.emulated = block;
    }

    public Block getEmulated()
    {
        return this.emulated;
    }

    public int getEmulatedMeta(int meta)
    {
        return meta;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z)
    {
        if (this.emulated == null)
        {
            return super.getPickBlock(target, world, x, y, z);
        }
        Item item = Item.getItemFromBlock(this.emulated);
        if (item == null)
        {
            return super.getPickBlock(target, world, x, y, z);
        }
        return new ItemStack(item, 1, this.getEmulatedMeta(world.getBlockMetadata(x, y, z)));
    }

    @Override
    public float getExplosionResistance(Entity exploder)
    {
        return Blocks.obsidian.getExplosionResistance(exploder);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune)
    {
        ArrayList<ItemStack> drops = new ArrayList<>();
        if (this.emulated != null)
        {
            Item item = Item.getItemFromBlock(this.emulated);
            if (item != null)
            {
                drops.add(new ItemStack(item, 1, this.getEmulatedMeta(metadata)));
            }
        }
        return drops;
    }

    @Override
    public Item getItemDropped(int metadata, Random rand, int fortune)
    {
        return this.emulated == null ? null : Item.getItemFromBlock(this.emulated);
    }

    @Override
    public int quantityDropped(Random random)
    {
        return this.emulated == null ? 0 : 1;
    }

    @Override
    public int damageDropped(int metadata)
    {
        return this.getEmulatedMeta(metadata);
    }

    @Override
    public int getHarvestLevel(int metadata)
    {
        return 3;
    }

    @Override
    public String getHarvestTool(int metadata)
    {
        return "pickaxe";
    }

    @Override
    public boolean isToolEffective(String tool, int metadata)
    {
        return "pickaxe".equals(tool);
    }

    @Override
    public int getRenderBlockPass()
    {
        if (this.emulated != null && this.emulated.getRenderBlockPass() != 0)
        {
            return this.emulated.getRenderBlockPass();
        }
        return super.getRenderBlockPass();
    }

    @Override
    public boolean isOpaqueCube()
    {
        if (this.emulated != null)
        {
            return this.emulated.isOpaqueCube();
        }
        return super.isOpaqueCube();
    }

    @Override
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side)
    {
        if (this.emulated != null)
        {
            return this.emulated.getIcon(side, this.getEmulatedMeta(world.getBlockMetadata(x, y, z)));
        }
        return super.getIcon(world, x, y, z, side);
    }

    @Override
    public IIcon getIcon(int side, int meta)
    {
        if (this.emulated != null)
        {
            return this.emulated.getIcon(side, this.getEmulatedMeta(meta));
        }
        return super.getIcon(side, meta);
    }
}
