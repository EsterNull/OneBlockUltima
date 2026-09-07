package ru.defea.oneblockultima.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class ItemLiquidDeath extends CustomPotion {
    public ItemLiquidDeath() {
        super("liquid_death",
                new MobEffectInstance[]{
                        new MobEffectInstance(MobEffects.HARM, 600 * 20, 123)
                });
    }

    @Nonnull
    @Override
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level worldIn, @Nonnull LivingEntity entityLiving) {
        if (!worldIn.isClientSide) {
            // Kill even in creative (out_of_world damage bypasses creative invulnerability)
            entityLiving.kill();
        }
        return super.finishUsingItem(stack, worldIn, entityLiving);
    }
}
