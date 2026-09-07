package ru.defea.oneblockultima;

import net.minecraft.client.gui.Font;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.TwoColumnListElement;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TwoColumnListElementTest {

    @BeforeClass
    public static void setUp() {
        TestBootstrap.prepare();
    }

    private static class Entry implements TwoColumnListElement.TwoColumnEntry {
        @Override
        public void drawLeft(int x, int y, int width, int height, boolean hovered, int index, Font font, int mouseX, int mouseY) {
        }

        @Override
        public void drawRight(int x, int y, int width, int height, boolean hovered, int index, Font font, int mouseX, int mouseY) {
        }

        @Override
        public boolean mouseClickedLeft(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) {
            return false;
        }

        @Override
        public boolean mouseClickedRight(int mouseX, int mouseY, int localX, int localY, int entryWidth, int entryHeight, int mouseButton) {
            return false;
        }
    }

    private TwoColumnListElement list(@SuppressWarnings("SameParameterValue") int total) {
        List<TwoColumnListElement.TwoColumnEntry> left = new ArrayList<>();
        List<TwoColumnListElement.TwoColumnEntry> right = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            left.add(new Entry());
            right.add(new Entry());
        }
        TwoColumnListElement el = new TwoColumnListElement(20)
                .leftEntries(left)
                .rightEntries(right)
                .scrollOffset(0);
        el.setComputedPosition(100, 100);
        el.setComputedSize(200, 200);
        return el;
    }

    @Test
    public void clickOnTrackStartsDragFromThatSpot() {
        TwoColumnListElement el = list(200);
        int maxScroll = 200 - 200 / 20;
        assertTrue(el.mouseClicked(296, 200, 0));
        assertEquals(Math.round(0.5f * maxScroll), el.getScrollOffset());
        assertTrue(el.mouseClickMove(296, 260, 0, 0));
        assertTrue(el.mouseClickMove(296, 200, 0, 0));
        assertEquals(Math.round(0.5f * maxScroll), el.getScrollOffset());
    }
}