package cc.ekblad.yeoldeshoppe;

import java.io.Serializable;

import org.bukkit.util.Vector;

/**
 * We need this because Vector doesn't implement Serializable.
 */
public class Vec3 implements Serializable {
    private static final long serialVersionUID = 4807412461433463380L;

    private final double x, y, z;
    public Vec3(Vector v) {
        x = v.getX();
        y = v.getY();
        z = v.getZ();
    }
    
    public Vector toVector() {
        return new Vector(x,y, z);
    }
}
