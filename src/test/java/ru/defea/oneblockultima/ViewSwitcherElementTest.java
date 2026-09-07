package ru.defea.oneblockultima;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.ViewElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;
import ru.defea.oneblockultima.gui.layout.ViewSwitcherElement;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import static org.junit.Assert.assertEquals;

public class ViewSwitcherElementTest {

    @BeforeClass
    public static void setUp() {
        TestBootstrap.prepare();
    }

    private static class Stub extends ViewElement<Stub> {
        private final int prefW;
        private final int prefH;

        Stub(int prefW, int prefH) {
            this.prefW = prefW;
            this.prefH = prefH;
        }

        @Override
        public void createWidgets(Screen screen, Font font, ViewFactory factory) {
        }

        @Override
        public void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        }

        @Override
        public int getPreferredWidth() {
            return prefW;
        }

        @Override
        public int getPreferredHeight() {
            return prefH;
        }
    }

    private static Screen screen() {
        return new Screen(Component.literal("")) {
            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            }
        };
    }

    private static int fullWidth() {
        return 980;
    }

    private static int fullHeight() {
        return 780;
    }

    @SuppressWarnings("SameParameterValue")
    private void assertBox(ViewElement<?> stub, int x, int y, int w, int h) {
        assertEquals(x, stub.getComputedX());
        assertEquals(y, stub.getComputedY());
        assertEquals(w, stub.getComputedWidth());
        assertEquals(h, stub.getComputedHeight());
    }

    @Test
    public void fitContentShrinksAndCentersActiveView() {
        ViewFactory factory = new ViewFactory(1000, 800).margin(8).padding(2).gap(6);
        ViewSwitcherElement switcher = new ViewSwitcherElement().widthPercent(100).flexible(true);
        factory.add(switcher);

        Stub stub = new Stub(200, 150);
        switcher.replaceView(0, stub);
        switcher.setView(0);
        switcher.fitContent(true);

        factory.build(screen(), null);

        int x = 10 + (fullWidth() - 200) / 2;
        int y = 10 + (fullHeight() - 150) / 2;
        assertBox(stub, x, y, 200, 150);
    }

    @Test
    public void withoutFitContentActiveViewFillsSwitcherBox() {
        ViewFactory factory = new ViewFactory(1000, 800).margin(8).padding(2).gap(6);
        ViewSwitcherElement switcher = new ViewSwitcherElement().widthPercent(100).flexible(true);
        factory.add(switcher);

        Stub stub = new Stub(200, 150);
        switcher.replaceView(0, stub);
        switcher.setView(0);

        factory.build(screen(), null);

        assertEquals(fullWidth(), stub.getComputedWidth());
        assertEquals(fullHeight(), stub.getComputedHeight());
    }
}