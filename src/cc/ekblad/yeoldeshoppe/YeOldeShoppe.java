package cc.ekblad.yeoldeshoppe;

import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class YeOldeShoppe extends JavaPlugin {
    private final String LOG_PREFIX = "[Ye Olde Shoppe] ";
    private final Logger logger = Logger.getLogger("Minecraft");
    public final WarpManager warps = new WarpManager();
    public SignShopManager shops;
    
    public void log(String s) {
        logger.info(LOG_PREFIX + s);
    }
    
    @Override
    public void onEnable() {
        SignShopManager.initialize(this, getDataFolder() + "/shops.bin");
        shops = SignShopManager.getInstance();
        getCommand("ywarp").setExecutor(new YWarpHandler(this));
        warps.load(getConfig().getConfigurationSection("warps"));
        new EventListener(this); // We don't need a reference to this, so...
    }
    
    @Override
    public void onDisable() {
        warps.save(getConfig().createSection("warps"));
        shops.saveShops(getDataFolder() + "/shops.bin");
        saveConfig();
    }
}
