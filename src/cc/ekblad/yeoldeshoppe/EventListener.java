package cc.ekblad.yeoldeshoppe;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;

public class EventListener implements Listener {
    private final YeOldeShoppe plugin;

    public EventListener(YeOldeShoppe plug) {
        this.plugin = plug;
        plug.getServer().getPluginManager().registerEvents(this, plug);
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent evt) {
        if(!evt.isCancelled() && evt.hasBlock()) {
            Block b = evt.getClickedBlock();
            
            // We only deal with the normal world, for simplicity
            Environment env = b.getLocation().getWorld().getEnvironment();
            if(env != World.Environment.NORMAL) {
                return;
            }
            
            if(evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if(b.getType() == Material.WALL_SIGN) {
                    SignShop shop = plugin.shops.getShopAt(b.getLocation());
                    if(shop != null) {
                        shop.interact(evt);
                    }
                    return;
                } else if(b.getType() == Material.CHEST) {
                    if(!plugin.shops.tryOpenShopChest(evt.getPlayer(), b.getLocation())) {
                        evt.setCancelled(true);
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent evt) {
        Block b = evt.getBlock();
        
        // You can only open shops in the normal world!
        Environment env = b.getLocation().getWorld().getEnvironment();
        if(env != World.Environment.NORMAL) {
            return;
        }

        if(b.getType() == Material.CHEST) {
            if(!plugin.shops.tryCreateChest(b, evt.getPlayer())) {
                evt.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onSignChange(SignChangeEvent evt) {
        Environment env = evt.getBlock().getLocation().getWorld().getEnvironment();
        if(env != World.Environment.NORMAL) {
            return;
        }

        if(evt.getLine(3).isEmpty()) {
            if(evt.getLine(2).matches(SignShop.TRAVEL_REGEX) ||
               evt.getLine(2).matches(SignShop.BUYING_REGEX) ||
               evt.getLine(1).matches(SignShop.SELLING_REGEX)) {
                evt.setLine(3, "[" + evt.getPlayer().getName() + "]");
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent evt) {
        Environment env = evt.getBlock().getLocation().getWorld().getEnvironment();
        if(env != World.Environment.NORMAL) {
            return;
        }

        if(!plugin.shops.tryBreak(evt.getPlayer(), evt.getBlock().getLocation())) {
            evt.setCancelled(true);
        }
    }
}
