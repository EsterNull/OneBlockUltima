package ru.defea.oneblockultima.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;

public abstract class BlockCompressedBase extends Block {
    private final int maxLevel;
    private final IntegerProperty levelProperty;
    private final String baseName;

    protected BlockCompressedBase(IntegerProperty levelProperty, Properties properties, String baseName)
    {
        super(properties);
        this.levelProperty = levelProperty;
        this.baseName = baseName;
        this.maxLevel = levelProperty.getPossibleValues().size();
        this.registerDefaultState(this.defaultBlockState().setValue(levelProperty, 0));
    }

    public abstract IntegerProperty getLevelProperty();

    public String getBaseName()
    {
        return this.baseName;
    }

    public int getMaxLevel()
    {
        return this.maxLevel;
    }

    public int getLevel(BlockState state)
    {
        return state.getValue(this.getLevelProperty());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(this.getLevelProperty());
    }
}
