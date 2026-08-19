package ru.defea.oneblockultima;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import cpw.mods.fml.client.IModGuiFactory;
import ru.defea.oneblockultima.gui.ModMainSettings;

import java.util.Set;

@SuppressWarnings("unused")
public class ModGuiFactory implements IModGuiFactory
{
    @Override
    public void initialize(Minecraft minecraftInstance)
    {
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass()
    {
        return ModMainSettings.class;
    }

    @Override
    public Set<IModGuiFactory.RuntimeOptionCategoryElement> runtimeGuiCategories()
    {
        return null;
    }

    @Override
    public IModGuiFactory.RuntimeOptionGuiHandler getHandlerFor(IModGuiFactory.RuntimeOptionCategoryElement element)
    {
        return null;
    }
}
