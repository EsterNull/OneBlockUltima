package ru.defea.oneblockultima.gui.containers;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
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
    private static final int OFFSCREEN_SLOT_X = -32000;
    private static final int OFFSCREEN_SLOT_Y = -32000;

    private final World world;
    private final int generatorX;
    private final int generatorY;
    private final int generatorZ;
    private final EntityPlayer player;

    public ContainerOneBlock(EntityPlayer player, World world, int generatorX, int generatorY, int generatorZ)
    {
        this.world = world;
        this.generatorX = generatorX;
        this.generatorY = generatorY;
        this.generatorZ = generatorZ;
        this.player = player;

        addPlayerInventorySlots(player.inventory);

        if (!world.isRemote && player instanceof EntityPlayerMP)
        {
            TileEntityOneBlockGenerator generator = getGenerator();
            if (generator != null)
            {
                ModEvents.ensureGeneratorAccess(world, generatorX, generatorY, generatorZ, player, generator);
            }

            PacketSyncPlayerData.sendToPlayer(player);

            if (generator != null)
            {
                net.minecraft.network.Packet packet = generator.getDescriptionPacket();
                if (packet != null)
                {
                    ((EntityPlayerMP) player).playerNetServerHandler.sendPacket(packet);
                }
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn)
    {
        return playerIn.getDistanceSq(generatorX + BLOCK_CENTER_OFFSET, generatorY + BLOCK_CENTER_OFFSET, generatorZ + BLOCK_CENTER_OFFSET) <= PLAYER_INTERACT_RANGE_SQ;
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

    public void selectSet(String setId)
    {
        if (world.isRemote)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorX, generatorY, generatorZ, PacketOneBlockAction.Action.SELECT_SET, setId));
            return; // Возвращаем true, чтобы клиент не показывал ошибку
        }

        applySelectSet(setId);
    }

    public void upgradeSet(String setId)
    {
        if (world.isRemote)
        {
            applyLocalBalancePreview(setId);
            ModMessages.sendToServer(new PacketOneBlockAction(generatorX, generatorY, generatorZ, PacketOneBlockAction.Action.UPGRADE_SET, setId));
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
        if (currentLevel > 0 && currentLevel >= set.getMaxLevel())
        {
            return;
        }
        int cost = currentLevel <= 0 ? set.unlockCost : set.getLevel(currentLevel + 1).upgradeCost;
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
            ModMessages.sendToServer(new PacketOneBlockAction(generatorX, generatorY, generatorZ, PacketOneBlockAction.Action.TOGGLE_FLUIDS, ""));
            return;
        }

        applyToggleFluidGeneration();
    }

    public void toggleMobGeneration()
    {
        if (world.isRemote)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorX, generatorY, generatorZ, PacketOneBlockAction.Action.TOGGLE_MOBS, ""));
            return;
        }

        applyToggleMobGeneration();
    }

    public void toggleChestGeneration()
    {
        if (world.isRemote)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorX, generatorY, generatorZ, PacketOneBlockAction.Action.TOGGLE_CHESTS, ""));
            return;
        }

        applyToggleChestGeneration();
    }

    public void toggleSaplingGeneration()
    {
        if (world.isRemote)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorX, generatorY, generatorZ, PacketOneBlockAction.Action.TOGGLE_SAPLINGS, ""));
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

        generator.tryRegenerateSlotBlock();
        detectAndSendChanges();

        if (player instanceof EntityPlayerMP)
        {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            // Send the tile update
            net.minecraft.network.Packet packet = generator.getDescriptionPacket();
            if (packet != null)
            {
                playerMP.playerNetServerHandler.sendPacket(packet);
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
                    player.addChatComponentMessage(new ChatComponentText(I18n.format("gui.oneblockultima.msg.unlock_requirements")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
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
                    player.addChatComponentMessage(new ChatComponentText(I18n.format("gui.oneblockultima.msg.need") + ": " + cost + ", " + I18n.format("gui.oneblockultima.msg.have") + ": " + currency).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
                }
                return false;
            }

            if (!data.spendCurrency(cost))
            {
                if (player instanceof EntityPlayerMP)
                {
                    player.addChatComponentMessage(new ChatComponentText(I18n.format("gui.oneblockultima.msg.unlock_fail")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
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
                    player.addChatComponentMessage(new ChatComponentText(I18n.format("gui.oneblockultima.msg.unlock_fail")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
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
                player.addChatComponentMessage(new ChatComponentText(I18n.format("gui.oneblockultima.msg.unlocked")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
            }
        }
        else
        {
            // Attempt to upgrade the set
            if (currentLevel >= set.getMaxLevel())
            {
                OneBlockUltima.getLogger().info("[OneBlock] Max level reached!");
                return false;
            }

            int cost = set.getLevel(currentLevel + 1).upgradeCost;
            assert data != null;
            double currency = data.getCurrency();

            OneBlockUltima.getLogger().info("[OneBlock] Upgrade attempt - Have: " + currency + ", Need: " + cost);

            if (currency < cost)
            {
                if (player instanceof EntityPlayerMP)
                {
                    player.addChatComponentMessage(new ChatComponentText(I18n.format("gui.oneblockultima.msg.need") + ": " + cost + ", " + I18n.format("gui.oneblockultima.msg.have") + ": " + currency).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
                }
                return false;
            }

            if (!data.spendCurrency(cost))
            {
                if (player instanceof EntityPlayerMP)
                {
                    player.addChatComponentMessage(new ChatComponentText(I18n.format("gui.oneblockultima.msg.upgrade_fail")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
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
                    player.addChatComponentMessage(new ChatComponentText(I18n.format("gui.oneblockultima.msg.upgrade_fail")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
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

        generator.tryRegenerateSlotBlock();

        detectAndSendChanges();

        if (player instanceof EntityPlayerMP)
        {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            ModEvents.ensureGeneratorAccess(world, generatorX, generatorY, generatorZ, player, generator);

            // Send the tile update
            net.minecraft.network.Packet packet = generator.getDescriptionPacket();
            if (packet != null)
            {
                playerMP.playerNetServerHandler.sendPacket(packet);
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
        generator.tryRegenerateSlotBlock();
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
        generator.tryRegenerateSlotBlock();
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
        generator.tryRegenerateSlotBlock();
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
        generator.tryRegenerateSlotBlock();
        updateTileEntity(generator);
    }

    private void updateTileEntity(TileEntityOneBlockGenerator generator) {
        if (player instanceof EntityPlayerMP)
        {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            net.minecraft.network.Packet packet = generator.getDescriptionPacket();
            if (packet != null)
            {
                playerMP.playerNetServerHandler.sendPacket(packet);
            }
        }
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

    public EntityPlayer getPlayer()
    {
        return player;
    }
}
