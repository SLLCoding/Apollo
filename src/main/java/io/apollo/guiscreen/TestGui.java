/*
 * ****************************************************************
 *  Copyright (C) 2020-2021 developed by Icovid
 *
 *  TestGui.java is part of Apollo Client. [8/29/20, 11:13 AM]
 *
 *  TestGui.java can not be copied and/or distributed without the express
 *  permission of Icovid
 *
 *  Contact: Icovid#3888
 * ****************************************************************
 */

package io.apollo.guiscreen;

import io.apollo.utils.ApolloFontRenderer;
import io.apollo.utils.DrawUtils;
import io.apollo.utils.GLRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.newdawn.slick.SlickException;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

public class TestGui extends GuiScreen {

    public void updateScreen() { if (mc.theWorld != null) super.updateScreen();}

    public ApolloFontRenderer apolloFontRenderer;

    /* Called when Gui is Opened in Screen size is Changes */
    public void initGui() {
        try {
            apolloFontRenderer = new ApolloFontRenderer(ApolloFontRenderer.ROBOTO, 20);
        } catch (FontFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SlickException e) {
            e.printStackTrace();
        }
    }

    /* Called Every Tick when Gui is Open - Used for Render Elements */
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        DrawUtils.drawHallowCircle(100, 100, 50, Color.BLUE, 5);
        DrawUtils.drawCircle(100, 100, 25, Color.CYAN);
        apolloFontRenderer.drawString("Apollo Client", 100, 200, new Color(255, 255, 255, 255));
    }
}
