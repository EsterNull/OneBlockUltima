package ru.defea.oneblockultima.gui.layout;

import static ru.defea.oneblockultima.Constants.*;

/**
 * Green button for positive/saving actions.
 */
public class SuccessButtonElement extends ButtonElement<SuccessButtonElement>
{
    public SuccessButtonElement(int id, String text)
    {
        super(id, text);
    }

    @Override
    protected int widgetFillColor(boolean hovered)
    {
        if (!enabled) return DISABLED_BUTTON_FILL;
        return hovered ? GREEN : DARK_GREEN;
    }

    @Override
    protected int widgetTextColor(boolean hovered)
    {
        if (!enabled) return DISABLED_BUTTON_TEXT;
        return SUCCESS_COLOR;
    }
}
