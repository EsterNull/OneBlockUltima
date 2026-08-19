package ru.defea.oneblockultima.event;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockOneBlockGenerator;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.item.ModItems;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.network.PacketSyncBlockSetConfig;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.update.UpdateChecker;
import ru.defea.oneblockultima.util.BlockUtil;
import ru.defea.oneblockultima.util.MobIdUtil;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;
import ru.defea.oneblockultima.world.OneBlockWorldType;
import ru.defea.oneblockultima.world.SpawnConfigData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static ru.defea.oneblockultima.Constants.BLOCK_CENTER_OFFSET;
import static ru.defea.oneblockultima.Constants.BLOCK_TOP_OFFSET;
import static ru.defea.oneblockultima.Constants.NBT_GUIDE_BOOK_GIVEN;
import static ru.defea.oneblockultima.Constants.NBT_OBU_GENERATED;
import static ru.defea.oneblockultima.Constants.OVERWORLD_DIMENSION_ID;
import static ru.defea.oneblockultima.block.BlockOneBlockGenerator.FLUID_BARRIER_Y;
import static ru.defea.oneblockultima.block.BlockOneBlockGenerator.GENERATED_BLOCK_Y;
import static ru.defea.oneblockultima.block.BlockOneBlockGenerator.GENERATOR_X;
import static ru.defea.oneblockultima.block.BlockOneBlockGenerator.GENERATOR_Y;
import static ru.defea.oneblockultima.block.BlockOneBlockGenerator.GENERATOR_Z;

public class ModEvents
{
    public ModEvents()
    {
    }

    private static final Map<Pos, Boolean> processingBlocks = new HashMap<>();
    private static final Map<Pos, UUID> pendingGeneratorOwners = new HashMap<>();
    private static final Map<String, Long> lastAccessDeniedMessageTicks = new HashMap<>();
    private static final Map<Pos, GeneratedBlockRegistry.GeneratedBlockEntry> pendingMobSpawnEntries = new HashMap<>();
    private static final Map<Pos, UUID> lastBreakPlayers = new HashMap<>();
    private static int lastBreakCleanupTick = 0;
    public static final Map<UUID, Double> lastDisplayedCurrency = new HashMap<>();
    private static long suppressItemUseUntil = 0L;

    public static boolean trySendAccessDeniedMessage(UUID playerId, int x, int y, int z, long worldTick)
    {
        if (playerId == null)
        {
            return false;
        }

        String key = playerId + ":" + x + "," + y + "," + z;
        Long lastTick = lastAccessDeniedMessageTicks.get(key);
        if (lastTick != null && lastTick == worldTick)
        {
            return false;
        }

        lastAccessDeniedMessageTicks.put(key, worldTick);
        return true;
    }

