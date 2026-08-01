package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.command.CommandOBUSell;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.defea.oneblockultima.Constants.NBT_OBU_GENERATED;

public class CommandOBUSellTest {

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
    }

    @Test
    public void isObuGeneratedReturnsFalseForEmpty() {
        assertFalse(CommandOBUSell.isObuGenerated(ItemStack.EMPTY));
    }

    @Test
    public void isObuGeneratedReturnsFalseForItemWithNoNbt() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        assertFalse(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedReturnsFalseForItemWithUnrelatedNbt() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean("someOtherTag", true);
        assertFalse(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedReturnsFalseWhenKeyExistsButFalse() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean(NBT_OBU_GENERATED, false);
        assertFalse(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedReturnsTrueWhenTagIsTrue() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);
        assertTrue(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedReturnsTrueForBlockItemWithTag() {
        net.minecraft.item.ItemStack stack = new ItemStack(net.minecraft.init.Blocks.STONE);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);
        assertTrue(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedReturnsTrueForDirtWithTag() {
        net.minecraft.item.ItemStack stack = new ItemStack(net.minecraft.init.Blocks.DIRT);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);
        assertTrue(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedReturnsFalseForEmptyNbtCompound() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.setTagCompound(new NBTTagCompound());
        assertFalse(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedReturnsTrueWithAdditionalTags() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean(NBT_OBU_GENERATED, true);
        nbt.setString("customName", "My Diamond");
        nbt.setInteger("CustomModelData", 123);
        stack.setTagCompound(nbt);
        assertTrue(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedMultipleStacksSameNbt() {
        ItemStack stack1 = new ItemStack(Items.DIAMOND, 5);
        stack1.setTagCompound(new NBTTagCompound());
        stack1.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);

        ItemStack stack2 = new ItemStack(Items.DIAMOND, 1);
        stack2.setTagCompound(new NBTTagCompound());
        stack2.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);

        assertTrue(CommandOBUSell.isObuGenerated(stack1));
        assertTrue(CommandOBUSell.isObuGenerated(stack2));
    }

    @Test
    public void isObuGeneratedCopiedStackPreservesTag() {
        ItemStack original = new ItemStack(Items.DIAMOND);
        original.setTagCompound(new NBTTagCompound());
        original.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);

        ItemStack copy = original.copy();
        assertTrue(CommandOBUSell.isObuGenerated(copy));
    }

    @Test
    public void isObuGeneratedModifiedCopyDoesNotAffectOriginal() {
        ItemStack original = new ItemStack(Items.DIAMOND);
        original.setTagCompound(new NBTTagCompound());
        original.getTagCompound().setBoolean(NBT_OBU_GENERATED, true);

        ItemStack copy = original.copy();
        copy.getTagCompound().setBoolean(NBT_OBU_GENERATED, false);

        assertTrue(CommandOBUSell.isObuGenerated(original));
        assertFalse(CommandOBUSell.isObuGenerated(copy));
    }
}
