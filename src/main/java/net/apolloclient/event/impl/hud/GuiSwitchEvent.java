/*⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤
Copyright (C) 2020-2021 developed by Icovid and Apollo Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see https://www.gnu.org/licenses/.

Contact: Icovid#3888 @ https://discord.com
⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤*/

package net.apolloclient.event.impl.hud;

import net.apolloclient.event.Event;
import net.minecraft.client.gui.GuiScreen;

/**
 * Fired when current {@link GuiScreen} is changed.
 *
 * @author MatthewTGM | MatthewTGM#4058
 * @since 1.0.0
 */
public class GuiSwitchEvent extends Event {

  public final GuiScreen guiScreen;

  /** @param guiScreen the GuiScreen. */
  public GuiSwitchEvent(GuiScreen guiScreen) {
    this.guiScreen = guiScreen;
  }
}
