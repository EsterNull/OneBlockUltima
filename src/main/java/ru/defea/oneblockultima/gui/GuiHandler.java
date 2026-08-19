package ru.defea.oneblockultima.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.gui.containers.ContainerClaimGenerator;
import ru.defea.oneblockultima.gui.containers.ContainerOneBlock;

import javax.annotation.Nullable;

public class GuiHandler implements IGuiHandler
{
    public static final int ONE_BLOCK_GUI = 0;
    public static final int CLAIM_GENERATOR_GUI = 1;

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        OneBlockUltima.getRawLogger().info("[MenuDebug] getServerGuiElement ID={} at ({},{},{})", ID, x, y, z);
        if (ID == ONE_BLOCK_GUI)
        {
            return new ContainerOneBlock(player, world, x, y, z);
        }

        if (ID == CLAIM_GENERATOR_GUI)
        {
            return new ContainerClaimGenerator(player, world, x, y, z);
        }

        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        OneBlockUltima.getRawLogger().info("[MenuDebug] getClientGuiElement ID={} at ({},{},{})", ID, x, y, z);
        if (ID == ONE_BLOCK_GUI)
        {
            return new GuiOneBlock(player, world, x, y, z);
        }

        if (ID == CLAIM_GENERATOR_GUI)
        {
            return new GuiClaimGenerator(x, y, z);
        }

        return null;
    }

    public static void open(EntityPlayer player, int generatorX, int generatorY, int generatorZ)
    {
        OneBlockUltima.getRawLogger().info("[MenuDebug] GuiHandler.open ONE_BLOCK_GUI at ({},{},{})", generatorX, generatorY, generatorZ);
        player.openGui(OneBlockUltima.instance, ONE_BLOCK_GUI, player.worldObj, generatorX, generatorY, generatorZ);
    }

    public static void openClaimScreen(EntityPlayer player, int generatorX, int generatorY, int generatorZ)
    {
        OneBlockUltima.getRawLogger().info("[MenuDebug] GuiHandler.openClaimScreen CLAIM_GENERATOR_GUI at ({},{},{})", generatorX, generatorY, generatorZ);
        player.openGui(OneBlockUltima.instance, CLAIM_GENERATOR_GUI, player.worldObj, generatorX, generatorY, generatorZ);
    }
}
