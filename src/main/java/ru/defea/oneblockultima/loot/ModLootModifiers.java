package ru.defea.oneblockultima.loot;

import com.mojang.serialization.MapCodec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.defea.oneblockultima.OneBlockUltima;

public final class ModLootModifiers
{
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, OneBlockUltima.MODID);

    public static final RegistryObject<MapCodec<OneBlockDropModifier>> ONE_BLOCK_DROPS =
            MODIFIERS.register("oneblock_drops", () -> OneBlockDropModifier.CODEC);

    public static void register(IEventBus bus)
    {
        MODIFIERS.register(bus);
    }
}
