package ru.defea.oneblockultima.event;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.GuiHandler;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.network.PacketSyncBlockSetConfig;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.update.UpdateChecker;
import ru.defea.oneblockultima.util.BlockUtil;
import ru.defea.oneblockultima.world.GeneratedBlockRegistry;
import ru.defea.oneblockultima.world.OneBlockWorldType;
import ru.defea.oneblockultima.world.SpawnConfigData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static ru.defea.oneblockultima.Constants.*;
import static ru.defea.oneblockultima.block.BlockOneBlockGenerator.*;

@Mod.EventBusSubscriber(modid = OneBlockUltima.MODID)
public final class ModEvents
{
    private ModEvents()
    {
    }

    private static final java.util.Map<BlockPos, Boolean> processingBlocks = new java.util.HashMap<>();
    private static final Map<BlockPos, UUID> pendingGeneratorOwners = new HashMap<>();
    private static final Map<String, Long> lastAccessDeniedMessageTicks = new HashMap<>();
    private static final Map<BlockPos, GeneratedBlockRegistry.GeneratedBlockEntry> pendingMobSpawnEntries = new HashMap<>();
    private static final Map<BlockPos, UUID> lastBreakPlayers = new HashMap<>();
    private static int lastBreakCleanupTick = 0;

    static final Map<UUID, Double> lastDisplayedCurrency = new HashMap<>();

    public static boolean trySendAccessDeniedMessage(UUID playerId, BlockPos pos, long worldTick)
    {
        if (playerId == null || pos == null)
        {
            return false;
        }

        String key = playerId + ":" + pos.toLong();
        Long lastTick = lastAccessDeniedMessageTicks.get(key);
        if (lastTick != null && lastTick == worldTick)
        {
            return false;
        }

        lastAccessDeniedMessageTicks.put(key, worldTick);
        return true;
    }

