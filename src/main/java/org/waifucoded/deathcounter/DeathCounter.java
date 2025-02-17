package org.waifucoded.deathcounter;

import com.google.inject.Inject;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Plugin("deathcounter")
public class DeathCounter {

    private final PluginContainer container;
    private final Logger logger;
    private final Map<UUID, Integer> deathCounts = new HashMap<>();
    private final Path dataFile;

    @Inject
    public DeathCounter(PluginContainer container, Logger logger, @DefaultConfig(sharedRoot = false) Path dataFile) {
        this.container = container;
        this.logger = logger;
        this.dataFile = dataFile;
    }

    @Listener
    public void onConstructPlugin(final ConstructPluginEvent event) {
        this.logger.info("Deathcounter plugin is loading...");
        loadDeathCounts();
    }

    @Listener
    public void onPlayerDeath(final DestructEntityEvent.Death event) {
        if (!(event.entity() instanceof ServerPlayer)) {
            return; // Ignore non-player entities
        }

        // Explicitly cast the entity to a ServerPlayer
        ServerPlayer player = (ServerPlayer) event.entity();

        // Get the player's UUID and increment their death count
        UUID playerId = player.uniqueId();
        int newDeathCount = deathCounts.getOrDefault(playerId, 0) + 1;
        deathCounts.put(playerId, newDeathCount);

        // Log the updated death count
        logger.info("{} has now died {} times.", player.name(), newDeathCount);

        // Persist the updated data to the file
        saveDeathCounts();
    }

    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) {
        Parameter.Value<ServerPlayer> targetPlayer = Parameter.player().optional().key("player").build();

        event.register(this.container, Command.builder()
                .addParameter(targetPlayer)
                .executor(ctx -> {
                    ServerPlayer sender = ctx.cause().first(ServerPlayer.class).orElse(null);
                    if (sender == null) {
                        return CommandResult.error(Component.text("You must be a player to use this command!"));
                    }

                    // Get the target player or default to sender
                    ServerPlayer target = ctx.one(targetPlayer).orElse(sender);

                    // Ensure the target exists in the deathCounts map
                    if (!deathCounts.containsKey(target.uniqueId())) {
                        sender.sendMessage(Identity.nil(), Component.text(
                                target.name() + " has not died yet.",
                                NamedTextColor.YELLOW
                        ));
                        return CommandResult.success();
                    }

                    int deaths = deathCounts.getOrDefault(target.uniqueId(), 0);
                    sender.sendMessage(Identity.nil(), Component.text(
                            target.name() + " has died " + deaths + " times.",
                            NamedTextColor.RED
                    ));
                    return CommandResult.success();
                })
                .build(), "deaths");
    }

    private void loadDeathCounts() {
        if (!Files.exists(dataFile)) {
            return;
        }

        try {
            InputStream is = Files.newInputStream(dataFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String dataJson = br.lines().collect(Collectors.joining("\n"));
            br.close();

            DataContainer data = DataFormats.JSON.get().read(dataJson);
            for (DataQuery key : data.keys(false)) {
                deathCounts.put(UUID.fromString(key.toString()), data.getInt(key).orElse(0));
            }
        } catch (IOException e) {
            logger.error("Failed to load death counts!", e);
        }
    }

    // Add the saveDeathCounts method here
    private void saveDeathCounts() {
        try {
            // Convert deathCounts map to a DataContainer
            DataContainer data = DataContainer.createNew();
            for (Map.Entry<UUID, Integer> entry : deathCounts.entrySet()) {
                data.set(DataQuery.of(entry.getKey().toString()), entry.getValue());
            }

            // Write the DataContainer to the file in JSON format
            String jsonData = DataFormats.JSON.get().write(data);
            Files.write(dataFile, jsonData.getBytes());

            logger.info("Successfully saved death counts to file.");
        } catch (IOException e) {
            logger.error("Failed to save death counts!", e);
        }
    }
}
