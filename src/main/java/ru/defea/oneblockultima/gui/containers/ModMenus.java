package ru.defea.oneblockultima.gui.containers;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.defea.oneblockultima.OneBlockUltima;

public final class ModMenus
{
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.Keys.MENU_TYPES, OneBlockUltima.MODID);

    public static final RegistryObject<MenuType<ContainerOneBlock>> ONE_BLOCK =
            MENUS.register("one_block", () -> IForgeMenuType.create((IContainerFactory<ContainerOneBlock>) (windowId, inv, buf) -> new ContainerOneBlock(windowId, inv, buf)));

    public static final RegistryObject<MenuType<ContainerClaimGenerator>> CLAIM_GENERATOR =
            MENUS.register("claim_generator", () -> IForgeMenuType.create((IContainerFactory<ContainerClaimGenerator>) (windowId, inv, buf) -> new ContainerClaimGenerator(windowId, inv, buf)));

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus)
    {
        MENUS.register(bus);
    }
}
