package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.event.ModEvents;
import ru.defea.oneblockultima.gui.GuiHandler;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class BlockOneBlockGenerator extends Block implements ITileEntityProvider
{
    public static final BlockPos GENERATOR_POS = new BlockPos(0, 63, 0);
    public static final BlockPos GENERATED_BLOCK_POS = GENERATOR_POS.up();
    public static final BlockPos FLUID_BARRIER_POS = GENERATED_BLOCK_POS.up();

    private static final AxisAlignedBB COLLISION_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 2.0D, 1.0D);
    private static final AxisAlignedBB HIGHLIGHT_AABB = new AxisAlignedBB(0.0D, 1.0D, 0.0D, 1.0D, 2.0D, 1.0D);
    private static final AxisAlignedBB BOUNDING_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.001D, 1.0D);

    public BlockOneBlockGenerator()
    {
        super(Material.GROUND);
        setHardness(-1.0F);
        setResistance(6000000.0F);
        setHarvestLevel("pickaxe", 0);
        setCreativeTab(OneBlockUltima.modTab);
        setRegistryName(OneBlockUltima.MODID, "one_block_generator");
        setUnlocalizedName("one_block_generator");
        this.setLightOpacity(0);
    }

    @Override
    public boolean onBlockActivated(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, EntityPlayer player, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (player.isSneaking())
        {
            return false;
        }

        if (world.isRemote)
        {
            return true;
        }

        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
            if (generator.isFree() && generator.canBeClaimedBy(player.getUniqueID()))
            {
                GuiHandler.openClaimScreen(player, pos);
                return true;
            }

            if (!ModEvents.ensureGeneratorAccess(world, pos, player, generator))
            {
                return true;
            }

            GuiHandler.open(player, pos);
            return true;
        }

        GuiHandler.open(player, pos);
        return true;
    }

    @Override
    public boolean hasTileEntity(@Nonnull IBlockState state)
    {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta)
    {
        return new TileEntityOneBlockGenerator();
    }

    @Override
    @Nonnull
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state)
    {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    @Nonnull
    public AxisAlignedBB getBoundingBox(@Nonnull IBlockState state, @Nonnull IBlockAccess source, @Nonnull BlockPos pos)
    {
        return BOUNDING_AABB;
    }

    @Override
    @Nonnull
    public AxisAlignedBB getSelectedBoundingBox(@Nonnull IBlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos)
    {
        return HIGHLIGHT_AABB.offset(pos);
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(@Nonnull IBlockState blockState, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos)
    {
        return COLLISION_AABB;
    }

    @Override
    public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox, @Nonnull java.util.List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean p_185477_7_)
    {
        super.addCollisionBoxToList(state, worldIn, pos, entityBox, collidingBoxes, entityIn, p_185477_7_);
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(@Nonnull IBlockState blockState, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end)
    {
        return rayTrace(pos, start, end, HIGHLIGHT_AABB);
    }

    @Override
    public boolean isOpaqueCube(@Nonnull IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isFullBlock(@Nonnull IBlockState state)
    {
        return false;
    }

    @Override
    public boolean canSustainPlant(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing direction, @Nonnull IPlantable plantable)
    {
        if (direction != EnumFacing.UP)
        {
            return false;
        }

        EnumPlantType plantType = plantable.getPlantType(world, pos.offset(direction));
        return plantType == EnumPlantType.Crop
                || plantType == EnumPlantType.Plains
                || plantType == EnumPlantType.Beach;
    }

    @Override
    public void onBlockAdded(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state)
    {
        if (!world.isRemote)
        {
            ModEvents.applyPendingGeneratorOwner(world, pos);

            // Ставим барьер над генератором
            ensureFluidBarrier(world, pos);

            world.scheduleUpdate(pos, this, 1);
        }
        super.onBlockAdded(world, pos, state);
    }

    @Override
    public void updateTick(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Random rand)
    {
        if (world.isRemote)
        {
            return;
        }

        // Проверяем, нужно ли восстановить барьер
        ensureFluidBarrier(world, pos);

        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            ModEvents.applyPendingGeneratorOwner(world, pos);
            ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
        }
    }

    @Override
    public void neighborChanged(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos)
    {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        if (world.isRemote)
        {
            return;
        }

        IBlockState currentState = world.getBlockState(fromPos);
        if (currentState != Blocks.AIR.getDefaultState()) {
            return;
        }

        if (world.getBlockState(fromPos.down()).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            ensureFluidBarrier(world, pos);

            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                if (net.minecraftforge.fml.common.Loader.isModLoaded("multimine"))
                {
                    world.scheduleUpdate(pos, this, 7);
                }
                else
                {
                    ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
                }
            }
        }
    }

    private static void ensureFluidBarrier(World world, BlockPos generatorPos)
    {
        BlockPos barrierPos = generatorPos.up(2);
        IBlockState barrierState = world.getBlockState(barrierPos);
        Block barrierBlock = barrierState.getBlock();

        if (barrierBlock == ModBlocks.FLUID_BARRIER)
        {
            return;
        }

        if (barrierBlock != Blocks.AIR)
        {
            boolean generatedSlotEmpty = world.getBlockState(generatorPos.up()).getBlock() == Blocks.AIR;
            if ((barrierBlock instanceof net.minecraft.block.BlockLever
                    || barrierBlock instanceof net.minecraft.block.BlockButton
                    || barrierBlock instanceof net.minecraft.block.BlockTorch)
                    && (generatedSlotEmpty || !barrierBlock.canPlaceBlockAt(world, barrierPos)))
            {
                barrierBlock.dropBlockAsItem(world, barrierPos, barrierState, 0);
                world.setBlockToAir(barrierPos);
                OneBlockUltima.getLogger().info("[Generator] Dropped attachable {} at {} via ensureFluidBarrier (generatedSlotEmpty={})", barrierBlock.getLocalizedName(), barrierPos, generatedSlotEmpty);
            }
            else
            {
                return;
            }
        }

        world.setBlockState(barrierPos, ModBlocks.FLUID_BARRIER.getDefaultState(), 3);
        OneBlockUltima.getLogger().info("[Generator] BARRIER placed at {}", barrierPos);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, @Nonnull IBlockState state)
    {
        if (world.getBlockState(pos.down()).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            GeneratedBlockRegistry.get(world).remove(pos);
        }
        super.breakBlock(world, pos, state);
    }
}
