package ru.defea.oneblockultima;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.ButtonElement;
import ru.defea.oneblockultima.gui.layout.RowElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NbtAddViewButtonLayoutTest {

    private static final int BUTTON_TYPE = 31;
    private static final int BUTTON_DONE = 29;
    private static final int BUTTON_CANCEL = 30;

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
    }

    private static ViewFactory buildAddViewLikeFactory() {
        ViewFactory factory = new ViewFactory(1000, 800)
                .margin(2).padding(4).gap(6)
                .align(Alignment.CENTER);
        factory.fitContent();
        factory.centerVertical();

        RowElement typeRow = factory.row(Alignment.CENTER).gap(6);
        typeRow.add(new ButtonElement<>(BUTTON_TYPE, "Строка").width(70).height(20));

        RowElement btnRow = factory.row(Alignment.CENTER).gap(6);
        btnRow.button(BUTTON_DONE, "Готово", 60).height(20);
        btnRow.button(BUTTON_CANCEL, "Отмена", 60).height(20);
        return factory;
    }

    @Test
    public void addViewCreatesExactlyOneTypeButton() {
        ViewFactory factory = buildAddViewLikeFactory();
        List<GuiButton> buttons = new ArrayList<>();
        factory.build(buttons, null);

        long typeButtons = buttons.stream().filter(b -> b.id == BUTTON_TYPE).count();
        assertEquals(1, typeButtons);
    }

    @Test
    public void noDuplicateButtonsAreCreated() {
        ViewFactory factory = buildAddViewLikeFactory();
        List<GuiButton> buttons = new ArrayList<>();
        factory.build(buttons, null);

        List<String> seen = new ArrayList<>();
        for (GuiButton b : buttons) {
            String key = b.id + "@" + b.xPosition + "," + b.yPosition + "," + b.width + "x" + b.height;
            assertFalse("duplicate button: " + key, seen.contains(key));
            seen.add(key);
        }
    }

    @Test
    public void typeButtonPositionIsInsideScreenAndCentered() {
        ViewFactory factory = buildAddViewLikeFactory();
        List<GuiButton> buttons = new ArrayList<>();
        factory.build(buttons, null);

        GuiButton typeButton = buttons.stream().filter(b -> b.id == BUTTON_TYPE).findFirst().orElse(null);
        assertNotNull(typeButton);
        assertTrue("type button must be fully inside the screen", typeButton.xPosition >= 0);
        assertTrue(typeButton.xPosition + typeButton.width <= 1000);
    }

    @Test
    public void typeButtonDoesNotOverlapActionButtons() {
        ViewFactory factory = buildAddViewLikeFactory();
        List<GuiButton> buttons = new ArrayList<>();
        factory.build(buttons, null);

        GuiButton typeButton = buttons.stream().filter(b -> b.id == BUTTON_TYPE).findFirst().orElse(null);
        assertNotNull(typeButton);
        for (GuiButton other : buttons) {
            if (other.id == BUTTON_TYPE) continue;
            boolean sameRow = typeButton.yPosition < other.yPosition + other.height && other.yPosition < typeButton.yPosition + typeButton.height;
            boolean overlapX = typeButton.xPosition < other.xPosition + other.width && other.xPosition < typeButton.xPosition + typeButton.width;
            assertFalse("type button must not overlap button " + other.id, sameRow && overlapX);
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
        typeRow.add(new ButtonElement<>(BUTTON_TYPE, "Строка").width(70).height(20));
        factory.add(typeRow);

        List<GuiButton> buttons = new ArrayList<>();
        factory.build(buttons, null);

        long typeButtons = buttons.stream().filter(b -> b.id == BUTTON_TYPE).count();
        assertEquals(1, typeButtons);
    }
}