    public static void trySendAccessDeniedMessage(EntityPlayer player, BlockPos pos, long worldTick)
    {
        if (!(player instanceof EntityPlayerMP))
        {
            return;
        }

        if (!trySendAccessDeniedMessage(player.getUniqueID(), pos, worldTick))
        {
            return;
        }

        player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("generator.access.denied").setStyle(new net.minecraft.util.text.Style().setColor(net.minecraft.util.text.TextFormatting.RED)));
    }

    public static boolean ensureGeneratorAccess(World world, BlockPos pos, EntityPlayer player, TileEntityOneBlockGenerator generator)
    {
        if (generator == null || player == null)
        {
            return false;
        }

        if (world != null && pos != null)
        {
            applyPendingGeneratorOwner(world, pos);
        }

        return generator.hasAccess(player);
    }

    public static void registerPendingGeneratorOwner(World world, BlockPos pos, UUID playerId)
    {
        if (world == null || pos == null || playerId == null)
        {
            return;
        }

        pendingGeneratorOwners.put(pos, playerId);
    }

    public static void applyPendingGeneratorOwner(World world, BlockPos pos)
    {
        if (world == null || pos == null)
        {
            return;
        }

        UUID playerId = pendingGeneratorOwners.get(pos);
        if (playerId == null)
        {
            return;
        }

        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityOneBlockGenerator))
        {
            OneBlockUltima.getLogger().warn("[OwnerDebug] applyPendingGeneratorOwner: TE at {} is NOT a generator! it's {}", pos, tileEntity != null ? tileEntity.getClass().getSimpleName() : "NULL");
            return;
        }

        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
        OneBlockUltima.getLogger().info("[OwnerDebug] applyPendingGeneratorOwner: pos={}, playerId={}, currentOwnerId={}, isFree={}", pos, playerId, generator.getOwnerId(), generator.isFree());
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
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.player == null || event.player.world == null || event.player.world.isRemote)
        {
            return;
        }

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(event.player);
        if (data instanceof ru.defea.oneblockultima.capability.OneBlockPlayerData)
        {
            OneBlockPlayerDataProvider.loadFromEntity(event.player, data);
            PacketSyncPlayerData.sendToPlayer(event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.player.world.isRemote)
        {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        World world = player.world;
        if (world.provider.getDimension() != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        // Проверяем, есть ли у игрока своя точка возрождения
        boolean hasSpawn = player.isSpawnForced(0);

        // Если у игрока нет своей точки возрождения (кровать или команда)
        //noinspection ConstantConditions
        BlockPos spawnPos = new BlockPos(FLUID_BARRIER_POS);
        if (!hasSpawn)
        {
            world.setSpawnPoint(spawnPos);
            player.setSpawnPoint(spawnPos, true);
            player.setSpawnChunk(spawnPos, true, player.dimension);
            player.setPositionAndUpdate(spawnPos.getX() + BLOCK_CENTER_OFFSET, spawnPos.getY(), spawnPos.getZ() + BLOCK_CENTER_OFFSET);
            player.connection.setPlayerLocation(spawnPos.getX() + BLOCK_CENTER_OFFSET, spawnPos.getY(), spawnPos.getZ() + BLOCK_CENTER_OFFSET, player.rotationYaw, player.rotationPitch);
        }

        // Синхронизируем данные игрока после респавна
        PacketSyncPlayerData.sendToPlayer(player);
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event)
    {
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimension() != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        createWorldIcon(world);

        SpawnConfigData data = SpawnConfigData.get(world);
        if (!data.spawnInitialized)
        {
            world.setSpawnPoint(new BlockPos(FLUID_BARRIER_POS));
            data.spawnInitialized = true;
            data.markDirty();
        }

        if (world.getBlockState(GENERATOR_POS).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            if (world.getBlockState(FLUID_BARRIER_POS).getBlock() == Blocks.AIR)
            {
                world.setBlockState(FLUID_BARRIER_POS, ModBlocks.FLUID_BARRIER.getDefaultState(), 2);
                OneBlockUltima.getLogger().info("[Generator] BARRIER placed at {} on world load", FLUID_BARRIER_POS);
            }

            TileEntity tileEntity = world.getTileEntity(GENERATOR_POS);
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);

                if (!registry.isGenerated(GENERATED_BLOCK_POS))
                {
                    ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
                }
            }
            else
            {
                world.scheduleUpdate(GENERATOR_POS, ModBlocks.ONE_BLOCK_GENERATOR, 1);
            }
            return;
        }

        if (world.getBlockState(GENERATOR_POS).getMaterial().isReplaceable()
                || world.getBlockState(GENERATOR_POS).getBlock().isAir(world.getBlockState(GENERATOR_POS), world, GENERATOR_POS))
        {
            world.setBlockState(GENERATOR_POS, ModBlocks.ONE_BLOCK_GENERATOR.getDefaultState(), 3);

            TileEntity createdGenerator = world.getTileEntity(GENERATOR_POS);
            if (createdGenerator instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) createdGenerator;
                generator.setOwnerId(null);
            }

            if (world.getBlockState(FLUID_BARRIER_POS).getBlock() == Blocks.AIR)
            {
                world.setBlockState(FLUID_BARRIER_POS, ModBlocks.FLUID_BARRIER.getDefaultState(), 2);
                OneBlockUltima.getLogger().info("[Generator] BARRIER placed at {} on generator creation", FLUID_BARRIER_POS);
            }

            TileEntity tileEntity = world.getTileEntity(GENERATOR_POS);
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
            }
            else
            {
                world.scheduleUpdate(GENERATOR_POS, ModBlocks.ONE_BLOCK_GENERATOR, 1);
            }
        }
    }

    private static void createWorldIcon(World world)
    {
        File worldDir = world.getSaveHandler().getWorldDirectory();
        File iconFile = new File(worldDir, "icon.png");

        // Если иконка уже есть - не перезаписываем
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

            // Копируем файл
            Files.copy(input, iconFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            OneBlockUltima.getLogger().info("Icon created for world: {}", worldDir.getName());
        }
        catch (IOException e)
        {
            OneBlockUltima.getLogger().warn("Failed to create icon.png for OneBlock world {}", worldDir.getName(), e);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.player == null || event.player.world == null || event.player.world.isRemote)
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
                OneBlockUltima.getLogger().info("[Sync] Sending BlockSetConfig packet to {}", event.player.getName());
                ModMessages.sendToPlayer(packet, (EntityPlayerMP) event.player);
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[Sync] Failed to send BlockSetConfig", e);
        }

        World world = event.player.world;
        if (world.provider.getDimension() != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        TileEntity tileEntity = world.getTileEntity(GENERATOR_POS);
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
            if (generator.isFree())
            {
                generator.tryAssignOwnerIfEligible(event.player.getUniqueID());
            }
        }

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (world.provider.getDimension() != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        SpawnConfigData data = SpawnConfigData.get(world);
        BlockPos spawnPos = new BlockPos(FLUID_BARRIER_POS);
        if (!data.spawnInitialized)
        {
            world.setSpawnPoint(spawnPos);
            data.spawnInitialized = true;
            data.markDirty();
        }

        BlockPos currentSpawn = world.getSpawnPoint();
        if (!currentSpawn.equals(spawnPos))
        {
            world.setSpawnPoint(spawnPos);
        }

        player.setSpawnPoint(spawnPos, true);
        player.setSpawnChunk(spawnPos, true, player.dimension);

        IOneBlockPlayerData playerData = OneBlockPlayerDataProvider.get(player);
        if (playerData != null)
        {
            OneBlockPlayerDataProvider.loadFromEntity(player, playerData);
        }

        if (!data.spawnTeleportDone)
        {
            player.setPositionAndUpdate(spawnPos.getX() + BLOCK_CENTER_OFFSET, spawnPos.getY(), spawnPos.getZ() + BLOCK_CENTER_OFFSET);
            player.connection.setPlayerLocation(spawnPos.getX() + BLOCK_CENTER_OFFSET, spawnPos.getY(), spawnPos.getZ() + BLOCK_CENTER_OFFSET, player.rotationYaw, player.rotationPitch);
            data.spawnTeleportDone = true;
            data.markDirty();
        }

        PacketSyncPlayerData.sendToPlayer(player);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != Phase.END) return;
        if (event.player == null || event.player.world == null) return;

        try
        {
            net.minecraft.entity.player.EntityPlayer player = event.player;
            net.minecraft.world.World world = player.world;
            GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);

            int baseY = (int) Math.floor(player.posY - 0.1D);
            // Check the block at player's feet and the block immediately below it
            for (int dy = 0; dy <= 1; dy++)
            {
                BlockPos checkPos = new BlockPos(player.posX, baseY - dy, player.posZ);
                if (!registry.isGenerated(checkPos)) continue;

                net.minecraft.block.state.IBlockState state = world.getBlockState(checkPos);
                boolean hasCollision = state.getCollisionBoundingBox(world, checkPos) != null && !state.getMaterial().isReplaceable();
                double relY = player.posY - checkPos.getY();
                // If there's no collision and player is falling into the block space, stop the fall and snap player above
                if (!hasCollision && player.motionY < 0 && relY <= 1.2D)
                {
                    player.motionY = 0.0D;
                    player.fallDistance = 0.0F;
                    player.onGround = true;
                    player.setPosition(player.posX, checkPos.getY() + 1.0D, player.posZ);
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
        catch (Exception ignored) { }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event)
    {
        if (event.phase != Phase.END || event.world == null || event.world.isRemote)
        {
            return;
        }

        for (TileEntity tileEntity : event.world.loadedTileEntityList)
        {
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                ((TileEntityOneBlockGenerator) tileEntity).tickInvites();
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
    public static void onLivingSpawn(LivingSpawnEvent.CheckSpawn event)
    {
        if (event.getWorld().isRemote)
        {
            return;
        }

        World world = event.getWorld();
        if (world.provider.getDimension() != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        if (ModSettings.get().getMobWorldGeneration())
        {
            return;
        }

        if (!event.isSpawner())
        {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(net.minecraftforge.event.entity.EntityJoinWorldEvent event)
    {
        if (event.getWorld().isRemote)
        {
            return;
        }

        if (!(event.getEntity() instanceof net.minecraft.entity.item.EntityItem))
        {
            return;
        }

        net.minecraft.entity.item.EntityItem entityItem = (net.minecraft.entity.item.EntityItem) event.getEntity();
        World world = event.getWorld();

        if (world.provider.getDimension() != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        BlockPos pos = new BlockPos(entityItem.posX, entityItem.posY, entityItem.posZ);
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

        net.minecraft.item.ItemStack stack = entityItem.getItem();
        if (stack.isEmpty())
        {
            return;
        }

        player.inventory.addItemStackToInventory(stack);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.PlaceEvent event)
    {
        if (event.getWorld().isRemote)
        {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState placedState = event.getPlacedBlock();

        // Передаём obuGenerated из ItemStack при повторном размещении через PlaceEvent
        if (event.getPlayer() != null)
        {
            ItemStack heldItem = event.getPlayer().getHeldItemMainhand();
            if (!heldItem.isEmpty() && heldItem.hasTagCompound()) {
                assert heldItem.getTagCompound() != null;
                if (heldItem.getTagCompound().hasKey(NBT_OBU_GENERATED)
                        && heldItem.getTagCompound().getBoolean(NBT_OBU_GENERATED)) {
                    TileEntity placedTE = world.getTileEntity(pos);
                    if (placedTE != null) {
                        placedTE.getTileData().setBoolean(NBT_OBU_GENERATED, true);
                        placedTE.markDirty();
                    }

                    GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
                    if (!registry.isGenerated(pos)) {
                        Block placedBlock = placedState.getBlock();
                        String blockRegistry = placedBlock.getRegistryName() != null ? placedBlock.getRegistryName().toString() : "";
                        int blockMeta = placedBlock.getMetaFromState(placedState);
                        registry.markGenerated(pos, pos, "", 0, 0, blockRegistry, blockMeta);
                    }
                }
            }
        }

        // OneBlock-specific logic
        if (world.provider.getDimension() != OVERWORLD_DIMENSION_ID || world.getWorldInfo().getTerrainType() != OneBlockWorldType.ONE_BLOCK)
        {
            return;
        }

        if (placedState.getBlock() == ModBlocks.ONE_BLOCK_GENERATOR && event.getPlayer() != null)
        {
            OneBlockUltima.getLogger().info("[OwnerDebug] PlaceEvent fired for generator at {} by player {}", pos, event.getPlayer().getName());
            registerPendingGeneratorOwner(world, pos, event.getPlayer().getUniqueID());

            TileEntity tileEntity = world.getTileEntity(pos);
            OneBlockUltima.getLogger().info("[OwnerDebug] tileEntity at pos {}: {}", pos, tileEntity != null ? tileEntity.getClass().getSimpleName() : "NULL");
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
                OneBlockUltima.getLogger().info("[OwnerDebug] generator.isFree()={}, ownerId={}, player={}", generator.isFree(), generator.getOwnerId(), event.getPlayer().getName());
                if (generator.isFree())
                {
                    boolean result = generator.assignOwnerForPlacement(event.getPlayer().getUniqueID());
                    OneBlockUltima.getLogger().info("[OwnerDebug] assignOwnerForPlacement result={}, new ownerId={}", result, generator.getOwnerId());
                }
                else
                {
                    OneBlockUltima.getLogger().warn("[OwnerDebug] generator is NOT free, skipping assignOwnerForPlacement! ownerId={}", generator.getOwnerId());
                }
            }
        }

        IBlockState replacementState = BlockUtil.getReplacementStateForGeneratorPlacement(placedState, world.getBlockState(pos.down()));
        if (replacementState != placedState)
        {
            world.setBlockState(pos, replacementState, 3);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getWorld().isRemote || event.getEntityPlayer() == null)
        {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();

        BlockPos clickedPos = event.getPos();
        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(event.getWorld());

        BlockPos generatorPos = null;
        if (event.getWorld().getBlockState(clickedPos).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            generatorPos = clickedPos;
        }
        else if (event.getWorld().getBlockState(clickedPos.down()).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            generatorPos = clickedPos.down();
        }
        else
        {
            BlockPos registryGenPos = registry.getGeneratorPos(clickedPos);
            if (registryGenPos != null && event.getWorld().getBlockState(registryGenPos).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
            {
                generatorPos = registryGenPos;
            }
        }

        if (player.isSneaking())
        {
            if (generatorPos != null && event.getHand() == EnumHand.MAIN_HAND)
            {
                ItemStack heldItem = player.getHeldItemMainhand();
                if (!heldItem.isEmpty() && heldItem.getItem() instanceof net.minecraft.item.ItemBlock)
                {
                    TileEntity te = event.getWorld().getTileEntity(generatorPos);
                    if (te instanceof TileEntityOneBlockGenerator)
                    {
                        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) te;
                        if (generator.hasAccess(player))
                        {
                            Vec3d hitVec = event.getHitVec();
                            BlockPos placePos = getBlockPos(hitVec, generatorPos);

                            if (!placePos.equals(generatorPos))
                            {
                                IBlockState placeState = ((net.minecraft.item.ItemBlock) heldItem.getItem()).getBlock().getStateForPlacement(
                                        event.getWorld(), placePos, Objects.requireNonNull(event.getFace()), (float) (hitVec.x - placePos.getX()),
                                        (float) (hitVec.y - placePos.getY()), (float) (hitVec.z - placePos.getZ()),
                                        heldItem.getMetadata(), player, EnumHand.MAIN_HAND);
                                placeState = BlockUtil.getReplacementStateForGeneratorPlacement(placeState, event.getWorld().getBlockState(placePos.down()));
                                event.getWorld().setBlockState(placePos, placeState, 3);
                                if (!player.isCreative())
                                {
                                    heldItem.shrink(1);
                                }

                                TileEntity placedTE = event.getWorld().getTileEntity(placePos);
                                if (placedTE instanceof TileEntityOneBlockGenerator)
                                {
                                    ((TileEntityOneBlockGenerator) placedTE).assignOwnerForPlacement(player.getUniqueID());
                                }

                                if (heldItem.hasTagCompound()) {
                                    assert heldItem.getTagCompound() != null;
                                    if (heldItem.getTagCompound().hasKey(NBT_OBU_GENERATED) && heldItem.getTagCompound().getBoolean(NBT_OBU_GENERATED)) {
                                        if (placedTE != null) {
                                            NBTTagCompound teNbt = placedTE.getTileData();
                                            teNbt.setBoolean(NBT_OBU_GENERATED, true);
                                            placedTE.markDirty();
                                        }

                                        if (!registry.isGenerated(placePos)) {
                                            Block placedBlock = placeState.getBlock();
                                            String blockReg = placedBlock.getRegistryName() != null ? placedBlock.getRegistryName().toString() : "";
                                            int blockMeta = placedBlock.getMetaFromState(placeState);
                                            registry.markGenerated(placePos, placePos, "", 0, 0, blockReg, blockMeta);
                                        }
                                    }
                                }

                                event.setCanceled(true);
                                return;
                            }
                        }
                    }
                }
            }
            return;
        }

        boolean denied = false;

        if (generatorPos != null)
        {
            TileEntity generatorTile = event.getWorld().getTileEntity(generatorPos);
            if (generatorTile instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatorTile;
                OneBlockUltima.getLogger().info("[OwnerDebug] RightClick generator at {}, isFree={}, ownerId={}, player={}", generatorPos, generator.isFree(), generator.getOwnerId(), player.getName());
                if (generator.isFree() && generator.canBeClaimedBy(player.getUniqueID()))
                {
                    OneBlockUltima.getLogger().info("[OwnerDebug] Opening claim screen");
                    event.setCanceled(true);
                    GuiHandler.openClaimScreen(player, generatorPos);
                    return;
                }

                if (!ensureGeneratorAccess(event.getWorld(), generatorPos, player, generator))
                {
                    OneBlockUltima.getLogger().warn("[OwnerDebug] ACCESS DENIED for player {} on generator at {}", player.getName(), generatorPos);
                    denied = true;
                }
                else
                {
                    OneBlockUltima.getLogger().info("[OwnerDebug] ACCESS GRANTED for player {} on generator at {}", player.getName(), generatorPos);
                }
            }
        }

        if (denied)
        {
            event.setCanceled(true);
            trySendAccessDeniedMessage(player, generatorPos, event.getWorld().getTotalWorldTime());
            return;
        }

        if (generatorPos != null)
        {
            event.setCanceled(true);
            GuiHandler.open(player, generatorPos);
        }
    }

    private static BlockPos getBlockPos(Vec3d hitVec, BlockPos generatorPos) {
        double relX = hitVec.x - generatorPos.getX();
        double relY = hitVec.y - generatorPos.getY();
        double relZ = hitVec.z - generatorPos.getZ();

        BlockPos placePos;
        if (relY >= 1.99) {
            placePos = generatorPos.up(2);
        } else if (relX <= 0.01) {
            placePos = generatorPos.up().west();
        } else if (relX >= 0.99) {
            placePos = generatorPos.up().east();
        } else if (relZ <= 0.01) {
            placePos = generatorPos.up().north();
        } else if (relZ >= 0.99) {
            placePos = generatorPos.up().south();
        } else {
            placePos = generatorPos.up();
        }
        return placePos;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if (event.getWorld().isRemote)
        {
            return;
        }

        BlockPos pos = event.getPos();
        World world = event.getWorld();
        EntityPlayer breaker = event.getPlayer();

        if (breaker != null && world.provider.getDimension() == OVERWORLD_DIMENSION_ID && world.getWorldInfo().getTerrainType() == OneBlockWorldType.ONE_BLOCK)
        {
            lastBreakPlayers.put(pos, breaker.getUniqueID());
        }

        if (world.getBlockState(pos).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            event.setCanceled(true);
            return;
        }

        if (processingBlocks.getOrDefault(event.getPos(), false))
        {
            return;
        }

        TileEntity generatorTile = world.getTileEntity(pos);
        if (generatorTile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatorTile;
            if (!generator.hasAccess(event.getPlayer()))
            {
                event.setCanceled(true);
                trySendAccessDeniedMessage(event.getPlayer(), pos, world.getTotalWorldTime());
                return;
            }
        }

        if (world.getBlockState(pos).getBlock() == ModBlocks.FLUID_BARRIER && world.getBlockState(pos.down(2)).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            world.setBlockState(pos, ModBlocks.FLUID_BARRIER.getDefaultState(), 2);
            OneBlockUltima.getLogger().info("[Generator] BARRIER placed at {} after block break", pos);
            event.setCanceled(true);
            return;
        }

        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(event.getWorld());
        GeneratedBlockRegistry.GeneratedBlockEntry entry = registry.getEntry(event.getPos());
        if (entry == null)
        {
            OneBlockUltima.getLogger().warn("[BreakDebug] No generated entry found for broken block {}", event.getPos());
            return;
        }

        if (breaker != null && entry.generatorPos != null)
        {
            TileEntity genTile = world.getTileEntity(entry.generatorPos);
            if (genTile instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator gen = (TileEntityOneBlockGenerator) genTile;
                if (!gen.hasAccess(breaker))
                {
                    event.setCanceled(true);
                    trySendAccessDeniedMessage(breaker, entry.generatorPos, world.getTotalWorldTime());
                    return;
                }
            }
        }

        OneBlockUltima.getLogger().warn("[BreakDebug] Generated entry found for {} -> generator {} set={} level={}", event.getPos(), entry.generatorPos, entry.setId, entry.level);

        pendingMobSpawnEntries.put(event.getPos(), entry);

        spawnMobOnBlockBreak(world, event.getPos(), entry);
        registry.remove(event.getPos());

        assert entry.generatorPos != null;
        TileEntity generatedTile = world.getTileEntity(entry.generatorPos);
        EntityPlayer player = event.getPlayer();
        if (generatedTile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatedTile;
            if (player == null)
            {
                OneBlockUltima.getLogger().info("[BreakDebug] Non-player break detected at {} for generator {}", event.getPos(), entry.generatorPos);
                if (!generator.canProcessNonPlayerBreak(world.getTotalWorldTime()))
                {
                    OneBlockUltima.getLogger().info("[BreakDebug] Non-player break blocked by cooldown for generator {} at tick {}", entry.generatorPos, world.getTotalWorldTime());
                    event.setCanceled(true);
                    return;
                }

                generator.markNonPlayerBreak(world.getTotalWorldTime());
                OneBlockUltima.getLogger().info("[BreakDebug] Non-player break accepted for generator {} at tick {}", entry.generatorPos, world.getTotalWorldTime());
            }
        }

        if (world.getBlockState(event.getPos()).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            processingBlocks.put(event.getPos(), true);
        }

        if (player == null && generatedTile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatedTile;
            OneBlockUltima.getLogger().info("[BreakDebug] Invoking non-player generation for generator {} at tick {}", entry.generatorPos, world.getTotalWorldTime());
            generator.tryGenerateBlock();
        }
        else if (player != null)
        {
            OneBlockUltima.getLogger().info("[BreakDebug] Player break detected at {} for generator {}", event.getPos(), entry.generatorPos);
        }

        if (player != null)
        {
            IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(player);
            if (data != null)
            {
                // Only award currency and count broken blocks for blocks directly from generator
                if (!entry.generatorPos.equals(event.getPos()))
                {
                    if (BlockPriceConfig.get().getBalanceMode() == BlockPriceConfig.BalanceMode.BREAK_BLOCK)
                    {
                        double blockPrice = BlockPriceConfig.get().getPrice(entry.blockRegistry, entry.blockMeta);
                        if (blockPrice > 0) data.addCurrency(blockPrice);
                    }
                    data.addBrokenBlocks(entry.setId, 1);
                }
                PacketSyncPlayerData.sendToPlayer(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHarvestDrops(BlockEvent.HarvestDropsEvent event)
    {
        if (event.getWorld().isRemote)
        {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
        GeneratedBlockRegistry.GeneratedBlockEntry entry = registry.getEntry(pos);
        GeneratedBlockRegistry.GeneratedBlockEntry savedEntry = pendingMobSpawnEntries.remove(pos);

        boolean isTrackedBlock = entry != null || savedEntry != null;
        GeneratedBlockRegistry.GeneratedBlockEntry mobSpawnEntry = entry != null ? entry : savedEntry;

        if (!isTrackedBlock)
        {
            return;
        }

        UUID breakerId = lastBreakPlayers.remove(pos);
        EntityPlayer player = breakerId != null ? world.getPlayerEntityByUUID(breakerId) : event.getHarvester();

        // Mob spawn and registry removal now handled in onBlockBreak

        // Получаем стандартные дропы
        List<net.minecraft.item.ItemStack> drops = new java.util.ArrayList<>(event.getDrops());

        // Проверяем, есть ли реальные дропы
        boolean hasRealDrops = false;
        for (net.minecraft.item.ItemStack drop : drops)
        {
            if (!drop.isEmpty())
            {
                hasRealDrops = true;
                break;
            }
        }

        // Если дропов нет - проверяем, должен ли блок дропаться вообще
        if (!hasRealDrops)
        {
            net.minecraft.block.Block block = event.getState().getBlock();

            // Проверяем, может ли блок быть добыт без специальных условий
            // noinspection ConstantConditions
            if (block != null && block != Blocks.AIR)
            {
                // Список блоков, которые не должны дропаться без инструмента
                boolean shouldDropBlock = isShouldDropBlock(block, player, drops);

                if (shouldDropBlock)
                {
                    net.minecraft.item.Item item = net.minecraft.item.Item.getItemFromBlock(block);
                    // noinspection ConstantConditions
                    if (item != null && item != net.minecraft.init.Items.AIR)
                    {
                        net.minecraft.item.ItemStack blockStack = new net.minecraft.item.ItemStack(item, 1, block.getMetaFromState(event.getState()));
                        drops.add(blockStack);
                    }
                }
            }
        }

        if (player == null)
        {
            TileEntity generatedTile = world.getTileEntity(mobSpawnEntry.generatorPos);
            if (generatedTile instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatedTile;
                generator.markNonPlayerBreak(world.getTotalWorldTime());
                generator.tryGenerateBlock();
            }
            return;
        }

        // Blocks re-placed by the player (generatorPos == pos) should drop normally to world
        boolean isPlayerRePlaced = mobSpawnEntry.generatorPos != null && mobSpawnEntry.generatorPos.equals(pos);
        if (isPlayerRePlaced)
        {
            // Tag obuGenerated on drops and let them fall naturally
            for (net.minecraft.item.ItemStack drop : event.getDrops())
            {
                if (drop.isEmpty()) continue;
                if (!drop.hasTagCompound())
                {
                    drop.setTagCompound(new NBTTagCompound());
                }
                assert drop.getTagCompound() != null;
                drop.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);
            }
            return;
        }

        // Очищаем все дропы
        event.getDrops().clear();

        // Определяем, есть ли obuGenerated — любой блок из GeneratedBlockRegistry считается сгенерированным

        // Добавляем дропы в инвентарь игрока
        for (net.minecraft.item.ItemStack drop : drops)
        {
            if (drop.isEmpty()) continue;

            net.minecraft.item.ItemStack remaining = drop.copy();
            if (remaining.getTagCompound() == null) {
                remaining.setTagCompound(new NBTTagCompound());
            }
            remaining.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);
            if (!player.inventory.addItemStackToInventory(remaining))
            {
                net.minecraft.entity.item.EntityItem entityItem = new net.minecraft.entity.item.EntityItem(
                        event.getWorld(),
                        pos.getX() + BLOCK_CENTER_OFFSET,
                        pos.getY() + BLOCK_CENTER_OFFSET,
                        pos.getZ() + BLOCK_CENTER_OFFSET,
                        remaining
                );
                event.getWorld().spawnEntity(entityItem);
            }
        }
    }

    private static boolean isShouldDropBlock(Block block, EntityPlayer player, List<ItemStack> drops) {
        if (player == null)
        {
            return true;
        }

        boolean shouldDropBlock = true;

        // Проверяем на траву
        if (block == Blocks.TALLGRASS || block == Blocks.DOUBLE_PLANT || block == Blocks.DEADBUSH)
        {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (heldItem.isEmpty() || heldItem.getItem() != net.minecraft.init.Items.SHEARS)
            {
                shouldDropBlock = false;
            }
        }

        // Проверяем на листья
        if (block == Blocks.LEAVES || block == Blocks.LEAVES2)
        {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (heldItem.isEmpty() || heldItem.getItem() != net.minecraft.init.Items.SHEARS)
            {
                boolean hasSapling = false;
                for (ItemStack drop : drops)
                {
                    if (!drop.isEmpty() && drop.getItem() == net.minecraft.init.Items.DYE)
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

        // Проверяем на паутину
        if (block == Blocks.WEB)
        {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (heldItem.isEmpty() || heldItem.getItem() != net.minecraft.init.Items.SHEARS)
            {
                shouldDropBlock = false;
            }
        }
        return shouldDropBlock;
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event)
    {
        if (event.getWorld().isRemote)
        {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();

        // Восстанавливаем барьер над генератором, если слот барьера снова стал AIR
        // (например, после того как блок, поставленный над генератором, был сломан)
        if (world.getBlockState(pos.down(2)).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR
                && world.getBlockState(pos).getBlock() == Blocks.AIR)
        {
            world.setBlockState(pos, ModBlocks.FLUID_BARRIER.getDefaultState(), 3);
            OneBlockUltima.getLogger().info("[Generator] BARRIER restored at " + pos + " from NeighborNotify");
            return;
        }

        // Проверяем только блок над генератором
        if (world.getBlockState(event.getPos().down()).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            return;
        }

        // Проверяем, не AIR ли теперь блок
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != Blocks.AIR)
        {
            // Блок существует
            processingBlocks.put(pos, false);
        }
    }

    private static void spawnMobOnBlockBreak(World world, BlockPos pos, GeneratedBlockRegistry.GeneratedBlockEntry entry)
    {
        TileEntity tile = world.getTileEntity(entry.generatorPos);
        if (tile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tile;
            if (generator.isDisableMobGeneration())
            {
                OneBlockUltima.getLogger().info("[Mob Spawn] Mob generation disabled on generator at {}", entry.generatorPos);
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

        OneBlockUltima.getLogger().info("[Mob Spawn] Set found: {}", set.id);
        BlockSetConfig.SetLevelDefinition levelDefinition = set.getLevel(entry.level);
        if (levelDefinition == null)
        {
            OneBlockUltima.getLogger().info("[Mob Spawn] Level definition is null for level={}", entry.level);
            return;
        }

        OneBlockUltima.getLogger().info("[Mob Spawn] Level definition found. Total mobs: {}", levelDefinition.mobs != null ? levelDefinition.mobs.size() : 0);
        BlockSetConfig.MobEntryDefinition mobEntry = levelDefinition.pickMob(world.rand);
        if (mobEntry == null || mobEntry.registry == null || mobEntry.registry.isEmpty())
        {
            OneBlockUltima.getLogger().info("[Mob Spawn] pickMob returned null or empty registry");
            return;
        }

        OneBlockUltima.getLogger().info("[Mob Spawn] Attempting to spawn mob: {} count={}", mobEntry.registry, mobEntry.count);
        for (int i = 0; i < Math.max(1, mobEntry.count); i++)
        {
            Entity entity = EntityList.createEntityByIDFromName(new ResourceLocation(mobEntry.registry), world);
            if (entity == null)
            {
                OneBlockUltima.getLogger().info("[Mob Spawn] Failed to create entity for registry: {}", mobEntry.registry);
                continue;
            }

            OneBlockUltima.getLogger().info("[Mob Spawn] Successfully spawned mob: {}", mobEntry.registry);
            entity.setPosition(pos.getX() + BLOCK_CENTER_OFFSET, pos.getY() + BLOCK_TOP_OFFSET, pos.getZ() + BLOCK_CENTER_OFFSET);

            // Применяем NBT теги к мобу если они есть
            if (mobEntry.nbtTags != null && !mobEntry.nbtTags.hasNoTags())
            {
                OneBlockUltima.getLogger().info("[Mob Spawn] Applying NBT tags to mob: {}", mobEntry.nbtTags);
                ru.defea.oneblockultima.util.BlockUtil.applyNbtToEntity(entity, mobEntry.nbtTags);
            }

            world.spawnEntity(entity);
        }
    }

    @SubscribeEvent
    public static void attachCapabilities(net.minecraftforge.event.AttachCapabilitiesEvent<net.minecraft.entity.Entity> event)
    {
        if (event.getObject() instanceof EntityPlayer)
        {
            event.addCapability(
                    new ResourceLocation(OneBlockUltima.MODID, "player_data"),
                    new OneBlockPlayerDataProvider()
            );
        }
    }

    @SubscribeEvent
    public static void clonePlayer(net.minecraftforge.event.entity.player.PlayerEvent.Clone event)
    {
        IOneBlockPlayerData oldData = OneBlockPlayerDataProvider.get(event.getOriginal());
        IOneBlockPlayerData newData = OneBlockPlayerDataProvider.get(event.getEntityPlayer());
        if (oldData instanceof ru.defea.oneblockultima.capability.OneBlockPlayerData
                && newData instanceof ru.defea.oneblockultima.capability.OneBlockPlayerData)
        {
            ru.defea.oneblockultima.capability.OneBlockPlayerData oldPlayerData =
                    (ru.defea.oneblockultima.capability.OneBlockPlayerData) oldData;
            ru.defea.oneblockultima.capability.OneBlockPlayerData newPlayerData =
                    (ru.defea.oneblockultima.capability.OneBlockPlayerData) newData;

            newPlayerData.copyFrom(oldPlayerData);
            OneBlockPlayerDataProvider.saveToEntity(event.getEntityPlayer(), newPlayerData);
        }
    }
}
