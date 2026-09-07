package ru.defea.oneblockultima;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.RowElement;
import ru.defea.oneblockultima.gui.layout.TextFieldElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import static org.junit.Assert.*;

public class TextFieldElementFitTest {

    private static final String LONG_VALUE = "minecraft:chests/village_blacksmith";

    @BeforeClass
    public static void setUp() {
        TestBootstrap.installFakeMinecraft();
    }

    private static Screen screen() {
        return new Screen(Component.literal("")) {
            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            }
        };
    }

    private static EditBox buildField(@SuppressWarnings("SameParameterValue") int baseWidth, String text) {
        ViewFactory factory = new ViewFactory(1000, 800).margin(2).padding(4).gap(6);
        factory.fitContent();
        RowElement row = factory.row(Alignment.LEFT).gap(6);
        TextFieldElement field = new TextFieldElement(baseWidth).text(text).fitToText();
        row.add(field);
        factory.build(screen(), TestBootstrap.getStubFont());
        EditBox tf = field.getTextField();
        assertNotNull(tf);
        return tf;
    }

    @Test
    public void fitToTextFieldKeepsFullValueAndGrowsToFitLongText() {
        EditBox tf = buildField(200, LONG_VALUE);
        assertEquals(LONG_VALUE, tf.getValue());
        int needed = 6 * LONG_VALUE.length() + 8;
        assertTrue("widget width " + tf.getWidth() + " must be >= " + needed, tf.getWidth() >= needed);
    }

    @Test
    public void fitToTextDoesNotShrinkConfiguredWidthForShortText() {
        EditBox tf = buildField(200, "short");
        assertEquals(200, tf.getWidth());
    }

    @Test
    public void fitToTextAppliesToValueFieldUsedByNbtEditor() {
        ViewFactory factory = new ViewFactory(640, 480).margin(2).padding(4).gap(6);
        factory.fitContent();
        RowElement row = factory.row(Alignment.LEFT).gap(6);
        TextFieldElement field = new TextFieldElement(280).text(LONG_VALUE).fitToText();
        row.add(field);
        factory.build(screen(), TestBootstrap.getStubFont());

        EditBox tf = field.getTextField();
        assertNotNull(tf);
        int needed = 6 * LONG_VALUE.length() + 8;
        assertTrue("widget width " + tf.getWidth() + " must be >= " + needed, tf.getWidth() >= needed);
    }
}