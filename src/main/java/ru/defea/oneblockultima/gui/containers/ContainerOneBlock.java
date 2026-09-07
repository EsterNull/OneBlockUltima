package ru.defea.oneblockultima.gui.containers;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.event.ModEvents;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.network.PacketOneBlockAction;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;

import static ru.defea.oneblockultima.Constants.BLOCK_CENTER_OFFSET;
import static ru.defea.oneblockultima.Constants.PLAYER_INTERACT_RANGE_SQ;

public class ContainerOneBlock extends AbstractContainerMenu
{
    public static final MenuType<ContainerOneBlock> TYPE = ModMenus.ONE_BLOCK.get();

    private final Level level;
    private final BlockPos generatorPos;
    private final Player player;

    public ContainerOneBlock(int id, Inventory inv, BlockPos generatorPos)
    {
        super(TYPE, id);
        this.player = inv.player;
        this.level = player.level();
        this.generatorPos = generatorPos;

        if (!level.isClientSide && player instanceof ServerPlayer)
        {
            TileEntityOneBlockGenerator generator = getGenerator();
            if (generator != null)
            {
                ModEvents.ensureGeneratorAccess(level, generatorPos, player, generator);
            }

            PacketSyncPlayerData.sendToPlayer((ServerPlayer) player);

            if (generator != null)
            {
                ClientboundBlockEntityDataPacket packet = generator.getUpdatePacket();
                if (packet != null)
                {
                    ((ServerPlayer) player).connection.send(packet);
                }
            }
        }
    }

    public ContainerOneBlock(int id, Inventory inv, FriendlyByteBuf buf)
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

