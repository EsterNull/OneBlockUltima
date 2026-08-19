package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.config.BlockSetConfig;
import ru.defea.oneblockultima.util.MobIdUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Guards the shipped default blocksets.json against mob ids that cannot exist in
 * 1.7.10 (rabbit, endermite, shulker, wither_skeleton were removed during the port).
 */
public class BlockSetConfigMobResolutionTest
{
    @BeforeClass
    public static void loadDefaultConfig() throws Exception
    {
        Bootstrap.register();
        Path tempDir = Files.createTempDirectory("oneblockultima-config-test");
        BlockSetConfig.reset();
        BlockSetConfig.load(tempDir.toFile());
    }

    @Test
    public void everyMinecraftMobInDefaultConfigResolvesIn17()
    {
        List<String> broken = new ArrayList<>();
        for (BlockSetConfig.BlockSetDefinition set : BlockSetConfig.get().getSets())
        {
            for (BlockSetConfig.MobElementDefinition mob : set.mobs)
            {
                if (mob == null || mob.registry == null || !mob.registry.startsWith("minecraft:"))
                {
                    continue;
                }
                if (MobIdUtil.resolveKey(mob.registry) == null)
                {
                    broken.add(mob.registry + " (set " + set.id + ")");
                }
            }
        }
        assertTrue("unresolvable vanilla mobs in 1.7.10: " + broken, broken.isEmpty());
    }
}
