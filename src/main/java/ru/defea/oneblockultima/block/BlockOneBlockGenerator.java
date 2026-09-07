package ru.defea.oneblockultima.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.IPlantable;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.event.ModEvents;
import ru.defea.oneblockultima.gui.containers.ContainerClaimGenerator;
import ru.defea.oneblockultima.gui.containers.ContainerOneBlock;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class BlockOneBlockGenerator extends Block implements EntityBlock
{
    public static final BlockPos GENERATOR_POS = new BlockPos(0, 0, 0);
    public static final BlockPos GENERATED_BLOCK_POS = GENERATOR_POS.above();
    public static final BlockPos FLUID_BARRIER_POS = GENERATED_BLOCK_POS.above();

    private static final AABB COLLISION_AABB = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 2.0D, 1.0D);
    private static final AABB HIGHLIGHT_AABB = new AABB(0.0D, 1.0D, 0.0D, 1.0D, 2.0D, 1.0D);
    private static final AABB BOUNDING_AABB = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 0.001D, 1.0D);

    public BlockOneBlockGenerator()
    {
        super(Properties.of().mapColor(MapColor.DIRT).strength(-1.0F, 6000000.0F).noCollission());
    }

    private static InteractionResult handleUse(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (player.isShiftKeyDown())
        {
            return InteractionResult.PASS;
        }

        if (world.isClientSide)
        {
            return InteractionResult.SUCCESS;
        }

        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
            if (generator.isFree() && generator.canBeClaimedBy(player.getUUID()))
            {
                ((ServerPlayer) player).openMenu(claimMenuProvider(pos), (buf) -> buf.writeBlockPos(pos));
                return InteractionResult.SUCCESS;
            }

            if (!ModEvents.ensureGeneratorAccess(world, pos, player, generator))
            {
                return InteractionResult.SUCCESS;
            }

            ((ServerPlayer) player).openMenu(mainMenuProvider(pos), (buf) -> buf.writeBlockPos(pos));
            return InteractionResult.SUCCESS;
        }

        ((ServerPlayer) player).openMenu(mainMenuProvider(pos), (buf) -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
    {
        return handleUse(state, world, pos, player, hit);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        InteractionResult result = handleUse(state, world, pos, player, hit);
        return result == InteractionResult.PASS ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SUCCESS;
    }

    private static MenuProvider claimMenuProvider(BlockPos pos)
    {
        return new MenuProvider()
        {
            @Override
            public Component getDisplayName()
            {
                return Component.literal("");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player p)
            {
                return new ContainerClaimGenerator(id, inv, pos);
            }
        };
    }

    private static MenuProvider mainMenuProvider(BlockPos pos)
    {
        return new MenuProvider()
        {
            @Override
            public Component getDisplayName()
            {
                return Component.literal("");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player p)
            {
                return new ContainerOneBlock(id, inv, pos);
            }
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state)
    {
        return new TileEntityOneBlockGenerator(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide)
        {
            return null;
        }

        if (type != ru.defea.oneblockultima.tile.ModTileEntities.ONE_BLOCK_GENERATOR.get())
        {
            return null;
        }

        return (lvl, pos, st, be) -> TileEntityOneBlockGenerator.serverTick(lvl, pos, st, (TileEntityOneBlockGenerator) be);
    }

    @Nonnull
    @Override
    public RenderShape getRenderShape(@Nonnull BlockState state)
    {
        return RenderShape.INVISIBLE;
    }

    // Fully transparent to sight and raycast: the crosshair/block-picking never targets the
    // generator itself, clicks pass through to whatever is above it.
    @Nonnull
    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter source, @Nonnull BlockPos pos, @Nonnull CollisionContext context)
    {
        return Shapes.empty();
    }

    @Nonnull
    @Override
    public VoxelShape getInteractionShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos)
    {
        return Shapes.empty();
    }

    @Nonnull
    @Override
    public VoxelShape getCollisionShape(@Nonnull BlockState state, @Nonnull BlockGetter worldIn, @Nonnull BlockPos pos, @Nonnull CollisionContext context)
    {
        return Shapes.create(COLLISION_AABB);
    }

    @Override
    public boolean canSustainPlant(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Direction direction, @Nonnull IPlantable plantable)
    {
        if (direction != Direction.UP)
        {
            return false;
        }

        net.minecraftforge.common.PlantType plantType = plantable.getPlantType(world, pos.relative(direction));
        return plantType == net.minecraftforge.common.PlantType.CROP
                || plantType == net.minecraftforge.common.PlantType.PLAINS
                || plantType == net.minecraftforge.common.PlantType.BEACH;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving)
    {
        if (!world.isClientSide)
        {
            ModEvents.applyPendingGeneratorOwner(world, pos);
            ensureFluidBarrier(world, pos);
            world.scheduleTick(pos, this, 1);
        }
        super.onPlace(state, world, pos, oldState, isMoving);
    }

    @Override
    public void tick(BlockState state, net.minecraft.server.level.ServerLevel world, BlockPos pos, RandomSource random)
    {
        if (world.isClientSide)
        {
            return;
        }

        ensureFluidBarrier(world, pos);

        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            ModEvents.applyPendingGeneratorOwner(world, pos);
            ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
        }
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos, boolean isMoving)
    {
        super.neighborChanged(state, world, pos, blockIn, fromPos, isMoving);
        if (world.isClientSide)
        {
            return;
        }

        BlockState currentState = world.getBlockState(fromPos);
        if (currentState != Blocks.AIR.defaultBlockState())
        {
            return;
        }

        if (world.getBlockState(fromPos.below()).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            ensureFluidBarrier(world, pos);

            BlockEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
            }
        }
    }

    public static void ensureFluidBarrier(Level world, BlockPos generatorPos)
    {
        BlockPos barrierPos = generatorPos.above(2);
        BlockState barrierState = world.getBlockState(barrierPos);
        Block barrierBlock = barrierState.getBlock();

        if (barrierBlock == ModBlocks.FLUID_BARRIER)
        {
            return;
        }

        if (barrierBlock != Blocks.AIR)
        {
            // The barrier's full shape is what stops liquids from passing through this
            // slot — never destroy a liquid that reached it, that would just turn it
            // into air. When the liquid disappears, the barrier will be placed here.
            if (!barrierState.getFluidState().isEmpty() || barrierBlock instanceof LiquidBlock)
            {
                return;
            }

            boolean generatedSlotEmpty = world.getBlockState(generatorPos.above()).getBlock() == Blocks.AIR;
            boolean nonSolidOccupant = !barrierState.isSolidRender(world, barrierPos)
                    && (generatedSlotEmpty || !barrierState.canSurvive(world, barrierPos));
            if (nonSolidOccupant)
            {
                world.destroyBlock(barrierPos, true);
                OneBlockUltima.logDebug("[Generator] Dropped non-solid occupant {} at {} via ensureFluidBarrier (generatedSlotEmpty={})", barrierBlock, barrierPos, generatedSlotEmpty);
            }
            else
            {
                return;
            }
        }

        world.setBlock(barrierPos, ModBlocks.FLUID_BARRIER.defaultBlockState(), 3);
        OneBlockUltima.logDebug("[Generator] BARRIER placed at {}", barrierPos);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (world.getBlockState(pos.below()).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            GeneratedBlockRegistry.get(world).remove(pos);
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }
}
