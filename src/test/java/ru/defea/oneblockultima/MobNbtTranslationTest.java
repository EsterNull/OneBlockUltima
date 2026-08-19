package ru.defea.oneblockultima;

import net.minecraft.entity.Entity;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.util.BlockUtil;
import ru.defea.oneblockultima.util.MobIdUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Guards the 1.12.2 -> 1.7.10 NBT translation applied to mob entities by
 * {@link BlockUtil#applyNbtToEntity}. The shared config stores equipment as
 * 1.12.2 "HandItems"/"ArmorItems" lists with namespaced string item ids, while
 * 1.7.10 {@code EntityLiving} restores equipment from a 5-slot "Equipment" list
 * with numeric ids.
 */
public class MobNbtTranslationTest
{
    @BeforeClass
    public static void bootstrap() throws Exception
    {
        Bootstrap.register();
    }

    private static Entity createEntity(String registry)
    {
        World world = TestDummyWorld.newWorld(false);
        return MobIdUtil.createEntity(registry, world);
    }

    @Test
    public void handItemsBowBecomesEquipmentHeldBow()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList handItems = new NBTTagList();
        NBTTagCompound bow = new NBTTagCompound();
        bow.setString("id", "minecraft:bow");
        bow.setByte("Count", (byte) 1);
        handItems.appendTag(bow);
        nbt.setTag("HandItems", handItems);

        Entity entity = createEntity("minecraft:skeleton");
        assertNotNull(entity);

        BlockUtil.applyNbtToEntity(entity, nbt);

        ItemStack held = ((net.minecraft.entity.EntityLivingBase) entity).getHeldItem();
        assertNotNull("skeleton should hold the bow from HandItems", held);
        assertEquals("bow item id should be translated to a numeric id", Items.bow, held.getItem());
    }

    @Test
    public void handItemsWithOffhandIgnoresOffhand()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList handItems = new NBTTagList();
        NBTTagCompound sword = new NBTTagCompound();
        sword.setString("id", "minecraft:golden_sword");
        sword.setByte("Count", (byte) 1);
        handItems.appendTag(sword);
        NBTTagCompound emptyOffhand = new NBTTagCompound();
        handItems.appendTag(emptyOffhand);
        nbt.setTag("HandItems", handItems);

        Entity entity = createEntity("minecraft:zombie_pigman");
        assertNotNull(entity);

        BlockUtil.applyNbtToEntity(entity, nbt);

        ItemStack held = ((net.minecraft.entity.EntityLivingBase) entity).getHeldItem();
        assertNotNull(held);
        assertEquals(Items.golden_sword, held.getItem());
    }

    @Test
    public void ageTagAppliesToBabyAgeableModel()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("Age", -100);

        Entity entity = createEntity("minecraft:sheep");
        assertNotNull(entity);

        BlockUtil.applyNbtToEntity(entity, nbt);

        assertTrue("baby ageable (Age < 0) should render as child",
                entity instanceof net.minecraft.entity.EntityAgeable
                        && ((net.minecraft.entity.EntityAgeable) entity).isChild());
    }

    @Test
    public void applyNbtDoesNotMutateSourceTags()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList handItems = new NBTTagList();
        NBTTagCompound bow = new NBTTagCompound();
        bow.setString("id", "minecraft:bow");
        handItems.appendTag(bow);
        nbt.setTag("HandItems", handItems);

        Entity entity = createEntity("minecraft:skeleton");
        assertNotNull(entity);
        BlockUtil.applyNbtToEntity(entity, nbt);

        assertTrue("source HandItems must be left untouched", nbt.hasKey("HandItems"));
        assertFalse("source must not be rewritten to Equipment", nbt.hasKey("Equipment"));
    }

    @Test
    public void numericIdItemStacksStayIntact()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList handItems = new NBTTagList();
        NBTTagCompound stick = new NBTTagCompound();
        stick.setShort("id", (short) 280);
        stick.setByte("Count", (byte) 1);
        handItems.appendTag(stick);
        nbt.setTag("HandItems", handItems);

        Entity entity = createEntity("minecraft:zombie");
        assertNotNull(entity);
        BlockUtil.applyNbtToEntity(entity, nbt);

        ItemStack held = ((net.minecraft.entity.EntityLivingBase) entity).getHeldItem();
        assertNotNull(held);
        assertEquals(Items.stick, held.getItem());
    }
}
