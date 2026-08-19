package ru.defea.oneblockultima.tile;

import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.registry.GameRegistry;
import ru.defea.oneblockultima.OneBlockUltima;

public final class ModTileEntities
{
    private ModTileEntities()
    {
    }

    public static void register()
    {
        GameRegistry.registerTileEntity(
                TileEntityOneBlockGenerator.class,
                new ResourceLocation(OneBlockUltima.MODID, "one_block_generator").toString()
        );
    }
}
