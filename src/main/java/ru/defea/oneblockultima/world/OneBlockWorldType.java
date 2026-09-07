package ru.defea.oneblockultima.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import ru.defea.oneblockultima.OneBlockUltima;

public final class OneBlockWorldType
{
    public static final ResourceKey<Level> DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "oneblock"));

    public static final ResourceKey<DimensionType> DIMENSION_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "oneblock"));

    public static final ResourceKey<WorldPreset> PRESET_KEY =
            ResourceKey.create(Registries.WORLD_PRESET, ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "oneblock"));

    public static final ResourceKey<LevelStem> LEVEL_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "oneblock"));

    private OneBlockWorldType()
    {
    }

    public static void init()
    {
        // WorldPreset is datapack-driven in 1.21 and is provided as a JSON datapack
        // (data/oneblockultima/worldgen/world_preset/oneblock.json); nothing to register here.
    }

    public static boolean isOneBlockWorld(Level world)
    {
        return world != null && world.dimensionTypeRegistration().is(DIMENSION_TYPE);
    }

    public static boolean isEnabled(Level world)
    {
        return isOneBlockWorld(world);
    }
}
