package ru.defea.oneblockultima.gui.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import static ru.defea.oneblockultima.Constants.BLOCK_CENTER_OFFSET;
import static ru.defea.oneblockultima.Constants.PLAYER_INTERACT_RANGE_SQ;

public class ContainerClaimGenerator extends Container
{
    private final World world;
    private final BlockPos generatorPos;
    private final EntityPlayer player;

    public ContainerClaimGenerator(EntityPlayer player, World world, BlockPos generatorPos)
    {
        this.world = world;
        this.generatorPos = generatorPos;
        this.player = player;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn)
    {
        return playerIn.getDistanceSq(generatorPos.getX() + BLOCK_CENTER_OFFSET, generatorPos.getY() + BLOCK_CENTER_OFFSET, generatorPos.getZ() + BLOCK_CENTER_OFFSET) <= PLAYER_INTERACT_RANGE_SQ;
    }

    public boolean claimOwnership()
    {
        if (world.isRemote)
        {
            return false;
        }

        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null || !generator.isFree())
        {
            return false;
        }

        if (!generator.tryAssignOwnerIfEligible(player.getUniqueID()))
        {
            return false;
        }

        if (player instanceof EntityPlayerMP)
        {
            PacketSyncPlayerData.sendToPlayer(player);
        }
        return true;
    }

    public TileEntityOneBlockGenerator getGenerator()
    {
        if (world.getTileEntity(generatorPos) instanceof TileEntityOneBlockGenerator)
        {
            return (TileEntityOneBlockGenerator) world.getTileEntity(generatorPos);
        }

        return null;
    }

    public BlockPos getGeneratorPos()
    {
        return generatorPos;
    }
}
