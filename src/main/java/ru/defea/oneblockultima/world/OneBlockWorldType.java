package ru.defea.oneblockultima.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderFlat;

public class OneBlockWorldType extends WorldType
{
    public static final OneBlockWorldType ONE_BLOCK = new OneBlockWorldType();

    public OneBlockWorldType()
    {
        super("one_block");
    }

    public static void init()
    {
        // Force class loading / register the world type.
        ONE_BLOCK.getWorldTypeName();
    }

    public String getFlatGeneratorOptions()
    {
        // 1.7.10 flat preset format: <version>;<layers>;<biome>;<features>
        // Version must be 0..2 (3+ silently falls back to the default flat generator).
        // 1.7.10 has no void biome (added in 1.8), so use a single air layer + plains.
        return "2;0;1";
    }

    @Override
    public boolean isCustomizable()
    {
        return false;
    }

    @Override
    public IChunkProvider getChunkGenerator(World world, String generatorOptions)
    {
        // Use default flat world template if no options provided
        if (generatorOptions == null || generatorOptions.isEmpty())
        {
            generatorOptions = getFlatGeneratorOptions();
        }
        return new ChunkProviderFlat(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled(), generatorOptions);
    }
}
