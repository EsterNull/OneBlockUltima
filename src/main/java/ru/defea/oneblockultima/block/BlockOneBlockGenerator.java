package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.event.ModEvents;
import ru.defea.oneblockultima.gui.GuiHandler;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;

import java.util.List;
import java.util.Random;

public class BlockOneBlockGenerator extends Block implements ITileEntityProvider
{
    public static final int GENERATOR_X = 0;
    public static final int GENERATOR_Y = 63;
    public static final int GENERATOR_Z = 0;
    public static final int GENERATED_BLOCK_Y = GENERATOR_Y + 1;
    public static final int FLUID_BARRIER_Y = GENERATED_BLOCK_Y + 1;

    private static final AxisAlignedBB COLLISION_AABB = AxisAlignedBB.getBoundingBox(0.0D, 0.0D, 0.0D, 1.0D, 2.0D, 1.0D);
    private static final AxisAlignedBB HIGHLIGHT_AABB = AxisAlignedBB.getBoundingBox(0.0D, 1.0D, 0.0D, 1.0D, 2.0D, 1.0D);

    public BlockOneBlockGenerator()
    {
        super(Material.ground);
        setHardness(-1.0F);
        setResistance(6000000.0F);
        setHarvestLevel("pickaxe", 0);
        setCreativeTab(OneBlockUltima.modTab);
        setUnlocalizedName("one_block_generator");
        this.setTextureName("oneblockultima:one_block_generator_texture");
        this.setLightOpacity(0);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        if (player.isSneaking())
        {
            return false;
        }

        if (world.isRemote)
        {
            return true;
        }

        return openGeneratorMenu(world, x, y, z, player);
    }

    public static boolean openGeneratorMenu(World world, int x, int y, int z, EntityPlayer player)
    {
        if (player == null)
        {
            return false;
        }

        OneBlockUltima.getRawLogger().info("[MenuDebug] openGeneratorMenu at ({},{},{})", x, y, z);

        TileEntity tileEntity = world != null ? world.getTileEntity(x, y, z) : null;
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
            OneBlockUltima.getRawLogger().info("[MenuDebug] TE is generator: isFree={}, ownerId={}",
                    generator.isFree(), generator.getOwnerId());
            if (generator.isFree() && generator.canBeClaimedBy(player.getUniqueID()))
            {
                OneBlockUltima.getRawLogger().info("[MenuDebug] opening CLAIM screen");
                GuiHandler.openClaimScreen(player, x, y, z);
                return true;
            }

            if (!ModEvents.ensureGeneratorAccess(world, x, y, z, player, generator))
            {
                OneBlockUltima.getRawLogger().info("[MenuDebug] access DENIED, no screen");
                return true;
            }

            OneBlockUltima.getRawLogger().info("[MenuDebug] opening MAIN menu");
            GuiHandler.open(player, x, y, z);
            return true;
        }

        OneBlockUltima.getRawLogger().info("[MenuDebug] TE is NOT a generator ({}), opening MAIN menu directly",
                tileEntity != null ? tileEntity.getClass().getSimpleName() : "null");
        GuiHandler.open(player, x, y, z);
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasTileEntity()
    {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta)
    {
        return new TileEntityOneBlockGenerator();
    }

