package net.minecraft.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import ru.defea.oneblockultima.TestFMLHooks;
import ru.defea.oneblockultima.block.ModBlocks;

/**
 * Test-only shadow of the vanilla {@code net.minecraft.init.Bootstrap}.
 *
 * <p>In a real game this class is invoked by the launch wrapper after FML has
 * been set up. Inside a plain JUnit worker there is no launch wrapper and the
 * genuine {@code Block.registerBlocks()}/{@code Item.registerItems()} calls
 * would blow up on {@code Loader.instance()} (its constructor requires a
 * {@code LaunchClassLoader}). This shadow installs a stub {@code Loader} first
 * and then registers the vanilla blocks and items exactly like the original.
 * The test sources directory precedes the minecraft jar on the test runtime
 * classpath, so this class is what {@code Bootstrap.register()} resolves to.
 * The {@code BlockFire}/{@code StatList}/dispenser parts are not needed by any
 * test and are omitted.</p>
 */
public class Bootstrap
{
    private static boolean alreadyRegistered;

    public static void register()
    {
        if (alreadyRegistered)
        {
            return;
        }
        alreadyRegistered = true;
        TestFMLHooks.installLoader();
        Block.registerBlocks();
        Item.registerItems();
        ModBlocks.registerBlocksAndItems();
    }

    static void registerDispenserBehaviors()
    {
    }
}
