package ru.defea.oneblockultima;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.ColumnElement;
import ru.defea.oneblockultima.gui.layout.RowElement;
import ru.defea.oneblockultima.gui.layout.SpacerElement;
import ru.defea.oneblockultima.gui.layout.ViewElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;
import ru.defea.oneblockultima.gui.layout.ViewSwitcherElement;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuiOneBlockLayoutTest {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int X_SIZE = WIDTH - 40;
    private static final int Y_SIZE = HEIGHT - 40;
    private static final int GUI_LEFT = (WIDTH - X_SIZE) / 2;
    private static final int GUI_TOP = (HEIGHT - Y_SIZE) / 2;
    private static final int HEADER_HEIGHT = 27;
    private static final int TAB_HEIGHT = 20;
    private static final int H_MARGIN = Math.min(24, X_SIZE / 24);
    private static final int CONTENT_WIDTH = Math.max(120, X_SIZE - H_MARGIN * 2);

    @BeforeClass
    public static void setUp() {
        Bootstrap.register();
    }

    private static class Stub extends ViewElement<Stub> {
        private final int prefW;
        private final int prefH;

        Stub(int prefW, int prefH) {
            this.prefW = prefW;
            this.prefH = prefH;
        }

        @Override
        public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
        }

        @Override
        public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
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

    private static class ClickConsumingStub extends Stub {
        ClickConsumingStub() {
            super(1, 1);
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            return true;
        }
    }

    private static class FakeTextField extends GuiTextField {
        private int clicks;
        private boolean fakeFocused;

        FakeTextField(int id, int x, int y) {
            //noinspection DataFlowIssue
            super(id, null, x, y, 120, 20);
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            clicks++;
            fakeFocused = mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height;
            return fakeFocused;
        }

        @Override
        public boolean isFocused() {
            return fakeFocused;
        }

        @Override
        public void setFocused(boolean focused) {
            fakeFocused = focused;
        }

        int getClicks() {
            return clicks;
        }
    }

    private ViewFactory createFactory() {
        return new ViewFactory(X_SIZE, Y_SIZE)
                .margin(0).padding(0)
                .gap(0)
                .align(Alignment.CENTER)
                .panel(0, 0);
    }

    private void assertInside(ViewElement<?> e, int minX, int minY, int maxX, int maxY) {
        assertTrue("x=" + e.getComputedX() + " < minX=" + minX,
                e.getComputedX() >= minX);
        assertTrue("y=" + e.getComputedY() + " < minY=" + minY,
                e.getComputedY() >= minY);
        assertTrue("x+w=" + (e.getComputedX() + e.getComputedWidth()) + " > maxX=" + maxX,
                e.getComputedX() + e.getComputedWidth() <= maxX);
        assertTrue("y+h=" + (e.getComputedY() + e.getComputedHeight()) + " > maxY=" + maxY,
                e.getComputedY() + e.getComputedHeight() <= maxY);
        assertTrue("width=" + e.getComputedWidth() + " must be > 0", e.getComputedWidth() > 0);
        assertTrue("height=" + e.getComputedHeight() + " must be > 0", e.getComputedHeight() > 0);
    }

    private ViewSwitcherElement addRoot(ViewFactory factory, ColumnElement view) {
        Stub header = new Stub(0, HEADER_HEIGHT).widthPercent(100);
        factory.add(header);
        Stub tabs = new Stub(0, TAB_HEIGHT).width(CONTENT_WIDTH);
        factory.add(tabs);
        ViewSwitcherElement switcher = new ViewSwitcherElement().width(CONTENT_WIDTH).flexible(true);
        factory.add(switcher);
        switcher.replaceView(0, view);
        switcher.setView(0);
        return switcher;
    }

    @Test
    public void rootLayoutMatchesInsetContentMetrics() {
        ColumnElement view = new ColumnElement().gap(8);
        view.add(new Stub(0, 124).widthPercent(100));

        ViewFactory factory = createFactory();
        ViewSwitcherElement switcher = addRoot(factory, view);
        factory.build(new ArrayList<>(), null, GUI_LEFT, GUI_TOP, X_SIZE, Y_SIZE);

        int contentTop = GUI_TOP + HEADER_HEIGHT + TAB_HEIGHT;

        assertEquals(GUI_LEFT + H_MARGIN, view.getComputedX());
        assertEquals(contentTop, view.getComputedY());
        assertEquals(CONTENT_WIDTH, view.getComputedWidth());
        assertEquals(Y_SIZE - (HEADER_HEIGHT + TAB_HEIGHT), view.getComputedHeight());

        assertEquals(GUI_LEFT + H_MARGIN, switcher.getComputedX());
        assertEquals(CONTENT_WIDTH, switcher.getComputedWidth());
        assertTrue("content must not overflow window",
                view.getComputedX() + view.getComputedWidth() <= GUI_LEFT + X_SIZE);
    }

    @Test
    public void setsViewKeepsChildrenInsideContentArea() {
        ColumnElement view = new ColumnElement().gap(8);

        Stub info = new Stub(0, 50).widthPercent(100);
        view.add(info);

        RowElement row1 = view.row(Alignment.LEFT);
        row1.button(0, "<").width(20).height(20);
        row1.add(new Stub(6, 0));
        row1.button(1, ">").width(20).height(20);

        RowElement row2 = view.row(Alignment.SPACE_BETWEEN).stretchToContent();
        row2.button(2, "select").width(200).height(20);
        row2.button(3, "upgrade").width(200).height(20);

        Stub panels = new Stub(0, 0).flexible(true);
        view.add(panels);

        ViewFactory factory = createFactory();
        addRoot(factory, view);
        factory.build(new ArrayList<>(), null, GUI_LEFT, GUI_TOP, X_SIZE, Y_SIZE);

        int minX = view.getComputedX();
        int minY = view.getComputedY();
        int maxX = minX + view.getComputedWidth();
        int maxY = minY + view.getComputedHeight();

        assertInside(info, minX, minY, maxX, maxY);
        assertInside(row1, minX, minY, maxX, maxY);
        assertInside(row2, minX, minY, maxX, maxY);
        assertInside(panels, minX, minY, maxX, maxY);
        assertTrue("panels height " + panels.getComputedHeight() + " too small", panels.getComputedHeight() > 50);

        assertTrue("prev button must be left of next button",
                row1.getChildren().get(0).getComputedX() < row1.getChildren().get(2).getComputedX());
        assertTrue("select button must be left of upgrade button",
                row2.getChildren().get(0).getComputedX() < row2.getChildren().get(1).getComputedX());
    }

    @Test
    public void donateViewKeepsQrAndButtonsInsideContentArea() {
        ColumnElement view = new ColumnElement().gap(8);

        Stub donateText = new Stub(0, 48).widthPercent(100);
        view.add(donateText);

        RowElement contentRow = view.row(Alignment.LEFT);
        ColumnElement qrCol = new ColumnElement().align(Alignment.CENTER).gap(4);
        qrCol.add(new Stub(64, 64));
        qrCol.add(new Stub(24, 11));
        contentRow.add(qrCol);

        ColumnElement buttonsCol = new ColumnElement().align(Alignment.LEFT).gap(12);
        buttonsCol.button(1000, "btc").width(CONTENT_WIDTH - 76).height(20);
        contentRow.add(buttonsCol);

        Stub spacer = new Stub(0, 0).flexible(true);
        view.add(spacer);
        Stub status = new Stub(0, 11).widthPercent(100);
        view.add(status);

        ViewFactory factory = createFactory();
        addRoot(factory, view);
        factory.build(new ArrayList<>(), null, GUI_LEFT, GUI_TOP, X_SIZE, Y_SIZE);

        int minX = view.getComputedX();
        int minY = view.getComputedY();
        int maxX = minX + view.getComputedWidth();
        int maxY = minY + view.getComputedHeight();

        assertInside(donateText, minX, minY, maxX, maxY);
        assertInside(contentRow, minX, minY, maxX, maxY);
        assertInside(qrCol, minX, minY, maxX, maxY);
        assertInside(buttonsCol, minX, minY, maxX, maxY);

        assertEquals(64, qrCol.getChildren().get(0).getComputedWidth());
        assertEquals(64, qrCol.getChildren().get(0).getComputedHeight());

        List<ViewElement<?>> buttons = buttonsCol.getChildren();
        assertEquals(1, buttons.size());
        assertInside(buttons.get(0), minX, minY, maxX, maxY);
        assertEquals(CONTENT_WIDTH - 76, buttons.get(0).getComputedWidth());
        assertEquals(20, buttons.get(0).getComputedHeight());
        assertTrue("qr must be left of buttons",
                qrCol.getComputedX() + qrCol.getComputedWidth() <= buttonsCol.getComputedX());
    }

    @Test
    public void setsListsRemainVisibleAcrossResolutions() {
        int[][] sizes = {
                {1280, 720},
                {960, 540},
                {640, 360},
                {512, 288},
                {480, 270},
                {384, 216},
        };
        int[] infoHeights = {30, 82};

        for (int[] size : sizes) {
            int w = size[0];
            int h = size[1];
            int xSize = w - 40;
            int ySize = h - 40;
            int guiLeft = (w - xSize) / 2;
            int guiTop = (h - ySize) / 2;
            int hMargin = Math.max(12, Math.min(24, xSize / 24));
            int contentWidth = Math.max(120, xSize - hMargin * 2);

            for (int infoH : infoHeights) {
                ViewFactory factory = new ViewFactory(xSize, ySize)
                        .margin(0).padding(0).gap(0)
                        .align(Alignment.CENTER).panel(0, 0);
                Stub header = new Stub(0, HEADER_HEIGHT).widthPercent(100);
                factory.add(header);
                Stub tabs = new Stub(0, TAB_HEIGHT).width(contentWidth);
                factory.add(tabs);
                ViewSwitcherElement switcher = new ViewSwitcherElement().width(contentWidth).flexible(true);
                factory.add(switcher);

                ColumnElement setsView = new ColumnElement().gap(8);
                setsView.add(new SpacerElement(2));
                int switcherHeight = ySize - HEADER_HEIGHT - TAB_HEIGHT;
                int cappedInfo = Math.min(infoH, Math.max(8, switcherHeight - 2 - 8 - (20 * 2 + 8 * 3) - 56));
                Stub info = new Stub(0, cappedInfo).widthPercent(100);
                setsView.add(info);
                RowElement row1 = setsView.row(Alignment.LEFT);
                row1.button(0, "<").width(20).height(20);
                row1.add(new SpacerElement(6, 0));
                row1.button(1, ">").width(20).height(20);
                RowElement row2 = setsView.row(Alignment.SPACE_BETWEEN).stretchToContent().gap(6);
                int selectWidth = (contentWidth - 6) / 2;
                row2.button(2, "select").width(selectWidth).height(20);
                row2.button(3, "upgrade").width(selectWidth).height(20);
                Stub panels = new Stub(0, 0).flexible(true);
                setsView.add(panels);

                switcher.replaceView(0, setsView);
                switcher.setView(0);
                factory.build(new ArrayList<>(), null, guiLeft, guiTop, xSize, ySize);

                int panelH = panels.getComputedHeight();
                int panelY = panels.getComputedY();
                int rowInterval = 9 + 4;
                int areaHeight = Math.max(0, panelH - rowInterval * 2);

                String label = "w=" + w + " h=" + h + " info=" + infoH;
                assertTrue(label + ": panels must be positioned, got y=" + panelY,
                        panelY > 0);
                assertTrue(label + ": panels height " + panelH + " must be positive",
                        panelH > 0);
                assertTrue(label + ": grid area height " + areaHeight + " must allow a scrollbar",
                        areaHeight > 0);

                if (w >= 480) {
                    assertTrue(label + ": grid area height " + areaHeight + " must fit at least one full row",
                            areaHeight >= 20);
                }

                int panelGap = Math.max(4, contentWidth / 40);
                int panelWidth = (contentWidth - panelGap) / 2;
                int availableWidth = panelWidth - 6 * 2;
                int halfWidth = (availableWidth - 8) / 2;
                int blockAreaWidth = halfWidth - 8;
                int blockCols = Math.max(2, blockAreaWidth / 20);
                int cellPadding = Math.max(1, Math.min(2, blockAreaWidth / 80));
                int cellSize = Math.max(14, Math.min(20, (blockAreaWidth - (blockCols - 1) * cellPadding) / blockCols));
                int step = cellSize + cellPadding;

                int mockTotal = 200;
                int mockRows = (mockTotal + blockCols - 1) / blockCols;
                int visibleRows = Math.min(mockRows, Math.max(1, areaHeight / step));
                int maxScroll = Math.max(0, mockRows - visibleRows);

                if (mockRows > visibleRows) {
                    assertTrue(label + ": overflowing grid must have room for a scrollbar (maxScroll=" + maxScroll + ", areaHeight=" + areaHeight + ")",
                            maxScroll > 0);
                }
            }
        }
    }

    @Test
    public void donateViewLaysOutButtonsAndStatusSpaceAcrossResolutions() {
        int[][] sizes = {
                {1280, 720},
                {960, 540},
                {640, 360},
                {480, 270},
                {480, 240},
        };
        for (int[] size : sizes) {
            int w = size[0];
            int h = size[1];
            int xSize = w - 40;
            int ySize = h - 40;
            int guiLeft = (w - xSize) / 2;
            int guiTop = (h - ySize) / 2;
            int hMargin = Math.max(12, Math.min(24, xSize / 24));
            int contentWidth = Math.max(120, xSize - hMargin * 2);

            ViewFactory factory = new ViewFactory(xSize, ySize)
                    .margin(0).padding(0).gap(0)
                    .align(Alignment.CENTER).panel(0, 0);
            Stub header = new Stub(0, HEADER_HEIGHT).widthPercent(100);
            factory.add(header);
            Stub tabs = new Stub(0, TAB_HEIGHT).width(contentWidth);
            factory.add(tabs);
            ViewSwitcherElement switcher = new ViewSwitcherElement().width(contentWidth).flexible(true);
            factory.add(switcher);

            ColumnElement donateView = new ColumnElement().gap(8);
            donateView.add(new SpacerElement(4));
            Stub donateText = new Stub(0, 13 * 4 - 4).widthPercent(100);
            donateView.add(donateText);
            RowElement contentRow = donateView.row(Alignment.LEFT);
            ColumnElement qrCol = new ColumnElement().align(Alignment.CENTER).gap(4);
            qrCol.add(new Stub(64, 64));
            qrCol.add(new Stub(24, 11));
            contentRow.add(qrCol);
            ColumnElement buttonsCol = new ColumnElement().align(Alignment.LEFT).gap(12);
            int donateBtnWidth = Math.max(120, contentWidth - 72 - 4);
            buttonsCol.button(1000, "btc").width(donateBtnWidth).height(20);
            buttonsCol.button(1001, "eth").width(donateBtnWidth).height(20);
            buttonsCol.button(1002, "steam").width(donateBtnWidth).height(20);
            contentRow.add(buttonsCol);
            Stub spacer = new Stub(0, 0).flexible(true);
            donateView.add(spacer);

            switcher.replaceView(0, donateView);
            switcher.setView(0);
            factory.build(new ArrayList<>(), null, guiLeft, guiTop, xSize, ySize);

            String label = "w=" + w + " h=" + h;
            assertTrue(label + ": content row must be positioned",
                    contentRow.getComputedY() > 0 && contentRow.getComputedHeight() > 0);
            assertTrue(label + ": buttons must be positioned",
                    buttonsCol.getComputedY() > 0 && buttonsCol.getComputedHeight() > 0);
            assertTrue(label + ": status area bottom must stay inside content area",
                    switcher.getComputedY() + switcher.getComputedHeight() <= guiTop + ySize);
            assertTrue(label + ": content row must be inside content area",
                    contentRow.getComputedY() + contentRow.getComputedHeight() <= guiTop + ySize);
        }
    }

    @Test
    public void clickingTextFieldUnfocusesOtherTextFields() {
        ViewFactory factory = createFactory();

        FakeTextField first = new FakeTextField(0, GUI_LEFT + 100, GUI_TOP + 100);
        FakeTextField second = new FakeTextField(1, GUI_LEFT + 300, GUI_TOP + 100);
        factory.addTextField(first);
        factory.addTextField(second);
        factory.add(new ClickConsumingStub());

        factory.mouseClicked(GUI_LEFT + 350, GUI_TOP + 110, 0);
        assertEquals("every text field must receive the click even when an element consumes it",
                1, first.getClicks());
        assertEquals("every text field must receive the click even when an element consumes it",
                1, second.getClicks());
        assertTrue("clicked field must gain focus", second.isFocused());
        assertFalse("other field must lose focus", first.isFocused());

        factory.mouseClicked(GUI_LEFT + 150, GUI_TOP + 110, 0);
        assertTrue("previously clicked field must regain focus", first.isFocused());
        assertFalse("other field must lose focus", second.isFocused());
    }

    @Test
    public void switchingViewsRebuildsWithoutDuplicatingWidgets() {
        ColumnElement setsView = new ColumnElement().gap(8);
        setsView.add(new Stub(0, 124).widthPercent(100));
        Stub panels = new Stub(0, 0).flexible(true);
        setsView.add(panels);

        ColumnElement donateView = new ColumnElement().gap(8);
        donateView.add(new Stub(0, 48).widthPercent(100));
        ColumnElement buttonsCol = new ColumnElement().align(Alignment.LEFT).gap(12);
        buttonsCol.button(1000, "btc").width(CONTENT_WIDTH - 76).height(20);
        donateView.add(buttonsCol);

        ViewFactory factory = createFactory();
        ViewSwitcherElement switcher = addRoot(factory, setsView);

        List<GuiButton> buttonList = new ArrayList<>();
        factory.build(buttonList, null, GUI_LEFT, GUI_TOP, X_SIZE, Y_SIZE);
        assertEquals(0, buttonList.size());

        switcher.replaceView(1, donateView);
        switcher.setView(1);
        buttonList.clear();
        factory.build(buttonList, null, GUI_LEFT, GUI_TOP, X_SIZE, Y_SIZE);
        assertEquals(1, buttonList.size());

        buttonList.clear();
        factory.build(buttonList, null, GUI_LEFT, GUI_TOP, X_SIZE, Y_SIZE);
        assertEquals(1, buttonList.size());

        assertInside(buttonsCol, donateView.getComputedX(), donateView.getComputedY(),
                donateView.getComputedX() + donateView.getComputedWidth(),
                donateView.getComputedY() + donateView.getComputedHeight());
    }
}
