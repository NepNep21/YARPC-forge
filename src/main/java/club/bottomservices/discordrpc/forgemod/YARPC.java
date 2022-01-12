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

import club.bottomservices.discordrpc.lib.*;
import club.bottomservices.discordrpc.lib.exceptions.NoDiscordException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;

@Mod("yarpc")
public class YARPC {
    private final RichPresence.Builder builder = new RichPresence.Builder().setTimestamps(System.currentTimeMillis() / 1000, null);
    private final Logger logger = LogManager.getLogger();

    private DiscordRPCClient client = null;
    private boolean shouldWork = false;
    private byte tickTimer = 0;

    public YARPC() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CONFIG);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        if (!Config.IS_ENABLED.get()) {
            return;
        }
        shouldWork = true;
        client = new DiscordRPCClient(new EventListener() {
            // Log4j adds a shutdown hook that stops the logging system, since the discord connection is also closed in a shutdown
            // hook and the run order isn't guaranteed, logging the connection closing is pointless
            @Override
            public void onReady(@Nonnull DiscordRPCClient client, @Nonnull User user) {
                logger.info("DiscordRPC ready");
                client.sendPresence(builder.build());
            }

            @Override
            public void onError(@Nonnull DiscordRPCClient client, IOException exception, ErrorEvent event) {
                if (exception != null) {
                    logger.error("DiscordRPC error with IOException", exception);
                } else if (event != null) {
                    logger.error("DiscordRPC error with ErrorEvent code {} and message {}", event.code, event.message);
                }
            }
        }, Config.APP_ID.get());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client.isConnected) {
                client.disconnect();
            }
        }, "YARPC Shutdown Hook"));

        try {
            client.connect();
        } catch (NoDiscordException e) {
            logger.error("Failed initial discord connection", e);
        }

        new Thread(() -> {
            while (true) {
                if (client.isConnected) {
                    client.sendPresence(builder.build());
                } else {
                    try {
                        client.connect();
                    } catch (NoDiscordException e) {
                        // Don't want to spam logs
                    }
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "YARPC Update Thread").start();
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        // Only run every 4 seconds
        if (shouldWork && ++tickTimer % 80 == 0 && event.phase == TickEvent.Phase.END) {
            tickTimer = 0;
            String largeImage = null;
            var minecraft = Minecraft.getInstance();
            var level = minecraft.level;
            String dimensionPath = "Main Menu";
            if (level != null) {
                String largeImageConfig = Config.LARGE_IMAGE.get();
                dimensionPath = level.dimension().location().toString();
                largeImage = largeImageConfig.isEmpty() ? dimensionPath : largeImageConfig;
            }
            String smallImage = Config.SMALL_IMAGE.get();
            smallImage = smallImage.isEmpty() ? null : smallImage;
            String smallText = Config.SMALL_TEXT.get();
            smallText = smallText.isEmpty() ? null : smallText;
            builder.setAssets(largeImage == null ? null : largeImage.replace(':', '_'), Config.LARGE_TEXT.get(), smallImage, smallText);

            String text = Config.DETAILS_FORMAT.get() + "\n" + Config.STATE_FORMAT.get();
            String placeholder = "%s";
            var player = minecraft.player;
            for (var arg : Config.FORMAT_ARGS.get()) {
                // Not redundant
                switch ((String) arg) {
                    case "DIMENSION" -> text = text.replaceFirst(placeholder, dimensionPath);
                    case "USERNAME" -> text = text.replaceFirst(placeholder, minecraft.getUser().getName());
                    case "HEALTH" -> text = text.replaceFirst(placeholder, "Health " + (player != null ? player.getHealth() : "0.0"));
                    case "HUNGER" -> text = text.replaceFirst(placeholder, "Food " + (player != null ? player.getFoodData().getFoodLevel() : "0"));
                    case "SERVER" -> {
                        ServerData currentServer = minecraft.getCurrentServer();
                        if (currentServer != null) {
                            text = text.replaceFirst(placeholder, currentServer.ip);
                        } else if (minecraft.isLocalServer()) {
                            text = text.replaceFirst(placeholder, "Singleplayer");
                        } else {
                            text = text.replaceFirst(placeholder, "Main Menu");
                        }
                    }
                    case "HELD_ITEM" -> text = text.replaceFirst(placeholder, "Holding " + (player != null ? player.getMainHandItem().getHoverName().getString() : "Air"));
                }
            }
            String[] split = text.split("\n");
            builder.setText(split[0], split[1]);
        }
    }
}
