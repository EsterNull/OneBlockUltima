package ru.defea.oneblockultima.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public class RenderItemGeneratorCube implements IItemRenderer
{
    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type)
    {
        return type == ItemRenderType.INVENTORY || type == ItemRenderType.ENTITY
                || type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper)
    {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data)
    {
        Block block = Block.getBlockFromItem(item.getItem());
        if (block == null)
        {
            return;
        }

        int meta = item.getMetadata();
        RenderBlocks renderBlocks = RenderBlocks.getInstance();
        if (data != null && data.length > 0 && data[0] instanceof RenderBlocks)
        {
            renderBlocks = (RenderBlocks) data[0];
        }

        block.setBlockBoundsForItemRender();
        renderBlocks.setRenderBoundsFromBlock(block);

        Tessellator tessellator = Tessellator.instance;

        GL11.glPushMatrix();

        switch (type)
        {
            case INVENTORY:
                // Forge already applied the vanilla isometric inventory transform
                // (scale 10, flip, rotations). Only center the cube like
                // RenderBlocks.renderBlockAsItem does.
                GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
                break;
            case ENTITY:
                // Forge scales a non-3D block item down to 0.5 in the world.
                // Scale back up to a full block; Forge applies the continuous
                // ENTITY_ROTATION spin, so no fixed angle is needed here.
                GL11.glScalef(2.0F, 2.0F, 2.0F);
                GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
                break;
            case EQUIPPED:
            case EQUIPPED_FIRST_PERSON:
                // Forge already translated by (-0.5, -0.5, -0.5) to center the cube.
                break;
            default:
                GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
                break;
        }

        renderFace(tessellator, renderBlocks, block, meta, 0, 0, -1, 0);
        renderFace(tessellator, renderBlocks, block, meta, 1, 0, 1, 0);
        renderFace(tessellator, renderBlocks, block, meta, 2, 0, 0, -1);
        renderFace(tessellator, renderBlocks, block, meta, 3, 0, 0, 1);
        renderFace(tessellator, renderBlocks, block, meta, 4, -1, 0, 0);
        renderFace(tessellator, renderBlocks, block, meta, 5, 1, 0, 0);

        GL11.glPopMatrix();
    }

    private void renderFace(Tessellator tessellator, RenderBlocks renderBlocks, Block block, int meta,
                            int side, float nx, float ny, float nz)
    {
        tessellator.startDrawingQuads();
        tessellator.setNormal(nx, ny, nz);
        switch (side)
        {
            case 0:
                renderBlocks.renderFaceYNeg(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, side, meta));
                break;
            case 1:
                renderBlocks.renderFaceYPos(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, side, meta));
                break;
            case 2:
                renderBlocks.renderFaceZNeg(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, side, meta));
                break;
            case 3:
                renderBlocks.renderFaceZPos(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, side, meta));
                break;
            case 4:
                renderBlocks.renderFaceXNeg(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, side, meta));
                break;
            case 5:
                renderBlocks.renderFaceXPos(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, side, meta));
                break;
        }
        tessellator.draw();
    }
}