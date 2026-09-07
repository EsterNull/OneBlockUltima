package ru.defea.oneblockultima;

import net.minecraft.client.gui.Font;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.ScrollableListElement;
import ru.defea.oneblockultima.testutil.TestBootstrap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScrollableListElementTest {

    @BeforeClass
    public static void setUp() {
        TestBootstrap.prepare();
    }

    private static class Entry implements ScrollableListElement.ScrollableListEntry {
        @Override
        public void draw(int x, int y, int width, int height, boolean hovered, boolean selected, Font font, int mouseX, int mouseY) {
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int mouseXOffset, int mouseYOffset, int entryWidth, int entryHeight, int mouseButton) {
            return false;
        }
    }

    private ScrollableListElement list(@SuppressWarnings("SameParameterValue") int total) {
        List<ScrollableListElement.ScrollableListEntry> entries = new ArrayList<>();
        for (int i = 0; i < total; i++) entries.add(new Entry());
        ScrollableListElement el = new ScrollableListElement(20)
                .entries(entries)
                .scrollOffset(0);
        el.setComputedPosition(100, 100);
        el.setComputedSize(200, 200);
        return el;
    }

    @Test
    public void clickOnTrackStartsDragFromThatSpot() {
        ScrollableListElement el = list(200);
        int maxScroll = 200 - 200 / 20;
        assertTrue(el.mouseClicked(296, 200, 0));
        assertEquals(Math.round(0.5f * maxScroll), el.getScrollOffset());
        assertTrue(el.mouseClickMove(296, 260, 0, 0));
        assertTrue(el.mouseClickMove(296, 200, 0, 0));
        assertEquals(Math.round(0.5f * maxScroll), el.getScrollOffset());
    }

    @Test
    public void clickOutsideTrackIsIgnored() {
        ScrollableListElement el = list(200);
        assertFalse(el.mouseClicked(200, 200, 0));
        assertEquals(0, el.getScrollOffset());
    }
}