    @Override
    public int getRenderType()
    {
        // Invisible in the world, matching the 1.12.x reference (EnumBlockRenderType.INVISIBLE).
        return -1;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side)
    {
        // The generator itself renders nothing. Return true so that the faces of
        // the neighboring blocks (the generated block above and the platform below)
        // are still rendered and do not leave visible holes.
        return true;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z)
    {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z)
    {
        return HIGHLIGHT_AABB.getOffsetBoundingBox(x, y, z);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
    {
        return COLLISION_AABB.getOffsetBoundingBox(x, y, z);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB entityBox, List collidingBoxes, Entity entityIn)
    {
        super.addCollisionBoxesToList(world, x, y, z, entityBox, collidingBoxes, entityIn);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end)
    {
        // Do not allow aiming at the generator's slot box (between y+1 and y+2) from below
        // the generator, e.g. while standing in the void under the platform.
        if (start.yCoord < y + 1.0D)
        {
            return null;
        }
        MovingObjectPosition intercept = HIGHLIGHT_AABB.getOffsetBoundingBox(x, y, z).calculateIntercept(start, end);
        if (intercept == null)
        {
            return null;
        }
        return new MovingObjectPosition(x, y, z, intercept.sideHit, intercept.hitVec);
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plantable)
    {
        if (direction != ForgeDirection.UP)
        {
            return false;
        }

        EnumPlantType plantType = plantable.getPlantType(world, x, y + 1, z);
        return plantType == EnumPlantType.Crop
                || plantType == EnumPlantType.Plains
                || plantType == EnumPlantType.Beach;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z)
    {
        if (!world.isRemote)
        {
            ModEvents.applyPendingGeneratorOwner(world, x, y, z);

            // Place the barrier above the generator
            ensureFluidBarrier(world, x, y, z);

            world.scheduleBlockUpdate(x, y, z, this, 1);
        }
        super.onBlockAdded(world, x, y, z);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random rand)
    {
        if (world.isRemote)
        {
            return;
        }

        // Check whether the barrier needs to be restored
        ensureFluidBarrier(world, x, y, z);

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            ModEvents.applyPendingGeneratorOwner(world, x, y, z);
            ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighborBlock)
    {
        super.onNeighborBlockChange(world, x, y, z, neighborBlock);
        if (world.isRemote)
        {
            return;
        }

        if (world.getBlock(x, y + 1, z) != Blocks.air)
        {
            return;
        }

        ensureFluidBarrier(world, x, y, z);

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            if (cpw.mods.fml.common.Loader.isModLoaded("multimine"))
            {
                world.scheduleBlockUpdate(x, y, z, this, 7);
            }
            else
            {
                ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
            }
        }
    }

    public static void ensureFluidBarrier(World world, int generatorX, int generatorY, int generatorZ)
    {
        int barrierY = generatorY + 2;
        Block barrierBlock = world.getBlock(generatorX, barrierY, generatorZ);

        if (barrierBlock == ModBlocks.FLUID_BARRIER)
        {
            return;
        }

        if (barrierBlock != Blocks.air)
        {
            boolean generatedSlotEmpty = world.getBlock(generatorX, generatorY + 1, generatorZ) == Blocks.air;
            if ((barrierBlock instanceof net.minecraft.block.BlockLever
                    || barrierBlock instanceof net.minecraft.block.BlockButton
                    || barrierBlock instanceof net.minecraft.block.BlockTorch)
                    && (generatedSlotEmpty || !barrierBlock.canPlaceBlockAt(world, generatorX, barrierY, generatorZ)))
            {
                barrierBlock.dropBlockAsItem(world, generatorX, barrierY, generatorZ, world.getBlockMetadata(generatorX, barrierY, generatorZ), 0);
                world.setBlockToAir(generatorX, barrierY, generatorZ);
                OneBlockUltima.getLogger().info("[Generator] Dropped attachable {} at {} via ensureFluidBarrier (generatedSlotEmpty={})", barrierBlock.getLocalizedName(), barrierPos(generatorX, barrierY, generatorZ), generatedSlotEmpty);
            }
            else
            {
                return;
            }
        }

        world.setBlock(generatorX, barrierY, generatorZ, ModBlocks.FLUID_BARRIER, 0, 3);
        OneBlockUltima.getLogger().info("[Generator] BARRIER placed at {}", barrierPos(generatorX, barrierY, generatorZ));
    }

    private static String barrierPos(int x, int y, int z)
    {
        return "[" + x + ", " + y + ", " + z + "]";
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta)
    {
        if (world.getBlock(x, y - 1, z) == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            GeneratedBlockRegistry.get(world).remove(x, y, z);
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
