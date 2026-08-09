package ru.defea.oneblockultima.gui.containers;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
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

public class ContainerOneBlock extends Container
{
    private final World world;
    private final BlockPos generatorPos;
    private final EntityPlayer player;

    public ContainerOneBlock(EntityPlayer player, World world, BlockPos generatorPos)
    {
        this.world = world;
        this.generatorPos = generatorPos;
        this.player = player;

        if (!world.isRemote && player instanceof EntityPlayerMP)
        {
            TileEntityOneBlockGenerator generator = getGenerator();
            if (generator != null)
            {
                ModEvents.ensureGeneratorAccess(world, generatorPos, player, generator);
            }

            PacketSyncPlayerData.sendToPlayer(player);

            if (generator != null)
            {
                net.minecraft.network.play.server.SPacketUpdateTileEntity packet = generator.getUpdatePacket();
                if (packet != null)
                {
                    ((EntityPlayerMP) player).connection.sendPacket(packet);
                }
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn)
    {
        return playerIn.getDistanceSq(generatorPos.getX() + BLOCK_CENTER_OFFSET, generatorPos.getY() + BLOCK_CENTER_OFFSET, generatorPos.getZ() + BLOCK_CENTER_OFFSET) <= PLAYER_INTERACT_RANGE_SQ;
    }

    public void selectSet(String setId)
    {
        if (world.isRemote)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.SELECT_SET, setId));
            return; // Возвращаем true, чтобы клиент не показывал ошибку
        }

