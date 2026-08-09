package ru.defea.oneblockultima.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants;
import ru.defea.oneblockultima.OneBlockUltima;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class CustomPotion extends ItemPotion {
    private final PotionEffect[] potionEffects;

    public CustomPotion(String name, PotionEffect[] potionEffects) {
        setCreativeTab(OneBlockUltima.modTab);
        this.setRegistryName(name);
        this.setUnlocalizedName(name);
        this.potionEffects = potionEffects;
    }

    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable NBTTagCompound nbt) {
        // Add effects when the item is created
        addPotionEffectsToStack(stack);
        return super.initCapabilities(stack, nbt);
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            ItemStack stack = new ItemStack(this);
            items.add(stack);
        }
    }

    @Override
    @Nonnull
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        return net.minecraft.util.text.translation.I18n.translateToLocal(this.getUnlocalizedNameInefficiently(stack) + ".name").trim();
    }

    @Override
    @Nonnull
    public ItemStack onItemUseFinish(@Nonnull ItemStack stack, @Nonnull World worldIn, @Nonnull EntityLivingBase entityLiving) {
        ItemStack resultStack = super.onItemUseFinish(stack, worldIn, entityLiving);

        if (!worldIn.isRemote) {
            for (PotionEffect potionEffect : potionEffects)
            {
                entityLiving.addPotionEffect(potionEffect);
            }
            entityLiving.addPotionEffect(new PotionEffect(MobEffects.POISON, 30 * 20, 2));
        }

        return resultStack;
    }

    public void addPotionEffectsToStack(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
        }

        NBTTagList effectsList = nbt.getTagList("CustomPotionEffects", Constants.NBT.TAG_COMPOUND);

        // Save the effect in NBT
        for (PotionEffect potionEffect : potionEffects)
        {
            NBTTagCompound effectTag = new NBTTagCompound();
            potionEffect.writeCustomPotionEffectToNBT(effectTag);
            effectsList.appendTag(effectTag);
        }

        nbt.setTag("CustomPotionEffects", effectsList);
        stack.setTagCompound(nbt);
    }
}
