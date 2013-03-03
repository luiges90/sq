package sq;

import java.util.HashSet;
import java.util.Set;
import org.newdawn.slick.*;
import org.newdawn.slick.geom.*;
import glabs.RandomUtility;
import sq.Gene.FireMethod;
import sq.Gene.Weapon;

/**
 *
 * @author Peter
 */
public class AI extends MovingEntity{
    
    private AIGene gene;
    private boolean friendly;
    
    private MovingEntity owner;
    
    private int longetivity = 0;
    
    private float nearHitScore = 0;
    
    private int hp;
    
    private Sound destroySound, fireSound, teleportSound;
    
    private boolean stealth = false;
    
    private AI(boolean friendly, MovingEntity owner, Shape s, AIGene gene, int speed, float direction){
        super(s, gene.getMaxSpeed(), gene.getLifetime(), speed, direction);
        this.friendly = friendly;
        this.gene = gene;
        this.owner = owner;
        try {
            this.destroySound = new Sound("destroy.wav");
            this.fireSound = new Sound("fire.wav");
            this.teleportSound = new Sound("teleport.wav");
        } catch (SlickException ex) {
            ex.printStackTrace();
        }
    }
    
    public static AI createEnemy(AIGene gene, Shape placementAvoidArea){
        int placeX, placeY;
        do {
            placeX = RandomUtility.randBetween(0, SqMain.FIELD_WIDTH);
            placeY = RandomUtility.randBetween(0, SqMain.FIELD_HEIGHT);
        } while (placementAvoidArea.contains(placeX, placeY));
        
        AI e = new AI(false, null, new Rectangle(placeX, placeY, gene.getSize(), gene.getSize()), 
                gene, gene.getMaxSpeed(), RandomUtility.randBetween(0.0f, (float) (2 * Math.PI)));
        
        for (Weapon i : e.gene.getWeapons()){
            i.fireCooldownCountdown = RandomUtility.randBetween(0, i.bulletGene.lifetime);
        }
        
        e.hp = gene.hp;
        
        e.teleportCountdown = RandomUtility.randBetween(0, e.gene.teleportPeriod + RandomUtility.randBetween(0, e.gene.teleportVariance));
        
        return e;
    }
    
    public static AI createBullet(MovingEntity firer, boolean friendly, float x, float y, float toX, float toY, AIGene gene){
        Shape s = friendly ? new Ellipse(x, y, gene.getSize(), gene.getSize()) : new Rectangle(x, y, gene.getSize(), gene.getSize());
        AI b = new AI(friendly, firer, s, 
                gene, gene.getMaxSpeed(), (float) Math.atan2(toY - y, toX - x));

        return b;
    }
    
    public boolean isFriendly(){
        return friendly;
    }
    
    public Set<AI> fire(Weapon w, int toX, int toY){
        return fire(w, toX, toY, false);
    }
    
    public Set<AI> fire(Weapon w, int toX, int toY, boolean ignoreReload){
        if (this.isDestroyed()) return null;

        Set<AI> bullets = new HashSet<>();
        if (w.fireCooldownCountdown > 0 && !ignoreReload) return null;
        
        fireSound.play();

        if (!ignoreReload) {w.fireCooldownCountdown = w.fireCooldownTime + w.fireVariance;}

        double d = Math.atan2(toY - this.getY(), toX - this.getX());
        for (int j = 0; j < w.shotCount; ++j){
            double targetD = d - (w.shotDistance * (w.shotCount - 1)) / 2 + w.shotDistance * j 
                    + RandomUtility.randBetween(-w.shotDistanceVariance, w.shotDistanceVariance);
            double targetX = this.getX() + 100 * Math.cos(targetD);
            double targetY = this.getY() + 100 * Math.sin(targetD);
            bullets.add(AI.createBullet(this, false, this.getX(), this.getY(), (float) targetX, (float) targetY, w.bulletGene));
        }
        
        return bullets;
    }
    
    public Set<AI> destroy(SqMain game, boolean byCollision){
        Player player = game.getPlayer();
        Set<AI> addedAIs = new HashSet<>();
        Set<AI> bullets;
        for (Weapon w : gene.weapons){
            if (w.fireMethod == FireMethod.COUNTER){
                int old = w.shotCount;
                w.shotCount *= 10;
                if (game.getPlayer().isDestroyed()){
                    bullets = this.fire(w, (int) game.getPlayer().getX(), (int) game.getPlayer().getY());
                } else {
                    bullets = this.fire(w, RandomUtility.randBetween(0, SqMain.FIELD_WIDTH / 2), RandomUtility.randBetween(0, SqMain.FIELD_HEIGHT / 2), true);
                }
                w.shotCount = old;
                if (bullets != null){
                    addedAIs.addAll(bullets);
                }
            }
        }
        if (this.stealth){
            if (this.gene.counterDuringStealth){
                for (Weapon w : gene.weapons){
                    if (!game.getPlayer().isDestroyed()){
                        bullets = this.fire(w, (int) game.getPlayer().getX(), (int) game.getPlayer().getY(), true);
                    } else {
                        bullets = this.fire(w, RandomUtility.randBetween(0, SqMain.FIELD_WIDTH / 2), RandomUtility.randBetween(0, SqMain.FIELD_HEIGHT / 2));
                    }
                    if (bullets != null){
                        addedAIs.addAll(bullets);
                    }
                }
            }
            if (this.gene.invincibleDuringStealth){
                return addedAIs;
            }
        }
        hp--;
        if (hp <= 0){
            if (byCollision){
                destroySound.play();
            }
            super.destroy();
        } else {
            if (gene.teleportOnHit){
                do {
                    this.setLocation(RandomUtility.randBetween(0, SqMain.FIELD_WIDTH), RandomUtility.randBetween(0, SqMain.FIELD_HEIGHT));
                } while (this.getShape().intersects(new Ellipse(player.getX(), player.getY(), 120, 120)));
                teleportSound.play();
            }
        }
        return addedAIs;
    }

