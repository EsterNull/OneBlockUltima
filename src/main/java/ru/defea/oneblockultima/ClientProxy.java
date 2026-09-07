package ru.defea.oneblockultima;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ru.defea.oneblockultima.gui.GuiCaseRoulette;
import ru.defea.oneblockultima.gui.GuiGuideBook;

public class ClientProxy extends CommonProxy
{
    @Override
    public void openGuideBookGui(Player player)
    {
        Minecraft.getInstance().setScreen(new GuiGuideBook());
    }

    @Override
    public void openCaseRouletteGui(Player player, ItemStack caseStack)
    {
        Minecraft.getInstance().setScreen(new GuiCaseRoulette(caseStack));
    }
}
