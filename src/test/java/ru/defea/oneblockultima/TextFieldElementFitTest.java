package ru.defea.oneblockultima;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.RowElement;
import ru.defea.oneblockultima.gui.layout.TextFieldElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TextFieldElementFitTest {

    private static final String LONG_VALUE = "minecraft:chests/village_blacksmith";

    static class StubFontRenderer extends FontRenderer {
        StubFontRenderer() {
            //noinspection DataFlowIssue
            super(null, null, null, false);
        }

        @Override
        public int getCharWidth(char character) {
            return 6;
        }

        @Override
        public int getStringWidth(String text) {
            return 6 * text.length();
        }
    }

    private static sun.misc.Unsafe unsafe() throws Exception {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        Bootstrap.register();
        Minecraft mc = (Minecraft) unsafe().allocateInstance(Minecraft.class);
        Field f = Minecraft.class.getDeclaredField("theMinecraft");
        f.setAccessible(true);
        f.set(null, mc);
    }

    private static FontRenderer stubFontRenderer() throws Exception {
        return (FontRenderer) unsafe().allocateInstance(StubFontRenderer.class);
    }

    private static GuiTextField buildField(@SuppressWarnings("SameParameterValue") int baseWidth, String text) throws Exception {
        ViewFactory factory = new ViewFactory(1000, 800).margin(2).padding(4).gap(6);
        factory.fitContent();
        RowElement row = factory.row(Alignment.LEFT).gap(6);
        TextFieldElement field = new TextFieldElement(baseWidth).text(text).fitToText();
        row.add(field);
        List<GuiButton> buttons = new ArrayList<>();
        factory.build(buttons, stubFontRenderer());
        assertEquals(1, factory.getTextFields().size());
        return factory.getTextFields().get(0);
    }

    @Test
    public void fitToTextFieldKeepsFullValueAndGrowsToFitLongText() throws Exception {
        GuiTextField tf = buildField(200, LONG_VALUE);
        assertEquals(LONG_VALUE, tf.getText());
        int needed = 6 * LONG_VALUE.length() + 8;
        assertTrue("widget width " + tf.width + " must be >= " + needed, tf.width >= needed);
    }

    @Test
    public void fitToTextDoesNotShrinkConfiguredWidthForShortText() throws Exception {
        GuiTextField tf = buildField(200, "short");
        assertEquals(200, tf.width);
    }

    @Test
    public void fitToTextAppliesToValueFieldUsedByNbtEditor() throws Exception {
        ViewFactory factory = new ViewFactory(640, 480).margin(2).padding(4).gap(6);
        factory.fitContent();
        RowElement row = factory.row(Alignment.LEFT).gap(6);
        TextFieldElement field = new TextFieldElement(280).text(LONG_VALUE).fitToText();
        row.add(field);
        List<GuiButton> buttons = new ArrayList<>();
        factory.build(buttons, stubFontRenderer());

        GuiTextField tf = factory.getTextFields().get(0);
        int needed = 6 * LONG_VALUE.length() + 8;
        assertTrue("widget width " + tf.width + " must be >= " + needed, tf.width >= needed);
    }
}
