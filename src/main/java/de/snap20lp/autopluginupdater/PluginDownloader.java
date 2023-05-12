package de.snap20lp.autopluginupdater;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;

public class PluginDownloader {
    private final UpdatePlugin updatePlugin;

    public PluginDownloader(UpdatePlugin updatePlugin) {
        this.updatePlugin = updatePlugin;
        if (Bukkit.getPluginManager().getPlugin(updatePlugin.name()) == null || Core.getInstance().getConfig().getStringList("ignoredPlugins").contains(updatePlugin.name()))
            return;
        String resourceID = updatePlugin.url().split("\\.")[3].replaceAll("[^0-9]", "");
        logMessage("Checking " + updatePlugin.name() + " with resourceID: " + resourceID);

        final File[] possiblePluginFile = {null};
        Core.getInstance().pluginToRealPluginNameMap.forEach((s, s2) -> {
            if (s2.equals(updatePlugin.name())) {
                File file = new File(s);
                if (file.length() / 1024 / 1024 > Core.getInstance().getConfig().getInt("maxSizeToCheck")) {
                    logMessage("§cPlugin is too big to check! (maxSizeToCheck in config)");
                    return;
                }
                if (file.length() == 0)
                    return;
                possiblePluginFile[0] = file;
            }
        });

        if (possiblePluginFile[0] == null) {
            logMessage("§cNo plugin file found to compare hash!");
            return;
        }

        File tempDownloaded = new File(Core.getInstance().getTempFolder().toString() + "\\" + updatePlugin.name() + "-temp.jar");
        try {
            InputStream in = new URL("https://api.spiget.org/v2/resources/" + resourceID + "/download").openStream();
            Files.copy(in, Paths.get(tempDownloaded.getPath()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logMessage("§cCannot download resource for update check (Is this plugin pointing at the right SpigotURL?) | " + e.getMessage());
            return;
        }

        if (!tempDownloaded.exists()) {
            logMessage("§cCannot download resource for update check (Is this plugin pointing at the right SpigotURL?)");
        }

        try {
            if (!HashHelper.compareFileHash(possiblePluginFile[0], tempDownloaded)) {
                Bukkit.getPluginManager().getPlugin(updatePlugin.name()).getPluginLoader().disablePlugin(Bukkit.getPluginManager().getPlugin(updatePlugin.name()));
                logMessage("§eUpdate detected! Replacing with new version...");

                Files.write(possiblePluginFile[0].toPath(), new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                try {
                    String fileName = updatePlugin.name() + "-Updated-" + System.currentTimeMillis() / 1000 + ".jar";
                    Path of = Path.of("plugins/" + fileName);
                    Files.move(tempDownloaded.toPath(), of, StandardCopyOption.REPLACE_EXISTING);
                    logMessage("§aSuccessfully updated!");
                    if (Core.getInstance().getConfig().getBoolean("autoReEnablePlugins"))
                        Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().loadPlugin(of.toFile()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                logMessage("§aNo updates detected! " + updatePlugin.name() + " is up to date :)");
            }
        } catch (Exception e) {
            logMessage("§cError while updating! " + e.getMessage());
        }
    }

    private void logMessage(String log) {
        Bukkit.getConsoleSender().sendMessage("§6 [" + updatePlugin.name() + "-Updater] " + log);
    }

}
