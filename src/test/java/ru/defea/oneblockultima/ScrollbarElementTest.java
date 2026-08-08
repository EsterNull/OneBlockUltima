package ru.defea.oneblockultima;

import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.ScrollbarElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScrollbarElementTest {

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
    }

    @SuppressWarnings("SameParameterValue")
    private ScrollbarElement bar(int total, int visible, int offset, int y, int height) {
        ScrollbarElement sb = new ScrollbarElement()
                .totalItems(total)
                .visibleItems(visible)
                .scrollOffset(offset)
                .trackWidth(6);
        sb.setComputedPosition(100, y);
        sb.setComputedSize(6, height);
        return sb;
    }

    @Test
    public void clickAtTopOfTrackResetsScrollToZero() {
        ScrollbarElement sb = bar(200, 10, 50, 100, 200);
        assertTrue(sb.mouseClicked(103, 100, 0));
        assertEquals(0, sb.getScrollOffset());
    }

    @Test
    public void clickAtBottomOfTrackScrollsToMax() {
        ScrollbarElement sb = bar(200, 10, 0, 100, 200);
        assertTrue(sb.mouseClicked(103, 300, 0));
        assertEquals(sb.getMaxScroll(), sb.getScrollOffset());
    }

    @Test
    public void clickInMiddleOfTrackScrollsToMiddle() {
        ScrollbarElement sb = bar(200, 10, 0, 100, 200);
        assertTrue(sb.mouseClicked(103, 200, 0));
        assertEquals(Math.round(0.5f * sb.getMaxScroll()), sb.getScrollOffset());
    }

    @Test
    public void clickOutsideTrackIsIgnored() {
        ScrollbarElement sb = bar(200, 10, 25, 100, 200);
        assertFalse(sb.mouseClicked(200, 200, 0));
        assertEquals(25, sb.getScrollOffset());
    }

    @Test
    public void clickOnNonScrollableBarIsIgnored() {
        ScrollbarElement sb = bar(10, 10, 0, 100, 200);
        assertFalse(sb.mouseClicked(103, 150, 0));
        assertEquals(0, sb.getScrollOffset());
    }

    @Test
    public void rightClickOnTrackIsIgnored() {
        ScrollbarElement sb = bar(200, 10, 25, 100, 200);
        assertFalse(sb.mouseClicked(103, 200, 1));
        assertEquals(25, sb.getScrollOffset());
    }

    @Test
    public void clickOnTrackStartsDragFromThatSpot() {
        ScrollbarElement sb = bar(200, 10, 0, 100, 200);
        assertTrue(sb.mouseClicked(103, 200, 0));
        int mid = Math.round(0.5f * sb.getMaxScroll());
        assertEquals(mid, sb.getScrollOffset());
        assertTrue(sb.mouseClickMove(103, 260, 0, 0));
        assertTrue(sb.mouseClickMove(103, 200, 0, 0));
        assertEquals(mid, sb.getScrollOffset());
    }
}
