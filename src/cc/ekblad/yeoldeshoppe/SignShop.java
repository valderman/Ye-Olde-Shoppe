package cc.ekblad.yeoldeshoppe;

import java.io.Serializable;
import java.util.Random;
import java.util.Scanner;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class SignShop implements Serializable {
    private static final long serialVersionUID = -8546025142276173154L;

    public class NotAShopSignException extends Exception {
        private final String msg;

        @Override
        public String getMessage() {
            return msg;
        }
        
        public NotAShopSignException(String msg) {
            this.msg = msg;
        }
        static final long serialVersionUID = 0;
    }

    public class NotYourSignException extends Exception {
        private final String msg;

        @Override
        public String getMessage() {
            return msg;
        }
        
        public NotYourSignException(String msg) {
            this.msg = msg;
        }
        static final long serialVersionUID = 0;
    }

    public class AlreadyShopException extends Exception {
        private final String msg;

        @Override
        public String getMessage() {
            return msg;
        }
        
        public AlreadyShopException(String msg) {
            this.msg = msg;
        }
        static final long serialVersionUID = 0;
    }

    private static final String TOO_EXPENSIVE_FOR_YOU =
            "I'm sorry, but you can't afford that!";
    private static final String OUT_OF_ORDER =
            "Unfortunately, we can't take any passengers on this route right now.";
    private static final String OUT_OF_STOCK =
            "I'm sorry but we're currently all out of stock. Please come back later!";
    private static final String OUT_OF_CASH =
            "I'm sorry but we can't afford to buy anything from you at the moment.";
    private static final String ONLY_BUY_QUANTITY_OF =
            "I'm sorry but we only buy that item in quantities of";
    private static final String THANKS =
            "Thank you very much, please come again!";
    private static final String SQUADALA =
            "Squadala, we're off!";
    private static final String HORRID_WEATHER =
            "Are you mad?! If we go out into this storm, we'll be lost for sure!";
    private static final String[] DISALLOWED = {
            "STOP RIGHT THERE, CRIMINAL SCUM!",
            "NOBODY BREAKS THE LAW ON MY WATCH!",
            "THEN PAY WITH YOUR BLOOD!"
        };
    /**
     * Deal this amount of damage to players who illicitly try to tamper with
     * the shop.
     */
    private static final int CRIMINAL_SCUM_DAMAGE = 1;

    public static final String TRAVEL_REGEX = "^[Ff]are: *([0-9]+) *g$";
    public static final String SELLING_REGEX = "^[Bb]uy *([0-9]+) for *([0-9]+) *g$";
    public static final String BUYING_REGEX = "^[Ss]ell *([0-9]+) for *([0-9]+) *g$";

    private final boolean isTravelShop;
    private final String destination;
    private final int sellingAt; // When buying from the shop, you pay this
    private final int buyingAt;  // When selling to the shop, it pays you this
    private final int sellingNo; // When buying, you get this many items
    private final int buyingNo;  // When selling, you get this many items
    private final Material currency = Material.GOLD_INGOT; // hardcode for now
    private final Material shopItem; // material this shop is buying/selling
    private final byte shopItemData;
    private final String owner;
    private final Vec3 sign, block, chest; 
    
    /**
     * Create a new sign shop and add it to the given manager's shop list.
     * @param mgr The shop manager that will keep track of this shop
     * @param loc Location of the shop sign
     * @param b Shop block
     * @param p Name of the block's owner
     * @throws NotAShopSignException
     */
    public SignShop(Location loc, Block b, Player p)
            throws NotAShopSignException, NotYourSignException,
                   AlreadyShopException {
        
        // We can only place a box below a sign if the sign is on a wall... 
        if(b.getType() != Material.WALL_SIGN) {
            throw new NotAShopSignException("Above block is " + b.getType() + ", not a sign");
        }
        Sign s = (Sign)b.getState();
        String[] lines = s.getLines();
        
        // Only ops can create shops in someone else's name
        if(!lines[3].isEmpty() &&
           lines[3].charAt(0) == '[' &&
           lines[3].charAt(lines[3].length()-1) == ']' &&
           !lines[3].equals("[" + p.getName() + "]")) {
            if(p.hasPermission("yeoldeshoppe.opshop")) {
                this.owner = lines[3].substring(1, lines[3].length()-1);
            } else {
                throw new NotYourSignException("Placing player doesn't have its name on the sign");
            }
        } else {
            this.owner = p.getName();
        }
        
        
        // sign, chest and loc are the shop sign, the block it's attached to
        // and the chest underneath, respectively.
        this.sign = new Vec3(loc.toVector());
        this.chest = new Vec3(b.getRelative(BlockFace.DOWN).getLocation().toVector());
        org.bukkit.material.Sign signdata;
        signdata = (org.bukkit.material.Sign)b.getState().getData();
        
        Vector blockVector = b.getRelative(signdata.getAttachedFace()).getLocation().toVector();
        if(SignShopManager.getInstance().getShopAt(blockVector.toLocation(p.getWorld())) != null) {
            throw new AlreadyShopException("One block can't be part of two shops!");
        }
        this.block = new Vec3(blockVector);

        if(lines[2].matches(TRAVEL_REGEX)) {
            // It's a travel shop!
            this.isTravelShop = true;
            if(lines[1].length() > 0) {
                this.destination = lines[0] + " " + lines[1];
            } else {
                this.destination = lines[0];
            }
            this.sellingAt = Integer.parseInt(lines[2].replaceFirst(TRAVEL_REGEX, "$1"));
            this.buyingAt = sellingNo = buyingNo = 0;
            this.shopItem = null;
            this.shopItemData = 0;
        } else {
            boolean isShopAtAll = false;

            String itemid = lines[0].replace(' ', '_').toUpperCase();
            if(itemid.equals("SLAVES")) {
                this.shopItem = Material.MONSTER_EGG;
                this.shopItemData = 120;
            } else {
                this.shopItemData = 0;
                try {
                    this.shopItem = Material.valueOf(itemid);
                } catch(IllegalArgumentException e) {
                    // An illegal argument here means we haven't been given a
                    // proper item ID, so this isn't a shop.
                    throw new NotAShopSignException("First line not an item ID");
                }
            }

            this.isTravelShop = false;
            this.destination = null;
            
            // It's a shop that sells stuff!
            if(lines[1].matches(SELLING_REGEX)) {
                String sell = lines[1].replaceFirst(SELLING_REGEX, "$1 $2");
                Scanner sc = new Scanner(sell);
                sellingNo = sc.nextInt();
                sellingAt = sc.nextInt();
                isShopAtAll = true;
            } else {
                sellingNo = sellingAt = 0;
            }

            // It's a shop that buys stuff!
            if(lines[2].matches(BUYING_REGEX)) {
                String sell = lines[2].replaceFirst(BUYING_REGEX, "$1 $2");
                Scanner sc = new Scanner(sell);
                buyingNo = sc.nextInt();
                buyingAt = sc.nextInt();
                isShopAtAll = true;
            } else {
                buyingNo = buyingAt = 0;
            }

            if(!isShopAtAll) {
                throw new NotAShopSignException("Sign text is not shop formatted");
            }
        }
        
        // We store all three related blocks, so we can check for block
        // destruction easily.
        SignShopManager manager = SignShopManager.getInstance();
        manager.shops.put(sign.toVector(), this);
        manager.shops.put(chest.toVector(), this);
        manager.shops.put(block.toVector(), this);
    }
    
    /**
     * Returns the name of this shop's owner.
     */
    public String getOwner() {
        return this.owner;
    }
    
    /**
     * Remove this shop from its manager, if the given player is allowed to
     * do so.
     */
    public boolean tryRemove(Player p) {
        if(tamperingAllowed(p)) {
            SignShopManager manager = SignShopManager.getInstance();
            manager.shops.remove(sign.toVector());
            manager.shops.remove(chest.toVector());
            manager.shops.remove(block.toVector());
            return true;
        }
        return false;
    }
    
    /**
     * Try to open this shop's chest, returning true if OK.
     * @param p Player who wants to open the chest.
     * @return
     */
    public boolean tryOpen(Player p) {
        return tamperingAllowed(p);
    }
    
    /**
     * Returns true if the given player is allowed to destroy or loot this
     * shop.
     * @param p
     * @return
     */
    public boolean tamperingAllowed(Player p) {
        if(p.getName().equalsIgnoreCase(owner) ||
           p.hasPermission("yeoldeshoppe.opshop")) {
            return true;
        } else {
            p.sendMessage(DISALLOWED[new Random().nextInt(DISALLOWED.length)]);
            p.damage(CRIMINAL_SCUM_DAMAGE);
            return false;
        }
    }
    
    /**
     * Interacts with the given sign shop.
     * @param evt The event that led to this interaction.
     */
    @SuppressWarnings("deprecation")
    public void interact(PlayerInteractEvent evt) {
        Player p = evt.getPlayer();
        Chest c = (Chest)this.chest.toVector().toLocation(p.getWorld()).getBlock().getState();
        Inventory chest = c.getInventory();
        
        if(p.getItemInHand().getType() == this.currency) {
            if(p.getInventory().contains(this.currency, this.sellingAt)) {
                ItemStack price = new ItemStack(currency, sellingAt);
                
                if(this.isTravelShop) {
                    Vector dest = SignShopManager.getInstance().plugin.warps.getWarp(this.destination);
                    if(p.getWorld().isThundering()) {
                        p.sendMessage(HORRID_WEATHER);
                    } else if(dest == null) {
                        p.sendMessage(OUT_OF_ORDER);
                    } else {
                        p.getInventory().removeItem(price);
                        chest.addItem(price);
                        World thisWorld = evt.getClickedBlock().getWorld();
                        p.sendMessage(SQUADALA);
                        evt.getPlayer().teleport(dest.toLocation(thisWorld));
                    }
                } else {

                    // Player clicked with gold, so we're selling
                    if(sellingAt > 0) {
                        if(chest.contains(shopItem, sellingNo)) {
                            ItemStack wares = new ItemStack(shopItem, sellingNo, (short)0, shopItemData);
                            p.getInventory().removeItem(price);
                            chest.removeItem(wares);
                            chest.addItem(price);
                            p.getInventory().addItem(wares);
                            p.sendMessage(THANKS);
                        } else {
                            p.sendMessage(OUT_OF_STOCK);
                        }
                    }
                }
            } else {
                p.sendMessage(TOO_EXPENSIVE_FOR_YOU);
            }
        } else if(p.getItemInHand().getType() == this.shopItem) {
            // Player clicked with resource, so we're buying 
            
            if(p.getInventory().contains(shopItem, buyingNo)) {
                if(chest.contains(currency, buyingAt)) {
                    ItemStack price = new ItemStack(currency, buyingAt);
                    ItemStack wares = new ItemStack(shopItem, buyingNo, (short)0, shopItemData);
                    chest.removeItem(price);
                    p.getInventory().removeItem(wares);
                    chest.addItem(wares);
                    p.getInventory().addItem(price);
                    p.sendMessage(THANKS);
                } else {
                    p.sendMessage(OUT_OF_CASH);
                }
            } else {
                p.sendMessage(ONLY_BUY_QUANTITY_OF + " " + buyingNo);
            }
        }
        p.updateInventory();
    }
}
