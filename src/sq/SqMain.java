package sq;

import glabs.RandomUtility;
import sq.waveGenerators.DefaultGenerator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.newdawn.slick.*;
import org.newdawn.slick.state.*;
import java.awt.Font;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.newdawn.slick.font.effects.ColorEffect;
import sq.Gene.Weapon;
 
/**
 * Sq. Handles main game loop logic. Keep tracks of the game world.
 * @author Peter
 */
public class SqMain extends BasicGameState 
{
    public static final int FIELD_WIDTH = 600;
    public static final int FIELD_HEIGHT = 600;
    
    private Player player;
    private Set<AI> playerBullets;
    
    private Set<AI> enemies;

    private int stateId;
    
    private int wave;
    private WaveGeneratorInterface waveGenerator;
    
    private UnicodeFont scoreFont, waveFont;
    
    private int gameoverWaitCountdown;
    private boolean gameover;
    
    private Set<AIGene> enemyGenes;
    
    private Set<Pickup> pickups;
    
    private int gameTime;
    
    @Override
    public int getID() {
        return stateId;
    }

    public SqMain(int stateId)
    {
        this.stateId = stateId;
    }
    
    public Set<AI> getPlayerBullets(){
        return playerBullets;
    }

    @Override
    public void init(GameContainer gc, StateBasedGame sbg) throws SlickException
    {
        player = Player.createInitialPlayer();
        wave = 0;
        waveGenerator = new DefaultGenerator();
        playerBullets = new HashSet<>();
        enemies = new HashSet<>();
        pickups = new HashSet<>();
        
        scoreFont = new UnicodeFont(new Font("Century Gothic", Font.PLAIN, 24));
        scoreFont.getEffects().add(new ColorEffect(java.awt.Color.white));
        scoreFont.addAsciiGlyphs();
        scoreFont.loadGlyphs();
        
        waveFont = new UnicodeFont(new Font("Century Gothic", Font.PLAIN, 128));
        waveFont.getEffects().add(new ColorEffect(java.awt.Color.DARK_GRAY));
        waveFont.addAsciiGlyphs();
        waveFont.loadGlyphs();
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("genes.sgn"))){
            enemyGenes = (Set<AIGene>) ois.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException ex){
            enemyGenes = new HashSet<>();
        }
        
        waveGenerator.loadData();
        
        gameTime = 0;
        
        this.checkNextWave();
    }
    
    public int getGameTime(){
        return gameTime;
    }

    @Override
    public void update(GameContainer gc, StateBasedGame sbg, int delta) throws SlickException
    {
        this.checkCollisions();
        this.cleanupDestroyedInstances();
        this.checkNextWave();
        this.handlePlayerControl(gc, delta);
        this.updateEverything(delta);
        gameTime += delta;
    }

    @Override
    public void render(GameContainer gc, StateBasedGame sbg, Graphics g) throws SlickException
    {
        //draw text
        String killsStr = "Kills: " + player.getKills();
        String livesStr = "Lives: " + player.getLives();
        String waveStr = Integer.toString(wave);
        scoreFont.drawString(8, FIELD_HEIGHT - scoreFont.getHeight(killsStr) - 8, killsStr);
        scoreFont.drawString(FIELD_WIDTH - scoreFont.getWidth(livesStr) - 8, FIELD_HEIGHT - scoreFont.getHeight(livesStr) - 8, livesStr);
        waveFont.drawString(FIELD_WIDTH / 2 - waveFont.getWidth(waveStr) / 2, FIELD_HEIGHT / 2 - waveFont.getHeight(waveStr) / 2, waveStr);
        
        //draw 
        for (AI b : playerBullets){
            b.draw(g);
        }
        for (AI e : enemies){
            e.draw(g);
        }
        for (Pickup p : pickups){
            p.draw(g);
        }
        
        player.draw(g);
        
    }
    
    private void handlePlayerControl(GameContainer gc, int delta){
        Input input = gc.getInput();
        if (input.isKeyDown(Input.KEY_W)){
            player.control(3 * Math.PI / 2, delta);
        }
        if (input.isKeyDown(Input.KEY_A)){
            player.control(Math.PI, delta);
        }
        if (input.isKeyDown(Input.KEY_S)){
            player.control(Math.PI / 2, delta);
        }
        if (input.isKeyDown(Input.KEY_D)){
            player.control(0, delta);
        }
        if (input.isKeyDown(Input.KEY_ENTER)){
            gc.setPaused(!gc.isPaused());
        }
        if (input.isMouseButtonDown(0)){
            for (Weapon w : player.getGene().weapons){
                Set<AI> b = player.fire(w, input.getMouseX(), input.getMouseY());
                if (b != null){
                    playerBullets.addAll(b);
                }
            }
        }
    }
    
    private void checkCollisions(){
        Set<AI> addedEnemies = new HashSet<>();
        for (AI i : playerBullets){
            for (AI j : enemies){
                if (i.getShape().intersects(j.getShape()) && !i.isDestroyed() && !j.isDestroyed()){
                    addedEnemies.addAll(i.destroy(this, true));
                    addedEnemies.addAll(j.destroy(this, true));
                    if (j.getGene().getLifetime() < 0 && j.isDestroyed()){
                        i.getOwner().scoreKill();
                        if (RandomUtility.chance(5)){
                            Pickup e = Pickup.createPickup(j.getX(), j.getY());
                            pickups.add(e);
                        }
                    }
                }
            }
        }
        enemies.addAll(addedEnemies);
        
        if (!player.isInvincible()){
            for (AI i : enemies){
                if (i.getShape().intersects(player.getShape()) && !i.isDestroyed() && !player.isDestroyed()){
                    playerBullets.addAll(player.destroy());
                    AI owner = i;
                    while (owner.getOwner() != null){
                        owner = (AI) owner.getOwner();
                    }
                    owner.scoreKill();
                    if (player.lostLife()){
                        gameover = true;
                        gameoverWaitCountdown = 3000;
                    }
                }
            }
        }
        
        for (Pickup p : pickups){
            if (!p.isDestroyed() && p.getShape().intersects(player.getShape())){
                p.takenByPlayer(this);
            }
        }
    }
    
    private void cleanupDestroyedInstances(){
        Iterator<AI> it = playerBullets.iterator();
        while (it.hasNext()){
            AI b = it.next();
            if (b.isDestroyed()){
                it.remove();
            }
        }
        Iterator<Pickup> pit = pickups.iterator();
        while (pit.hasNext()){
            Pickup p = pit.next();
            if (p.isDestroyed()){
                pit.remove();
            }
        }
    }
    
    private void updateEverything(int delta){
        player.update(delta, this);
        for (AI b : playerBullets){
            b.update(delta, this);
        }
        
        Set<AI> addedEnemies = new HashSet<>();
        for (AI e : enemies){
            addedEnemies.addAll(e.update(delta, this));
        }
        enemies.addAll(addedEnemies);
        
        if (gameover){
            this.gameoverWaitCountdown -= delta;  
            if (this.gameoverWaitCountdown <= 0){
                cleanup();
                System.exit(0);
            }
        }
    }
    
    public void cleanup(){
        new Hiscore().addNewEntry(wave, player.getKills()).save();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("genes.sgn"))){
            oos.writeObject(enemyGenes);
        } catch (IOException ex){
            //ignore
        }
        waveGenerator.saveData();
    }
    
    private void checkNextWave(){
        for (AI i : enemies){
            if (!i.isDestroyed() && !i.isFriendly()){
                return;
            }
        }

        wave++;
        enemies = new HashSet<>(waveGenerator.generateWave(wave, player, enemies, enemyGenes));

    }
    
    public int getWave(){
        return wave;
    }
    
    public int getPlayerKills(){
        return player.getKills();
    }
    
    public Player getPlayer(){
        return player;
    }

}