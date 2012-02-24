package cc.ekblad.yeoldeshoppe;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class YWarpHandler implements CommandExecutor {
    private final YeOldeShoppe plugin;

    public YWarpHandler(YeOldeShoppe plug) {
        this.plugin = plug;
    }
    
    /**
     * String together a series of arguments to create a location name.
     * @param args Array of words to concatenate.
     * @param offset Start from this offset.
     * @return
     */
    private String locName(String[] args, int offset) {
        StringBuilder sb = new StringBuilder(args[offset]);
        for(int i = offset+1; i < args.length; ++i) {
            sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
    
    @Override
    public boolean onCommand(CommandSender from,
                             Command cmd,
                             String lbl,
                             String[] args) {
        
        // /ywarp add Name - creates a warp point where you're standing
        if(args.length >= 2 && args[0].equalsIgnoreCase("add")) {
            if(!(from instanceof Player)) {
                from.sendMessage("Only players can create warp points!");
            }
            Player p = (Player)from;

            String name = locName(args, 1);
            plugin.warps.setWarp(name, p.getLocation().toVector());
            p.sendMessage("Your current position is now called " + name);
            return true;
        }
        
        // /ywarp del Name - removes the warp point by the given name
        if(args.length >= 2 && args[0].equalsIgnoreCase("del")) {
            String name = locName(args, 1);
            Vector loc = plugin.warps.getWarp(name);
            plugin.warps.delWarp(name);
            from.sendMessage(loc + " is no longer known as " + name);
            return true;
        }
        return false;
    }
}