    public void selectSet(String setId)
    {
        if (level.isClientSide)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.SELECT_SET, setId));
            return;
        }

        applySelectSet(setId);
    }

    public void upgradeSet(String setId)
    {
        if (level.isClientSide)
        {
            applyLocalBalancePreview(setId);
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.UPGRADE_SET, setId));
            return;
        }

        applyUpgradeSet(setId);
    }

    private void applyLocalBalancePreview(String setId)
    {
        if (!level.isClientSide)
        {
            return;
        }

        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null)
        {
            return;
        }

        BlockSetConfig.BlockSetDefinition set = BlockSetConfig.get().getSet(setId);
        if (set == null)
        {
            return;
        }

        int currentLevel = generator.getSetLevel(setId);
        int cost = currentLevel <= 0 ? set.unlockCost : set.getLevel(currentLevel + 1) != null ? set.getLevel(currentLevel + 1).upgradeCost : 0;
        if (cost <= 0)
        {
            return;
        }

        ru.defea.oneblockultima.capability.IOneBlockPlayerData data = ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider.get(player);
        if (data == null || data.getCurrency() < cost)
        {
            return;
        }

        data.spendCurrency(cost);
        ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider.saveToEntity(player, data);
    }

    public void toggleFluidGeneration()
    {
        if (level.isClientSide)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.TOGGLE_FLUIDS, ""));
            return;
        }

        applyToggleFluidGeneration();
    }

    public void toggleMobGeneration()
    {
        if (level.isClientSide)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.TOGGLE_MOBS, ""));
            return;
        }

        applyToggleMobGeneration();
    }

    public void toggleChestGeneration()
    {
        if (level.isClientSide)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.TOGGLE_CHESTS, ""));
            return;
        }

        applyToggleChestGeneration();
    }

    public void toggleSaplingGeneration()
    {
        if (level.isClientSide)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.TOGGLE_SAPLINGS, ""));
            return;
        }

        applyToggleSaplingGeneration();
    }

    public void applySelectSet(String setId)
    {
        OneBlockUltima.logDebug("[OneBlock] applySelectSet called with setId: " + setId);

        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null)
        {
            OneBlockUltima.logDebug("[OneBlock] Generator is NULL!");
            return;
        }

        BlockSetConfig.BlockSetDefinition set = BlockSetConfig.get().getSet(setId);
        if (set == null)
        {
            OneBlockUltima.logDebug("[OneBlock] Set is NULL in config!");
            return;
        }

        int currentLevel = generator.getSetLevel(setId);
        if (currentLevel <= 0)
        {
            OneBlockUltima.logDebug("[OneBlock] Set not unlocked! Level: " + currentLevel);
            return;
        }

        String currentSelected = generator.getSelectedSetId();
        if (setId.equals(currentSelected))
        {
            OneBlockUltima.logDebug("[OneBlock] Set already selected: " + setId);
            return;
        }

        OneBlockUltima.logDebug("[OneBlock] Setting selectedSetId on generator...");
        generator.setSelectedSetId(setId);
        if (!generator.ensureOwnership(player.getUUID()))
        {
            generator.setSelectedSetId(currentSelected);
            return;
        }

        OneBlockUltima.logDebug("[OneBlock] Generator selectedSetId is now: " + generator.getSelectedSetId());

        if (level.isEmptyBlock(generator.getBlockPos().above()))
        {
            generator.tryGenerateBlock();
        }
        broadcastChanges();

        if (player instanceof ServerPlayer)
        {
            ServerPlayer playerMP = (ServerPlayer) player;

            ClientboundBlockEntityDataPacket packet = generator.getUpdatePacket();
            if (packet != null)
            {
                playerMP.connection.send(packet);
            }

            PacketSyncPlayerData.sendToPlayer((ServerPlayer) player);
        }
    }

    public boolean applyUpgradeSet(String setId)
    {
        OneBlockUltima.logDebug("[OneBlock] applyUpgradeSet called with setId: " + setId);

        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null)
        {
            OneBlockUltima.logDebug("[OneBlock] Generator is NULL!");
            return false;
        }

        BlockSetConfig.BlockSetDefinition set = BlockSetConfig.get().getSet(setId);
        if (set == null)
        {
            OneBlockUltima.logDebug("[OneBlock] Set is NULL in config!");
            return false;
        }

        String currentSelected = generator.getSelectedSetId();

        ru.defea.oneblockultima.capability.IOneBlockPlayerData data =
                ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            OneBlockUltima.logDebug("[OneBlock] Player data is NULL!");
            if (player instanceof ServerPlayer)
                return false;
        }

        int currentLevel = generator.getSetLevel(setId);
        OneBlockUltima.logDebug("[OneBlock] Current level: " + currentLevel);

        if (currentLevel <= 0)
        {
            if (!set.hasUnlockRequirementsMet(data, generator))
            {
                if (player instanceof ServerPlayer)
                {
                    player.sendSystemMessage(Component.translatable("gui.oneblockultima.msg.unlock_requirements").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
                }
                return false;
            }

            int cost = set.unlockCost;
            assert data != null;
            double currency = data.getCurrency();

            OneBlockUltima.logDebug("[OneBlock] Unlock attempt - Have: " + currency + ", Need: " + cost);

            if (currency < cost)
            {
                if (player instanceof ServerPlayer)
                {
                    player.sendSystemMessage(Component.translatable("gui.oneblockultima.msg.need")
                            .append(Component.literal(": " + cost + ", "))
                            .append(Component.translatable("gui.oneblockultima.msg.have"))
                            .append(Component.literal(": " + currency))
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
                }
                return false;
            }

            if (!data.spendCurrency(cost))
            {
                if (player instanceof ServerPlayer)
                {
                    player.sendSystemMessage(Component.translatable("gui.oneblockultima.msg.unlock_fail").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
                }
                return false;
            }

            OneBlockPlayerDataProvider.saveToEntity(player, data);

            boolean success = generator.upgradeSet(setId, cost, set.getMaxLevel());
            if (!success)
            {
                data.addCurrency(cost);
                if (player instanceof ServerPlayer)
                {
                    player.sendSystemMessage(Component.translatable("gui.oneblockultima.msg.unlock_fail").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
                }
                return false;
            }

            generator.setSelectedSetId(setId);
            if (!generator.ensureOwnership(player.getUUID()))
            {
                generator.setSelectedSetId(currentSelected);
                return false;
            }

            OneBlockUltima.logDebug("[OneBlock] Set unlocked and selected: " + setId);
            if (player instanceof ServerPlayer)
            {
                player.sendSystemMessage(Component.translatable("gui.oneblockultima.msg.unlocked").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
            }
        }
        else
        {
            BlockSetConfig.SetLevelDefinition nextLevel = set.getLevel(currentLevel + 1);
            if (nextLevel == null)
            {
                OneBlockUltima.logDebug("[OneBlock] Max level reached!");
                return false;
            }

            int cost = nextLevel.upgradeCost;
            assert data != null;
            double currency = data.getCurrency();

            OneBlockUltima.logDebug("[OneBlock] Upgrade attempt - Have: " + currency + ", Need: " + cost);

            if (currency < cost)
            {
                if (player instanceof ServerPlayer)
                {
                    player.sendSystemMessage(Component.translatable("gui.oneblockultima.msg.need")
                            .append(Component.literal(": " + cost + ", "))
                            .append(Component.translatable("gui.oneblockultima.msg.have"))
                            .append(Component.literal(": " + currency))
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
                }
                return false;
            }

            if (!data.spendCurrency(cost))
            {
                if (player instanceof ServerPlayer)
                {
                    player.sendSystemMessage(Component.translatable("gui.oneblockultima.msg.upgrade_fail").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
                }
                return false;
            }

            OneBlockPlayerDataProvider.saveToEntity(player, data);

            boolean success = generator.upgradeSet(setId, cost, set.getMaxLevel());
            if (!success)
            {
                data.addCurrency(cost);
                if (player instanceof ServerPlayer)
                {
                    player.sendSystemMessage(Component.translatable("gui.oneblockultima.msg.upgrade_fail").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
                }
                return false;
            }

            generator.setSelectedSetId(setId);
            if (!generator.ensureOwnership(player.getUUID()))
            {
                generator.setSelectedSetId(currentSelected);
                return false;
            }

            OneBlockUltima.logDebug("[OneBlock] Set upgraded to level " + (currentLevel + 1) + " and selected: " + setId);
        }

        broadcastChanges();

        if (player instanceof ServerPlayer)
        {
            ServerPlayer playerMP = (ServerPlayer) player;

            ModEvents.ensureGeneratorAccess(level, generatorPos, player, generator);

            ClientboundBlockEntityDataPacket packet = generator.getUpdatePacket();
            if (packet != null)
            {
                playerMP.connection.send(packet);
            }

            PacketSyncPlayerData.sendToPlayer((ServerPlayer) player);
            OneBlockPlayerDataProvider.saveToEntity(player, data);
        }
        return true;
    }

    public void applyToggleFluidGeneration()
    {
        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null)
        {
            return;
        }

        generator.setDisableFluidGeneration(!generator.isDisableFluidGeneration());
        if (level.isEmptyBlock(generator.getBlockPos().above()))
        {
            generator.tryGenerateBlock();
        }
        updateTileEntity(generator);
    }

    public void applyToggleMobGeneration()
    {
        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null)
        {
            return;
        }

        generator.setDisableMobGeneration(!generator.isDisableMobGeneration());
        if (level.isEmptyBlock(generator.getBlockPos().above()))
        {
            generator.tryGenerateBlock();
        }
        updateTileEntity(generator);
    }

    public void applyToggleChestGeneration()
    {
        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null)
        {
            return;
        }

        generator.setDisableChestGeneration(!generator.isDisableChestGeneration());
        if (level.isEmptyBlock(generator.getBlockPos().above()))
        {
            generator.tryGenerateBlock();
        }
        updateTileEntity(generator);
    }

    public void applyToggleSaplingGeneration()
    {
        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null)
        {
            return;
        }

        generator.setDisableSaplingGeneration(!generator.isDisableSaplingGeneration());
        if (level.isEmptyBlock(generator.getBlockPos().above()))
        {
            generator.tryGenerateBlock();
        }
        updateTileEntity(generator);
    }

    private void updateTileEntity(TileEntityOneBlockGenerator generator) {
        if (player instanceof ServerPlayer)
        {
            ServerPlayer playerMP = (ServerPlayer) player;
            ClientboundBlockEntityDataPacket packet = generator.getUpdatePacket();
            if (packet != null)
            {
                playerMP.connection.send(packet);
            }
        }
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
