package ru.defea.oneblockultima.gui.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import static ru.defea.oneblockultima.Constants.BLOCK_CENTER_OFFSET;
import static ru.defea.oneblockultima.Constants.PLAYER_INTERACT_RANGE_SQ;

public class ContainerClaimGenerator extends AbstractContainerMenu
{
    public static final MenuType<ContainerClaimGenerator> TYPE = ModMenus.CLAIM_GENERATOR.get();

    private final Level level;
    private final BlockPos generatorPos;
    private final Player player;

    public ContainerClaimGenerator(int id, Inventory inv, BlockPos generatorPos)
    {
        super(TYPE, id);
        this.player = inv.player;
        this.level = player.level();
        this.generatorPos = generatorPos;
    }

    public ContainerClaimGenerator(int id, Inventory inv, FriendlyByteBuf buf)
    {
        this(id, inv, buf.readBlockPos());
    }

    @Override
    public boolean stillValid(Player playerIn)
    {
        return playerIn.distanceToSqr(generatorPos.getX() + BLOCK_CENTER_OFFSET, generatorPos.getY() + BLOCK_CENTER_OFFSET, generatorPos.getZ() + BLOCK_CENTER_OFFSET) <= PLAYER_INTERACT_RANGE_SQ;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index)
    {
        return ItemStack.EMPTY;
    }

    public boolean claimOwnership()
    {
        if (level.isClientSide)
        {
            return false;
        }

        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null || !generator.isFree())
        {
            return false;
        }

        if (!generator.tryAssignOwnerIfEligible(player.getUUID()))
        {
            return false;
        }

        if (player instanceof ServerPlayer)
        {
            PacketSyncPlayerData.sendToPlayer((ServerPlayer) player);
        }
        return true;
    }

    public TileEntityOneBlockGenerator getGenerator()
    {
        if (level.getBlockEntity(generatorPos) instanceof TileEntityOneBlockGenerator)
        {
            return (TileEntityOneBlockGenerator) level.getBlockEntity(generatorPos);
        }

        return null;
    }

    public BlockPos getGeneratorPos()
    {
        return generatorPos;
    }

    public Player getPlayer()
    {
        return player;
    }

    public Level getLevel()
    {
        return level;
    }
}