        applySelectSet(setId);
    }

    public void upgradeSet(String setId)
    {
        if (world.isRemote)
        {
            applyLocalBalancePreview(setId);
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.UPGRADE_SET, setId));
            return; // Возвращаем true, чтобы клиент не показывал ошибку
        }

        applyUpgradeSet(setId);
    }

    private void applyLocalBalancePreview(String setId)
    {
        if (!world.isRemote)
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
        if (world.isRemote)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.TOGGLE_FLUIDS, ""));
            return;
        }

        applyToggleFluidGeneration();
    }

    public void toggleMobGeneration()
    {
        if (world.isRemote)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.TOGGLE_MOBS, ""));
            return;
        }

        applyToggleMobGeneration();
    }

    public void toggleChestGeneration()
    {
        if (world.isRemote)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.TOGGLE_CHESTS, ""));
            return;
        }

        applyToggleChestGeneration();
    }

    public void toggleSaplingGeneration()
    {
        if (world.isRemote)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.TOGGLE_SAPLINGS, ""));
            return;
        }

        applyToggleSaplingGeneration();
    }

    public void applySelectSet(String setId)
    {
        OneBlockUltima.getLogger().info("[OneBlock] applySelectSet called with setId: " + setId);

        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null)
        {
            OneBlockUltima.getLogger().info("[OneBlock] Generator is NULL!");
            return;
        }

        BlockSetConfig.BlockSetDefinition set = BlockSetConfig.get().getSet(setId);
        if (set == null)
        {
            OneBlockUltima.getLogger().info("[OneBlock] Set is NULL in config!");
            return;
        }

        int currentLevel = generator.getSetLevel(setId);
        if (currentLevel <= 0)
        {
            OneBlockUltima.getLogger().info("[OneBlock] Set not unlocked! Level: " + currentLevel);
            return;
        }

        // Check whether this set is already selected
        String currentSelected = generator.getSelectedSetId();
        if (setId.equals(currentSelected))
        {
            OneBlockUltima.getLogger().info("[OneBlock] Set already selected: " + setId);
            return;
        }

        OneBlockUltima.getLogger().info("[OneBlock] Setting selectedSetId on generator...");
        generator.setSelectedSetId(setId);
        if (!generator.ensureOwnership(player.getUniqueID()))
        {
            generator.setSelectedSetId(currentSelected);
            return;
        }

        OneBlockUltima.getLogger().info("[OneBlock] Generator selectedSetId is now: " + generator.getSelectedSetId());

        if (world.isAirBlock(generator.getPos().up()))
        {
            generator.tryGenerateBlock();
        }
        detectAndSendChanges();

        if (player instanceof EntityPlayerMP)
        {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            // Send the tile update
            net.minecraft.network.play.server.SPacketUpdateTileEntity packet = generator.getUpdatePacket();
            if (packet != null)
            {
                playerMP.connection.sendPacket(packet);
            }

            // Synchronize the player data
            PacketSyncPlayerData.sendToPlayer(player);
        }
    }

    public boolean applyUpgradeSet(String setId)
    {
        OneBlockUltima.getLogger().info("[OneBlock] applyUpgradeSet called with setId: " + setId);

        TileEntityOneBlockGenerator generator = getGenerator();
        if (generator == null)
        {
            OneBlockUltima.getLogger().info("[OneBlock] Generator is NULL!");
            return false;
        }

        BlockSetConfig.BlockSetDefinition set = BlockSetConfig.get().getSet(setId);
        if (set == null)
        {
            OneBlockUltima.getLogger().info("[OneBlock] Set is NULL in config!");
            return false;
        }

        String currentSelected = generator.getSelectedSetId();

        ru.defea.oneblockultima.capability.IOneBlockPlayerData data =
                ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider.get(player);
        if (data == null)
        {
            OneBlockUltima.getLogger().info("[OneBlock] Player data is NULL!");
            if (player instanceof EntityPlayerMP)
                return false;
        }

        int currentLevel = generator.getSetLevel(setId);
        OneBlockUltima.getLogger().info("[OneBlock] Current level: " + currentLevel);

        // Check whether the set is unlocked
        if (currentLevel <= 0)
        {
            // Attempt to unlock the set
            if (!set.hasUnlockRequirementsMet(data, generator))
            {
                if (player instanceof EntityPlayerMP)
                {
                    player.sendMessage(new TextComponentString(I18n.format("gui.oneblockultima.msg.unlock_requirements")).setStyle(new Style().setColor(TextFormatting.RED)));
                }
                return false;
            }

            int cost = set.unlockCost;
            assert data != null;
            double currency = data.getCurrency();

            OneBlockUltima.getLogger().info("[OneBlock] Unlock attempt - Have: " + currency + ", Need: " + cost);

            if (currency < cost)
            {
                if (player instanceof EntityPlayerMP)
                {
                    player.sendMessage(new TextComponentString(I18n.format("gui.oneblockultima.msg.need") + ": " + cost + ", " + I18n.format("gui.oneblockultima.msg.have") + ": " + currency).setStyle(new Style().setColor(TextFormatting.RED)));
                }
                return false;
            }

            if (!data.spendCurrency(cost))
            {
                if (player instanceof EntityPlayerMP)
                {
                    player.sendMessage(new TextComponentString(I18n.format("gui.oneblockultima.msg.unlock_fail")).setStyle(new Style().setColor(TextFormatting.RED)));
                }
                return false;
            }

            OneBlockPlayerDataProvider.saveToEntity(player, data);

            // Unlock the set
            boolean success = generator.upgradeSet(setId, cost, set.getMaxLevel());
            if (!success)
            {
                data.addCurrency(cost);
                if (player instanceof EntityPlayerMP)
                {
                    player.sendMessage(new TextComponentString(I18n.format("gui.oneblockultima.msg.unlock_fail")).setStyle(new Style().setColor(TextFormatting.RED)));
                }
                return false;
            }

            // After unlocking, automatically select the set
            generator.setSelectedSetId(setId);
            if (!generator.ensureOwnership(player.getUniqueID()))
            {
                generator.setSelectedSetId(currentSelected);
                return false;
            }

            OneBlockUltima.getLogger().info("[OneBlock] Set unlocked and selected: " + setId);
            if (player instanceof EntityPlayerMP)
            {
                player.sendMessage(new TextComponentString(I18n.format("gui.oneblockultima.msg.unlocked")).setStyle(new Style().setColor(TextFormatting.GREEN)));
            }
        }
        else
        {
            // Attempt to upgrade the set
            BlockSetConfig.SetLevelDefinition nextLevel = set.getLevel(currentLevel + 1);
            if (nextLevel == null)
            {
                OneBlockUltima.getLogger().info("[OneBlock] Max level reached!");
                return false;
            }

            int cost = nextLevel.upgradeCost;
            assert data != null;
            double currency = data.getCurrency();

            OneBlockUltima.getLogger().info("[OneBlock] Upgrade attempt - Have: " + currency + ", Need: " + cost);

            if (currency < cost)
            {
                if (player instanceof EntityPlayerMP)
                {
                    player.sendMessage(new TextComponentString(I18n.format("gui.oneblockultima.msg.need") + ": " + cost + ", " + I18n.format("gui.oneblockultima.msg.have") + ": " + currency).setStyle(new Style().setColor(TextFormatting.RED)));
                }
                return false;
            }

            if (!data.spendCurrency(cost))
            {
                if (player instanceof EntityPlayerMP)
                {
                    player.sendMessage(new TextComponentString(I18n.format("gui.oneblockultima.msg.upgrade_fail")).setStyle(new Style().setColor(TextFormatting.RED)));
                }
                return false;
            }

            OneBlockPlayerDataProvider.saveToEntity(player, data);

            // Upgrade the set
            boolean success = generator.upgradeSet(setId, cost, set.getMaxLevel());
            if (!success)
            {
                data.addCurrency(cost);
                if (player instanceof EntityPlayerMP)
                {
                    player.sendMessage(new TextComponentString(I18n.format("gui.oneblockultima.msg.upgrade_fail")).setStyle(new Style().setColor(TextFormatting.RED)));
                }
                return false;
            }

            // After upgrading, automatically select the set
            generator.setSelectedSetId(setId);
            if (!generator.ensureOwnership(player.getUniqueID()))
            {
                generator.setSelectedSetId(currentSelected);
                return false;
            }

            OneBlockUltima.getLogger().info("[OneBlock] Set upgraded to level " + (currentLevel + 1) + " and selected: " + setId);
        }

        detectAndSendChanges();

        if (player instanceof EntityPlayerMP)
        {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            ModEvents.ensureGeneratorAccess(world, generatorPos, player, generator);

            // Send the tile update
            net.minecraft.network.play.server.SPacketUpdateTileEntity packet = generator.getUpdatePacket();
            if (packet != null)
            {
                playerMP.connection.sendPacket(packet);
            }

            // Synchronize the player data
            PacketSyncPlayerData.sendToPlayer(player);
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
        if (world.isAirBlock(generator.getPos().up()))
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
        if (world.isAirBlock(generator.getPos().up()))
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
        if (world.isAirBlock(generator.getPos().up()))
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
        if (world.isAirBlock(generator.getPos().up()))
        {
            generator.tryGenerateBlock();
        }
        updateTileEntity(generator);
    }

    private void updateTileEntity(TileEntityOneBlockGenerator generator) {
        if (player instanceof EntityPlayerMP)
        {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            net.minecraft.network.play.server.SPacketUpdateTileEntity packet = generator.getUpdatePacket();
            if (packet != null)
            {
                playerMP.connection.sendPacket(packet);
            }
        }
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

    public EntityPlayer getPlayer()
    {
        return player;
    }
}