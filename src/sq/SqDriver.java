package sq;

import org.newdawn.slick.*;
import org.newdawn.slick.state.*;

/**
 * Contains the main class as the driver, and initialize states.
 * @author Peter
 */
public class SqDriver extends StateBasedGame{
 
    public static final int STATE_MAIN = 1;
    
    public SqDriver()
    {
        super("");
    }
    
    public static void main(String[] args) throws SlickException
    {
        AppGameContainer app = new AppGameContainer(new SqDriver());
        app.setDisplayMode(SqMain.FIELD_WIDTH, SqMain.FIELD_HEIGHT, false);
        app.setSmoothDeltas(true);
        app.setTargetFrameRate(60);
        app.setShowFPS(false);
        app.setIcons(new String[]{"icon.png", "icon24.png", "icon32.png"});
        app.start();
    }
    
    public boolean closeRequested(){
        main.cleanup();
        return true;
    }
    
    private SqMain main = new SqMain(STATE_MAIN);
    
    @Override
    public void initStatesList(GameContainer gameContainer) throws SlickException {
        this.addState(main);
        this.enterState(STATE_MAIN);
    }
     
}