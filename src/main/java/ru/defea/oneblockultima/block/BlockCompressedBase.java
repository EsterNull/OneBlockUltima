package ru.defea.oneblockultima.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import ru.defea.oneblockultima.OneBlockUltima;

import java.util.HashMap;
import java.util.Map;

public abstract class BlockCompressedBase extends Block {
    private static final Map<String, String> VANILLA_TEXTURES = new HashMap<>();

    static
    {
        VANILLA_TEXTURES.put("compressed_bedrock", "minecraft:bedrock");
        VANILLA_TEXTURES.put("compressed_redstone_block", "minecraft:redstone_block");
        VANILLA_TEXTURES.put("compressed_gold_block", "minecraft:gold_block");
        VANILLA_TEXTURES.put("compressed_iron_block", "minecraft:iron_block");
        VANILLA_TEXTURES.put("compressed_diamond_block", "minecraft:diamond_block");
        VANILLA_TEXTURES.put("compressed_stone_block", "minecraft:stone");
        VANILLA_TEXTURES.put("compressed_emerald_block", "minecraft:emerald_block");
        VANILLA_TEXTURES.put("compressed_lapis_block", "minecraft:lapis_block");
        VANILLA_TEXTURES.put("compressed_end_stone", "minecraft:end_stone");
        VANILLA_TEXTURES.put("compressed_netherrack", "minecraft:netherrack");
    }

    private static final Map<String, float[]> TINT_FACTORS = new HashMap<>();

    static
    {
        TINT_FACTORS.put("compressed_bedrock", new float[] { 0.7578F, 0.5432F, 0.3951F, 0.2931F });
        TINT_FACTORS.put("compressed_redstone_block", new float[] { 0.7244F, 0.4899F, 0.3297F, 0.2174F, 0.1421F, 0.0896F });
        TINT_FACTORS.put("compressed_gold_block", new float[] { 0.7290F, 0.5008F, 0.3430F, 0.2340F, 0.1075F });
        TINT_FACTORS.put("compressed_iron_block", new float[] { 0.7285F, 0.5013F, 0.3441F, 0.2351F, 0.1602F });
        TINT_FACTORS.put("compressed_diamond_block", new float[] { 0.7290F, 0.5005F, 0.3429F, 0.2340F, 0.1589F });
        TINT_FACTORS.put("compressed_stone_block", new float[] { 0.7290F, 0.5011F, 0.3433F, 0.2347F, 0.1584F, 0.0998F });
        TINT_FACTORS.put("compressed_emerald_block", new float[] { 0.7283F, 0.4994F, 0.3413F, 0.2325F, 0.1569F });
        TINT_FACTORS.put("compressed_lapis_block", new float[] { 0.7237F, 0.4907F, 0.3304F, 0.2196F, 0.1420F, 0.0890F });
        TINT_FACTORS.put("compressed_end_stone", new float[] { 0.7290F, 0.5011F, 0.3433F, 0.2347F, 0.1584F, 0.0998F });
        TINT_FACTORS.put("compressed_netherrack", new float[] { 0.7206F, 0.4862F, 0.3231F, 0.2113F, 0.1338F, 0.0843F });
    }

    private final String baseName;
    private final int maxLevel;

    protected BlockCompressedBase(Material material, String baseName, int maxLevel)
    {
        super(material);
        this.baseName = baseName;
        this.maxLevel = maxLevel;
        setCreativeTab(OneBlockUltima.modTab);
        this.setUnlocalizedName(baseName);
        String textureName = VANILLA_TEXTURES.get(baseName);
        if (textureName != null)
        {
            this.setTextureName(textureName);
        }
    }

    public int getMaxLevel()
    {
        return this.maxLevel;
    }

    public int getLevel(int meta)
    {
        return Math.max(0, Math.min(this.maxLevel - 1, meta));
    }

    @Override
    public int damageDropped(int meta)
    {
        return this.getLevel(meta);
    }

    public int getRenderColor(int meta)
    {
        float[] factors = TINT_FACTORS.get(this.baseName);
        int level = this.getLevel(meta);
        if (factors == null || level >= factors.length)
        {
            return 0xFFFFFFFF;
        }
        int t = Math.round(255.0F * factors[level]);
        return 0xFF000000 | (t << 16) | (t << 8) | t;
    }

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z)
    {
        return this.getRenderColor(world.getBlockMetadata(x, y, z));
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        if (world.getBlock(x, y - 1, z) == ModBlocks.ONE_BLOCK_GENERATOR)
        {
            return ModBlocks.ONE_BLOCK_GENERATOR.onBlockActivated(world, x, y - 1, z, player, side, hitX, hitY, hitZ);
        }
        return false;
    }
}
