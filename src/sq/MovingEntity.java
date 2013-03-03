package sq;

import java.util.HashSet;
import java.util.Set;
import org.newdawn.slick.geom.*;
import org.newdawn.slick.*;

/**
 * A moving entity capable of moving
 * @author Peter
 */
public abstract class MovingEntity {
    
    protected Shape shape;
    
    private Vector2f velocity;
    protected float maxSpeed;
    
    private int kills;
    
    private int lifetime;
    private boolean infiniteLifetime;

    private boolean destroyed = false;
    
    public MovingEntity(Shape s, float p, int t){
        this(s, p, t, 0, 0);
    }
    
    public MovingEntity(Shape s, float maxSpeed, int lifetime, float initialSpeed, float initialDirection){
        shape = s;
        this.maxSpeed = maxSpeed;
        setVelocity(initialSpeed, initialDirection);
        this.lifetime = lifetime;
        kills = 0;
        infiniteLifetime = lifetime < 0;
    }
    
    public Shape getShape(){
        return shape;
    }
    
    public boolean isDestroyed(){
        return destroyed;
    }
    
    public Set<AI> destroy(){
        destroyed = true;
        return new HashSet<>();
    }
    
    public void respawn(){
        destroyed = false;
    }

    public final Vector2f getVelocity(){
        return new Vector2f(velocity);
    }
    
    public final void setVelocity(Vector2f v){
        velocity = new Vector2f(v);
        if (velocity.lengthSquared() >= maxSpeed * maxSpeed){
            velocity = velocity.normalise().scale(maxSpeed);
        }
    }
    
    public final void setVelocity(float speed, float direction){
        Vector2f f = new Vector2f();
        f.x = (float) (speed * Math.cos(direction));
        f.y = (float) (speed * Math.sin(direction));
        velocity = f;
        if (velocity.lengthSquared() >= maxSpeed * maxSpeed){
            velocity = velocity.normalise().scale(maxSpeed);
        }
    }
    
    public final void setLocation(float x, float y){
        this.shape.setCenterX(x);
        this.shape.setCenterY(y);
    }
    
    public final void applyForce(Vector2f force){
        velocity.add(force);
        if (velocity.lengthSquared() >= maxSpeed * maxSpeed){
            velocity = velocity.normalise().scale(maxSpeed);
        }
    }
    
    public final void applyForce(double force, double degree){
        Vector2f f = new Vector2f();
        f.x = (float) (force * Math.cos(degree));
        f.y = (float) (force * Math.sin(degree));
        this.applyForce(f);
    }
    
    public final void move(int delta){
        shape.setCenterX(shape.getCenterX() + velocity.x);
        shape.setCenterY(shape.getCenterY() + velocity.y);
        if (shape.getCenterX() < 0){
            shape.setCenterX(SqMain.FIELD_WIDTH + shape.getCenterX());
        }
        if (shape.getCenterY() < 0){
            shape.setCenterY(SqMain.FIELD_HEIGHT + shape.getCenterY());
        }
        if (shape.getCenterX() > SqMain.FIELD_WIDTH){
            shape.setCenterX(shape.getCenterX() - SqMain.FIELD_WIDTH);
        }
        if (shape.getCenterY() > SqMain.FIELD_HEIGHT){
            shape.setCenterY(shape.getCenterY() - SqMain.FIELD_HEIGHT);
        }
    }
    
    public abstract void draw(Graphics g);
    
    public final void changeSpeed(float delta){
        float targetSpeed = velocity.length() + delta;
        if (targetSpeed > maxSpeed){
            targetSpeed = maxSpeed;
        }
        if (targetSpeed < 0){
            targetSpeed = 0;
        }
        velocity = velocity.normalise().scale(targetSpeed);
    }
    
    public Set<AI> update(int delta, SqMain game){
        if (!this.destroyed){
            move(delta);
            if (!infiniteLifetime){
                lifetime -= 15;
                if (lifetime < 0){
                    this.destroy();
                }
            }
        }
        return new HashSet<>();
    }
    
    public float getX(){
        return this.getShape().getCenterX();
    }
    
    public float getY(){
        return this.getShape().getCenterY();
    }
    
    public int getKills(){
        return kills;
    }
        
    public void scoreKill(){
        kills++;
    }
    
}
