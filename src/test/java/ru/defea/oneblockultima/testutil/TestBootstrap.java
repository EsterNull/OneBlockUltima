package ru.defea.oneblockultima.testutil;

import cpw.mods.modlauncher.api.IModuleLayerManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.DataVersion;
import net.minecraftforge.fml.loading.FMLPaths;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

/**
 * Minimal-environment bootstrap for headless JUnit runs.
 *
 * <p>Forge 1.21.1 has no equivalent of 1.12's {@code net.minecraft.init.Bootstrap.register()}.
 * A plain {@link Bootstrap#bootStrap()} dies during item registration because
 * {@code net.minecraft.world.item.Item.components()} routes through
 * {@code net.minecraftforge.event.ForgeEventFactory}'s static init, which constructs
 * {@code net.minecraftforge.fml.ModLoader}, whose constructor requires FML's
 * {@code LoadingModList}/{@code ModuleLayerManager} (null outside an FML launch).</p>
 *
 * <p>{@code prepare()} neutralises that chain before bootstrap by priming FML paths and
 * injecting Mockito stand-ins for the three static FML handles, so bootstrap completes and
 * {@code ItemStack}/{@code Items}, vanilla registries and NBT are fully usable.</p>
 *
 * <p>After a completed bootstrap the Forge block registry is frozen, so constructing mod blocks
 * (any {@code Block} subclass) throws. Tests that need real block instances must call
 * {@link #unfreezeBlockRegistry()} first. Client rendering (GuiGraphics/GLFW) and FML mod
 * registration ({@code DeferredRegister.get()}) remain unavailable.</p>
 */
public final class TestBootstrap {

    private static volatile boolean prepared;

    private TestBootstrap() {
    }

    private static final WorldVersion FAKE_VERSION = new WorldVersion() {
        @Override
        public DataVersion getDataVersion() {
            return new DataVersion(3955);
        }

        @Override
        public String getId() {
            return "test";
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public int getProtocolVersion() {
            return 769;
        }

        @Override
        public int getPackVersion(PackType type) {
            return 34;
        }

        @Override
        public Date getBuildTime() {
            return new Date();
        }

        @Override
        public boolean isStable() {
            return true;
        }
    };

    public static void prepare() {
        if (prepared) {
            return;
        }
        try {
            SharedConstants.setVersion(FAKE_VERSION);
        } catch (Throwable ignored) {
            // already set
        }
        try {
            prepareFmlPaths();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to prepare FML paths for tests", e);
        }
        patchFmlHandles();
        try {
            Bootstrap.bootStrap();
        } catch (Throwable t) {
            // Should not happen once FML handles are patched, but keep it from failing the suite.
            System.out.println("TestBootstrap: bootStrap finished with: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        prepared = true;
    }

    private static void patchFmlHandles() {
        try {
            Class<?> loadingModListClass = Class.forName("net.minecraftforge.fml.loading.LoadingModList");
            Object loadingModListMock = Mockito.mock(loadingModListClass);

            Field target = null;
            for (Field f : loadingModListClass.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == loadingModListClass) {
                    target = f;
                    break;
                }
            }
            if (target != null) {
                target.setAccessible(true);
                target.set(null, loadingModListMock);
            }

            Class<?> fmlLoaderClass = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
            for (Field f : fmlLoaderClass.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == loadingModListClass) {
                    f.setAccessible(true);
                    f.set(null, loadingModListMock);
                }
            }

            IModuleLayerManager layerManagerMock = Mockito.mock(IModuleLayerManager.class);
            Mockito.when(layerManagerMock.getLayer(Mockito.any())).thenReturn(Optional.<ModuleLayer>of(ModuleLayer.boot()));
            for (Field f : fmlLoaderClass.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && IModuleLayerManager.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(null, layerManagerMock);
                }
            }
        } catch (Throwable t) {
            System.out.println("TestBootstrap: FML handle patch failed: " + t);
        }
    }

    /**
     * The Forge block registry is frozen after a completed {@link Bootstrap#bootStrap()}, which
     * makes every {@code new Block(...)} throw. Tests constructing mod block instances must call
     * this first.
     */
    public static void unfreezeBlockRegistry() {
        unfreezeRegistry(BuiltInRegistries.BLOCK);
    }

