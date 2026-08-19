package ru.defea.oneblockultima.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.LabelElement;
import ru.defea.oneblockultima.gui.layout.SpacerElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.network.PacketOneBlockAction;

import javax.annotation.Nonnull;

import static ru.defea.oneblockultima.Constants.LIGHT_GRAY_COLOR_1;

public class GuiClaimGenerator extends GuiScreen
{
    private static final int BUTTON_CLAIM = 0;

    private final int generatorX;
    private final int generatorY;
    private final int generatorZ;
    private ViewFactory factory;

    public GuiClaimGenerator(int generatorX, int generatorY, int generatorZ)
    {
        this.generatorX = generatorX;
        this.generatorY = generatorY;
        this.generatorZ = generatorZ;
    }

    @Override
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        buildView();
    }

    @SuppressWarnings("unchecked")
    private void buildView()
    {
        factory = new ViewFactory(width, height)
                .margin(8).padding(4)
                .gap(6)
                .align(Alignment.CENTER)
                .centerVertical();

        factory.title(I18n.format("gui.oneblockultima.claim_title"));
        factory.add(new LabelElement(I18n.format("gui.oneblockultima.claim_description")).color(LIGHT_GRAY_COLOR_1).centered());
        factory.add(new SpacerElement(12));
        factory.button(BUTTON_CLAIM, I18n.format("gui.oneblockultima.claim_owner"));

        factory.build(buttonList, fontRendererObj);
    }

    @Override
    protected void actionPerformed(@Nonnull GuiButton button)
    {
        factory.actionPerformed(button);

        if (button.id == BUTTON_CLAIM)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorX, generatorY, generatorZ, PacketOneBlockAction.Action.CLAIM_OWNER, ""));
            mc.thePlayer.closeScreen();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        factory.draw(fontRendererObj, mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

}
