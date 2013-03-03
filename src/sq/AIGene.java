package sq;

import org.newdawn.slick.Color;
import sq.annotations.*;

/**
 *
 * @author Peter
 */
public class AIGene extends Gene {

    @NoMutate
    private static final long serialVersionUID = 1L;

    protected MoveMethod moveMethod;
    
    @Min(0.001)
    protected float chaseRatio = 0.5f;
    @Min(1)
    protected int hp = 1;
    
    protected boolean teleportOnHit = false;
   
    protected StealthMethod stealth = StealthMethod.NONE;
    
    @NeedsStealth protected boolean counterDuringStealth = false;
    @NeedsStealth protected boolean invincibleDuringStealth = false;
    
    @NeedsStealth protected int stealthPeriod = 500;
    @NeedsStealth protected int nonStealthPeriod = 500;
    
    protected boolean randomTeleport = false;
    @NeedsTrue("randomTeleport") protected int teleportPeriod = 1000;
    @NeedsTrue("randomTeleport") protected int teleportVariance = 300;

    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + (this.moveMethod != null ? this.moveMethod.hashCode() : 0);
        hash = 17 * hash + Float.floatToIntBits(this.chaseRatio);
        hash = 17 * hash + this.hp;
        hash = 17 * hash + (this.teleportOnHit ? 1 : 0);
        hash = 17 * hash + (this.stealth != null ? this.stealth.hashCode() : 0);
        hash = 17 * hash + (this.counterDuringStealth ? 1 : 0);
        hash = 17 * hash + (this.invincibleDuringStealth ? 1 : 0);
        hash = 17 * hash + this.stealthPeriod;
        hash = 17 * hash + this.nonStealthPeriod;
        hash = 17 * hash + (this.randomTeleport ? 1 : 0);
        hash = 17 * hash + this.teleportPeriod;
        hash = 17 * hash + this.teleportVariance;
        return hash;
    }

    public boolean equals(Object o){
        if (!(o instanceof AIGene)) {
            return false;
        }
        AIGene g = (AIGene) o;
        return super.equals(o) && g.moveMethod.equals(this.moveMethod) && g.chaseRatio == this.chaseRatio && g.hp == this.hp &&
                g.teleportOnHit == this.teleportOnHit && g.stealth.equals(this.stealth) && g.counterDuringStealth == this.counterDuringStealth &&
                g.invincibleDuringStealth == this.invincibleDuringStealth && g.randomTeleport == this.randomTeleport &&
                g.teleportPeriod == this.teleportPeriod && g.teleportVariance == this.teleportVariance &&
                g.stealthPeriod == this.stealthPeriod && g.nonStealthPeriod == this.nonStealthPeriod;
    }

    public AIGene() {
        super();
        //do nothing
    }
   
    public AIGene(AIGene g) {
        super(g);
        moveMethod = g.moveMethod;
        chaseRatio = g.chaseRatio;
        hp = g.hp;
        teleportOnHit = g.teleportOnHit;
    }

    public static AIGene initialPlayerBulletGene() {
        AIGene gene = new AIGene();
        gene.color = Color.lightGray;
        gene.size = 4;
        gene.lifetime = 330;
        gene.maxSpeed = 15;
        gene.moveMethod = MoveMethod.STRAIGHT;
        return gene;
    }

    public static AIGene initialEnemyGene() {
        AIGene gene = new AIGene();
        gene.color = Color.white;
        gene.size = 24;
        gene.maxSpeed = 3;
        gene.lifetime = -1;
        gene.moveMethod = MoveMethod.STRAIGHT;
        gene.weapons.add(Gene.Weapon.initialWeapon(gene.color, false));
        return gene;
    }

    public static AIGene initialEnemyBulletGene(Color color) {
        AIGene gene = new AIGene();
        gene.color = color;
        gene.size = 12;
        gene.maxSpeed = 6;
        gene.lifetime = 1200;
        gene.moveMethod = MoveMethod.STRAIGHT;
        return gene;
    }

    public MoveMethod getMoveMethod() {
        return moveMethod;
    }

}
