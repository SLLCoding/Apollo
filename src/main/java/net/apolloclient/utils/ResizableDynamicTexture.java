package net.apolloclient.utils;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class ResizableDynamicTexture extends DynamicTexture {
    public ResizableDynamicTexture(BufferedImage bufferedImage, int texWidth, int texHeight) {
        super(texWidth, texHeight);
        bufferedImage.getRGB(0, 0, texWidth, texHeight, super.getTextureData(), 0, texWidth);
        this.updateDynamicTexture();
    }
}
