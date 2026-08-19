package ru.defea.oneblockultima.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import ru.defea.oneblockultima.OneBlockUltima;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class CustomPotion extends ItemPotion {
    private final PotionEffect[] potionEffects;
    @SideOnly(Side.CLIENT)
    private IIcon potionIcon;

    public CustomPotion(String name, PotionEffect[] potionEffects) {
        setCreativeTab(OneBlockUltima.modTab);

        this.setUnlocalizedName(name);
        this.potionEffects = potionEffects;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void getSubItems(Item item, CreativeTabs tab, List items) {
        if (tab == CreativeTabs.tabAllSearch || this.getCreativeTab() == tab) {
            ItemStack stack = new ItemStack(this);
            addPotionEffectsToStack(stack);
            items.add(stack);
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return StatCollector.translateToLocal(this.getUnlocalizedName() + ".name").trim();
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityPlayer entityLiving) {
        ItemStack resultStack = super.onItemUseFinish(stack, worldIn, entityLiving);

        if (!worldIn.isRemote) {
            for (PotionEffect potionEffect : potionEffects)
            {
                entityLiving.addPotionEffect(potionEffect);
            }
            entityLiving.addPotionEffect(new PotionEffect(Potion.poison.getId(), 30 * 20, 2));
            ru.defea.oneblockultima.achievement.ModAchievements.unlockByConsume(entityLiving, this);
        }

        return resultStack;
    }

    public void addPotionEffectsToStack(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
        }

        NBTTagList effectsList = nbt.getTagList("CustomPotionEffects", Constants.NBT.TAG_COMPOUND);

        for (PotionEffect potionEffect : potionEffects)
        {
            NBTTagCompound effectTag = new NBTTagCompound();
            potionEffect.writeCustomPotionEffectToNBT(effectTag);
            effectsList.appendTag(effectTag);
        }

        nbt.setTag("CustomPotionEffects", effectsList);
        stack.setTagCompound(nbt);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        this.potionIcon = register.registerIcon(this.getIconString());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return this.potionIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return this.potionIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return 16777215;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        Map<Integer, PotionEffect> effects = new LinkedHashMap<Integer, PotionEffect>();
        if (potionEffects != null) {
            for (PotionEffect effect : potionEffects) {
                if (effect != null) {
                    effects.put(effect.getPotionID(), effect);
                }
            }
        }

        List nbtEffects = getEffects(stack);
        if (nbtEffects != null) {
            for (Object obj : nbtEffects) {
                if (obj instanceof PotionEffect) {
                    PotionEffect effect = (PotionEffect) obj;
                    if (!effects.containsKey(effect.getPotionID())) {
                        effects.put(effect.getPotionID(), effect);
                    }
                }
            }
        }

        if (effects.isEmpty()) {
            lines.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("potion.empty").trim());
            return;
        }

        for (PotionEffect effect : effects.values()) {
            Potion potion = effect.getPotionID() >= 0 && effect.getPotionID() < Potion.potionTypes.length
                    ? Potion.potionTypes[effect.getPotionID()] : null;
            String s = StatCollector.translateToLocal(effect.getEffectName()).trim();

            if (effect.getAmplifier() > 0) {
                s = s + " " + StatCollector.translateToLocal("potion.potency." + effect.getAmplifier()).trim();
            }

            if (effect.getDuration() > 20) {
                s = s + " (" + Potion.getDurationString(effect) + ")";
            }

            if (potion != null && potion.isBadEffect()) {
                lines.add(EnumChatFormatting.RED + s);
            } else {
                lines.add(EnumChatFormatting.GRAY + s);
            }
        }
    }
}
