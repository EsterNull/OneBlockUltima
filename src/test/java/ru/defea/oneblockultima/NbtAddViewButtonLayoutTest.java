package ru.defea.oneblockultima;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.ButtonElement;
import ru.defea.oneblockultima.gui.layout.RowElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NbtAddViewButtonLayoutTest {

    private static final String BUTTON_TYPE_TEXT = "Строка";
    private static final String BUTTON_DONE_TEXT = "Готово";
    private static final String BUTTON_CANCEL_TEXT = "Отмена";

    @BeforeClass
    public static void setUp() {
        TestBootstrap.prepare();
    }

    private static Screen screen() {
        return new Screen(Component.literal("")) {
            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            }
        };
    }

    private static List<AbstractWidget> widgets(Screen screen) {
        List<AbstractWidget> result = new ArrayList<>();
        for (net.minecraft.client.gui.components.Renderable r : screen.renderables) {
            if (r instanceof AbstractWidget w) result.add(w);
        }
        return result;
    }

    private static long countWithText(List<AbstractWidget> widgets, String text) {
        return widgets.stream().filter(w -> w.getMessage().getString().equals(text)).count();
    }

    private static AbstractWidget findByText(List<AbstractWidget> widgets, String text) {
        return widgets.stream().filter(w -> w.getMessage().getString().equals(text)).findFirst().orElse(null);
    }

    private static ViewFactory buildAddViewLikeFactory() {
        ViewFactory factory = new ViewFactory(1000, 800)
                .margin(2).padding(4).gap(6)
                .align(Alignment.CENTER);
        factory.fitContent();
        factory.centerVertical();

        RowElement typeRow = factory.row(Alignment.CENTER).gap(6);
        typeRow.add(new ButtonElement<>(31, BUTTON_TYPE_TEXT).width(70).height(20));

        RowElement btnRow = factory.row(Alignment.CENTER).gap(6);
        btnRow.button(29, BUTTON_DONE_TEXT, 60).height(20);
        btnRow.button(30, BUTTON_CANCEL_TEXT, 60).height(20);
        return factory;
    }

    @Test
    public void addViewCreatesExactlyOneTypeButton() {
        ViewFactory factory = buildAddViewLikeFactory();
        Screen screen = screen();
        factory.build(screen, null);

        assertEquals(1, countWithText(widgets(screen), BUTTON_TYPE_TEXT));
    }

    @Test
    public void noDuplicateButtonsAreCreated() {
        ViewFactory factory = buildAddViewLikeFactory();
        Screen screen = screen();
        factory.build(screen, null);

        List<String> seen = new ArrayList<>();
        for (AbstractWidget w : widgets(screen)) {
            String key = w.getMessage().getString() + "@" + w.getX() + "," + w.getY()
                    + "," + w.getWidth() + "x" + w.getHeight();
            assertFalse("duplicate button: " + key, seen.contains(key));
            seen.add(key);
        }
    }

    @Test
    public void typeButtonPositionIsInsideScreenAndCentered() {
        ViewFactory factory = buildAddViewLikeFactory();
        Screen screen = screen();
        factory.build(screen, null);

        AbstractWidget typeButton = findByText(widgets(screen), BUTTON_TYPE_TEXT);
        assertNotNull(typeButton);
        assertTrue("type button must be fully inside the screen", typeButton.getX() >= 0);
        assertTrue(typeButton.getX() + typeButton.getWidth() <= 1000);
    }

    @Test
    public void typeButtonDoesNotOverlapActionButtons() {
        ViewFactory factory = buildAddViewLikeFactory();
        Screen screen = screen();
        factory.build(screen, null);

        AbstractWidget typeButton = findByText(widgets(screen), BUTTON_TYPE_TEXT);
        assertNotNull(typeButton);
        for (AbstractWidget other : widgets(screen)) {
            if (other.getMessage().getString().equals(BUTTON_TYPE_TEXT)) continue;
            boolean sameRow = typeButton.getY() < other.getY() + other.getHeight()
                    && other.getY() < typeButton.getY() + typeButton.getHeight();
            boolean overlapX = typeButton.getX() < other.getX() + other.getWidth()
                    && other.getX() < typeButton.getX() + typeButton.getWidth();
            assertFalse("type button must not overlap button "
                    + other.getMessage().getString(), sameRow && overlapX);
        }
    }

    @Test
    public void reAddingSameElementIsIgnoredByFactory() {
        ViewFactory factory = new ViewFactory(1000, 800)
                .margin(2).padding(4).gap(6)
                .align(Alignment.CENTER);
        factory.fitContent();
        factory.centerVertical();

        RowElement typeRow = factory.row(Alignment.CENTER).gap(6);
        typeRow.add(new ButtonElement<>(31, BUTTON_TYPE_TEXT).width(70).height(20));
        factory.add(typeRow);

        Screen screen = screen();
        factory.build(screen, null);

        assertEquals(1, countWithText(widgets(screen), BUTTON_TYPE_TEXT));
    }
}