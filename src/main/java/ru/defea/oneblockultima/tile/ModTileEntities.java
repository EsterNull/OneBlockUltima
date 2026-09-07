package ru.defea.oneblockultima.tile;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.ModBlocks;

public final class ModTileEntities
{
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, OneBlockUltima.MODID);

    public static final RegistryObject<BlockEntityType<TileEntityOneBlockGenerator>> ONE_BLOCK_GENERATOR =
            TILES.register("one_block_generator", () -> BlockEntityType.Builder.of(TileEntityOneBlockGenerator::new, ModBlocks.ONE_BLOCK_GENERATOR).build(null));

    private ModTileEntities()
    {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus)
    {
        TILES.register(bus);
    }
}
