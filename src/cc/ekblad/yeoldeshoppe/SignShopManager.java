package cc.ekblad.yeoldeshoppe;

import java.io.*;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SignShopManager {
    // Public so that SignShop() can add itself; ugly...
    public final HashMap<Vector, SignShop> shops;
    public final YeOldeShoppe plugin;
    
    private static SignShopManager theMgr;

    public static void initialize(YeOldeShoppe plugin, String file) {
        theMgr = new SignShopManager(plugin, file);
    }
    
    public static SignShopManager getInstance() {
        return theMgr;
    }

    private SignShopManager(YeOldeShoppe plug, String file) {
        this.plugin = plug;
        this.shops = this.loadShops(file);
    }
    
    /**
     * Load old shops from file. Or at least make a try; Java serialization is
     * not to be trusted. Should probably replace this with some SQLite or
     * something.
     * @param file
     * @return
     */
    private HashMap<Vector, SignShop> loadShops(String file) {
        File f = new File(file);
        if(f.isFile() && f.canRead()) {
            try {
                ObjectInputStream is = new ObjectInputStream(new FileInputStream(f));
                Object obj = is.readObject();
                is.close();
                VectorMap map = (VectorMap)obj;
                plugin.log((map.size()/3) + " shops loaded.");
                return map.toHashMap();
            } catch(IOException e) {
                plugin.log("IOException while loading old shops: " + e);
            } catch(ClassNotFoundException e) {
                plugin.log("ClassNotFoundException while loading old shops: "+ e);
            }
            return new HashMap<Vector, SignShop>();
        } else {
            plugin.log("No old shops to load, starting with a clean slate");
            return new HashMap<Vector, SignShop>();
        }
    }
    
    public void saveShops(String file) {
        File f = new File(file);
        try {
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f));
            os.writeObject(new VectorMap(shops));
            os.flush();
            os.close();
        } catch(IOException e) {
            plugin.log("IOException while saving old shops: " + e);
        }
    }
    
    /**
     * Attempt to create a chest. This method is called by the event listener
     * whenever a chest is created in the normal world. If there is a shop-
     * formatted sign immediately above the chest, it becomes a shop.
     * @param b The newly placed chest.
     * @param p The player who placed the chest.
     * @return True if the chest could be created, as a shop or normal chest,
     *         otherwise false.
     */
    public boolean tryCreateChest(Block b, Player p) {
        Block sign = b.getLocation().getBlock().getRelative(BlockFace.UP);
        Location pos = sign.getLocation();
        try {
            // Constructor adds itself to the shops hashmap.
            new SignShop(pos, sign, p);
            return true;
        } catch (SignShop.NotAShopSignException e) {
            // Well, we couldn't create a shop, but maybe a normal chest?
            // We can't do that next to a shop though, as a double chest shop
            // won't be protected.
            return !nextToShopChest(b);
        } catch (SignShop.NotYourSignException e) {
            p.sendMessage("Your name is not on the sign.");
            return false;
        } catch (SignShop.AlreadyShopException e) {
            p.sendMessage("You can't attach two shop signs to the same block.");
            return false;
        }
    }
    
    /**
     * Returns true if the given block is next to a shop chest.
     * @param b
     * @return
     */
    public boolean nextToShopChest(Block b) {
        Block blk = b.getRelative(BlockFace.EAST);
        if(blk.getType() == Material.CHEST &&
           null != shops.get(blk.getLocation().toVector())) {
            return true;
        }

        blk = b.getRelative(BlockFace.WEST);
        if(blk.getType() == Material.CHEST &&
            null != shops.get(blk.getLocation().toVector())) {
             return true;
        }
        
        blk = b.getRelative(BlockFace.NORTH);
        if(blk.getType() == Material.CHEST &&
            null != shops.get(blk.getLocation().toVector())) {
             return true;
        }
        
        blk = b.getRelative(BlockFace.SOUTH);
        return (blk.getType() == Material.CHEST &&
                null != shops.get(blk.getLocation().toVector()));
    }
    
    /**
     * Try to break a block. If the block is part of a shop, a check is made
     * to see whether you're allowed to destroy it. If you are, the shop gets
     * removed from the list of shops.
     * @param p Player to check breakability for.
     * @param loc Location to check for breakability.
     * @return True if it's OK to break the block, otherwise false.
     */
    public boolean tryBreak(Player p, Location loc) {
        SignShop s = shops.get(loc.toVector());
        if(s == null) {
            return true;
        }
        return s.tryRemove(p);
    }

    /**
     * Try to open a given chest. If the chest is part of a shop, a check is
     * made to see whether you're allowed to open it or not.
     * @param p Player who wants to open the chest.
     * @param loc Location of chest who may or may not want to be opened.
     * @return
     */
    public boolean tryOpenShopChest(Player p, Location loc) {
        SignShop shop = getShopAt(loc);
        if(shop != null) {
            return shop.tryOpen(p);
        }
        return true;
    }
    
    /**
     * Get the shop residing at the given position.
     * @param pos Position of the shop's sign.
     * @return The shop in the given location, or null if none exists.
     */
    public SignShop getShopAt(Location pos) {
        return shops.get(pos.toVector());
    }

}
