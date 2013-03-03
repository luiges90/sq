package sq;

import glabs.RandomUtility;
import java.util.HashSet;
import java.util.Set;
import org.newdawn.slick.*;
import org.newdawn.slick.geom.*;
import sq.Gene.FireMethod;
import sq.Gene.Weapon;

/**
 *
 * @author Peter
 */
public class Player extends MovingEntity {

    private float moveForce = 0.0667f;
    
    private float friction = 0.0222f;

    private int respawnCountdown;
    
    private Gene gene;

    private int lives;
    
    private boolean respawnInvincible;
    private int respawnInvincibleCountdown;
    
    private Sound destroySound, fireSound;
    
    private Player(Gene g){
        super(new Ellipse(SqMain.FIELD_WIDTH / 2, SqMain.FIELD_HEIGHT / 2, g.getSize(), g.getSize()), g.getMaxSpeed(), g.getLifetime());
        this.gene = g;
        this.respawnInvincible = false;
        try {
            this.destroySound = new Sound("playerDestroy.wav");
            this.fireSound = new Sound("playerFire.wav");
        } catch (SlickException ex) {
            ex.printStackTrace();
        }
    }
    
    public static Player createInitialPlayer(){
        Gene gene = Gene.initialPlayerGene();
        
        Player p = new Player(gene);
        p.lives = 5;
        
        return p;
    }
    
    public void control(double degree, int delta){
        applyForce(moveForce * delta, degree);
    }
    
    public Set<AI> fire(Weapon w, int toX, int toY){
        if (this.isDestroyed()) return null;
        
        Set<AI> bullets = new HashSet<>();
        if (w.fireCooldownCountdown > 0) return null;
        
        fireSound.play();

        w.fireCooldownCountdown = w.fireCooldownTime;

        if (w.fireMethod == FireMethod.RANDOM){
            toX = RandomUtility.randBetween(0, SqMain.FIELD_WIDTH);
            toY = RandomUtility.randBetween(0, SqMain.FIELD_HEIGHT);
        }
        
        double d = Math.atan2(toY - this.getY(), toX - this.getX());
        for (int j = 0; j < w.shotCount; ++j){
            double targetD = d - (w.shotDistance * (w.shotCount - 1)) / 2 + w.shotDistance * j;
            double targetX = this.getX() + 100 * Math.cos(targetD);
            double targetY = this.getY() + 100 * Math.sin(targetD);
            bullets.add(AI.createBullet(this, true, this.getX(), this.getY(), (float) targetX, (float) targetY, w.bulletGene));
        }
        
        return bullets;
    }

    public boolean isInvincible(){
        return this.respawnInvincible;
    }

    public Set<AI> update(int delta, SqMain game){
        super.update(delta, game);
        
        for (Weapon i : this.gene.getWeapons()){
            i.fireCooldownCountdown -= delta;
        }
        
        this.changeSpeed(-friction * delta);
        
        this.respawnInvincibleCountdown -= delta;
        if (this.respawnInvincibleCountdown <= 0){
            this.respawnInvincible = false;
        }
        
        if (this.isDestroyed() && this.lives > 0){
            this.respawnCountdown -= delta;
            if (this.respawnCountdown <= 0){
                this.respawn();
                this.respawnInvincible = true;
                this.respawnInvincibleCountdown = 3000;
            }
        }
        
        return new HashSet<>();
    }

    @Override
    public void draw(Graphics g) {
        if (!this.isDestroyed()){
            if (!this.respawnInvincible || this.respawnInvincibleCountdown / 200 % 2 == 0){
                g.setColor(gene.getColor());
                g.fill(this.getShape());
            }
        }
    }
    
    public int getLives(){
        return lives;
    }
    
    public boolean lostLife(){
        lives--;
        destroySound.play();
        if (lives <= 0) return true;
        
        this.setVelocity(new Vector2f(0, 0));
        this.respawnCountdown = 3000;
        this.setLocation(SqMain.FIELD_WIDTH / 2, SqMain.FIELD_HEIGHT / 2);
        return false;
    }
    
    public void gainLife(){
        lives++;
    }
    
    public Gene getGene(){
        return gene;
    }
    
    public void mutate(){
        gene = gene.mutate(false, true);
    }

}
