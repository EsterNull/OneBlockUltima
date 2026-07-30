package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

public class GridElement extends ViewElement {
    public interface CellRenderer {
        void draw(int x, int y, int cellWidth, int cellHeight, int row, int col,
                  boolean hovered, boolean selected, FontRenderer fr, int mouseX, int mouseY, float partialTicks);
    }

    public interface CellClickHandler {
        boolean onClick(int row, int col, int mouseX, int mouseY, int mouseButton);
    }

    private final int rows;
    private final int cols;
    private int cellSize = 16;
    private int cellGap = 3;
    private CellRenderer cellRenderer;
    private CellClickHandler clickHandler;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int hoveredRow = -1;
    private int hoveredCol = -1;

    public GridElement(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    public GridElement cellSize(int size) {
        this.cellSize = size;
        return this;
    }

    public GridElement cellGap(int gap) {
        this.cellGap = gap;
        return this;
    }

    public GridElement renderer(CellRenderer renderer) {
        this.cellRenderer = renderer;
        return this;
    }

    public GridElement clickHandler(CellClickHandler handler) {
        this.clickHandler = handler;
        return this;
    }

    public GridElement select(int row, int col) {
        this.selectedRow = row;
        this.selectedCol = col;
        return this;
    }

    public int getSelectedRow() { return selectedRow; }
    public int getSelectedCol() { return selectedCol; }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        hoveredRow = -1;
        hoveredCol = -1;

        int actualCellSize = getActualCellSize();
        int totalW = cols * actualCellSize + (cols - 1) * cellGap;
        int totalH = rows * actualCellSize + (rows - 1) * cellGap;
        int startX = computedX + (computedWidth - totalW) / 2;
        int startY = computedY + (computedHeight - totalH) / 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cx = startX + c * (actualCellSize + cellGap);
                int cy = startY + r * (actualCellSize + cellGap);
                boolean hovered = mouseX >= cx && mouseX < cx + actualCellSize &&
                                  mouseY >= cy && mouseY < cy + actualCellSize;
                boolean selected = r == selectedRow && c == selectedCol;

                if (hovered) {
                    hoveredRow = r;
                    hoveredCol = c;
                }

                if (cellRenderer != null) {
                    cellRenderer.draw(cx, cy, actualCellSize, actualCellSize, r, c, hovered, selected, fr, mouseX, mouseY, partialTicks);
                } else {
                    int bg = selected ? 0xFF00EE00 : (hovered ? 0xFF3F5060 : 0xFF2A2F34);
                    Gui.drawRect(cx, cy, cx + actualCellSize, cy + actualCellSize, bg);
                }
            }
        }
    }

    private int getActualCellSize() {
        if (computedWidth <= 0 || cellSize <= 0) return Math.max(4, cellSize);
        int fromWidth = (computedWidth - (cols - 1) * cellGap) / cols;
        return Math.max(cellSize, fromWidth);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (clickHandler == null) return false;

        int actualCellSize = getActualCellSize();
        int totalW = cols * actualCellSize + (cols - 1) * cellGap;
        int totalH = rows * actualCellSize + (rows - 1) * cellGap;
        int startX = computedX + (computedWidth - totalW) / 2;
        int startY = computedY + (computedHeight - totalH) / 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cx = startX + c * (actualCellSize + cellGap);
                int cy = startY + r * (actualCellSize + cellGap);
                if (mouseX >= cx && mouseX < cx + actualCellSize && mouseY >= cy && mouseY < cy + actualCellSize) {
                    return clickHandler.onClick(r, c, mouseX, mouseY, mouseButton);
                }
            }
        }
        return false;
    }

    @Override
    public int getPreferredWidth() {
        return cols * cellSize + (cols - 1) * cellGap;
    }

    @Override
    public int getPreferredHeight() {
        return rows * cellSize + (rows - 1) * cellGap;
    }
}
