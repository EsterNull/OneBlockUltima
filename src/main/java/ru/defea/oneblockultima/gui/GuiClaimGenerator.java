package ru.defea.oneblockultima.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.LabelElement;
import ru.defea.oneblockultima.gui.layout.SpacerElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.network.PacketOneBlockAction;

public class GuiClaimGenerator extends GuiScreen
{
    private static final int BUTTON_CLAIM = 0;

    private final BlockPos generatorPos;
    private ViewFactory factory;

    public GuiClaimGenerator(EntityPlayer player, World world, BlockPos generatorPos)
    {
        this.generatorPos = generatorPos;
    }

    @Override
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        buildView();
    }

    private void buildView()
    {
        factory = new ViewFactory(width, height)
                .margin(8).padding(4)
                .gap(6)
                .align(Alignment.CENTER)
                .centerVertical()
                .panel(0xCC22272E, 0xFF3A3F44);

        factory.title(I18n.format("gui.oneblockultima.claim_title"));
        factory.add(new LabelElement(I18n.format("gui.oneblockultima.claim_description")).color(0xCCCCCC).centered(true));
        factory.add(new SpacerElement(12));
        factory.button(BUTTON_CLAIM, I18n.format("gui.oneblockultima.claim_owner"));

        factory.build(buttonList, fontRenderer);
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        factory.actionPerformed(button);

        if (button.id == BUTTON_CLAIM)
        {
            ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.CLAIM_OWNER, ""));
            mc.player.closeScreen();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        factory.draw(fontRenderer, mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() { return true; }
}
