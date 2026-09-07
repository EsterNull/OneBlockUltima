package ru.defea.oneblockultima;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.ButtonElement;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import static org.junit.Assert.assertEquals;
import static ru.defea.oneblockultima.Constants.*;

public class ButtonElementTest {

    @BeforeClass
    public static void setUp() {
        TestBootstrap.prepare();
    }

    private static class ExposedButton extends ButtonElement<ExposedButton> {
        ExposedButton(int id, String text) {
            super(id, text);
        }

        public int fill(boolean hovered) {
            return widgetFillColor(hovered);
        }

        public int textColor(boolean hovered) {
            return widgetTextColor(hovered);
        }
    }

    @Test
    public void fillColorNormalUsesDefaultAndHovered() {
        ExposedButton b = new ExposedButton(0, "x");
        assertEquals(DARK_GRAY_COLOR_1, b.fill(false));
        assertEquals(GRAY_COLOR_6, b.fill(true));
    }

    @Test
    public void fillColorDisabledAlwaysUsesDisabledFill() {
        ExposedButton b = new ExposedButton(0, "x");
        b.enabled(false);
        assertEquals(DISABLED_BUTTON_FILL, b.fill(false));
        assertEquals(DISABLED_BUTTON_FILL, b.fill(true));
    }

    @Test
    public void textColorDisabledAlwaysUsesDisabledText() {
        ExposedButton b = new ExposedButton(0, "x");
        b.enabled(false);
        assertEquals(DISABLED_BUTTON_TEXT, b.textColor(false));
        assertEquals(DISABLED_BUTTON_TEXT, b.textColor(true));
    }

    @Test
    public void textColorEnabledUsesConfiguredColors() {
        ExposedButton b = new ExposedButton(0, "x");
        assertEquals(WHITE_COLOR_1, b.textColor(false));
        assertEquals(WHITE_COLOR_1, b.textColor(true));
    }

    @Test
    public void textColorUsesCustomColors() {
        ExposedButton b = new ExposedButton(0, "x");
        b.setTextColor(RED_COLOR);
        b.setTextColorHovered(GREEN_COLOR);
        assertEquals(RED_COLOR, b.textColor(false));
        assertEquals(GREEN_COLOR, b.textColor(true));
    }

    @Test
    public void getIdReturnsButtonId() {
        ExposedButton b = new ExposedButton(42, "x");
        assertEquals(42, b.getId());
    }
}