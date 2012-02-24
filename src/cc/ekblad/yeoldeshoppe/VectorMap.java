package cc.ekblad.yeoldeshoppe;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import org.bukkit.util.Vector;

/**
 * Since Vector isn't serializable, we'll just have to deal with it ourselves. 
 */
public class VectorMap implements Serializable {
    private static final long serialVersionUID = 8311065308517130366L;

    private class KVPair implements Serializable {
        private static final long serialVersionUID = 6071077561673818722L;
        public final double x, y, z;
        public final SignShop v;
        public KVPair(Vector k, SignShop v) {
            x = k.getX();
            y = k.getY();
            z = k.getZ();
            this.v = v;
        }
    }

    private final ArrayList<KVPair> pairs = new ArrayList<KVPair>();

    public VectorMap(Map<Vector,SignShop> m) {
        for(Object obj: m.keySet()) {
            Vector k = (Vector)obj;
            pairs.add(new KVPair(k, m.get(k)));
        }
    }
    
    public HashMap<Vector, SignShop> toHashMap() {
        HashMap<Vector, SignShop> map = new HashMap<Vector, SignShop>();
        for(KVPair p: pairs) {
            map.put(new Vector(p.x, p.y, p.z), p.v);
        }
        return map;
    }
    
    public int size() {
        return pairs.size();
    }
}