    public static void trySendAccessDeniedMessage(EntityPlayer player, int x, int y, int z, long worldTick)
    {
        if (!(player instanceof EntityPlayerMP))
        {
            return;
        }

        if (!trySendAccessDeniedMessage(player.getUniqueID(), x, y, z, worldTick))
        {
            return;
        }

        player.addChatComponentMessage(
                new ChatComponentTranslation("generator.access.denied")
                        .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
    }

    public static boolean ensureGeneratorAccess(World world, int x, int y, int z, EntityPlayer player, TileEntityOneBlockGenerator generator)
    {
        if (generator == null || player == null)
        {
            return false;
        }

        if (world != null)
        {
            applyPendingGeneratorOwner(world, x, y, z);
        }

        return generator.hasAccess(player);
    }

    public static void registerPendingGeneratorOwner(World world, int x, int y, int z, UUID playerId)
    {
        if (world == null || playerId == null)
        {
            return;
        }

        pendingGeneratorOwners.put(new Pos(x, y, z), playerId);
    }

    public static void applyPendingGeneratorOwner(World world, int x, int y, int z)
    {
        if (world == null)
        {
            return;
        }

        Pos pos = new Pos(x, y, z);
        UUID playerId = pendingGeneratorOwners.get(pos);
        if (playerId == null)
        {
            return;
        }

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntityOneBlockGenerator))
        {
            OneBlockUltima.getLogger().warn("[OwnerDebug] applyPendingGeneratorOwner: TE at ({},{},{}) is NOT a generator! it's {}",
                    x, y, z, tileEntity != null ? tileEntity.getClass().getSimpleName() : "NULL");
            return;
        }

        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
        OneBlockUltima.getLogger().info("[OwnerDebug] applyPendingGeneratorOwner: pos=({},{},{}), playerId={}, currentOwnerId={}, isFree={}",
                x, y, z, playerId, generator.getOwnerId(), generator.isFree());
        if (generator.assignOwnerForPlacement(playerId))
        {
            OneBlockUltima.getLogger().info("[OwnerDebug] applyPendingGeneratorOwner: SUCCESS, new ownerId={}", generator.getOwnerId());
            pendingGeneratorOwners.remove(pos);
        }
        else
        {
            OneBlockUltima.getLogger().warn("[OwnerDebug] applyPendingGeneratorOwner: FAILED to assign owner");
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.player == null || event.player.worldObj == null || event.player.worldObj.isRemote)
        {
            return;
        }

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(event.player);
        if (data instanceof OneBlockPlayerData)
        {
            OneBlockPlayerDataProvider.loadFromEntity(event.player, data);
            PacketSyncPlayerData.sendToPlayer(event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.player.worldObj.isRemote)
        {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        World world = player.worldObj;
        if (world.provider.dimensionId != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        boolean hasBedSpawn = player.getBedLocation(OVERWORLD_DIMENSION_ID) != null;

        int sx = GENERATOR_X;
        int sy = FLUID_BARRIER_Y;
        int sz = GENERATOR_Z;

        if (!hasBedSpawn)
        {
            world.setSpawnLocation(sx, sy, sz);
            player.setSpawnChunk(new ChunkCoordinates(sx, sy, sz), true, player.dimension);
            player.setPositionAndUpdate(sx + BLOCK_CENTER_OFFSET, sy, sz + BLOCK_CENTER_OFFSET);
            player.playerNetServerHandler.setPlayerLocation(sx + BLOCK_CENTER_OFFSET, sy, sz + BLOCK_CENTER_OFFSET, player.rotationYaw, player.rotationPitch);
        }

        PacketSyncPlayerData.sendToPlayer(player);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        World world = event.world;
        if (world.isRemote || world.provider.dimensionId != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        createWorldIcon(world);

        SpawnConfigData data = SpawnConfigData.get(world);
        if (!data.spawnInitialized)
        {
            world.setSpawnLocation(GENERATOR_X, FLUID_BARRIER_Y, GENERATOR_Z);
            data.spawnInitialized = true;
            data.markDirty();
        }

        int gx = GENERATOR_X;
        int gy = GENERATOR_Y;
        int gz = GENERATOR_Z;
        int barrierX = GENERATOR_X;
        int barrierY = FLUID_BARRIER_Y;
        int barrierZ = GENERATOR_Z;

        if (world.getBlock(gx, gy, gz) == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            if (world.getBlock(barrierX, barrierY, barrierZ) == Blocks.air)
            {
                world.setBlock(barrierX, barrierY, barrierZ, ModBlocks.FLUID_BARRIER, 0, 2);
                OneBlockUltima.getLogger().info("[Generator] BARRIER placed at ({},{},{}) on world load", barrierX, barrierY, barrierZ);
            }

            TileEntity tileEntity = world.getTileEntity(gx, gy, gz);
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);

                if (!registry.isGenerated(GENERATOR_X, GENERATED_BLOCK_Y, GENERATOR_Z))
                {
                    ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
                }
            }
            else
            {
                world.scheduleBlockUpdate(gx, gy, gz, ModBlocks.ONE_BLOCK_GENERATOR, 1);
            }
            return;
        }

        Block blockAtGenerator = world.getBlock(gx, gy, gz);
        if (blockAtGenerator == null || blockAtGenerator == Blocks.air || blockAtGenerator.getMaterial().isReplaceable())
        {
            world.setBlock(gx, gy, gz, ModBlocks.ONE_BLOCK_GENERATOR, 0, 3);

            TileEntity createdGenerator = world.getTileEntity(gx, gy, gz);
            if (createdGenerator instanceof TileEntityOneBlockGenerator)
            {
                ((TileEntityOneBlockGenerator) createdGenerator).setOwnerId(null);
            }

            if (world.getBlock(barrierX, barrierY, barrierZ) == Blocks.air)
            {
                world.setBlock(barrierX, barrierY, barrierZ, ModBlocks.FLUID_BARRIER, 0, 2);
                OneBlockUltima.getLogger().info("[Generator] BARRIER placed at ({},{},{}) on generator creation", barrierX, barrierY, barrierZ);
            }

            TileEntity tileEntity = world.getTileEntity(gx, gy, gz);
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
            }
            else
            {
                world.scheduleBlockUpdate(gx, gy, gz, ModBlocks.ONE_BLOCK_GENERATOR, 1);
            }
        }
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event)
    {
        if (event.world == null || event.world.isRemote)
        {
            return;
        }

        GeneratedBlockRegistry.get(event.world).flushPendingDirty();
    }

