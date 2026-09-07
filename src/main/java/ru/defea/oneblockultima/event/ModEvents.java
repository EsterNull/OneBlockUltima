package ru.defea.oneblockultima.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import ru.defea.oneblockultima.OneBlockUltima;
import ru.defea.oneblockultima.block.BlockOneBlockGenerator;
import ru.defea.oneblockultima.block.ModBlocks;
import ru.defea.oneblockultima.capability.IOneBlockPlayerData;
import ru.defea.oneblockultima.capability.OneBlockPlayerDataProvider;
import ru.defea.oneblockultima.config.BlockPriceConfig;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.config.ModSettings;
import ru.defea.oneblockultima.gui.containers.ContainerClaimGenerator;
import ru.defea.oneblockultima.gui.containers.ContainerOneBlock;
import ru.defea.oneblockultima.item.ItemCase;
import ru.defea.oneblockultima.item.ModItems;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.network.PacketSyncBlockSetConfig;
import ru.defea.oneblockultima.network.PacketSyncPlayerData;
import ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator;
import ru.defea.oneblockultima.update.UpdateChecker;
import ru.defea.oneblockultima.util.BlockUtil;
import ru.defea.oneblockultima.util.CaseUtil;
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

    private static final Map<BlockPos, Boolean> processingBlocks = new HashMap<>();
    private static final Map<BlockPos, UUID> pendingGeneratorOwners = new HashMap<>();
    private static final Map<String, Long> lastAccessDeniedMessageTicks = new HashMap<>();
    private static final Map<BlockPos, GeneratedBlockRegistry.GeneratedBlockEntry> pendingMobSpawnEntries = new HashMap<>();
    private static final Map<BlockPos, UUID> lastBreakPlayers = new HashMap<>();
    private static final Map<BlockPos, Long> pendingBreakDrops = new HashMap<>();

    private static final Map<BlockPos, Long> pendingMarkOnlyBreakDrops = new HashMap<>();

    private static final Set<BlockPos> placedGeneratedBlocks = new HashSet<>();
    private static int lastBreakCleanupTick = 0;

    static final Map<UUID, Double> lastDisplayedCurrency = new HashMap<>();

    public static boolean trySendAccessDeniedMessage(UUID playerId, BlockPos pos, long worldTick)
    {
        if (playerId == null || pos == null)
        {
            return false;
        }

        String key = playerId + ":" + pos.asLong();
        Long lastTick = lastAccessDeniedMessageTicks.get(key);
        if (lastTick != null && lastTick == worldTick)
        {
            return false;
        }

        lastAccessDeniedMessageTicks.put(key, worldTick);
        return true;
    }

    public static void trySendAccessDeniedMessage(Player player, BlockPos pos, long worldTick)
    {
        if (!(player instanceof ServerPlayer))
        {
            return;
        }

        if (!trySendAccessDeniedMessage(player.getUUID(), pos, worldTick))
        {
            return;
        }

        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("generator.access.denied")
                .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.ChatFormatting.RED)));
    }

    public static boolean ensureGeneratorAccess(Level world, BlockPos pos, Player player, TileEntityOneBlockGenerator generator)
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

    public static void registerPendingGeneratorOwner(Level world, BlockPos pos, UUID playerId)
    {
        if (world == null || pos == null || playerId == null)
        {
            return;
        }

        pendingGeneratorOwners.put(pos, playerId);
    }

    public static void applyPendingGeneratorOwner(Level world, BlockPos pos)
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

        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (!(tileEntity instanceof TileEntityOneBlockGenerator))
        {
            OneBlockUltima.logDebugWarn("[OwnerDebug] applyPendingGeneratorOwner: TE at {} is NOT a generator! it's {}", pos, tileEntity != null ? tileEntity.getClass().getSimpleName() : "NULL");
            return;
        }

        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
        OneBlockUltima.logDebug("[OwnerDebug] applyPendingGeneratorOwner: pos={}, playerId={}, currentOwnerId={}, isFree={}", pos, playerId, generator.getOwnerId(), generator.isFree());
        if (generator.assignOwnerForPlacement(playerId))
        {
            OneBlockUltima.logDebug("[OwnerDebug] applyPendingGeneratorOwner: SUCCESS, new ownerId={}", generator.getOwnerId());
            pendingGeneratorOwners.remove(pos);
        }
        else
        {
            OneBlockUltima.logDebugWarn("[OwnerDebug] applyPendingGeneratorOwner: FAILED to assign owner");
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() == null || event.getEntity().level() == null || event.getEntity().level().isClientSide())
        {
            return;
        }

        IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(event.getEntity());
        if (data instanceof ru.defea.oneblockultima.capability.OneBlockPlayerData)
        {
            OneBlockPlayerDataProvider.loadFromEntity(event.getEntity(), data);
            PacketSyncPlayerData.sendToPlayer((ServerPlayer) event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        Player player = event.getEntity();
        if (player.level().isClientSide())
        {
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        Level world = player.level();
        if (!OneBlockWorldType.isOneBlockWorld(world))
        {
            return;
        }

        boolean hasBedSpawn = serverPlayer.getRespawnPosition() != null;

        BlockPos spawnPos = new BlockPos(FLUID_BARRIER_POS);
        if (!hasBedSpawn)
        {
            ((ServerLevel) world).setDefaultSpawnPos(spawnPos, 0.0F);
            serverPlayer.setRespawnPosition(world.dimension(), spawnPos, 0.0F, true, true);
            player.teleportTo(spawnPos.getX() + BLOCK_CENTER_OFFSET, spawnPos.getY(), spawnPos.getZ() + BLOCK_CENTER_OFFSET);
        }

        IOneBlockPlayerData respawnData = OneBlockPlayerDataProvider.get(serverPlayer);
        if (respawnData != null)
        {
            OneBlockPlayerDataProvider.loadFromEntity(serverPlayer, respawnData);
            OneBlockUltima.getLogger().warn("[OBU-Balance] PlayerRespawnEvent: currency after loadFromEntity={}",
                    respawnData.getCurrency());
        }
        PacketSyncPlayerData.sendToPlayer(serverPlayer);
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event)
    {
        Level world = (Level) event.getLevel();
        if (world.isClientSide() || !OneBlockWorldType.isOneBlockWorld(world))
        {
            return;
        }

        OneBlockUltima.logDebug("[WorldInit] onLevelLoad ENTER dim={} genBlock={} barrier={}", world.dimension().location(), world.getBlockState(GENERATOR_POS).getBlock().getDescriptionId(), world.getBlockState(FLUID_BARRIER_POS).getBlock().getDescriptionId());

        createWorldIcon(world);

        SpawnConfigData data = SpawnConfigData.get(world);
        if (!data.spawnInitialized)
        {
            ((ServerLevel) world).setDefaultSpawnPos(new BlockPos(FLUID_BARRIER_POS), 0.0F);
            data.spawnInitialized = true;
            data.setDirty();
        }

        if (world.getBlockState(GENERATOR_POS).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            if (world.getBlockState(FLUID_BARRIER_POS).getBlock() == Blocks.AIR)
            {
                world.setBlock(FLUID_BARRIER_POS, ModBlocks.FLUID_BARRIER.defaultBlockState(), 2);
                OneBlockUltima.logDebug("[Generator] BARRIER placed at {} on world load", FLUID_BARRIER_POS);
            }

            BlockEntity tileEntity = world.getBlockEntity(GENERATOR_POS);
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
                world.scheduleTick(GENERATOR_POS, ModBlocks.ONE_BLOCK_GENERATOR, 1);
            }
            return;
        }

        if (world.getBlockState(GENERATOR_POS).canBeReplaced()
                || world.getBlockState(GENERATOR_POS).isAir())
        {
            world.setBlock(GENERATOR_POS, ModBlocks.ONE_BLOCK_GENERATOR.defaultBlockState(), 3);

            BlockEntity createdGenerator = world.getBlockEntity(GENERATOR_POS);
            if (createdGenerator instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) createdGenerator;
                generator.setOwnerId(null);
            }

            if (world.getBlockState(FLUID_BARRIER_POS).getBlock() == Blocks.AIR)
            {
                world.setBlock(FLUID_BARRIER_POS, ModBlocks.FLUID_BARRIER.defaultBlockState(), 2);
                OneBlockUltima.logDebug("[Generator] BARRIER placed at {} on generator creation", FLUID_BARRIER_POS);
            }

            BlockEntity tileEntity = world.getBlockEntity(GENERATOR_POS);
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                ((TileEntityOneBlockGenerator) tileEntity).tryGenerateBlock();
            }
            else
            {
                world.scheduleTick(GENERATOR_POS, ModBlocks.ONE_BLOCK_GENERATOR, 1);
            }
        }

        OneBlockUltima.logDebug("[WorldInit] onLevelLoad DONE dim={} genBlock={} generated={}", world.dimension().location(), world.getBlockState(GENERATOR_POS).getBlock().getDescriptionId(), GeneratedBlockRegistry.get(world).isGenerated(GENERATED_BLOCK_POS));
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event)
    {
        Level world = (Level) event.getLevel();
        if (world == null || world.isClientSide())
        {
            return;
        }

        GeneratedBlockRegistry.get(world).flushPendingDirty();
    }

    private static void createWorldIcon(Level world)
    {
        if (!(world instanceof ServerLevel))
        {
            return;
        }
        File worldDir = ((ServerLevel) world).getServer().getWorldPath(LevelResource.ROOT).toFile();
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
            OneBlockUltima.logDebug("Icon created for world: {}", worldDir.getName());
        }
        catch (IOException e)
        {
            OneBlockUltima.getLogger().warn("Failed to create icon.png for OneBlock world {}", worldDir.getName(), e);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() == null || event.getEntity().level() == null || event.getEntity().level().isClientSide())
        {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();

        UpdateChecker.checkForUpdates(player);

        try
        {
            String json = BlockSetConfig.get().toJson();
            OneBlockUltima.logDebug("[Sync] BlockSetConfig JSON length: {}", json != null ? json.length() : "null");
            if (json != null && !json.isEmpty())
            {
                PacketSyncBlockSetConfig packet = new PacketSyncBlockSetConfig(json);
                OneBlockUltima.logDebug("[Sync] Sending BlockSetConfig packet to {}", player.getName().getString());
                ModMessages.sendToPlayer(packet, player);
            }
        }
        catch (Exception e)
        {
            OneBlockUltima.getLogger().error("[Sync] Failed to send BlockSetConfig", e);
        }

        giveGuideBookOnFirstJoin(player);

        Level world = player.level();
        if (!OneBlockWorldType.isOneBlockWorld(world))
        {
            return;
        }

        BlockEntity tileEntity = world.getBlockEntity(GENERATOR_POS);
        if (tileEntity instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
            if (generator.isFree())
            {
                generator.tryAssignOwnerIfEligible(player.getUUID());
            }
        }

        SpawnConfigData data = SpawnConfigData.get(world);
        BlockPos spawnPos = new BlockPos(FLUID_BARRIER_POS);
        if (!data.spawnInitialized)
        {
            ((ServerLevel) world).setDefaultSpawnPos(spawnPos, 0.0F);
            data.spawnInitialized = true;
            data.setDirty();
        }

        BlockPos currentSpawn = world.getSharedSpawnPos();
        if (currentSpawn == null || !currentSpawn.equals(spawnPos))
        {
            ((ServerLevel) world).setDefaultSpawnPos(spawnPos, 0.0F);
        }

        player.getRespawnPosition();

        IOneBlockPlayerData playerData = OneBlockPlayerDataProvider.get(player);
        if (playerData != null)
        {
            OneBlockPlayerDataProvider.loadFromEntity(player, playerData);
        }

        if (!data.spawnTeleportDone)
        {
            player.teleportTo(spawnPos.getX() + BLOCK_CENTER_OFFSET, spawnPos.getY(), spawnPos.getZ() + BLOCK_CENTER_OFFSET);
            player.setYRot(player.getYRot());
            player.setXRot(player.getXRot());
            data.spawnTeleportDone = true;
            data.setDirty();
        }

        PacketSyncPlayerData.sendToPlayer(player);
    }

    private static void giveGuideBookOnFirstJoin(ServerPlayer player)
    {
        if (player == null || player.level() == null || !OneBlockWorldType.isOneBlockWorld(player.level()))
        {
            return;
        }

        CompoundTag playerData = player.getPersistentData();
        if (playerData.getBoolean(NBT_GUIDE_BOOK_GIVEN))
        {
            return;
        }

        playerData.putBoolean(NBT_GUIDE_BOOK_GIVEN, true);
        ItemStack guideBook = new ItemStack(ModItems.GUIDE_BOOK);
        if (!player.getInventory().add(guideBook))
        {
            ItemEntity entityItem = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), guideBook);
            player.level().addFreshEntity(entityItem);
        }
        OneBlockUltima.logDebug("[GuideBook] Given guide book to {}", player.getName().getString());
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player == null || player.level() == null) return;

        try
        {
            Level world = player.level();
            if (!OneBlockWorldType.isOneBlockWorld(world))
            {
                return;
            }
            GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);

            int baseY = (int) Math.floor(player.getY() - 0.1D);
            for (int dy = 0; dy <= 1; dy++)
            {
                BlockPos checkPos = BlockPos.containing(player.getX(), baseY - dy, player.getZ());
                if (!registry.isGenerated(checkPos)) continue;

                BlockState state = world.getBlockState(checkPos);
                boolean hasCollision = !state.getCollisionShape(world, checkPos).isEmpty() && !state.canBeReplaced();
                double relY = player.getY() - checkPos.getY();
                if (!hasCollision && player.getDeltaMovement().y < 0 && relY <= 1.2D)
                {
                    player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                    player.fallDistance = 0.0F;
                    player.setOnGround(true);
                    player.setPos(player.getX(), checkPos.getY() + 1.0D, player.getZ());
                    break;
                }
                if (hasCollision && relY <= 1.2D && relY >= 0.0D)
                {
                    player.setOnGround(true);
                    break;
                }
            }
        }
        catch (Exception ignored) { }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.level == null || event.level.isClientSide())
        {
            return;
        }

        if (!OneBlockWorldType.isOneBlockWorld(event.level))
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
                    if (generator == null || generator.isRemoved() || generator.getLevel() != event.level)
                    {
                        iterator.remove();
                        continue;
                    }
                    generator.tickInvites();
                }
            }
        }

        if (!lastAccessDeniedMessageTicks.isEmpty())
        {
            long worldTick = event.level.getGameTime();
            lastAccessDeniedMessageTicks.values().removeIf(lastTick -> lastTick != worldTick);
        }

        if (!lastBreakPlayers.isEmpty() && ++lastBreakCleanupTick >= 200)
        {
            lastBreakCleanupTick = 0;
            lastBreakPlayers.clear();
            pendingBreakDrops.clear();
            pendingMarkOnlyBreakDrops.clear();
        }
    }

    @SubscribeEvent
    public static void onLivingSpawn(MobSpawnEvent.PositionCheck event)
    {
        if (event.getLevel().isClientSide())
        {
            return;
        }

        Level world = (Level) event.getLevel();
        if (!OneBlockWorldType.isOneBlockWorld(world))
        {
            return;
        }

        if (ModSettings.get().getMobWorldGeneration())
        {
            return;
        }

        if (event.getSpawner() == null)
        {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide())
        {
            return;
        }

        if (!(event.getEntity() instanceof ItemEntity))
        {
            return;
        }

        ItemEntity entityItem = (ItemEntity) event.getEntity();
        Level world = event.getLevel();

        if (!OneBlockWorldType.isOneBlockWorld(world))
        {
            return;
        }

        BlockPos pos = BlockPos.containing(entityItem.getX(), entityItem.getY(), entityItem.getZ());
        boolean autoLoot = pendingBreakDrops.containsKey(pos);
        boolean markOnly = !autoLoot && pendingMarkOnlyBreakDrops.containsKey(pos);
        if (!autoLoot && !markOnly)
        {
            return;
        }

        ItemStack stack = entityItem.getItem();
        if (stack.isEmpty())
        {
            return;
        }
        if (!(stack.getItem() instanceof ItemCase))
        {
            markGenerated(stack);
        }

        if (!autoLoot)
        {
            return;
        }

        UUID breakerId = lastBreakPlayers.get(pos);
        if (breakerId == null)
        {
            return;
        }

        Player player = world.getPlayerByUUID(breakerId);
        if (player == null)
        {
            return;
        }

        player.getInventory().add(stack);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event)
    {
        if (event.getLevel().isClientSide())
        {
            return;
        }

        Level world = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState placedState = event.getPlacedBlock();

        if (event.getEntity() instanceof Player)
        {
            Player placer = (Player) event.getEntity();
            GeneratedBlockRegistry placedRegistry = GeneratedBlockRegistry.get(world);
            if (placedRegistry.isGenerated(pos))
            {
                placedRegistry.remove(pos);
                OneBlockUltima.logDebug("[BreakDebug] Player placed block at {} -> removed generated registry entry", pos);
            }

            ItemStack heldItem = placer.getMainHandItem();
            boolean isGeneratedItem = !heldItem.isEmpty()
                    && heldItem.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
                    && heldItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).getUnsafe().getBoolean(NBT_OBU_GENERATED);
            if (isGeneratedItem)
            {
                placedGeneratedBlocks.add(pos);
                OneBlockUltima.logDebug("[BreakDebug] Player placed generated block at {} -> mark-only on future break", pos);
            }
            else
            {
                placedGeneratedBlocks.remove(pos);
            }
        }

        if (!OneBlockWorldType.isOneBlockWorld(world))
        {
            return;
        }

        if (placedState.getBlock() == ModBlocks.ONE_BLOCK_GENERATOR && event.getEntity() instanceof Player)
        {
            Player placer = (Player) event.getEntity();
            OneBlockUltima.logDebug("[OwnerDebug] PlaceEvent fired for generator at {} by player {}", pos, placer.getName().getString());
            registerPendingGeneratorOwner(world, pos, placer.getUUID());

            BlockEntity tileEntity = world.getBlockEntity(pos);
            OneBlockUltima.logDebug("[OwnerDebug] tileEntity at pos {}: {}", pos, tileEntity != null ? tileEntity.getClass().getSimpleName() : "NULL");
            if (tileEntity instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tileEntity;
                OneBlockUltima.logDebug("[OwnerDebug] generator.isFree()={}, ownerId={}, player={}", generator.isFree(), generator.getOwnerId(), placer.getName().getString());
                if (generator.isFree())
                {
                    boolean result = generator.assignOwnerForPlacement(placer.getUUID());
                    OneBlockUltima.logDebug("[OwnerDebug] assignOwnerForPlacement result={}, new ownerId={}", result, generator.getOwnerId());
                }
                else
                {
                    OneBlockUltima.logDebugWarn("[OwnerDebug] generator is NOT free, skipping assignOwnerForPlacement! ownerId={}", generator.getOwnerId());
                }
            }
        }

        BlockState replacementState = BlockUtil.getReplacementStateForGeneratorPlacement(placedState, world.getBlockState(pos.below()));
        if (replacementState != placedState)
        {
            world.setBlock(pos, replacementState, 3);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event)
    {
        Player player = event.getEntity();
        if (player == null)
        {
            return;
        }

        Level world = event.getLevel();

        if (!OneBlockWorldType.isOneBlockWorld(world))
        {
            return;
        }

        BlockPos clickedPos = event.getPos();
        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);

        BlockPos generatorPos = null;
        if (world.getBlockState(clickedPos).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            generatorPos = clickedPos;
        }
        else if (world.getBlockState(clickedPos.below()).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            generatorPos = clickedPos.below();
        }
        else if (registry != null)
        {
            BlockPos registryGenPos = registry.getGeneratorPos(clickedPos);
            if (registryGenPos != null && world.getBlockState(registryGenPos).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
            {
                generatorPos = registryGenPos;
            }
        }

        if (generatorPos == null)
        {
            BlockPos scan = clickedPos;
            for (int i = 0; i < 12 && scan != null; i++)
            {
                scan = scan.below();
                BlockState scanState = world.getBlockState(scan);
                if (scanState.getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
                {
                    generatorPos = scan;
                    break;
                }
            }
        }

        if (generatorPos != null)
        {
            OneBlockUltima.logDebug("[GenClick] right-click at {} resolved generator {}", clickedPos, generatorPos);
        }
        else
        {
            OneBlockUltima.logDebug("[GenClick] right-click at {} state={} no generator found in column",
                    clickedPos, world.getBlockState(clickedPos).getBlock());
        }

        if (player.isShiftKeyDown())
        {
            if (!world.isClientSide() && generatorPos != null && event.getHand() == InteractionHand.MAIN_HAND)
            {
                ItemStack heldItem = player.getMainHandItem();
                if (!heldItem.isEmpty() && heldItem.getItem() instanceof BlockItem)
                {
                    BlockEntity te = world.getBlockEntity(generatorPos);
                    if (te instanceof TileEntityOneBlockGenerator)
                    {
                        TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) te;
                        if (generator.hasAccess(player))
                        {
                            BlockHitResult hitResult = event.getHitVec();
                            Vec3 hitVec = hitResult.getLocation();
                            BlockPos placePos = getBlockPos(hitVec, generatorPos);

                            if (!placePos.equals(generatorPos))
                            {
                                BlockState placeState = ((BlockItem) heldItem.getItem()).getBlock()
                                        .getStateForPlacement(new BlockPlaceContext(player, event.getHand(), heldItem,
                                                new BlockHitResult(hitVec, event.getFace(), placePos, false)));
                                if (placeState == null)
                                {
                                    placeState = ((BlockItem) heldItem.getItem()).getBlock().defaultBlockState();
                                }
                                placeState = BlockUtil.getReplacementStateForGeneratorPlacement(placeState, world.getBlockState(placePos.below()));
                                world.setBlock(placePos, placeState, 3);
                                if (!player.isCreative())
                                {
                                    heldItem.shrink(1);
                                }

                                BlockEntity placedTE = world.getBlockEntity(placePos);
                                if (placedTE instanceof TileEntityOneBlockGenerator)
                                {
                                    ((TileEntityOneBlockGenerator) placedTE).assignOwnerForPlacement(player.getUUID());
                                }

                                if (registry.isGenerated(placePos))
                                {
                                    registry.remove(placePos);
                                    OneBlockUltima.logDebug("[BreakDebug] Shift-right-click placed block at {} -> removed generated registry entry", placePos);
                                }

                                boolean isPlacedGeneratedItem = heldItem.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
                                        && heldItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).getUnsafe().getBoolean(NBT_OBU_GENERATED);
                                if (isPlacedGeneratedItem)
                                {
                                    placedGeneratedBlocks.add(placePos);
                                    OneBlockUltima.logDebug("[BreakDebug] Shift-right-click placed generated block at {} -> mark-only on future break", placePos);
                                }
                                else
                                {
                                    placedGeneratedBlocks.remove(placePos);
                                }

                                event.setCanceled(true);
                                event.setCancellationResult(InteractionResult.SUCCESS);
                                return;
                            }
                        }
                    }
                }
            }
            return;
        }

        // The generator menu opens on a right-click on the generator block itself (gen+0) or on
        // the generated block sitting on top of it (gen+1). The fluid barrier (gen+2) is fully
        // transparent, so clicks pass through it and hit the generated block / the generator.
        boolean isGeneratorClicked = generatorPos != null && generatorPos.equals(clickedPos);
        boolean isGeneratedBlockClicked = generatorPos != null
                && generatorPos.above().equals(clickedPos)
                && world.getBlockState(clickedPos).getBlock() != ModBlocks.FLUID_BARRIER
                && (registry == null || registry.isGenerated(clickedPos));
        if (!(isGeneratorClicked || isGeneratedBlockClicked))
        {
            if (generatorPos != null)
            {
                OneBlockUltima.logDebug("[GenClick] right-click at {} ignored (menu opens on the generator block at {} or the generated block above it)",
                        clickedPos, generatorPos);
            }
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (world.isClientSide())
        {
            return;
        }

        final BlockPos genPos = generatorPos;
        BlockEntity generatorTile = world.getBlockEntity(generatorPos);
        if (generatorTile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatorTile;
            OneBlockUltima.logDebug("[OwnerDebug] RightClick generator at {}, isFree={}, ownerId={}, player={}", generatorPos, generator.isFree(), generator.getOwnerId(), player.getName().getString());
            if (generator.isFree() && generator.canBeClaimedBy(player.getUUID()))
            {
                OneBlockUltima.logDebug("[OwnerDebug] Opening claim screen");
                ((ServerPlayer) player).openMenu(new MenuProvider() {
                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player p) {
                        return new ContainerClaimGenerator(id, inv, genPos);
                    }

                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Claim Generator");
                    }
                }, (buf) -> buf.writeBlockPos(genPos));
                return;
            }

            if (!ensureGeneratorAccess(world, generatorPos, player, generator))
            {
                OneBlockUltima.logDebugWarn("[OwnerDebug] ACCESS DENIED for player {} on generator at {}", player.getName().getString(), generatorPos);
                trySendAccessDeniedMessage(player, generatorPos, world.getGameTime());
                return;
            }
            OneBlockUltima.logDebug("[OwnerDebug] ACCESS GRANTED for player {} on generator at {}", player.getName().getString(), generatorPos);
        }

        ((ServerPlayer) player).openMenu(new MenuProvider() {
            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player p) {
                return new ContainerOneBlock(id, inv, genPos);
            }

            @Override
            public Component getDisplayName() {
                return Component.literal("One Block");
            }
        }, (buf) -> buf.writeBlockPos(genPos));
    }

    private static BlockPos getBlockPos(Vec3 hitVec, BlockPos generatorPos) {
        double relX = hitVec.x - generatorPos.getX();
        double relY = hitVec.y - generatorPos.getY();
        double relZ = hitVec.z - generatorPos.getZ();

        BlockPos placePos;
        if (relY >= 1.99) {
            placePos = generatorPos.above(2);
        } else if (relX <= 0.01) {
            placePos = generatorPos.above().west();
        } else if (relX >= 0.99) {
            placePos = generatorPos.above().east();
        } else if (relZ <= 0.01) {
            placePos = generatorPos.above().north();
        } else if (relZ >= 0.99) {
            placePos = generatorPos.above().south();
        } else {
            placePos = generatorPos.above();
        }
        return placePos;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if (event.getLevel().isClientSide())
        {
            return;
        }

        BlockPos pos = event.getPos();
        Level world = (Level) event.getLevel();

        if (world.getBlockState(pos).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            event.setCanceled(true);
            return;
        }

        if (!OneBlockWorldType.isOneBlockWorld(world))
        {
            return;
        }

        Player breaker = event.getPlayer();

        if (processingBlocks.getOrDefault(event.getPos(), false))
        {
            return;
        }

        BlockEntity generatorTile = world.getBlockEntity(pos);
        if (generatorTile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatorTile;
            if (!generator.hasAccess(event.getPlayer()))
            {
                event.setCanceled(true);
                trySendAccessDeniedMessage(event.getPlayer(), pos, world.getGameTime());
                return;
            }
        }

        if (world.getBlockState(pos).getBlock() == ModBlocks.FLUID_BARRIER && world.getBlockState(pos.below(2)).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            world.setBlock(pos, ModBlocks.FLUID_BARRIER.defaultBlockState(), 2);
            OneBlockUltima.logDebug("[Generator] BARRIER placed at {} after block break", pos);
            event.setCanceled(true);
            return;
        }

        GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
        GeneratedBlockRegistry.GeneratedBlockEntry entry = registry.getEntry(event.getPos());
        if (entry == null)
        {
            lastBreakPlayers.remove(pos);
            pendingBreakDrops.remove(pos);
            pendingMobSpawnEntries.remove(pos);
            if (placedGeneratedBlocks.remove(pos))
            {
                pendingMarkOnlyBreakDrops.put(pos, world.getGameTime());
                OneBlockUltima.logDebug("[BreakDebug] Breaking player-placed generated block at {} -> mark drops only", pos);
            }
            OneBlockUltima.logDebug("[BreakDebug] No generated entry found for broken block {}", event.getPos());
            return;
        }

        net.minecraft.world.level.block.Block actualBrokenBlock = world.getBlockState(pos).getBlock();
        String actualBlockRegistryId = BuiltInRegistries.BLOCK.getKey(actualBrokenBlock).toString();
        if (entry.blockRegistry != null && !entry.blockRegistry.equals(actualBlockRegistryId))
        {
            registry.remove(event.getPos());
            lastBreakPlayers.remove(pos);
            pendingBreakDrops.remove(pos);
            pendingMobSpawnEntries.remove(pos);
            if (placedGeneratedBlocks.remove(pos))
            {
                pendingMarkOnlyBreakDrops.put(pos, world.getGameTime());
                OneBlockUltima.logDebug("[BreakDebug] Broken block {} does not match generated entry {} (was {}), removing registry entry",
                        event.getPos(), actualBlockRegistryId, entry.blockRegistry);
            }
            return;
        }

        placedGeneratedBlocks.remove(pos);

        if (breaker != null)
        {
            lastBreakPlayers.put(pos, breaker.getUUID());
        }
        pendingBreakDrops.put(pos, world.getGameTime());

        if (breaker != null && entry.generatorPos != null)
        {
            BlockEntity genTile = world.getBlockEntity(entry.generatorPos);
            if (genTile instanceof TileEntityOneBlockGenerator)
            {
                TileEntityOneBlockGenerator gen = (TileEntityOneBlockGenerator) genTile;
                if (!gen.hasAccess(breaker))
                {
                    event.setCanceled(true);
                    trySendAccessDeniedMessage(breaker, entry.generatorPos, world.getGameTime());
                    return;
                }
            }
        }

        OneBlockUltima.logDebug("[BreakDebug] Generated entry found for {} -> generator {} set={} level={}", event.getPos(), entry.generatorPos, entry.setId, entry.level);

        pendingMobSpawnEntries.put(event.getPos(), entry);

        if (world.getBlockState(pos).getBlock() == ModBlocks.CASE_BLOCK)
        {
            OneBlockUltima.logDebug("[BreakDebug] Case block broken at {} setId={} level={}", pos, entry.setId, entry.level);
            if (breaker != null)
            {
                BlockSetConfig.BlockSetDefinition set = BlockSetConfig.get().getSet(entry.setId);
                if (set != null && set.hasCaseEntries())
                {
                    ItemStack caseStack = CaseUtil.createCaseItem(world, set);
                    if (!caseStack.isEmpty())
                    {
                        int nbContents = CaseUtil.readContents(caseStack, world.registryAccess()).size();
                        boolean added = breaker.getInventory().add(caseStack);
                        if (!added)
                        {
                            world.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(world,
                                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, caseStack));
                        }
                        else if (breaker instanceof net.minecraft.server.level.ServerPlayer)
                        {
                            breaker.getInventory().setChanged();
                            ((net.minecraft.server.level.ServerPlayer) breaker).inventoryMenu.broadcastChanges();
                        }
                        OneBlockUltima.logDebug("[BreakDebug] Case created set={} contents={} addedToInv={} (case item {})",
                                entry.setId, nbContents, added, added ? "placed in inventory" : "dropped as ItemEntity");
                    }
                    else
                    {
                        OneBlockUltima.logDebug("[BreakDebug] createCaseItem returned empty for set {}", entry.setId);
                    }
                }
                else
                {
                    OneBlockUltima.logDebug("[BreakDebug] Set {} has no case entries, case item not granted", entry.setId);
                }
            }
        }

        spawnMobOnBlockBreak(world, event.getPos(), entry);
        registry.remove(event.getPos());

        BlockEntity generatedTile = world.getBlockEntity(entry.generatorPos);
        if (generatedTile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatedTile;
            if (breaker == null)
            {
                OneBlockUltima.logDebug("[BreakDebug] Non-player break detected at {} for generator {}", event.getPos(), entry.generatorPos);
                if (!generator.canProcessNonPlayerBreak(world.getGameTime()))
                {
                    OneBlockUltima.logDebug("[BreakDebug] Non-player break blocked by cooldown for generator {} at tick {}", entry.generatorPos, world.getGameTime());
                    event.setCanceled(true);
                    return;
                }

                generator.markNonPlayerBreak(world.getGameTime());
                OneBlockUltima.logDebug("[BreakDebug] Non-player break accepted for generator {} at tick {}", entry.generatorPos, world.getGameTime());
            }
        }

        if (world.getBlockState(event.getPos()).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            processingBlocks.put(event.getPos(), true);
        }

        if (breaker == null && generatedTile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) generatedTile;
            OneBlockUltima.logDebug("[BreakDebug] Invoking non-player generation for generator {} at tick {}", entry.generatorPos, world.getGameTime());
            generator.tryGenerateBlock();
        }
        else if (breaker != null)
        {
            OneBlockUltima.logDebug("[BreakDebug] Player break detected at {} for generator {}", event.getPos(), entry.generatorPos);
        }

        if (breaker != null)
        {
            IOneBlockPlayerData data = OneBlockPlayerDataProvider.get(breaker);
            if (data != null)
            {
                if (!entry.generatorPos.equals(event.getPos()))
                {
                    if (BlockPriceConfig.get().getBalanceMode() == BlockPriceConfig.BalanceMode.BREAK_BLOCK)
                    {
                        double blockPrice = BlockPriceConfig.get().getPrice(entry.blockRegistry, entry.blockMeta);
                        if (blockPrice > 0) data.addCurrency(blockPrice);
                    }
                    data.addBrokenBlocks(entry.setId, 1);
                }
                OneBlockPlayerDataProvider.saveToEntity(breaker, data);
                PacketSyncPlayerData.sendToPlayer((ServerPlayer) breaker);
            }
        }
    }

    public static GeneratedBlockRegistry.GeneratedBlockEntry consumePendingMobSpawnEntry(BlockPos pos)
    {
        return pendingMobSpawnEntries.remove(pos);
    }

    public static UUID consumeBreakerId(BlockPos pos)
    {
        return lastBreakPlayers.remove(pos);
    }

    private static void markGenerated(ItemStack stack)
    {
        net.minecraft.world.item.component.CustomData existing = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putBoolean(NBT_OBU_GENERATED, true);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event)
    {
        if (event.getLevel().isClientSide())
        {
            return;
        }

        Level world = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        if (world.getBlockState(pos.below(2)).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR
                && world.getBlockState(pos).getBlock() == Blocks.AIR)
        {
            world.setBlock(pos, ModBlocks.FLUID_BARRIER.defaultBlockState(), 3);
            OneBlockUltima.logDebug("[Generator] BARRIER restored at {} from NeighborNotify", pos);
            return;
        }

        BlockState state = world.getBlockState(pos);

        if (world.getBlockState(event.getPos().below()).getBlock() == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            if (state.getBlock() == Blocks.AIR)
            {
                OneBlockUltima.logDebug("[Generator] NeighborNotify: generated slot {} became AIR", pos);
                dropUnsupportedAttachables(world, pos);

                GeneratedBlockRegistry registry = GeneratedBlockRegistry.get(world);
                GeneratedBlockRegistry.GeneratedBlockEntry entry = registry.getEntry(pos);
                if (entry != null && !pendingMobSpawnEntries.containsKey(pos))
                {
                    spawnMobOnBlockBreak(world, pos, entry);
                    registry.remove(pos);
                }
            }

            BlockOneBlockGenerator.ensureFluidBarrier(world, event.getPos().below());
            return;
        }

        if (state.getBlock() != Blocks.AIR)
        {
            processingBlocks.put(pos, false);
        }
    }

    private static void dropUnsupportedAttachables(Level world, BlockPos generatedPos)
    {
        BlockPos abovePos = generatedPos.above();
        BlockState aboveState = world.getBlockState(abovePos);
        Block aboveBlock = aboveState.getBlock();
        if (aboveBlock == Blocks.AIR || aboveBlock == ModBlocks.FLUID_BARRIER)
        {
            return;
        }
        if (!(aboveBlock instanceof net.minecraft.world.level.block.LeverBlock)
                && !(aboveBlock instanceof net.minecraft.world.level.block.ButtonBlock)
                && !(aboveBlock instanceof net.minecraft.world.level.block.TorchBlock))
        {
            return;
        }
        net.minecraft.world.level.block.Block.dropResources(aboveState, world, abovePos);
        world.setBlock(abovePos, Blocks.AIR.defaultBlockState(), 3);
        OneBlockUltima.logDebug("[Generator] Dropped attachable {} at {} (generated block at {} removed)", aboveBlock.getName().getString(), abovePos, generatedPos);
    }

    private static void spawnMobOnBlockBreak(Level world, BlockPos pos, GeneratedBlockRegistry.GeneratedBlockEntry entry)
    {
        BlockEntity tile = world.getBlockEntity(entry.generatorPos);
        if (tile instanceof TileEntityOneBlockGenerator)
        {
            TileEntityOneBlockGenerator generator = (TileEntityOneBlockGenerator) tile;
            if (generator.isDisableMobGeneration())
            {
                OneBlockUltima.logDebug("[Mob Spawn] Mob generation disabled on generator at {}", entry.generatorPos);
                return;
            }
        }

        OneBlockUltima.logDebug("[Mob Spawn] Entry: setId={}, level={}", entry.setId, entry.level);
        BlockSetConfig.BlockSetDefinition set = BlockSetConfig.get().getSet(entry.setId);
        if (set == null)
        {
            OneBlockUltima.logDebug("[Mob Spawn] Set is null for setId={}", entry.setId);
            return;
        }

        OneBlockUltima.logDebug("[Mob Spawn] Set found: {}", set.id);
        BlockSetConfig.SetLevelDefinition levelDefinition = set.getLevelClamped(entry.level);
        if (levelDefinition == null)
        {
            OneBlockUltima.logDebug("[Mob Spawn] Level definition is null for level={}", entry.level);
            return;
        }

        OneBlockUltima.logDebug("[Mob Spawn] Level definition found. Total mobs: {}", levelDefinition.mobs != null ? levelDefinition.mobs.size() : 0);
        BlockSetConfig.MobEntryDefinition mobEntry = levelDefinition.pickMob(world.getRandom());
        if (mobEntry == null || mobEntry.registry == null || mobEntry.registry.isEmpty())
        {
            OneBlockUltima.logDebug("[Mob Spawn] pickMob returned null or empty registry");
            return;
        }

        OneBlockUltima.logDebug("[Mob Spawn] Attempting to spawn mob: {} count={}", mobEntry.registry, mobEntry.count);
        for (int i = 0; i < Math.max(1, mobEntry.count); i++)
        {
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(BlockUtil.normalizeLegacyId(mobEntry.registry)));
            Entity entity = entityType.create(world);
            if (entity == null)
            {
                OneBlockUltima.logDebug("[Mob Spawn] Failed to create entity for registry: {}", mobEntry.registry);
                continue;
            }

            OneBlockUltima.logDebug("[Mob Spawn] Successfully spawned mob: {}", mobEntry.registry);
            entity.setPos(pos.getX() + BLOCK_CENTER_OFFSET, pos.getY() + BLOCK_TOP_OFFSET, pos.getZ() + BLOCK_CENTER_OFFSET);

            if (mobEntry.nbtTags != null && !mobEntry.nbtTags.isEmpty())
            {
                OneBlockUltima.logDebug("[Mob Spawn] Applying NBT tags to mob: {}", mobEntry.nbtTags);
                ru.defea.oneblockultima.util.BlockUtil.applyNbtToEntity(entity, mobEntry.nbtTags);
            }

            if (entity instanceof Slime)
            {
                Slime slime = (Slime) entity;
                int size = 1 + world.getRandom().nextInt(4);
                slime.setSize(size, true);
                OneBlockUltima.logDebug("[Mob Spawn] Randomized slime size to {} (health {})", size, slime.getMaxHealth());
            }

            world.addFreshEntity(entity);
        }
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
        {
            event.addCapability(
                    ResourceLocation.fromNamespaceAndPath(OneBlockUltima.MODID, "player_data"),
                    new OneBlockPlayerDataProvider()
            );
        }
    }

    @SubscribeEvent
    public static void clonePlayer(net.minecraftforge.event.entity.player.PlayerEvent.Clone event)
    {
        IOneBlockPlayerData oldData = OneBlockPlayerDataProvider.get(event.getOriginal());
        IOneBlockPlayerData newData = OneBlockPlayerDataProvider.get(event.getEntity());
        if (oldData instanceof ru.defea.oneblockultima.capability.OneBlockPlayerData
                && newData instanceof ru.defea.oneblockultima.capability.OneBlockPlayerData)
        {
            ru.defea.oneblockultima.capability.OneBlockPlayerData oldPlayerData =
                    (ru.defea.oneblockultima.capability.OneBlockPlayerData) oldData;
            ru.defea.oneblockultima.capability.OneBlockPlayerData newPlayerData =
                    (ru.defea.oneblockultima.capability.OneBlockPlayerData) newData;

            double before = newPlayerData.getCurrency();
            newPlayerData.copyFrom(oldPlayerData);
            OneBlockPlayerDataProvider.saveToEntity(event.getEntity(), newPlayerData);
            OneBlockUltima.getLogger().warn("[OBU-Balance] PlayerEvent.Clone fired: old={} newBefore={} newAfter={}",
                    oldPlayerData.getCurrency(), before, newPlayerData.getCurrency());
        }
        else
        {
            OneBlockUltima.getLogger().warn("[OBU-Balance] PlayerEvent.Clone fired but capability missing oldData={} newData={}",
                    oldData, newData);
        }
    }
}
