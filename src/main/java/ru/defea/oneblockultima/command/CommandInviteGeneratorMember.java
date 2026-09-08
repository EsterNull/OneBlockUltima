package ru.defea.oneblockultima.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

public final class CommandInviteGeneratorMember
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("inviteGeneratorMember")
                .requires(s -> s.hasPermission(0))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"));
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        })));
    }

    private static void execute(CommandSourceStack source, ServerPlayer target)
    {
        ServerPlayer owner = source.getPlayer();
        if (owner == null)
        {
            source.sendFailure(Component.translatable("command.only_player").withStyle(ChatFormatting.RED));
            return;
        }

        Level world = owner.level();
        BlockPos generatorPos = owner.blockPosition().below();

        if (world.getBlockState(generatorPos).getBlock() != ModBlocks.ONE_BLOCK_GENERATOR)
        {
            source.sendFailure(Component.translatable("command.not_near_generator").withStyle(ChatFormatting.RED));
            return;
        }

        BlockEntity tileEntity = world.getBlockEntity(generatorPos);
        if (!(tileEntity instanceof TileEntityOneBlockGenerator))
        {
            source.sendFailure(Component.translatable("command.no_generator").withStyle(ChatFormatting.RED));
            return;
        }

        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
        if (!generator.isOwner(owner))
        {
            source.sendFailure(Component.translatable("command.inviteGeneratorMember.owner_only").withStyle(ChatFormatting.RED));
            return;
        }

        if (target.getUUID().equals(owner.getUUID()))
        {
            source.sendFailure(Component.translatable("command.inviteGeneratorMember.self_invite").withStyle(ChatFormatting.RED));
            return;
        }

        if (generator.isMemberLimitReached())
        {
            source.sendFailure(Component.translatable("command.inviteGeneratorMember.member_limit").withStyle(ChatFormatting.RED));
            return;
        }

        generator.addPendingInvite(target.getUUID(), owner.getUUID(), Math.max(1, ModSettings.get().getInviteDurationTicks()));
        target.sendSystemMessage(Component.translatable("command.inviteGeneratorMember.invitation_received").withStyle(ChatFormatting.GREEN));
        target.sendSystemMessage(Component.translatable("command.inviteGeneratorMember.access_warning").withStyle(ChatFormatting.YELLOW));
        owner.sendSystemMessage(Component.translatable("command.inviteGeneratorMember.invitation_sent", target.getName()).withStyle(ChatFormatting.GREEN));
    }
}