    @Override
    public void draw(Graphics g) {
        if (!this.isDestroyed() && !this.stealth){
            g.setColor(gene.getColor());
            g.pushTransform();
                g.rotate(this.getX(), this.getY(), (float) (Math.atan2(this.getVelocity().y, this.getVelocity().x) * 180 / Math.PI));
                g.fill(this.getShape());
            g.popTransform();
        }
    }
    
    private int teleportCountdown;
    public Set<AI> update(int delta, SqMain game){
        super.update(delta, game);
        if (this.isDestroyed()) return new HashSet<>();
        
        for (Weapon i : this.gene.getWeapons()){
            i.fireCooldownCountdown -= delta;
        }
        
        if (!this.isDestroyed()){
            longetivity += delta;
        }
        
        float thisTurnNearHitScore = 1.0f / (new Vector2f(game.getPlayer().getX(), game.getPlayer().getY()).distance(new Vector2f(this.getX(), this.getY())) 
                    - game.getPlayer().getShape().getWidth() - this.getShape().getWidth() / 2);
        if (this.owner == null){
            nearHitScore += thisTurnNearHitScore;
        } else if (this.owner instanceof AI){
            ((AI) this.getOwner()).nearHitScore += thisTurnNearHitScore;
        }
        
        Set<AI> addedAIs = new HashSet<>();
        Set<AI> bullets;
        
        //firings
        for (Weapon w : this.gene.weapons){
            switch (w.fireMethod){
                case COUNTER: break;
                case RANDOM:
                    bullets = this.fire(w, RandomUtility.randBetween(0, SqMain.FIELD_WIDTH / 2), RandomUtility.randBetween(0, SqMain.FIELD_HEIGHT / 2));
                    if (bullets != null){
                        addedAIs.addAll(bullets);
                    }
                    break;
                case AIM:
                    if (!game.getPlayer().isDestroyed()){
                        bullets = this.fire(w, (int) game.getPlayer().getX(), (int) game.getPlayer().getY());
                    } else {
                        bullets = this.fire(w, RandomUtility.randBetween(0, SqMain.FIELD_WIDTH / 2), RandomUtility.randBetween(0, SqMain.FIELD_HEIGHT / 2));
                    }
                    if (bullets != null){
                        addedAIs.addAll(bullets);
                    }
                    break;
            }
        }
        
        //movings
        switch (this.gene.getMoveMethod()){
            case STRAIGHT: break;
            case CHASE:
                if (!game.getPlayer().isDestroyed()){
                    double currentAngle = this.getVelocity().getTheta();
                    double targetAngle = new Vector2f(game.getPlayer().getX() - this.getX(), game.getPlayer().getY() - this.getY()).getTheta();
                    double turn = Math.signum(targetAngle - currentAngle) * gene.chaseRatio;
                    if (Math.abs(turn) >= Math.abs(targetAngle - currentAngle)){
                        this.setVelocity(this.getVelocity().length(), (float) (targetAngle * Math.PI / 180));
                    } else {
                        this.setVelocity(this.getVelocity().length(), (float) ((currentAngle + turn) * Math.PI / 180));
                    }
                }
                break;
        }
        
        //stealth
        switch (this.gene.stealth){
            case NONE: break;
            case PERIODIC:
                if (game.getGameTime() % (this.gene.stealthPeriod + this.gene.nonStealthPeriod) < this.gene.stealthPeriod){
                    this.stealth = true;
                } else {
                    this.stealth = false;
                }
                break;
            case RANDOM:
                if (RandomUtility.chance(2000 / (this.gene.stealthPeriod + this.gene.nonStealthPeriod))){
                    this.stealth = true;
                } else {
                    this.stealth = false;
                }
                break;
            case APPROACH:
                Vector2f thisLoc = new Vector2f(this.getX(), this.getY());
                Vector2f playerLoc = new Vector2f(game.getPlayer().getX(), game.getPlayer().getY());
                if (thisLoc.distance(playerLoc) < (this.gene.stealthPeriod + this.gene.nonStealthPeriod) / 10){
                    this.stealth = false;
                } else {
                    this.stealth = true;
                }
                break;
        }
        
        //random teleport
        if (this.gene.randomTeleport){
            teleportCountdown -= delta;
            if (teleportCountdown < 0){
                do {
                    this.setLocation(RandomUtility.randBetween(0, SqMain.FIELD_WIDTH), RandomUtility.randBetween(0, SqMain.FIELD_HEIGHT));
                } while (this.getShape().intersects(new Ellipse(game.getPlayer().getX(), game.getPlayer().getY(), 120, 120)));
                teleportSound.play();
                teleportCountdown = this.gene.teleportPeriod + RandomUtility.randBetween(0, this.gene.teleportVariance);
            }
        }
        
        return addedAIs;
    }
    
    public MovingEntity getOwner(){
        return owner;
    }
    
    public AIGene getGene(){
        return gene;
    }
    
    public int getLongetivity(){
        if (this.owner == null){
            return longetivity;
        } else {
            return ((AI) this.owner).getLongetivity();
        }
    }

    public float getNearHitScore(){
        if (this.owner == null){
            return nearHitScore;
        } else {
            return ((AI) this.owner).getNearHitScore();
        }
    }
}
