package ru.defea.oneblockultima;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.command.CommandOBUSell;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.defea.oneblockultima.Constants.NBT_OBU_GENERATED;

public class CommandOBUSellTest {

    @BeforeClass
    public static void setUp() {
        TestBootstrap.prepare();
    }

    private static ItemStack withGeneratedTag(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(NBT_OBU_GENERATED, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
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
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("someOtherTag", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        assertFalse(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedReturnsFalseWhenKeyExistsButFalse() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(NBT_OBU_GENERATED, false);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        assertFalse(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedReturnsTrueWhenTagIsTrue() {
        assertTrue(CommandOBUSell.isObuGenerated(withGeneratedTag(new ItemStack(Items.DIAMOND))));
    }

    @Test
    public void isObuGeneratedReturnsTrueForBlockItemWithTag() {
        assertTrue(CommandOBUSell.isObuGenerated(withGeneratedTag(new ItemStack(Blocks.STONE))));
    }

    @Test
    public void isObuGeneratedReturnsTrueForDirtWithTag() {
        assertTrue(CommandOBUSell.isObuGenerated(withGeneratedTag(new ItemStack(Blocks.DIRT))));
    }

    @Test
    public void isObuGeneratedReturnsFalseForEmptyNbtCompound() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
        assertFalse(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedReturnsTrueWithAdditionalTags() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean(NBT_OBU_GENERATED, true);
        nbt.putString("customName", "My Diamond");
        nbt.putInt("CustomModelData", 123);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        assertTrue(CommandOBUSell.isObuGenerated(stack));
    }

    @Test
    public void isObuGeneratedMultipleStacksSameNbt() {
        ItemStack stack1 = withGeneratedTag(new ItemStack(Items.DIAMOND, 5));
        ItemStack stack2 = withGeneratedTag(new ItemStack(Items.DIAMOND, 1));

        assertTrue(CommandOBUSell.isObuGenerated(stack1));
        assertTrue(CommandOBUSell.isObuGenerated(stack2));
    }

    @Test
    public void isObuGeneratedCopiedStackPreservesTag() {
        ItemStack original = withGeneratedTag(new ItemStack(Items.DIAMOND));
        ItemStack copy = original.copy();
        assertTrue(CommandOBUSell.isObuGenerated(copy));
    }

    @Test
    public void isObuGeneratedModifiedCopyDoesNotAffectOriginal() {
        ItemStack original = withGeneratedTag(new ItemStack(Items.DIAMOND));

        ItemStack copy = original.copy();
        CompoundTag copyNbt = copy.get(DataComponents.CUSTOM_DATA).getUnsafe().copy();
        copyNbt.putBoolean(NBT_OBU_GENERATED, false);
        copy.set(DataComponents.CUSTOM_DATA, CustomData.of(copyNbt));

        assertTrue(CommandOBUSell.isObuGenerated(original));
        assertFalse(CommandOBUSell.isObuGenerated(copy));
    }
}