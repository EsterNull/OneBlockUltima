package ru.defea.oneblockultima.gui.layout;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

import java.util.ArrayList;
import java.util.List;

import static ru.defea.oneblockultima.Constants.*;

public class TabBarElement extends ViewElement {
    private final List<Tab> tabs = new ArrayList<>();
    private int activeTab = 0;

    public static class Tab {
        public final String label;
        public final int id;
        public boolean visible = true;

        public Tab(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public Tab visible(boolean visible) {
            this.visible = visible;
            return this;
        }
    }

    public TabBarElement tab(int id, String label) {
        tabs.add(new Tab(id, label));
        return this;
    }

    public TabBarElement activeTab(int id) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id == id) {
                this.activeTab = i;
                break;
            }
        }
        return this;
    }

    public int getActiveTabId() {
        if (activeTab >= 0 && activeTab < tabs.size()) return tabs.get(activeTab).id;
        return -1;
    }

    public int getActiveTabIndex() {
        return activeTab;
    }

    public Tab getTab(int index) {
        if (index >= 0 && index < tabs.size()) return tabs.get(index);
        return null;
    }

    @Override
    public void createWidgets(List<GuiButton> buttonList, FontRenderer fontRenderer, ViewFactory factory) {
    }

    @Override
    public void draw(FontRenderer fr, int mouseX, int mouseY, float partialTicks) {
        int visibleCount = 0;
        for (Tab tab : tabs) {
            if (tab.visible) visibleCount++;
        }
        if (visibleCount == 0) return;

        int tabWidth = computedWidth / visibleCount;
        int cx = computedX;
        for (Tab tab : tabs) {
            if (!tab.visible) continue;

            boolean active = tabs.indexOf(tab) == activeTab;
            boolean hovered = mouseX >= cx && mouseX < cx + tabWidth &&
                              mouseY >= computedY && mouseY < computedY + computedHeight;

            int bg;
            if (active) bg = DARK_BLUE_GRAY_COLOR_1;
            else if (hovered) bg = DARK_GRAY_COLOR_2;
            else bg = PANEL_COLOR;
            Gui.drawRect(cx, computedY, cx + tabWidth, computedY + computedHeight, bg);

            int textColor = active ? WHITE_COLOR_1 : LIGHT_BLUE_GRAY_COLOR;
            fr.drawStringWithShadow(tab.label,
                cx + (tabWidth - fr.getStringWidth(tab.label)) / 2.0f,
                computedY + (computedHeight - 8) / 2.0f, textColor);

            cx += tabWidth;
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseY < computedY || mouseY >= computedY + computedHeight) return false;

        int visibleCount = 0;
        for (Tab tab : tabs) {
            if (tab.visible) visibleCount++;
        }
        if (visibleCount == 0) return false;

        int tabWidth = computedWidth / visibleCount;
        int cx = computedX;
        int idx = 0;
        for (Tab tab : tabs) {
            if (!tab.visible) { idx++; continue; }
            if (mouseX >= cx && mouseX < cx + tabWidth) {
                activeTab = idx;
                return true;
            }
            cx += tabWidth;
            idx++;
        }
        return false;
    }

    @Override
    public int getPreferredWidth() {
        return 300;
    }

    @Override
    public int getPreferredHeight() {
        return 20;
    }
}
