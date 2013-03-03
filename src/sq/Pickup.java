/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sq;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;
import org.newdawn.slick.geom.Ellipse;
import org.newdawn.slick.geom.Shape;

/**
 *
 * @author Peter
 */
public class Pickup extends MovingEntity {
    
    private Sound pickupSound;
    
    private Pickup(Shape s, Gene g){
        super(s, g.maxSpeed, g.lifetime);
        try {
            pickupSound = new Sound("powerup.wav");
        } catch (SlickException ex) {
            ex.printStackTrace();
        }
    }
    
    public static Pickup createPickup(float x, float y){
        Gene g = new Gene();
        g.size = 12;
        g.color = Color.white;
        g.lifetime = 10000;
        return new Pickup(new Ellipse(x, y, g.getSize(), g.getSize()), g);
    }

    @Override
    public void draw(Graphics g) {
        if (!this.isDestroyed()){
            g.setColor(Color.white);
            g.draw(this.getShape());
        }
    }
    
    public void takenByPlayer(SqMain game){
        game.getPlayer().mutate();
        pickupSound.play();
        this.destroy();
    }
    
}
