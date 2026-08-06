package ru.defea.oneblockultima.gui.layout;

import static ru.defea.oneblockultima.Constants.*;

/**
 * Красная кнопка-«опасность» для разрушающих действий (удаление).
 */
public class DangerButtonElement extends ButtonElement<DangerButtonElement>
{
    public DangerButtonElement(int id, String text)
    {
        super(id, text);
    }

    @Override
    protected int widgetFillColor(boolean hovered)
    {
        if (!enabled) return DISABLED_BUTTON_FILL;
        return hovered ? DARK_RED_COLOR_3 : DARK_RED_COLOR_1;
    }

    @Override
    protected int widgetTextColor(boolean hovered)
    {
        if (!enabled) return DISABLED_BUTTON_TEXT;
        return REDDISH_COLOR;
    }
}
