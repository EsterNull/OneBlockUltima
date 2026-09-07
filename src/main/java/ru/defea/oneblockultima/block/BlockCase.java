package ru.defea.oneblockultima.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

public class BlockCase extends Block
{
    private static final AABB CASE_AABB = new AABB(0.1875D, 0.0D, 0.125D, 0.8125D, 0.6875D, 0.875D);

    public BlockCase()
    {
        super(Properties.of().mapColor(MapColor.STONE).strength(1.5F, 10.0F).noOcclusion());
    }

    @Nonnull
    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter source, @Nonnull BlockPos pos, @Nonnull CollisionContext context)
    {
        return Shapes.create(CASE_AABB);
    }
}
