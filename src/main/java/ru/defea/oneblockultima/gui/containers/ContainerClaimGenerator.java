package ru.defea.oneblockultima.gui.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.world.World;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import static ru.defea.oneblockultima.Constants.BLOCK_CENTER_OFFSET;
import static ru.defea.oneblockultima.Constants.PLAYER_INTERACT_RANGE_SQ;

public class ContainerClaimGenerator extends Container
{
    private static final int OFFSCREEN_SLOT_X = -32000;
    private static final int OFFSCREEN_SLOT_Y = -32000;

    private final World world;
    private final int generatorX;
    private final int generatorY;
    private final int generatorZ;
    private final EntityPlayer player;

    public ContainerClaimGenerator(EntityPlayer player, World world, int generatorX, int generatorY, int generatorZ)
    {
        this.world = world;
        this.generatorX = generatorX;
        this.generatorY = generatorY;
        this.generatorZ = generatorZ;
        this.player = player;

        addPlayerInventorySlots(player.inventory);
    }

    private void addPlayerInventorySlots(InventoryPlayer playerInv)
    {
        for (int i = 0; i < 9; ++i)
        {
            this.addSlotToContainer(new Slot(playerInv, i, OFFSCREEN_SLOT_X, OFFSCREEN_SLOT_Y));
        }
        for (int i = 9; i < 36; ++i)
        {
            this.addSlotToContainer(new Slot(playerInv, i, OFFSCREEN_SLOT_X, OFFSCREEN_SLOT_Y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn)
    {
        return playerIn.getDistanceSq(generatorX + BLOCK_CENTER_OFFSET, generatorY + BLOCK_CENTER_OFFSET, generatorZ + BLOCK_CENTER_OFFSET) <= PLAYER_INTERACT_RANGE_SQ;
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
        if (world.getTileEntity(generatorX, generatorY, generatorZ) instanceof TileEntityOneBlockGenerator)
        {
            return (TileEntityOneBlockGenerator) world.getTileEntity(generatorX, generatorY, generatorZ);
        }

        return null;
    }

    public int getGeneratorX()
    {
        return generatorX;
    }

    public int getGeneratorY()
    {
        return generatorY;
    }

    public int getGeneratorZ()
    {
        return generatorZ;
    }
}
