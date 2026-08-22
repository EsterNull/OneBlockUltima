package ru.defea.oneblockultima.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemLiquidDeath extends CustomPotion {
    public ItemLiquidDeath() {
        super("liquid_death",
                new PotionEffect[]{
                        new PotionEffect(MobEffects.INSTANT_DAMAGE, 600 * 20, 123)
                });
    }

    @Override
    @Nonnull
    public ItemStack onItemUseFinish(@Nonnull ItemStack stack, @Nonnull World worldIn, @Nonnull EntityLivingBase entityLiving)
    {
        if (!worldIn.isRemote)
        {
            // Kill even in creative (OUT_OF_WORLD damage bypasses creative invulnerability)
            entityLiving.onKillCommand();
        }
        return super.onItemUseFinish(stack, worldIn, entityLiving);
    }
}