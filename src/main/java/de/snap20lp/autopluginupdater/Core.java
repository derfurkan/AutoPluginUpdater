package de.snap20lp.autopluginupdater;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import de.snap20lp.autopluginupdater.commands.Update;
import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;


public class Core extends JavaPlugin {

    private static Core instance;
    private final File tempFolder = new File(getDataFolder(), "AutoPluginUpdater-Temp");
    private final File updatePluginsFile = new File(getDataFolder(), "pluginSources.json");
    private final Gson gson = new Gson().newBuilder().setPrettyPrinting().create();
    public HashMap<String, String> pluginToRealPluginNameMap = new HashMap<>();

    private List<UpdatePlugin> updatePlugins;

    public static Core getInstance() {
        return instance;
    }

    public static <T> List<T> stringToArray(String s, Class<T[]> clazz) {
        if (s == null || getInstance().gson.fromJson(s, clazz) == null)
            return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(getInstance().gson.fromJson(s, clazz)));
    }

    public File getTempFolder() {
        return tempFolder;
    }

    private void prepareFiles() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            if (!updatePluginsFile.exists()) {
                updatePluginsFile.createNewFile();
            }
            if (tempFolder.exists())
                FileUtils.cleanDirectory(tempFolder);
            else
                tempFolder.mkdirs();
            tempFolder.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadUpdatePluginsFromJson() {
        try {
            updatePlugins = stringToArray(Files.readString(updatePluginsFile.toPath()), UpdatePlugin[].class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeUpdatePluginsToJson() {
        try {
            updatePluginsFile.delete();
            updatePluginsFile.createNewFile();
            Files.writeString(updatePluginsFile.toPath(), gson.toJson(updatePlugins));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void generatePluginNameMap() {
        Bukkit.getConsoleSender().sendMessage("§eGenerating pluginMap... (This may take a while)");
        pluginToRealPluginNameMap.clear();
        for (File pluginFile : new File("plugins").listFiles()) {
            if (!pluginFile.isFile() || !pluginFile.getName().endsWith(".jar") || pluginFile.length() / 1024 / 1024 > getConfig().getInt("maxSizeToCheck")) {
                continue;
            }

            try {
                ZipFile zipFile = new ZipFile(pluginFile);
                if (zipFile.isEncrypted()) {
                    continue;
                }
                zipFile.extractFile("plugin.yml", tempFolder.getAbsolutePath());
                File pluginYml = new File(tempFolder, "plugin.yml");
                if (!pluginYml.exists()) {
                    continue;
                }
                FileReader fileReader = new FileReader(pluginYml);
                StringBuilder pluginName = new StringBuilder();
                int i;
                while ((i = fileReader.read()) != -1) {
                    pluginName.append((char) i);
                }
                fileReader.close();
                pluginName = new StringBuilder(pluginName.toString().split("name: ")[1].split("\n")[0]);
                if (pluginName.toString().startsWith("AutoPluginUpdater") || getConfig().getStringList("ignoredPlugins").contains(pluginName.toString()) || pluginToRealPluginNameMap.containsValue(pluginName.toString().trim())) {
                    continue;
                }

                pluginToRealPluginNameMap.put(pluginFile.getAbsolutePath().trim(), pluginName.toString().trim());
                FileUtils.cleanDirectory(tempFolder);
            } catch (Exception ignore) {
            }
        }

        Bukkit.getConsoleSender().sendMessage("§aGenerated pluginMap! With " + pluginToRealPluginNameMap.size() + " entries!");
    }

    @Override
    public void onEnable() {
        prepareFiles();
        this.saveDefaultConfig();
        instance = this;
        updatePlugins = new ArrayList<>();
        getCommand("update").setExecutor(new Update());
        loadUpdatePluginsFromJson();
        Bukkit.getConsoleSender().sendMessage("§aAutoPluginUpdater starting in version " + getDescription().getVersion());
        generatePluginNameMap();
        searchForPluginSources();
        Bukkit.getConsoleSender().sendMessage("§aLoaded " + updatePlugins.size() + " updatable plugins!");
        if (getConfig().getBoolean("checkForUpdatesAtStartup"))
            searchForPluginUpdates();

    }

    public void searchForPluginUpdates() {
        Bukkit.getConsoleSender().sendMessage("§eStarting update process... (This may take a while)");
        pluginToRealPluginNameMap.forEach((s, s2) -> searchForPluginUpdate(s2));
        Bukkit.getConsoleSender().sendMessage("§aFinished update process!");
    }

    public void searchForPluginUpdate(String pluginName) {
        if (!updatePlugins.stream().map(UpdatePlugin::name).toList().contains(pluginName)) {
            Bukkit.getConsoleSender().sendMessage(" §cPlugin " + pluginName + " is not updatable because it is not in the pluginSources.json file!");
            return;
        }
        new PluginDownloader(updatePlugins.stream().filter(updatePlugin -> updatePlugin.name().equals(pluginName)).findFirst().get());
    }

    @Override
    public void onDisable() {
        try {
            FileUtils.cleanDirectory(tempFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void searchForPluginSources() {
        Bukkit.getConsoleSender().sendMessage("§eScanning for additional plugins sources... (This may take a while)");
        pluginToRealPluginNameMap.forEach((s, s2) -> {
            boolean alreadyAdded = false;
            for (UpdatePlugin updatePlugin : updatePlugins) {
                if (updatePlugin.name().equals(s2)) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (alreadyAdded)
                return;

            try {
                String url = searchForResourceAndGetURL(s2);
                if (url != null) {
                    updatePlugins.add(new UpdatePlugin(url, s2));
                    Bukkit.getConsoleSender().sendMessage(" ");
                    Bukkit.getConsoleSender().sendMessage("§eAdded possible plugin source §6" + url.split("resources")[1] + "§e for plugin §6" + FilenameUtils.getName(s));
                    Bukkit.getConsoleSender().sendMessage("§cIf this is not the correct source for the pluginFile, please change it in the pluginSources.json file!");
                    Bukkit.getConsoleSender().sendMessage(" ");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        writeUpdatePluginsToJson();
        Bukkit.getConsoleSender().sendMessage("§aScanned for additional plugin sources!");
    }

    private String searchForResourceAndGetURL(String resourceName) throws Exception {
        String apiRequest = "https://api.spiget.org/v2/search/resources/" + resourceName;
        URL url = new URL(apiRequest);
        Scanner sc = new Scanner(url.openStream());
        StringBuffer sb = new StringBuffer();
        while (sc.hasNext()) {
            sb.append(sc.next());
        }
        String resourceString = "https://www.spigotmc.org/";
        resourceString += gson.fromJson(sb.toString(), JsonElement.class).getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("file").get("url").getAsString();
        resourceString = resourceString.split("/download")[0];
        return resourceString;
    }

}
