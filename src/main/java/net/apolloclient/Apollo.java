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

package net.apolloclient;

import java.text.SimpleDateFormat;
import java.util.Date;

/* Main class for Apollo; holds all instances used through out client */
public class Apollo {

    // Apollo information used for display title and credits info.
    public static final String NAME = "Apollo", VERSION = "b0.1", MCVERSION = "1.8.9";

    // Public client instance used to retrieve any aspect of Apollo.
    public static final Apollo instance = new Apollo();

    // Main constructor used to instantiate all aspects of Apollo.
    public Apollo() { }

    /** Log Apollo instance stats after construction. **/
    public void postInitialisation() { log("Apollo Initiation Finished with 0 Modules and 0 Settings!"); }

    /** Used to log Apollo messages to console
     * @param message any string to be displayed in console. **/
    public static void log (String... message) { for (String out : message)  System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] [Apollo] " + out); }

}
