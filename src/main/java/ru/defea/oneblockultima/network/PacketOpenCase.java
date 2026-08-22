package ru.defea.oneblockultima.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import ru.defea.oneblockultima.util.CaseUtil;

import java.util.List;

public class PacketOpenCase implements IMessage
{
    private NBTTagCompound caseNbt;

    public PacketOpenCase()
    {
    }

    public PacketOpenCase(NBTTagCompound caseNbt)
    {
        this.caseNbt = caseNbt;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        caseNbt = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeTag(buf, caseNbt);
    }

    public static class Handler implements IMessageHandler<PacketOpenCase, IMessage>
    {
        @Override
        public IMessage onMessage(PacketOpenCase message, MessageContext ctx)
        {
            if (ctx.side.isClient())
            {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> process(player));
            return null;
        }

        private static void process(EntityPlayerMP player)
        {
            if (player == null)
            {
                return;
            }
            ItemStack held = findCaseStack(player);
            if (held.isEmpty())
            {
                return;
            }

            List<CaseUtil.WeightedStack> contents = CaseUtil.readContents(held);
            int index = CaseUtil.rollIndex(contents, player.world.rand);
            if (index < 0)
            {
                return;
            }

            ItemStack winner = contents.get(index).stack.copy();
            if (!player.isCreative())
            {
                held.shrink(1);
            }
            if (!player.inventory.addItemStackToInventory(winner.copy()))
            {
                dropItem(player, winner.copy());
            }

            player.sendMessage(new TextComponentTranslation("gui.oneblockultima.case.reward", winner.getDisplayName()));
            ModMessages.sendToPlayer(new PacketCaseResult(index), player);
        }

        private static ItemStack findCaseStack(EntityPlayerMP player)
        {
            ItemStack main = player.getHeldItemMainhand();
            if (CaseUtil.isCaseItem(main))
            {
                return main;
            }
            ItemStack off = player.getHeldItemOffhand();
            if (CaseUtil.isCaseItem(off))
            {
                return off;
            }
            return ItemStack.EMPTY;
        }

        private static void dropItem(EntityPlayerMP player, ItemStack stack)
        {
            EntityItem entityItem = new EntityItem(player.world, player.posX, player.posY + 0.5D, player.posZ, stack);
            entityItem.setNoPickupDelay();
            player.world.spawnEntity(entityItem);
        }
    }
}