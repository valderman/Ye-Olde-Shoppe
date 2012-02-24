package cc.ekblad.yeoldeshoppe;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

/**
 * Keeps track of warp points.
 * @author Anton Ekblad
 *
 */
public class WarpManager {
    private HashMap<String, Vector> warps = new HashMap<String, Vector>();

    /**
     * Create a new warp point, or modify an existing one.
     * @param name The name of the warp point.
     * @param loc The overworld location of the warp point.
     */
    public void setWarp(String name, Vector loc) {
        warps.put(name, loc);
    }

    /**
     * Remove a previously set warp point. Removal of a non-existent warp
     * point is a silent no-op. 
     * @param name Name of the warp point to remove.
     */
    public void delWarp(String name) {
        warps.remove(name);
    }
    
    /**
     * Get the location corresponding to a warp point name.
     * @param name Name to fetch location for.
     * @return The location corresponding to the given name. Returns null if
     *          the name does not exist.
     */
    public Vector getWarp(String name) {
        return warps.get(name);
    }
    
    /**
     * Load warp points from a configuration section.
     * @param cs
     */
    public void load(ConfigurationSection cs) {
        if(cs != null) {
            Map<String, Object> ws = cs.getValues(false);
            
            for(String k: ws.keySet()) {
                Scanner s = new Scanner((String)ws.get(k));
                s.useDelimiter(",");
                float x = s.nextFloat();
                float y = s.nextFloat();
                float z = s.nextFloat();
                warps.put(k, new Vector(x, y, z));
            }
        }
    }

    /**
     * Save warp points to a configuration section.
     * @param cs
     */
    public void save(ConfigurationSection cs) {
        for(String k: warps.keySet()) {
            Vector v = warps.get(k);
            String str = v.getBlockX() + "," +
                         v.getBlockY() + "," +
                         v.getBlockZ();
            cs.set(k, str);
        }
    }
}