    private static void createWorldIcon(World world)
    {
        File worldDir = world.getSaveHandler().getWorldDirectory();
        File iconFile = new File(worldDir, "icon.png");

        if (iconFile.exists())
        {
            return;
        }

        try (InputStream input = ModEvents.class.getResourceAsStream("/assets/oneblockultima/textures/gui/oneblock_logo.png"))
        {
            if (input == null)
            {
                OneBlockUltima.getLogger().warn("Could not find logo texture!");
                return;
            }

            if (!worldDir.exists() && !worldDir.mkdirs())
            {
                OneBlockUltima.getLogger().warn("Could not create world directory: {}", worldDir.getAbsolutePath());
                return;
            }

            Files.copy(input, iconFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            OneBlockUltima.getLogger().info("Icon created for world: {}", worldDir.getName());
        }
        catch (IOException e)
        {
            OneBlockUltima.getLogger().warn("Failed to create icon.png for OneBlock world {}", worldDir.getName(), e);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.player == null || event.player.worldObj == null || event.player.worldObj.isRemote)
        {
            return;
        }

        UpdateChecker.checkForUpdates((EntityPlayerMP) event.player);

        try
        {
            String json = BlockSetConfig.get().toJson();
            OneBlockUltima.getLogger().info("[Sync] BlockSetConfig JSON length: {}", json != null ? json.length() : "null");
            if (json != null && !json.isEmpty())
            {
                PacketSyncBlockSetConfig packet = new PacketSyncBlockSetConfig(json);
                OneBlockUltima.getLogger().info("[Sync] Sending BlockSetConfig packet to {}", event.player.getCommandSenderName());
                ModMessages.sendToPlayer(packet, (EntityPlayerMP) event.player);
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[Sync] Failed to send BlockSetConfig", e);
        }

        giveGuideBookOnFirstJoin((EntityPlayerMP) event.player);

        World world = event.player.worldObj;
        if (world.provider.dimensionId != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        TileEntity tileEntity = world.getTileEntity(GENERATOR_X, GENERATOR_Y, GENERATOR_Z);
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
            if (generator.isFree())
            {
                generator.tryAssignOwnerIfEligible(event.player.getUniqueID());
            }
        }

        EntityPlayerMP player = (EntityPlayerMP) event.player;

        SpawnConfigData data = SpawnConfigData.get(world);
        int sx = GENERATOR_X;
        int sy = FLUID_BARRIER_Y;
        int sz = GENERATOR_Z;

        if (!data.spawnInitialized)
        {
            world.setSpawnLocation(sx, sy, sz);
            data.spawnInitialized = true;
            data.markDirty();
        }

        ChunkCoordinates currentSpawn = world.getSpawnPoint();
        if (!currentSpawn.equals(new ChunkCoordinates(sx, sy, sz)))
        {
            world.setSpawnLocation(sx, sy, sz);
        }

        player.getBedLocation(OVERWORLD_DIMENSION_ID);

        IOneBlockPlayerData playerData = OneBlockPlayerDataProvider.get(player);
        if (playerData != null)
        {
            OneBlockPlayerDataProvider.loadFromEntity(player, playerData);
        }

        if (!data.spawnTeleportDone)
        {
            player.setPositionAndUpdate(sx + BLOCK_CENTER_OFFSET, sy, sz + BLOCK_CENTER_OFFSET);
            player.playerNetServerHandler.setPlayerLocation(sx + BLOCK_CENTER_OFFSET, sy, sz + BLOCK_CENTER_OFFSET, player.rotationYaw, player.rotationPitch);
            data.spawnTeleportDone = true;
            data.markDirty();
        }

        PacketSyncPlayerData.sendToPlayer(player);
    }

    private static void giveGuideBookOnFirstJoin(EntityPlayerMP player)
    {
        if (player == null || player.worldObj == null
                || player.worldObj.provider.dimensionId != OVERWORLD_DIMENSION_ID
                || player.worldObj.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        NBTTagCompound playerData = player.getEntityData();
        if (playerData.getBoolean(NBT_GUIDE_BOOK_GIVEN))
        {
            return;
        }

        playerData.setBoolean(NBT_GUIDE_BOOK_GIVEN, true);
        ItemStack guideBook = new ItemStack(ModItems.GUIDE_BOOK);
        if (!player.inventory.addItemStackToInventory(guideBook))
        {
            EntityItem entityItem = new EntityItem(player.worldObj, player.posX, player.posY + 0.5D, player.posZ, guideBook);
            player.worldObj.spawnEntityInWorld(entityItem);
        }
        OneBlockUltima.getLogger().info("[GuideBook] Given guide book to {}", player.getCommandSenderName());
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != Phase.END)
        {
            return;
        }
        if (event.player == null || event.player.worldObj == null)
        {
            return;
        }

        EntityPlayer tickedPlayer = event.player;
        if (!tickedPlayer.worldObj.isRemote)
        {
            ru.defea.oneblockultima.achievement.ModAchievements.tickPlayer(tickedPlayer);
        }

        try
        {
            EntityPlayer player = event.player;
            World world = player.worldObj;
            if (world.provider.dimensionId != OVERWORLD_DIMENSION_ID
                    || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
            {
                return;
            }
            GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);

            int baseY = (int) Math.floor(player.posY - 0.1D);
            int playerX = (int) Math.floor(player.posX);
            int playerZ = (int) Math.floor(player.posZ);

            // Check the block at player's feet and the block immediately below it
            for (int dy = 0; dy <= 1; dy++)
            {
                int checkY = baseY - dy;
                if (!registry.isGenerated(playerX, checkY, playerZ))
                {
                    continue;
                }

                Block block = world.getBlock(playerX, checkY, playerZ);
                boolean hasCollision = block != null
                        && block.getCollisionBoundingBoxFromPool(world, playerX, checkY, playerZ) != null
                        && !block.getMaterial().isReplaceable();
                double relY = player.posY - checkY;
                // If there's no collision and player is falling into the block space, stop the fall and snap player above
                if (!hasCollision && player.motionY < 0 && relY <= 1.2D)
                {
                    player.motionY = 0.0D;
                    player.fallDistance = 0.0F;
                    player.onGround = true;
                    player.setPosition(player.posX, checkY + 1.0D, player.posZ);
                    break;
                }
                // If there is collision, ensure player's onGround is set correctly when standing
                if (hasCollision && relY <= 1.2D && relY >= 0.0D)
                {
                    player.onGround = true;
                    break;
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event)
    {
        if (event.phase != Phase.END || event.world == null || event.world.isRemote)
        {
            return;
        }

        if (event.world.provider.dimensionId != OVERWORLD_DIMENSION_ID
                || event.world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        Set<TileEntityOneBlockGenerator> activeGenerators = TileEntityOneBlockGenerator.getActiveGenerators();
        if (!activeGenerators.isEmpty())
        {
            synchronized (activeGenerators)
            {
                Iterator<TileEntityOneBlockGenerator> iterator = activeGenerators.iterator();
                while (iterator.hasNext())
                {
                    TileEntityOneBlockGenerator generator = iterator.next();
                    if (generator == null || generator.isInvalid() || generator.getWorld() != event.world)
                    {
                        iterator.remove();
                        continue;
                    }
                    generator.tickInvites();

                    // Restore the fluid barrier above the generator if its slot became AIR again
                    // (1.7.10 has no BlockEvent.NeighborNotifyEvent, so this is checked per tick)
                    if (event.world.getBlock(generator.xCoord, generator.yCoord + 2, generator.zCoord) == Blocks.air)
                    {
                        BlockOneBlockGenerator.ensureFluidBarrier(event.world, generator.xCoord, generator.yCoord, generator.zCoord);
                    }
                }
            }
        }

        if (!lastAccessDeniedMessageTicks.isEmpty())
        {
            long worldTick = event.world.getTotalWorldTime();
            lastAccessDeniedMessageTicks.values().removeIf(lastTick -> lastTick != worldTick);
        }

        if (!lastBreakPlayers.isEmpty() && ++lastBreakCleanupTick >= 200)
        {
            lastBreakCleanupTick = 0;
            lastBreakPlayers.clear();
        }
    }

    @SubscribeEvent
    public void onLivingSpawn(LivingSpawnEvent.CheckSpawn event)
    {
        if (event.world.isRemote)
        {
            return;
        }

        World world = event.world;
        if (world.provider.dimensionId != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        if (ModSettings.get().getMobWorldGeneration())
        {
            return;
        }

        event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(net.minecraftforge.event.entity.EntityJoinWorldEvent event)
    {
        if (event.world.isRemote)
        {
            return;
        }

        if (!(event.entity instanceof EntityItem))
        {
            return;
        }

        EntityItem entityItem = (EntityItem) event.entity;
        World world = event.world;

        if (world.provider.dimensionId != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        Pos pos = new Pos((int) Math.floor(entityItem.posX), (int) Math.floor(entityItem.posY), (int) Math.floor(entityItem.posZ));
        UUID breakerId = lastBreakPlayers.get(pos);
        if (breakerId == null)
        {
            return;
        }

        EntityPlayer player = world.getPlayerEntityByUUID(breakerId);
        if (player == null)
        {
            return;
        }

        ItemStack stack = entityItem.getEntityItem();
        if (stack == null || stack.stackSize <= 0)
        {
            return;
        }

        player.inventory.addItemStackToInventory(stack);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event)
    {
        if (event.world.isRemote)
        {
            return;
        }

        World world = event.world;
        int px = event.blockSnapshot.x;
        int py = event.blockSnapshot.y;
        int pz = event.blockSnapshot.z;
        Block placedBlock = event.placedBlock;

        // Propagate obuGenerated from the ItemStack when re-placing through PlaceEvent
        if (event.player != null)
        {
            ItemStack heldItem = event.player.getHeldItem();
            if (heldItem != null && heldItem.hasTagCompound())
            {
                NBTTagCompound tag = heldItem.getTagCompound();
                if (tag.hasKey(NBT_OBU_GENERATED) && tag.getBoolean(NBT_OBU_GENERATED))
                {
                    GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
                    if (!registry.isGenerated(px, py, pz))
                    {
                        String blockRegistry = BlockUtil.getRegistryString(placedBlock);
                        int blockMeta = world.getBlockMetadata(px, py, pz);
                        registry.markGenerated(px, py, pz, px, py, pz, "", 0, 0, blockRegistry, blockMeta);
                    }
                }
            }
        }

        // OneBlock-specific logic
        if (world.provider.dimensionId != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        if (placedBlock == ModBlocks.ONE_BLOCK_GENERATOR && event.player != null)
        {
            OneBlockUltima.getLogger().info("[OwnerDebug] PlaceEvent fired for generator at ({},{},{}) by player {}", px, py, pz, event.player.getCommandSenderName());
            registerPendingGeneratorOwner(world, px, py, pz, event.player.getUniqueID());

            TileEntity tileEntity = world.getTileEntity(px, py, pz);
            OneBlockUltima.getLogger().info("[OwnerDebug] tileEntity at pos ({},{},{}): {}", px, py, pz, tileEntity != null ? tileEntity.getClass().getSimpleName() : "NULL");
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
                OneBlockUltima.getLogger().info("[OwnerDebug] generator.isFree()={}, ownerId={}, player={}", generator.isFree(), generator.getOwnerId(), event.player.getCommandSenderName());
                if (generator.isFree())
                {
                    boolean result = generator.assignOwnerForPlacement(event.player.getUniqueID());
                    OneBlockUltima.getLogger().info("[OwnerDebug] assignOwnerForPlacement result={}, new ownerId={}", result, generator.getOwnerId());
                }
                else
                {
                    OneBlockUltima.getLogger().warn("[OwnerDebug] generator is NOT free, skipping assignOwnerForPlacement! ownerId={}", generator.getOwnerId());
                }
            }
        }

        Block replacementBlock = BlockUtil.getReplacementBlockForGeneratorPlacement(placedBlock, world, px, py, pz);
        if (replacementBlock != null && replacementBlock != placedBlock)
        {
            world.setBlock(px, py, pz, replacementBlock, world.getBlockMetadata(px, py, pz), 3);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent event)
    {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
        {
            return;
        }

        if (event.entityPlayer == null || event.world == null)
        {
            return;
        }

        EntityPlayer player = event.entityPlayer;
        World world = event.world;

        if (world.provider.dimensionId != OVERWORLD_DIMENSION_ID
                || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        int genX;
        int genY;
        int genZ;

        if (world.getBlock(event.x, event.y, event.z) == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            genX = event.x;
            genY = event.y;
            genZ = event.z;
        }
        else if (world.getBlock(event.x, event.y - 1, event.z) == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            genX = event.x;
            genY = event.y - 1;
            genZ = event.z;
        }
        else
        {
            GeneratedBlockRegistry.GeneratedBlockEntry registryEntry = GeneratedBlockRegistry.get(world).getEntry(event.x, event.y, event.z);
            if (registryEntry == null)
            {
                return;
            }
            genX = registryEntry.generatorX;
            genY = registryEntry.generatorY;
            genZ = registryEntry.generatorZ;
            if (world.getBlock(genX, genY, genZ) != ModBlocks.ONE_BLOCK_GENERATOR)
            {
                return;
            }
        }

        if (!player.isSneaking())
        {
            if (world.isRemote)
            {
                suppressItemUseAfterClick(event, player);
                return;
            }
            OneBlockUltima.getRawLogger().info("[MenuDebug] Right-click (non-sneak) on ({},{},{}) resolved generator at ({},{},{})",
                    event.x, event.y, event.z, genX, genY, genZ);
            BlockOneBlockGenerator.openGeneratorMenu(world, genX, genY, genZ, player);
            event.setCanceled(true);
            return;
        }

        if (world.isRemote)
        {
            suppressItemUseAfterClick(event, player);
            player.swingItem();
            return;
        }

        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock))
        {
            return;
        }

        TileEntity tileEntity = world.getTileEntity(genX, genY, genZ);
        if (!(tileEntity instanceof TileEntityOneBlockGenerator))
        {
            return;
        }

        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
        if (!generator.hasAccess(player))
        {
            return;
        }

        int slotX = genX;
        int slotY = genY + 1;
        int slotZ = genZ;

        boolean clickedSlot = event.x == slotX && event.y == slotY && event.z == slotZ;
        boolean clickedGenerator = event.x == genX && event.y == genY && event.z == genZ;
        Block slotBlock = world.getBlock(slotX, slotY, slotZ);

        int tx;
        int ty;
        int tz;

        if (clickedSlot || clickedGenerator)
        {
            switch (event.face)
            {
                case 2:
                    tx = slotX;
                    ty = slotY;
                    tz = slotZ - 1;
                    break;
                case 3:
                    tx = slotX;
                    ty = slotY;
                    tz = slotZ + 1;
                    break;
                case 4:
                    tx = slotX - 1;
                    ty = slotY;
                    tz = slotZ;
                    break;
                case 5:
                    tx = slotX + 1;
                    ty = slotY;
                    tz = slotZ;
                    break;
                case 1:
                    if (clickedSlot && !isNonFullBlock(slotBlock))
                    {
                        tx = slotX;
                        ty = slotY + 1;
                        tz = slotZ;
                    }
                    else
                    {
                        tx = slotX;
                        ty = slotY;
                        tz = slotZ;
                    }
                    break;
                default:
                    tx = slotX;
                    ty = slotY;
                    tz = slotZ;
                    break;
            }
        }
        else
        {
            tx = event.x;
            ty = event.y;
            tz = event.z;

            switch (event.face)
            {
                case 0:
                    ty = event.y - 1;
                    break;
                case 1:
                    ty = event.y + 1;
                    break;
                case 2:
                    tz = event.z - 1;
                    break;
                case 3:
                    tz = event.z + 1;
                    break;
                case 4:
                    tx = event.x - 1;
                    break;
                case 5:
                    tx = event.x + 1;
                    break;
                default:
                    ty = event.y + 1;
                    break;
            }
        }

        // Do not place into the generator itself, shift it above instead
        if (tx == genX && ty == genY && tz == genZ)
        {
            ty = genY + 1;
        }

        event.setCanceled(true);

        boolean isGeneratedItem = heldItem.hasTagCompound()
                && heldItem.getTagCompound().hasKey(NBT_OBU_GENERATED)
                && heldItem.getTagCompound().getBoolean(NBT_OBU_GENERATED);

        ItemBlock itemBlock = (ItemBlock) heldItem.getItem();
        if (itemBlock.placeBlockAt(heldItem, player, world, tx, ty, tz, event.face, 0.5F, 0.5F, 0.5F, heldItem.getMetadata()))
        {
            Block placedBlock = world.getBlock(tx, ty, tz);
            if (placedBlock != null && placedBlock != Blocks.air)
            {
                Block replacement = BlockUtil.getReplacementBlockForGeneratorPlacement(placedBlock, world, tx, ty, tz);
                if (replacement != null && replacement != placedBlock)
                {
                    world.setBlock(tx, ty, tz, replacement, world.getBlockMetadata(tx, ty, tz), 3);
                    placedBlock = replacement;
                }

                world.playSoundEffect((double) tx + 0.5D, (double) ty + 0.5D, (double) tz + 0.5D,
                        placedBlock.stepSound.getDigResourcePath(),
                        (placedBlock.stepSound.getVolume() + 1.0F) / 2.0F,
                        placedBlock.stepSound.getFrequency() * 0.8F);
            }

            if (!player.capabilities.isCreativeMode)
            {
                heldItem.stackSize -= 1;
                if (heldItem.stackSize <= 0)
                {
                    player.inventory.mainInventory[player.inventory.currentItem] = null;
                }
            }

            TileEntity placedTE = world.getTileEntity(tx, ty, tz);
            if (placedTE instanceof TileEntityOneBlockGenerator)
            {
                ((TileEntityOneBlockGenerator) placedTE).assignOwnerForPlacement(player.getUniqueID());
            }

            if (isGeneratedItem)
            {
                GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
                if (!registry.isGenerated(tx, ty, tz))
                {
                    String blockReg = BlockUtil.getRegistryString(world.getBlock(tx, ty, tz));
                    int blockMeta = world.getBlockMetadata(tx, ty, tz);
                    registry.markGenerated(tx, ty, tz, tx, ty, tz, "", 0, 0, blockReg, blockMeta);
                }
            }
        }
    }

    private static boolean isNonFullBlock(Block block)
    {
        if (block == null || block == Blocks.air)
        {
            return true;
        }
        return block.getMaterial().isReplaceable() || !block.isOpaqueCube() || !block.renderAsNormalBlock();
    }

    private static void suppressItemUseAfterClick(PlayerInteractEvent event, EntityPlayer player)
    {
        suppressItemUseUntil = Minecraft.getSystemTime() + 250L;
        event.setCanceled(true);
        Minecraft.getMinecraft().getNetHandler().addToSendQueue(
                new C08PacketPlayerBlockPlacement(event.x, event.y, event.z, event.face,
                        player.getHeldItem(), 0.5F, 0.5F, 0.5F));
    }

    @SubscribeEvent
    public void onRightClickAir(PlayerInteractEvent event)
    {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR || event.world == null || !event.world.isRemote)
        {
            return;
        }
        if (Minecraft.getSystemTime() < suppressItemUseUntil)
        {
            suppressItemUseUntil = 0L;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if (event.world.isRemote)
        {
            return;
        }

        int bx = event.x;
        int by = event.y;
        int bz = event.z;
        World world = event.world;

        if (world.getBlock(bx, by, bz) == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            event.setCanceled(true);
            return;
        }

        if (world.provider.dimensionId != OVERWORLD_DIMENSION_ID
                || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        EntityPlayer breaker = event.getPlayer();
        Pos pos = new Pos(bx, by, bz);

        if (processingBlocks.getOrDefault(pos, false))
        {
            return;
        }

        TileEntity generatorTile = world.getTileEntity(bx, by, bz);
        if (generatorTile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatorTile;
            if (!generator.hasAccess(breaker))
            {
                event.setCanceled(true);
                trySendAccessDeniedMessage(breaker, bx, by, bz, world.getTotalWorldTime());
                return;
            }
        }

        if (world.getBlock(bx, by, bz) == ModBlocks.FLUID_BARRIER
                && world.getBlock(bx, by - 2, bz) == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            world.setBlock(bx, by, bz, ModBlocks.FLUID_BARRIER, 0, 2);
            OneBlockUltima.getLogger().info("[Generator] BARRIER placed at ({},{},{}) after block break", bx, by, bz);
            event.setCanceled(true);
            return;
        }

        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
        GeneratedBlockRegistry.GeneratedBlockEntry entry = registry.getEntry(bx, by, bz);
        if (entry == null)
        {
            lastBreakPlayers.remove(pos);
            pendingMobSpawnEntries.remove(pos);
            OneBlockUltima.getLogger().debug("[BreakDebug] No generated entry found for broken block ({},{},{})", bx, by, bz);
            return;
        }

        Pos generatorPos = new Pos(entry.generatorX, entry.generatorY, entry.generatorZ);

        if (breaker != null)
        {
            lastBreakPlayers.put(pos, breaker.getUniqueID());
        }

        if (breaker != null)
        {
            TileEntity genTile = world.getTileEntity(entry.generatorX, entry.generatorY, entry.generatorZ);
            if (genTile instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator gen = (TileEntityOneBlockGenerator) genTile;
                if (!gen.hasAccess(breaker))
                {
                    event.setCanceled(true);
                    trySendAccessDeniedMessage(breaker, entry.generatorX, entry.generatorY, entry.generatorZ, world.getTotalWorldTime());
                    return;
                }
            }
        }

        OneBlockUltima.getLogger().debug("[BreakDebug] Generated entry found for ({},{},{}) -> generator ({},{},{}) set={} level={}",
                bx, by, bz, entry.generatorX, entry.generatorY, entry.generatorZ, entry.setId, entry.level);

        pendingMobSpawnEntries.put(pos, entry);

        spawnMobOnBlockBreak(world, bx, by, bz, entry);
        registry.remove(bx, by, bz);

        TileEntity generatedTile = world.getTileEntity(entry.generatorX, entry.generatorY, entry.generatorZ);
        EntityPlayer player = event.getPlayer();
        if (generatedTile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatedTile;
            if (player == null)
            {
                OneBlockUltima.getLogger().info("[BreakDebug] Non-player break detected at ({},{},{}) for generator ({},{},{})",
                        bx, by, bz, entry.generatorX, entry.generatorY, entry.generatorZ);
                if (!generator.canProcessNonPlayerBreak(world.getTotalWorldTime()))
                {
                    OneBlockUltima.getLogger().info("[BreakDebug] Non-player break blocked by cooldown for generator ({},{},{}) at tick {}",
                            entry.generatorX, entry.generatorY, entry.generatorZ, world.getTotalWorldTime());
                    event.setCanceled(true);
                    return;
                }

                generator.markNonPlayerBreak(world.getTotalWorldTime());
                OneBlockUltima.getLogger().info("[BreakDebug] Non-player break accepted for generator ({},{},{}) at tick {}",
                        entry.generatorX, entry.generatorY, entry.generatorZ, world.getTotalWorldTime());
            }
        }

        if (world.getBlock(bx, by, bz) == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            processingBlocks.put(pos, true);
        }

        if (player == null && generatedTile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatedTile;
            OneBlockUltima.getLogger().info("[BreakDebug] Invoking non-player generation for generator ({},{},{}) at tick {}",
                    entry.generatorX, entry.generatorY, entry.generatorZ, world.getTotalWorldTime());
            generator.tryGenerateBlock();
        }
        else if (player != null)
        {
            OneBlockUltima.getLogger().info("[BreakDebug] Player break detected at ({},{},{}) for generator ({},{},{})",
                    bx, by, bz, entry.generatorX, entry.generatorY, entry.generatorZ);
        }

        if (player != null)
        {
            IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
            if (data != null)
            {
                // Only award currency and count broken blocks for blocks directly from generator
                if (!generatorPos.equals(pos))
                {
                    if (BlockPriceConfig.get().getBalanceMode() == BlockPriceConfig.BalanceMode.BREAK_BLOCK)
                    {
                        double blockPrice = BlockPriceConfig.get().getPrice(entry.blockRegistry, entry.blockMeta);
                        if (blockPrice > 0)
                        {
                            data.addCurrency(blockPrice);
                        }
                    }
                    data.addBrokenBlocks(entry.setId, 1);
                }
                OneBlockPlayerDataProvider.saveToEntity(player, data);
                PacketSyncPlayerData.sendToPlayer(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event)
    {
        if (event.world.isRemote)
        {
            return;
        }

        World world = event.world;
        if (world.provider.dimensionId != OVERWORLD_DIMENSION_ID
                || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        int bx = event.x;
        int by = event.y;
        int bz = event.z;
        Pos pos = new Pos(bx, by, bz);
        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
        GeneratedBlockRegistry.GeneratedBlockEntry entry = registry.getEntry(bx, by, bz);
        GeneratedBlockRegistry.GeneratedBlockEntry savedEntry = pendingMobSpawnEntries.remove(pos);

        boolean isTrackedBlock = entry != null || savedEntry != null;
        GeneratedBlockRegistry.GeneratedBlockEntry mobSpawnEntry = entry != null ? entry : savedEntry;

        if (!isTrackedBlock)
        {
            return;
        }

        Pos generatorPos = new Pos(mobSpawnEntry.generatorX, mobSpawnEntry.generatorY, mobSpawnEntry.generatorZ);

        UUID breakerId = lastBreakPlayers.remove(pos);
        EntityPlayer player = breakerId != null ? world.getPlayerEntityByUUID(breakerId) : event.harvester;

        // Get the standard drops
        List<ItemStack> drops = new ArrayList<>();
        if (event.drops != null)
        {
            drops.addAll(event.drops);
        }

        // Check whether there are any real drops
        boolean hasRealDrops = false;
        for (ItemStack drop : drops)
        {
            if (drop != null && drop.stackSize > 0)
            {
                hasRealDrops = true;
                break;
            }
        }

        // If there are no drops - check whether the block should drop at all
        if (!hasRealDrops)
        {
            Block block = event.block;

            // Check whether the block can be mined without special conditions
            if (block != null && block != Blocks.air)
            {
                // Blocks that should not drop without a tool
                boolean shouldDropBlock = isShouldDropBlock(block, player, drops);

                if (shouldDropBlock)
                {
                    Item item = Item.getItemFromBlock(block);
                    if (item != null)
                    {
                        ItemStack blockStack = new ItemStack(item, 1, event.blockMetadata);
                        drops.add(blockStack);
                    }
                }
            }
        }

        if (player == null)
        {
            TileEntity generatedTile = world.getTileEntity(mobSpawnEntry.generatorX, mobSpawnEntry.generatorY, mobSpawnEntry.generatorZ);
            if (generatedTile instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatedTile;
                generator.markNonPlayerBreak(world.getTotalWorldTime());
                generator.tryGenerateBlock();
            }
            return;
        }

        // Blocks re-placed by the player (generatorPos == pos) should drop normally to world
        boolean isPlayerRePlaced = generatorPos.equals(pos);
        if (isPlayerRePlaced)
        {
            // Tag obuGenerated on drops and let them fall naturally
            for (ItemStack drop : drops)
            {
                if (drop == null || drop.stackSize <= 0)
                {
                    continue;
                }
                if (!drop.hasTagCompound())
                {
                    drop.setTagCompound(new NBTTagCompound());
                }
                drop.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);
            }
            return;
        }

        // Clear all drops
        if (event.drops != null)
        {
            event.drops.clear();
        }

        // Add drops to the player's inventory
        for (ItemStack drop : drops)
        {
            if (drop == null || drop.stackSize <= 0)
            {
                continue;
            }

            ItemStack remaining = drop.copy();
            if (remaining.getTagCompound() == null)
            {
                remaining.setTagCompound(new NBTTagCompound());
            }
            remaining.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);
            if (!player.inventory.addItemStackToInventory(remaining))
            {
                EntityItem entityItem = new EntityItem(world, bx + BLOCK_CENTER_OFFSET, by + BLOCK_CENTER_OFFSET, bz + BLOCK_CENTER_OFFSET, remaining);
                world.spawnEntityInWorld(entityItem);
            }
        }
    }

    private static boolean isShouldDropBlock(Block block, EntityPlayer player, List<ItemStack> drops)
    {
        if (player == null)
        {
            return true;
        }

        boolean shouldDropBlock = true;

        // Check for grass
        if (block == Blocks.tallgrass || block == Blocks.double_plant || block == Blocks.deadbush)
        {
            ItemStack heldItem = player.getHeldItem();
            if (heldItem == null || heldItem.getItem() != Items.shears)
            {
                shouldDropBlock = false;
            }
        }

        // Check for leaves
        if (block == Blocks.leaves || block == Blocks.leaves2)
        {
            ItemStack heldItem = player.getHeldItem();
            if (heldItem == null || heldItem.getItem() != Items.shears)
            {
                boolean hasSapling = false;
                for (ItemStack drop : drops)
                {
                    if (drop != null && drop.getItem() == Items.dye)
                    {
                        hasSapling = true;
                        break;
                    }
                }
                if (!hasSapling)
                {
                    shouldDropBlock = false;
                }
            }
        }

        // Check for cobwebs
        if (block == Blocks.web)
        {
            ItemStack heldItem = player.getHeldItem();
            if (heldItem == null || heldItem.getItem() != Items.shears)
            {
                shouldDropBlock = false;
            }
        }
        return shouldDropBlock;
    }

    private static void spawnMobOnBlockBreak(World world, int bx, int by, int bz, GeneratedBlockRegistry.GeneratedBlockEntry entry)
    {
        TileEntity tile = world.getTileEntity(entry.generatorX, entry.generatorY, entry.generatorZ);
        if (tile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tile;
            if (generator.isDisableMobGeneration())
            {
                OneBlockUltima.getLogger().info("[Mob Spawn] Mob generation disabled on generator at ({},{},{})",
                        entry.generatorX, entry.generatorY, entry.generatorZ);
                return;
            }
        }

        OneBlockUltima.getLogger().info("[Mob Spawn] Entry: setId={}, level={}", entry.setId, entry.level);
        BlockSetConfig.BlockSetDefinition set = BlockSetConfig.get().getSet(entry.setId);
        if (set == null)
        {
            OneBlockUltima.getLogger().info("[Mob Spawn] Set is null for setId={}", entry.setId);
            return;
        }

        BlockSetConfig.SetLevelDefinition levelDefinition = set.getLevel(entry.level);
        if (levelDefinition == null)
        {
            OneBlockUltima.getLogger().info("[Mob Spawn] Level definition is null for level={}", entry.level);
            return;
        }

        BlockSetConfig.MobEntryDefinition mobEntry = levelDefinition.pickMob(world.rand);
        if (mobEntry == null || mobEntry.registry == null || mobEntry.registry.isEmpty())
        {
            OneBlockUltima.getLogger().info("[Mob Spawn] No mob entries for level={}", entry.level);
            return;
        }

        OneBlockUltima.getLogger().info("[Mob Spawn] Attempting to spawn mob: {} count={}", mobEntry.registry, mobEntry.count);
        for (int i = 0; i < Math.max(1, mobEntry.count); i++)
        {
            Entity entity = createMobEntity(mobEntry.registry, world);
            if (entity == null)
            {
                OneBlockUltima.getLogger().info("[Mob Spawn] Failed to create entity for registry: {}", mobEntry.registry);
                continue;
            }

            entity.setPosition(bx + BLOCK_CENTER_OFFSET, by + BLOCK_TOP_OFFSET, bz + BLOCK_CENTER_OFFSET);

            // Apply NBT tags to the mob if present
            if (mobEntry.nbtTags != null && !mobEntry.nbtTags.hasNoTags())
            {
                BlockUtil.applyNbtToEntity(entity, mobEntry.nbtTags);
            }

            // Randomize slime size (health scales with size automatically)
            if (entity instanceof EntitySlime)
            {
                EntitySlime slime = (EntitySlime) entity;
                int size = 1 + world.rand.nextInt(4);
                java.lang.reflect.Method setSlimeSize = findMethodByName(EntitySlime.class,
                        new Class<?>[] { int.class }, "setSlimeSize", "func_70799_a");
                if (setSlimeSize != null)
                {
                    try
                    {
                        setSlimeSize.invoke(slime, size);
                    }
                    catch (Exception e)
                    {
                        OneBlockUltima.getLogger().error("[Mob Spawn] Failed to randomize slime size", e);
                    }
                }
            }

            world.spawnEntityInWorld(entity);
        }
    }

    private static Entity createMobEntity(String registry, World world)
    {
        return MobIdUtil.createEntity(registry, world);
    }

    private static java.lang.reflect.Method findMethodByName(Class<?> clazz, Class<?>[] paramTypes, String... names)
    {
        for (String name : names)
        {
            try
            {
                java.lang.reflect.Method method = clazz.getDeclaredMethod(name, paramTypes);
                method.setAccessible(true);
                return method;
            }
            catch (NoSuchMethodException ignored)
            {
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onEntityConstructing(net.minecraftforge.event.entity.EntityEvent.EntityConstructing event)
    {
        if (event.entity instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) event.entity;
            if (player.getExtendedProperties(OneBlockPlayerDataProvider.EXT_KEY) == null)
            {
                player.registerExtendedProperties(OneBlockPlayerDataProvider.EXT_KEY, new OneBlockPlayerDataProvider());
            }
        }
    }

    @SubscribeEvent
    public void clonePlayer(net.minecraftforge.event.entity.player.PlayerEvent.Clone event)
    {
        IOneBlockPlayerData oldData = OneBlockPlayerDataProvider.get(event.original);
        IOneBlockPlayerData newData = OneBlockPlayerDataProvider.get(event.entityPlayer);
        if (oldData instanceof OneBlockPlayerData && newData instanceof OneBlockPlayerData)
        {
            OneBlockPlayerData oldPlayerData = (OneBlockPlayerData) oldData;
            OneBlockPlayerData newPlayerData = (OneBlockPlayerData) newData;

            newPlayerData.copyFrom(oldPlayerData);
            OneBlockPlayerDataProvider.saveToEntity(event.entityPlayer, newPlayerData);
        }
    }

    private static final class Pos
    {
        public final int x;
        public final int y;
        public final int z;

        private Pos(int x, int y, int z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Pos))
            {
                return false;
            }
            Pos pos = (Pos) o;
            return x == pos.x && y == pos.y && z == pos.z;
        }

        @Override
        public int hashCode()
        {
            return (x * 31 + y) * 31 + z;
        }
    }
}