    /**
     * Unfreezes an arbitrary vanilla registry so test code can create and register
     * mod-owned values (blocks, block entity types, ...).
     */
    public static void unfreezeRegistry(net.minecraft.core.Registry<?> registry) {
        try {
            Class<?> regClass = registry.getClass();
            Method unfreeze = null;
            Class<?> walk = regClass;
            while (walk != null && unfreeze == null) {
                try {
                    unfreeze = walk.getDeclaredMethod("unfreeze");
                } catch (NoSuchMethodException ignored) {
                    walk = walk.getSuperclass();
                }
            }
            if (unfreeze != null) {
                unfreeze.setAccessible(true);
                unfreeze.invoke(registry);
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Unable to unfreeze registry " + registry + " for tests", t);
        }
    }

    private static void prepareFmlPaths() throws IOException {
        Path tempRoot = Files.createTempDirectory("oneblockultima_test");
        Files.createDirectories(tempRoot.resolve("mods"));
        Files.createDirectories(tempRoot.resolve("config"));
        FMLPaths.loadAbsolutePaths(tempRoot);
    }

    private static volatile boolean tileEntitiesRegistered;

    /**
     * Registers the mod's one-block-generator block and its {@link net.minecraft.world.level.block.entity.BlockEntityType}
     * into the (unfrozen) vanilla registries so {@code new TileEntityOneBlockGenerator(...)} works headless.
     * Idempotent across test classes sharing one JVM.
     */
    public static void registerOneBlockTileEntity() {
        prepare();
        if (tileEntitiesRegistered) {
            return;
        }
        try {
            unfreezeBlockRegistry();
            unfreezeRegistry(BuiltInRegistries.BLOCK_ENTITY_TYPE);
            if (ru.defea.oneblockultima.block.ModBlocks.ONE_BLOCK_GENERATOR == null) {
                ru.defea.oneblockultima.block.ModBlocks.ONE_BLOCK_GENERATOR =
                        new ru.defea.oneblockultima.block.BlockOneBlockGenerator();
            }
            net.minecraftforge.registries.IForgeRegistry<net.minecraft.world.level.block.entity.BlockEntityType<?>> reg =
                    net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES;
            ((net.minecraftforge.registries.ForgeRegistry) reg).unfreeze();
            final net.minecraft.world.level.block.entity.BlockEntityType<ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator> bet =
                    net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                            ru.defea.oneblockultima.tile.TileEntityOneBlockGenerator::new,
                            ru.defea.oneblockultima.block.ModBlocks.ONE_BLOCK_GENERATOR).build(null);
            reg.register(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("oneblockultima", "one_block_generator"),
                    bet);
            if (ru.defea.oneblockultima.tile.ModTileEntities.ONE_BLOCK_GENERATOR.get() == bet) {
                tileEntitiesRegistered = true;
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Unable to register the one-block tile entity for tests", t);
        }
    }

    /** Set once by {@link #installFakeMinecraft()}. */
    public static boolean fakeMinecraftInstalled;

    private static StubFont installedStubFont;

    public static StubFont getStubFont() {
        return installedStubFont;
    }

    /**
     * Installs a fake client ({@code Minecraft.getInstance()}) with a deterministic stub font
     * ({@link #width(String)} == 6 * length, {@code lineHeight} == 9) so layout classes that fall
     * back to the live client font work headless. Idempotent.
     */
    public static synchronized void installFakeMinecraft() {
        prepare();
        if (fakeMinecraftInstalled) {
            return;
        }
        try {
            StubFont stubFont = (StubFont) unsafe().allocateInstance(StubFont.class);
            Field lineHeight = net.minecraft.client.gui.Font.class.getDeclaredField("lineHeight");
            lineHeight.setAccessible(true);
            lineHeight.setInt(stubFont, 9);
            Field splitterField = net.minecraft.client.gui.Font.class.getDeclaredField("splitter");
            splitterField.setAccessible(true);
            splitterField.set(stubFont, new net.minecraft.client.StringSplitter(
                    (codePoint, style) -> 6.0f));
            installedStubFont = stubFont;

            net.minecraft.client.Minecraft mc =
                    (net.minecraft.client.Minecraft) unsafe().allocateInstance(net.minecraft.client.Minecraft.class);
            Field fontField = net.minecraft.client.Minecraft.class.getDeclaredField("font");
            fontField.setAccessible(true);
            fontField.set(mc, stubFont);

            Field instanceField = net.minecraft.client.Minecraft.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, mc);
            fakeMinecraftInstalled = true;
        } catch (Throwable t) {
            throw new IllegalStateException("Unable to install the fake Minecraft client for tests", t);
        }
    }

    @SuppressWarnings("unchecked")
    private static sun.misc.Unsafe unsafe() throws Exception {
        java.lang.reflect.Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (sun.misc.Unsafe) theUnsafe.get(null);
    }

    /** Deterministic {@link net.minecraft.client.gui.Font} for headless GUI tests. */
    public static class StubFont extends net.minecraft.client.gui.Font {
        StubFont() {
            super(resourceLocation -> null, false);
        }

        @Override
        public int width(String text) {
            return text == null ? 0 : text.length() * 6;
        }

        @Override
        public int width(net.minecraft.network.chat.FormattedText text) {
            return text instanceof net.minecraft.network.chat.Component component
                    ? width(component.getString()) : 0;
        }
    }
}