package ru.defea.oneblockultima.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.RandomSource;

import javax.annotation.Nonnull;

public class BlockFluidBarrier extends Block
{
    public BlockFluidBarrier()
    {
        super(Properties.of().mapColor(MapColor.NONE).strength(-1.0F, 6000000.0F)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .noOcclusion());
    }

    // Fully transparent to the player's sight/raycast: left-clicks and right-clicks pass straight
    // through this slot to the block behind it (the generated block above the generator).
    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.BlockGetter source, @Nonnull BlockPos pos, @Nonnull net.minecraft.world.phys.shapes.CollisionContext context)
    {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getInteractionShape(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.BlockGetter level, @Nonnull BlockPos pos)
    {
        return Shapes.empty();
    }

    @Nonnull
    @Override
    public VoxelShape getCollisionShape(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.BlockGetter worldIn, @Nonnull BlockPos pos, @Nonnull CollisionContext context)
    {
        // Liquids and pathfinding check collisions via CollisionContext.empty() (its context has no
        // entity). They must see a full cube so liquid cannot flow through this slot in ANY direction.
        // A living entity always queries with a context carrying its entity — those pass through freely.
        if (context instanceof EntityCollisionContext
                && ((EntityCollisionContext) context).getEntity() != null)
        {
            return Shapes.empty();
        }

        return Shapes.block();
    }

    @Nonnull
    @Override
    public InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit)
    {
        // Defensive: clicks reaching this block (they normally pass straight through the empty
        // shape to the generated block / generator behind it) are consumed, never propagated.
        return InteractionResult.SUCCESS;
    }

    @Override
    public float getExplosionResistance(BlockState state, net.minecraft.world.level.BlockGetter world, BlockPos pos, Explosion explosion)
    {
        return 0.0F;
    }

    @Nonnull
    @Override
    public RenderShape getRenderShape(@Nonnull BlockState state)
    {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean isAir(@Nonnull BlockState state)
    {
        return true;
    }

    @Override
    public boolean canBeReplaced(@Nonnull BlockState state, @Nonnull net.minecraft.world.item.context.BlockPlaceContext useContext)
    {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.BlockGetter reader, @Nonnull BlockPos pos)
    {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction(@Nonnull BlockState state)
    {
        return PushReaction.IGNORE;
    }

    @Override
    public void animateTick(@Nonnull BlockState stateIn, @Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull RandomSource rand)
    {
        if (rand.nextInt(3) == 0)
        {
            double x = pos.getX() + 0.2D + 0.6D * rand.nextDouble();
            double y = pos.getY() + 0.2D + 0.6D * rand.nextDouble();
            double z = pos.getZ() + 0.2D + 0.6D * rand.nextDouble();
            double vx = (rand.nextDouble() - 0.5D) * 0.15D;
            double vy = 0.05D + 0.1D * rand.nextDouble();
            double vz = (rand.nextDouble() - 0.5D) * 0.15D;
            worldIn.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, x, y, z, vx, vy, vz);
        }
    }
}
