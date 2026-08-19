package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.world.gen.FlatGeneratorInfo;
import net.minecraft.world.gen.FlatLayerInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.world.OneBlockWorldType;

import java.util.List;

import static org.junit.Assert.*;

public class OneBlockWorldTypeTest {

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
    }

    @Test
    public void flatTemplateIsValidVoidWorld() {
        FlatGeneratorInfo info = FlatGeneratorInfo.createFlatGeneratorFromString(
                OneBlockWorldType.ONE_BLOCK.getFlatGeneratorOptions()
        );

        assertNotNull(info);
        List<?> layers = info.getFlatLayers();
        assertEquals(1, layers.size());

        FlatLayerInfo layer = (FlatLayerInfo) layers.get(0);
        assertEquals(Blocks.air, layer.func_151536_b());
        assertEquals(1, layer.getLayerCount());
    }

    @Test
    public void flatTemplateParsesWithoutDefaultFallback() {
        // 1.12.2-style preset "3;minecraft:air;127" has version 3, which is > 2 in 1.7.10,
        // so FlatGeneratorInfo silently returns the DEFAULT flat generator instead.
        FlatGeneratorInfo oldPreset = FlatGeneratorInfo.createFlatGeneratorFromString("3;minecraft:air;127");
        List<?> oldLayers = oldPreset.getFlatLayers();
        assertTrue("default fallback must not be a single air layer", oldLayers.size() != 1
                || ((FlatLayerInfo) oldLayers.get(0)).func_151536_b() != Blocks.air);
    }

    @Test
    public void flatTemplateBiomeIsValid() {
        FlatGeneratorInfo info = FlatGeneratorInfo.createFlatGeneratorFromString(
                OneBlockWorldType.ONE_BLOCK.getFlatGeneratorOptions()
        );
        assertEquals(1, info.getBiome());
    }

    @Test
    public void worldTypeIsNotCustomizable() {
        assertFalse(OneBlockWorldType.ONE_BLOCK.isCustomizable());
    }
}
