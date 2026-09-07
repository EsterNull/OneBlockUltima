package ru.defea.oneblockultima.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import ru.defea.oneblockultima.gui.containers.ContainerClaimGenerator;
import ru.defea.oneblockultima.gui.layout.Alignment;
import ru.defea.oneblockultima.gui.layout.LabelElement;
import ru.defea.oneblockultima.gui.layout.SpacerElement;
import ru.defea.oneblockultima.gui.layout.ViewFactory;
import ru.defea.oneblockultima.network.ModMessages;
import ru.defea.oneblockultima.network.PacketOneBlockAction;

import static ru.defea.oneblockultima.Constants.LIGHT_GRAY_COLOR_1;

public class GuiClaimGenerator extends AbstractContainerScreen<ContainerClaimGenerator>
{
    private static final int BUTTON_CLAIM = 0;

    private final BlockPos generatorPos;
    private ViewFactory factory;

    public GuiClaimGenerator(ContainerClaimGenerator container, Inventory inv, Component title)
    {
        super(container, inv, title);
        this.generatorPos = container.getGeneratorPos();
        this.imageWidth = 220;
        this.imageHeight = 120;
    }

    @Override
    public void init()
    {
        buildView();
    }

    private void buildView()
    {
        factory = new ViewFactory(this.width, this.height)
                .margin(8).padding(4)
                .gap(6)
                .align(Alignment.CENTER)
                .centerVertical();

        factory.title(I18n.get("gui.oneblockultima.claim_title"));
        factory.add(new LabelElement(I18n.get("gui.oneblockultima.claim_description")).color(LIGHT_GRAY_COLOR_1).centered());
        factory.add(new SpacerElement(12));
        factory.button(BUTTON_CLAIM, I18n.get("gui.oneblockultima.claim_owner"))
                .onPress(() -> {
                    ModMessages.sendToServer(new PacketOneBlockAction(generatorPos, PacketOneBlockAction.Action.CLAIM_OWNER, ""));
                    this.minecraft.setScreen(null);
                });

        factory.build(this, this.font);
        for (net.minecraft.client.gui.components.AbstractWidget w : factory.getWidgets()) {
            this.addRenderableWidget(w);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY)
    {
        if (factory != null)
        {
            factory.draw(g, this.font, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY)
    {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks)
    {
        super.render(g, mouseX, mouseY, partialTicks);
    }
}
