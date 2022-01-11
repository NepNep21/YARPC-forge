/*
Copyright (C) 2022 Nep Nep

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

Additional permission under GNU GPL version 3 section 7

If you modify this Program, or any covered work, by linking or combining it with Minecraft
(or a modified version of that library), containing parts covered by the terms of the Minecraft End User License Agreement,
the licensors of this Program grant you additional permission to convey the resulting work.
*/

package club.bottomservices.discordrpc.forgemod;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

import static net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import static net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class Config {
    private static final List<String> VALID_OPTIONS = List.of(
            "DIMENSION",
            "USERNAME",
            "HEALTH",
            "HUNGER",
            "SERVER",
            "HELD_ITEM"
    );

    public static final ForgeConfigSpec CONFIG;
    public static final BooleanValue IS_ENABLED;
    public static final ConfigValue<String> STATE_FORMAT;
    public static final ConfigValue<String> DETAILS_FORMAT;
    public static final ConfigValue<String> APP_ID;
    public static final ConfigValue<List<? extends String>> FORMAT_ARGS;
    public static final ConfigValue<String> LARGE_TEXT;
    public static final ConfigValue<String> SMALL_TEXT;
    public static final ConfigValue<String> LARGE_IMAGE;
    public static final ConfigValue<String> SMALL_IMAGE;

    static {
        var builder = new ForgeConfigSpec.Builder();

        IS_ENABLED = builder
                .comment("Whether this mod is enabled")
                .define("isEnabled", true);
        STATE_FORMAT = builder
                .comment("Format for the state line, use %s as a placeholder, up to 2 placeholders are allowed")
                .define("stateFormat", "%s | %s");
        DETAILS_FORMAT = builder
                .comment("Format for the details line, use %s as a placeholder, up to 2 placeholders are allowed")
                .define("detailsFormat", "%s | %s");
        APP_ID = builder
                .comment("Application id from discord for using custom assets, see https://discord.com/developers/applications/")
                .define("appId", "928401525842259979");
        FORMAT_ARGS = builder
                .comment("List of format arguments (DIMENSION, USERNAME, HEALTH, HUNGER, SERVER, HELD_ITEM)")
                .defineList("formatArgs", List.of(
                        VALID_OPTIONS.get(1), VALID_OPTIONS.get(2), VALID_OPTIONS.get(3), VALID_OPTIONS.get(0)
                ), object -> object instanceof String && VALID_OPTIONS.contains((String) object));
        LARGE_TEXT = builder
                .comment("Text for the large image")
                .define("largeText", "Playing minecraft");
        SMALL_TEXT = builder
                .comment("Text for the small image")
                .define("smallText", "With YARPC");
        LARGE_IMAGE = builder
                .comment("Key for the large image (Only change this if using a custom application!)")
                .define("largeImage", "");
        SMALL_IMAGE = builder
                .comment("Key for the small image (Only change this if using a custom application!)")
                .define("smallImage", "");

        CONFIG = builder.build();
    }
}
