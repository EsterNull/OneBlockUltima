package ru.defea.oneblockultima;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.client.RenderItemGeneratorCube;
import ru.defea.oneblockultima.event.ModEventsClient;
import ru.defea.oneblockultima.gui.GuiGuideBook;

@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy
{
    @Override
    public void preInit()
    {
        ModEventsClient modEventsClient = new ModEventsClient();
        MinecraftForge.EVENT_BUS.register(modEventsClient);
        FMLCommonHandler.instance().bus().register(modEventsClient);

        MinecraftForgeClient.registerItemRenderer(
                Item.getItemFromBlock(ModBlocks.ONE_BLOCK_GENERATOR),
                new RenderItemGeneratorCube());
    }

    @Override
    public void openGuideBookGui(EntityPlayer player)
    {
        Minecraft.getMinecraft().displayGuiScreen(new GuiGuideBook());
    }
}