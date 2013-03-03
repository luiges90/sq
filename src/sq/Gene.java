package sq;

import glabs.MiscUtility;
import glabs.RandomUtility;
import java.io.Serializable;
import java.util.Objects;
import sq.annotations.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.newdawn.slick.*;

/**
 *
 * @author Peter
 */
public class Gene implements Serializable{
    @NoMutate
    private static final long serialVersionUID = 1L;

    public enum MoveMethod {

        STRAIGHT, CHASE
    };
    
    public enum FireMethod {

        RANDOM, AIM, COUNTER
    };
    
    public enum StealthMethod {NONE, PERIODIC, RANDOM, APPROACH};
     
    public static class Weapon implements Serializable{
        @NoMutate
        private static final long serialVersionUID = 1L;
        public FireMethod fireMethod;
        public int fireVariance = 100;
        @Min(1) @NoPlayerMutate
        public int shotCount = 1;
        public float shotDistance = 0.174f; // pi / 18
        public float shotDistanceVariance = 0.017f;
        @NoMutate
        public int fireCooldownCountdown;
        public AIGene bulletGene;
        @Min(20)
        public int fireCooldownTime;
        
        public Weapon(){
            //do nothing
        }
        
        public Weapon(Weapon w){
            fireMethod = w.fireMethod;
            fireVariance = w.fireVariance;
            shotCount = w.shotCount;
            shotDistance = w.shotDistance;
            shotDistanceVariance = w.shotDistanceVariance;
            bulletGene = new AIGene(w.bulletGene);
            fireCooldownTime = w.fireCooldownTime;
        }

        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + (this.fireMethod != null ? this.fireMethod.hashCode() : 0);
            hash = 97 * hash + this.fireVariance;
            hash = 97 * hash + this.shotCount;
            hash = 97 * hash + Float.floatToIntBits(this.shotDistance);
            hash = 97 * hash + Float.floatToIntBits(this.shotDistanceVariance);
            hash = 97 * hash + Objects.hashCode(this.bulletGene);
            hash = 97 * hash + this.fireCooldownTime;
            return hash;
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof Weapon)) {
                return false;
            }
            Weapon w = (Weapon) o;
            return w.fireVariance == this.fireVariance && w.fireMethod.equals(this.fireMethod) && w.shotCount == this.shotCount &&
                    w.shotDistance == this.shotDistance && w.shotDistanceVariance == this.shotDistanceVariance &&
                    w.bulletGene.equals(this.bulletGene) && w.fireCooldownTime == this.fireCooldownTime;
        }

        @NoMutate private transient Field[] fieldCache;
        public final Field[] getAllFields(boolean includeAll) {
            List<Field> result = null;
            if (fieldCache == null){
                Field[] allFields = MiscUtility.getAllFields(this.getClass());
                result = new ArrayList<>();
                for (Field i : allFields) {
                    if (includeAll || !i.isAnnotationPresent(NoMutate.class)) {
                        result.add(i);
                    }
                }
                fieldCache = result.toArray(new Field[]{});
            }
            return fieldCache;
        }
        public final String toString() {
            Field[] f = getAllFields(true);
            StringBuilder sb = new StringBuilder();
            sb.append("Gene: ");
            for (Field i : f) {
                try {
                    sb.append(i.getName()).append(" = ").append(i.get(this)).append("; ");
                } catch (IllegalAccessException ex) {
                    throw new AssertionError();
                }
            }
            return sb.toString();
        }
        
        public static Weapon initialWeapon(Color color, boolean isPlayer){
            Weapon w = new Weapon();
            w.bulletGene = AIGene.initialEnemyBulletGene(color);
            w.fireMethod = FireMethod.RANDOM;
            w.fireCooldownTime = isPlayer ? 300 : 2000;
            w.fireCooldownCountdown = RandomUtility.randBetween(0, w.fireCooldownTime);
            return w;
        }
    }
    
    protected Color color;
    @Max(250)
    @Min(4)
    protected int size;
    @Max(50)
    protected int maxSpeed = 0;
    @Min(100)
    protected int lifetime;
    
    protected Set<Weapon> weapons = new HashSet<>();

    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.color);
        hash = 47 * hash + this.size;
        hash = 47 * hash + this.maxSpeed;
        hash = 47 * hash + this.lifetime;
        hash = 47 * hash + Objects.hashCode(this.weapons);
        return hash;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Gene)) {
            return false;
        }
        Gene g = (Gene) o;
        return g.color.equals(this.color) && g.size == this.size && g.maxSpeed == this.maxSpeed && g.lifetime == this.lifetime &&
                g.weapons.equals(this.weapons);
    }

    public Gene() {
        //do nothing
    }

    public Gene(Gene g) {
        color = new Color(g.color);
        size = g.size;
        maxSpeed = g.maxSpeed;
        lifetime = g.lifetime;
        weapons = new HashSet<>();
        for (Weapon w : g.weapons){
            weapons.add(new Weapon(w));
        }
    }

    public static Gene initialPlayerGene() {
        Gene gene = new Gene();
        gene.color = Color.lightGray;
        gene.size = 12;
        gene.maxSpeed = 10;
        gene.lifetime = -1;
        Weapon w = new Weapon();
        w.bulletGene = AIGene.initialPlayerBulletGene();
        w.fireCooldownTime = 30;
        w.fireMethod = FireMethod.AIM;
        gene.weapons.add(w);
        return gene;
    }

    @NoMutate private transient Field[] fieldCache;
    public final Field[] getAllFields(boolean includeAll) {
        List<Field> result = null;
        if (fieldCache == null){
            Field[] allFields = MiscUtility.getAllFields(this.getClass());
            result = new ArrayList<>();
            for (Field i : allFields) {
                if (includeAll || (!i.isAnnotationPresent(NoMutate.class) && !Modifier.isFinal(i.getModifiers()))) {
                    result.add(i);
                }
            }
            fieldCache = result.toArray(new Field[]{});
        }
        return fieldCache;
    }

    public final Field[] getAllFields() {
        return getAllFields(false);
    }
    
    private static Color randomColor(){
        float r, g, b;
        do {
            r = RandomUtility.randBetween(0.0f, 1.0f);
            g = RandomUtility.randBetween(0.0f, 1.0f);
            b = RandomUtility.randBetween(0.0f, 1.0f);
        } while (r + g + b < 0.3);
        return new Color(r, g, b);
    }

    protected static Object mutateObject(Object o, Object target, boolean tweak, boolean isPlayer) {
        if (o instanceof Color) {
            Color c = (Color) o;
            if (tweak) {
                c.add(new Color(RandomUtility.randBetween(-0.1f, 0.1f), RandomUtility.randBetween(-0.1f, 0.1f), RandomUtility.randBetween(-0.1f, 0.1f)));
            } else {
                c = randomColor();
            }
            return c;
        } else if (o instanceof Gene) {
            return ((Gene) o).mutate(tweak, isPlayer);
        } else if (o instanceof FireMethod && target instanceof Gene && (((Gene) target).lifetime < 0 || RandomUtility.chance(10))) {
            FireMethod m = (FireMethod) o;
            m = RandomUtility.randomPick(FireMethod.values());
            return m;
        } else if (o instanceof Enum && !tweak){
            Enum<?> e = (Enum) o;
            Enum<?> old = e;
            do {
                e = RandomUtility.randomPick(e.getClass().getEnumConstants());
            } while (e.equals(old));
            return e;
        } else if (o instanceof Set){
            Set<Object> s = (Set<Object>) o;
            Object i = RandomUtility.randomPick(s);
            if (i instanceof Weapon){
                Weapon old = (Weapon) i;
                if (RandomUtility.chance(25)){
                    s.add(old);
                    s.add(Weapon.initialWeapon(randomColor(), isPlayer));
                } else {
                    Field[] fields = old.getAllFields(false);
                    Field f;
                    do {
                        f = RandomUtility.randomPick(fields);
                    } while (!f.getName().equals("bulletGene") && RandomUtility.chance(33));
                    Weapon n = new Weapon(old);
                    mutateField(f, n, tweak, isPlayer);
                    Iterator<Object> it = s.iterator();
                    while (it.hasNext()){
                        if (it.next() == old){
                            it.remove();
                        }
                    }
                    s.add(n);
                }
            }
            return s;
        }
        return o;
    }
    
    private static void mutateField(Field f, Object target, boolean tweak, boolean isPlayer){
        if (f.isAnnotationPresent(NoPlayerMutate.class) && isPlayer){
            return;
        }
        if (f.isAnnotationPresent(NeedsStealth.class) && target instanceof AIGene && ((AIGene) target).stealth == StealthMethod.NONE){
            ((AIGene) target).stealth = (StealthMethod) mutateObject(((AIGene) target).stealth, target, tweak, isPlayer);
            return;
        }
        if (f.isAnnotationPresent(NeedsTrue.class) && target instanceof AIGene){
            String needsFieldName = f.getAnnotation(NeedsTrue.class).value();
            try {
                Field needField = target.getClass().getDeclaredField(needsFieldName);
                mutateField(needField, target, tweak, isPlayer);
            } catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            }
            return;
        }
        try {
            //try numbers
            double num = f.getDouble(target);
            if (f.getName().equals("lifetime") && num > 0 && RandomUtility.chance(100)){
                f.set(target, -1);
            } else if (num > 0) {
                if (num == 0 && RandomUtility.chance(20)){
                    num = 1;
                } else {
                    if (num == 1){
                        num = tweak ? 2 : RandomUtility.randBetween(2, 4);
                    } else if (num == 2 && tweak){
                        num = RandomUtility.randBetween(1, 3);
                    } else {
                        num *= tweak ? RandomUtility.randBetween(0.8f, 1.2f)
                            : (RandomUtility.chance(50) ? RandomUtility.randBetween(0.33f, 1.0f) : RandomUtility.randBetween(1.0f, 3.0f));
                    }
                }
                
                if (!tweak && RandomUtility.chance(10) && !f.isAnnotationPresent(Min.class)){
                    num = 0;
                }

                Min min = f.getAnnotation(Min.class);
                if (min != null && num < min.value()) {
                    num = min.value();
                }

                Max max = f.getAnnotation(Max.class);
                if (max != null && num > max.value()) {
                    num = max.value();
                }

                Object o = f.get(target);

                if (o instanceof Double) {
                    f.setDouble(target, num);
                } else if (o instanceof Float) {
                    f.setFloat(target, (float) num);
                } else if (o instanceof Long) {
                    f.setLong(target, Math.round(num));
                } else if (o instanceof Integer) {
                    f.setInt(target, (int) Math.round(num));
                } else if (o instanceof Short) {
                    f.setShort(target, (short) Math.round(num));
                } else if (o instanceof Byte) {
                    f.setByte(target, (byte) Math.round(num));
                }

            }

        } catch (IllegalArgumentException ex) { //not a number

            try {

                Object o = f.get(target);
                if (o instanceof Boolean) {
                    f.set(target, !((boolean) o));
                } else {
                    //primitive failed, mutate the object
                    f.set(target, mutateObject(o, target, tweak, isPlayer));
                }

            } catch (IllegalArgumentException | IllegalAccessException ex1) {
                System.err.println("Trying to mutate " + f + ", but failed.");
                throw new AssertionError();
            }

        } catch (IllegalAccessException ex) {
            System.err.println("Trying to mutate " + f + ", but failed.");
            throw new AssertionError();
        }
    }

    public final Gene mutate(boolean tweak, boolean isPlayer) {
        Gene result;
        try {
            result = this.getClass().getConstructor(this.getClass()).newInstance(this);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new UndeclaredThrowableException(ex);
        }

        Field[] fields = result.getAllFields();

        Field f;
        do {
            f = RandomUtility.randomPick(fields);
        } while (!f.getName().equals("weapons") && RandomUtility.chance(30));

        mutateField(f, result, tweak, isPlayer);

        return result;
    }

    public Color getColor() {
        return color;
    }

    public int getSize() {
        return size;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public int getLifetime() {
        return lifetime;
    }
    
    public Set<Weapon> getWeapons(){
        return weapons;
    }
    
    public final String toString() {
        Field[] f = getAllFields(true);
        StringBuilder sb = new StringBuilder();
        sb.append("Gene: ");
        for (Field i : f) {
            try {
                sb.append(i.getName()).append(" = ").append(i.get(this)).append("; ");
            } catch (IllegalAccessException ex) {
                throw new AssertionError();
            }
        }
        return sb.toString();
    }
}
