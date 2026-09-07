package ru.defea.oneblockultima.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public abstract class CustomPotion extends Item {
    private final MobEffectInstance[] potionEffects;

    public CustomPotion(String name, MobEffectInstance[] potionEffects) {
        super(new Item.Properties().stacksTo(1));
        this.potionEffects = potionEffects;
    }

    @Nonnull
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level worldIn, @Nonnull Player playerIn, @Nonnull InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        playerIn.startUsingItem(handIn);
        return InteractionResultHolder.consume(stack);
    }

    @Nonnull
    @Override
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level worldIn, @Nonnull LivingEntity entityLiving) {
        if (!worldIn.isClientSide) {
            for (MobEffectInstance potionEffect : potionEffects) {
                entityLiving.addEffect(new MobEffectInstance(potionEffect.getEffect(), potionEffect.getDuration(), potionEffect.getAmplifier()));
            }
            entityLiving.addEffect(new MobEffectInstance(MobEffects.POISON, 30 * 20, 2));
        }
        return new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
    }

    @Nonnull
    @Override
    public net.minecraft.sounds.SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }
}
