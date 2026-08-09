package ru.defea.oneblockultima.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
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
        if (ID == ONE_BLOCK_GUI)
        {
            return new ContainerOneBlock(player, world, new BlockPos(x, y, z));
        }

        if (ID == CLAIM_GENERATOR_GUI)
        {
            return new ContainerClaimGenerator(player, world, new BlockPos(x, y, z));
        }

        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        if (ID == ONE_BLOCK_GUI)
        {
            return new GuiOneBlock(player, world, new BlockPos(x, y, z));
        }

        if (ID == CLAIM_GENERATOR_GUI)
        {
            return new GuiClaimGenerator(new BlockPos(x, y, z));
        }

        return null;
    }

    public static void open(EntityPlayer player, BlockPos generatorPos)
    {
        player.openGui(OneBlockUltima.instance, ONE_BLOCK_GUI, player.world, generatorPos.getX(), generatorPos.getY(), generatorPos.getZ());
    }

    public static void openClaimScreen(EntityPlayer player, BlockPos generatorPos)
    {
        player.openGui(OneBlockUltima.instance, CLAIM_GENERATOR_GUI, player.world, generatorPos.getX(), generatorPos.getY(), generatorPos.getZ());
    }
}
