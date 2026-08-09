package ru.defea.oneblockultima;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import ru.defea.oneblockultima.gui.GuiGuideBook;

@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy
{
    @Override
    public void openGuideBookGui(EntityPlayer player)
    {
        Minecraft.getMinecraft().displayGuiScreen(new GuiGuideBook());
    }
}