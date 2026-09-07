package ru.defea.oneblockultima;

import net.minecraft.client.gui.Font;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.ButtonElement;
import ru.defea.oneblockultima.gui.layout.ButtonElement.LabelPosition;
import ru.defea.oneblockultima.gui.layout.ButtonToggleElement;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import static org.junit.Assert.*;
import static ru.defea.oneblockultima.Constants.*;
import static ru.defea.oneblockultima.gui.layout.ButtonElement.BUTTON_PADDING;

public class ButtonToggleElementTest {

    @BeforeClass
    public static void setUp() {
        TestBootstrap.installFakeMinecraft();
    }

    private static class ExposedToggle extends ButtonToggleElement {
        ExposedToggle(int id, boolean state) {
            super(id, state);
        }

        public int fill(boolean hovered) {
            return widgetFillColor(hovered);
        }

        public int textColor(boolean hovered) {
            return widgetTextColor(hovered);
        }

        public String getText() {
            return text;
        }
    }

    // --- state ---

    @Test
    public void constructorWithTrueStateIsTriggered() {
        assertTrue(new ButtonToggleElement(true).isStateTriggered());
    }

    @Test
    public void constructorWithFalseStateIsNotTriggered() {
        assertFalse(new ButtonToggleElement(false).isStateTriggered());
    }

    @Test
    public void toggleFlipsState() {
        ButtonToggleElement t = new ButtonToggleElement(false);
        t.toggle();
        assertTrue(t.isStateTriggered());
        t.toggle();
        assertFalse(t.isStateTriggered());
    }

    @Test
    public void stateTriggeredSetsState() {
        ButtonToggleElement t = new ButtonToggleElement(false);
        t.stateTriggered(true);
        assertTrue(t.isStateTriggered());
    }

    @Test
    public void toggleUpdatesText() {
        ExposedToggle t = new ExposedToggle(0, false);
        assertEquals("", t.getText());
        t.toggle();
        assertEquals("\u2714", t.getText());
        t.toggle();
        assertEquals("", t.getText());
    }

    // --- label ---

    @Test
    public void defaultLabelPositionIsRight() {
        ButtonToggleElement t = new ButtonToggleElement(false).label("x");
        assertEquals(LabelPosition.RIGHT, t.getLabelPosition());
    }

    @Test
    public void labelAndPositionConfigured() {
        ButtonToggleElement t = new ButtonToggleElement(false)
                .label("Test label", LabelPosition.LEFT);
        assertEquals("Test label", t.getLabel());
        assertEquals(LabelPosition.LEFT, t.getLabelPosition());
    }

    @Test
    public void preferredWidthWithoutLabelIsButtonPadding() {
        assertEquals(BUTTON_PADDING, new ButtonToggleElement(false).getPreferredWidth());
    }

    @Test
    public void preferredWidthIncludesLabelEstimate() {
        @SuppressWarnings("SpellCheckingInspection") ButtonToggleElement t = new ButtonToggleElement(false).label("abcd");
        assertEquals(BUTTON_PADDING + 6 + 4 * 6, t.getPreferredWidth());
    }

    @Test
    public void explicitWidthOverridesDefaultPreferredWidth() {
        ButtonToggleElement t = new ButtonToggleElement(false).width(24);
        assertEquals(24, t.getPreferredWidth());
        t.label("abcd");
        assertEquals(24 + 6 + 4 * 6, t.getPreferredWidth());
    }

    @Test
    public void buttonPaddingConstantIsSixteen() {
        assertEquals(16, BUTTON_PADDING);
    }

    @Test
    public void preferredHeightUsesFontLineHeightPlusPadding() {
        Font font = TestBootstrap.getStubFont();
        assertEquals(font.lineHeight + ButtonElement.BUTTON_HEIGHT_PADDING,
                new ButtonToggleElement(false).getPreferredHeight());
    }

    // --- colors ---

    @Test
    public void fillColorTriggeredHoveredUsesSuccessHovered() {
        ExposedToggle t = new ExposedToggle(0, true);
        assertEquals(SUCCESS_HOVERED_COLOR, t.fill(true));
    }

    @Test
    public void fillColorTriggeredNotHoveredUsesDarkGreen() {
        ExposedToggle t = new ExposedToggle(0, true);
        assertEquals(DARK_GREEN, t.fill(false));
    }

    @Test
    public void fillColorNotTriggeredUsesDefaultAndHovered() {
        ExposedToggle t = new ExposedToggle(0, false);
        assertEquals(DARK_GRAY_COLOR_1, t.fill(false));
        assertEquals(GRAY_COLOR_6, t.fill(true));
    }

    @Test
    public void fillColorDisabledIgnoresState() {
        ExposedToggle t = new ExposedToggle(0, true);
        t.enabled(false);
        assertEquals(DISABLED_BUTTON_FILL, t.fill(false));
        assertEquals(DISABLED_BUTTON_FILL, t.fill(true));
    }

    @Test
    public void textColorTriggeredUsesSuccess() {
        ExposedToggle t = new ExposedToggle(0, true);
        assertEquals(SUCCESS_COLOR, t.textColor(false));
        assertEquals(SUCCESS_COLOR, t.textColor(true));
    }

    @Test
    public void textColorDisabledUsesDisabledText() {
        ExposedToggle t = new ExposedToggle(0, true);
        t.enabled(false);
        assertEquals(DISABLED_BUTTON_TEXT, t.textColor(false));
        assertEquals(DISABLED_BUTTON_TEXT, t.textColor(true));
    }
}