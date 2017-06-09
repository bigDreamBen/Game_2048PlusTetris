/*
 * Group 22 
 */


/**Dictionary.
 * "pplMv" below refers to the movement required by players, e.g. a left movement
 * "fall" / "free fall" refers to the falling every 1s(EASY_MODE) or 0.5s(HARD_MODE)
 * "obj" means object(s)
 * "combination" refers to the process of combining 2 tiles of the same values to one.
 * "absorption". when tile A moves to B & causes combination. Then it is said A "absorbs" B.
 * "settled tiles" refers to tiles that players are no longer be able to move
 * "tileBeingCtrled" refers to the tile that is currently under a player's control
 * "top" refers to the upper edge coordinate of a tile
 * "left" refers to the left edge coordinate of a tile
 * "nextValue" refers to the value assigned to the next tile
 * "frame". roughly speaking, each time cycle() runs, a frame is made.
 * "curr" means current.
 * "posi" means position.
 * "coll" for collision.
 * "imagObstacle" for imaginary obstacle tile, which will be checked if it is really present.
 * "img" for image.
 */

/**Below is part of OUR GAME DESIGN, on the top of the given game feature requirement.
* the tileBeingCtrled may not take more than 1 pplMv at the same time.
* 
* free falling & down/left/rightPplMv of the tileBeingCtrled may occur simultaneously.
* 
* there is a consistency between the free falling of all the tiles that
* settled tiles fall when the tileBeingCtrled falls.
* i.e. the fall of settled tiles mostly depends on tileBeingCtrled
* 
* free fall of tileBeingCtrled is disabled during downPplMv so that speed of downPplMv looks constant.
* Nevertheless, settled tiles would still fall when it's time to fall, &
* downPplMv is yet still available during free falling.
* 
* when a player moves a tile down, it will go as far as it can,
* then stops immediately, & then it will try to fall down after 1s(EASY_MODE)
* or 0.5s(HARD_MODE). this ensures time for players to perform horizontal move
* after a downPplMv.
* 
* when tileBeingCtrled is located just above the lower boundary of the game field
* or another tile with a different value, downPplMv will be disabled. 
* this avoids the problem of no free falling problem provoked by infinite downPplMv.
* 
* there are 2 concepts closely related to fall which are "blocking" & "holding on".
* details seen below.
* 
* when a free fall of the tileBeingCtrled is blocked by the lower boundary/
* another tile, the player will immediately lose the control power over the tile.
* after the completion of the tile's last horizontal movement, if any,
* a new tile would then be created for the player to control immediately 
* 
* in case, it's time for tileBeingCtrled to fall while it is moving horizontally over
* other tiles. the free falling is then held on until the completion of 
* the horizontal move. this avoids incomplete horizontal move stemmed from
* the blocking of free fall.
* 
* pressing key e/h to switch between hard & easy mode.
* 
* when tile A of the same value move towards B, & combine with it. it is said
* A absorbs B, and hence by implication, in our design, B would be gone in the end.
* 
* when absorption begins, tiles would maintain their original movement until the end
* of absorption.
* 
* the absorption is finished, when one entirely overlaps/passes through another.
* 
* the amount of score increment is equal to the value of the newly combined tile. As it is harder
* to make a higher value, such score increment method is adopted to give players greater
* reward when a tile of higher value is made.
* 
* the tileBeingCtrled always has the priority to cause/join an absorption in case 3 tiles
* with the same value collide simultaneously.
* 
* the speed of fall & downPplMv are set to be the same to ensure:
* whenever a Tile object is ready to take a pplMv, it is laying over one single grid only.
* consider this example.
* in case, 2 settled tiles combine due to free fall. then, tileBeingCtrled takes a downPplMv, &
* therefore moves towards the 2 settled tiles quickly. if downPplMv speed > fall speed, tileBeingCtrled
* may collide with them before the finish of the absorption. when they collide, tileBeingCtrled would
* then stop moving, become laying over 2 grids, & ready to take another pplMv.
* To prevent such situation from happening, in our design, pplMv speed is made equal to fall speed.
* 
* fps shouldn't be lower than 2*2x + 1 where x = the number of frames needed to complete a pplMv.
* this is to ensure there is possibility for players to move tileBeingCtrled to the rightmost
* column before its 1st free fall in hard mode.
*/


/**Below is an OVERVIEW OF OUR PROGRAM DESIGN.
 * Every tile, including settled tile, has its own path.
 * For example, the path for a settled tile is falling down when tileBeingCtrled falls.
 * However, sometimes, the falling would be blocked by another tile/ the lower boundary of 
 * the game field. 
 * In case, any problem in the path is found, then the path will be modified, e.g.
 * a settled tile would like to fall down & go beyond the lower boundary,
 * then, its free fall will be canceled.
 * 
 * In our design, player is allowed to modify the path of tileBeingCtrled, e.g. move
 * it left by a grid. Then, this program will check if this movement problematic, &
 * modify the path if needed. afterwards, the tiles' position will be updated according
 * to the modified paths. in the end, the tiles will be displayed. These procedures
 * are going to be repeated when running this program.
 */

package Tetris2048;

import static Tetris2048.GameFieldData.*;
import game.v2.Console;
import game.v2.Game;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Time;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
        

/**
 * Game skeleton of 2048
 *
 * @author Van
 */
public class Tetris2048 extends Game {

    private static TileList tileList;
    private static Scorer scorer;
    private static AIPlayer ai;
    private static boolean aiOn;
    private static boolean paused;
    private static Calendar lastCycleTime;
    private static Time gameDuration;
    private static long gameDuration_milliSec = 0;
    private static Sound bgm, soundEffect;
    /*
     Main method
     */
    public static void main(String[] args) throws IOException {

        try {
            soundEffect = new Sound();
            soundEffect.setSong(SOUND_EFFECT_NAME, Sound.SOUND_EFFECT);
            scorer = new Scorer(soundEffect);
            tileList = new TileList(TileList.EASY_MODE, scorer);
            ai = new AIPlayer(tileList);        
            JOptionPane.showMessageDialog(null, "Press 'E' and 'H' to switch between easy and hard modes.\n"
                            + "Press 'A' to turn on or off AI mode.\n"
                            + "The victory condition is creating a tile with value 2048~\n"
                            + "Please Enjoy this game ~ :)", "Tetris2048", JOptionPane.INFORMATION_MESSAGE);
            gameDuration = new Time(0);
            bgm = new Sound();
//            if (bgm.setSong(BGM_RELATIVE_PATH, Sound.BGM)) {
            if (bgm.setSong(BGM_NAME, Sound.BGM)) {
                bgm.start();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Tetris2048.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Tetris2048.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        
        //This is to set the beginning values
//        tileList.nextValueListOn(new int[]{2,4,8,2,8,2});
        
        /*
         Customize the console window per your need but do not show it yet.
         */
        Console.getInstance()
                .setTitle("Tetris 2048")
                .setWidth(450)
                .setHeight(700)
                .setTheme(Console.Theme.LIGHT);

        /*
         Similar to the Console class, use the chaining setters to configure the game. Call start() at the end of
         the chain to start the game loop.
         */
        new Tetris2048()
                .setFps(FPS) // set frame rate
                .setShowFps(true) // set to display fps on screen
                .setBackground(Console.loadImage(BOARD_IMG_RELATIVE_PATH)) // set background image
                .start();                                               // start game loop
    }

    /**
     * **********************************************************************************************
     * There are three abstract methods must be overriden: protected abstract
     * void cycle(); protected abstract void keyPressed(KeyEvent e); protected
     * abstract void mouseClicked(MouseEvent e);
     */
    @Override
    protected void cycle() {
        if (aiOn)
        {
            console.drawText((int)(LEFT_BOUNDARY + GRID_SIZE * 1.6), NEW_TILE_TOP + 22,
                            "AI on", new Font(null,2,26), Color.yellow);
            if (!paused)
            {
                int input = ai.getAINextInput();
                if (input > 0)
                    if (moveTile(input))
                        ai.nextInputTaken();
            }
        }
        
        tileList.updateAndDisplay();
        if (tileList.getHighestValue() >= 2048)
        {
            showWinMsg();   
        }
        if (tileList.exceededUpperBoundary())
        {
            showLoseMsg();   
        }
        
        scorer.displayScores();
        handleGameDuration();
                
        lastCycleTime = Calendar.getInstance();
    }
    
    @Override
    protected void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_A)
            aiOn = !aiOn;
        setMode(keyCode);
        testUse(keyCode);
        pauseOrContinue(keyCode);
        if (!paused)
        {   
            if (!aiOn)
                moveTile(keyCode);
        }
        
    }
    
    @Override
    protected void mouseClicked(MouseEvent e) {
    }
 
    private boolean moveTile(int keyCode)
    {
        TileList.Tile latestTile = tileList.getLatestTile();
        switch(keyCode)
        {
            case KeyEvent.VK_LEFT:
                return latestTile.setLeftMvOfAGrid();
            case KeyEvent.VK_RIGHT:
                return latestTile.setRightMvOfAGrid();
            case KeyEvent.VK_DOWN:
                return latestTile.setDownMvOfGrids();   
        }
        return false;
    }
    private boolean setMode(int keyCode)
    {
        switch(keyCode)
        {
            case KeyEvent.VK_E:
                return tileList.setMode(TileList.EASY_MODE);
            case KeyEvent.VK_H:
                return tileList.setMode(TileList.HARD_MODE);
        }
        return false;
    }
    private boolean pauseOrContinue(int keyCode)
    {
        switch(keyCode)
        {
            case KeyEvent.VK_SPACE:
                paused = !paused;
                return tileList.pauseOrContinue();
        }
        return false;
    }
    //function for test only
    private boolean testUse(int keyCode)
    {
        switch(keyCode)
        {
            case KeyEvent.VK_2:
                return tileList.testBySetNextValue(2);
            case KeyEvent.VK_4:
                return tileList.testBySetNextValue(4);
            case KeyEvent.VK_8:
                return tileList.testBySetNextValue(8);
                
            case KeyEvent.VK_0:
                return tileList.testBySetNextValue(1024);
                
//            case KeyEvent.VK_1:
//                ai.testColList();
//                return true;
            default:
                return false;
        }   
    }
    
    private void handleGameDuration()
    {
        if (lastCycleTime == null)
            lastCycleTime = Calendar.getInstance();
        increaseGameDuration();
        displayGameDuration();
    }
    
    private void increaseGameDuration()
    {
        if (paused)
            return;
        gameDuration_milliSec += Calendar.getInstance().getTimeInMillis() - lastCycleTime.getTimeInMillis();
        gameDuration.setTime(gameDuration_milliSec);
    }
    
    private void displayGameDuration()
    {
        String mm, ss;
        int min, sec;
        min = gameDuration.getMinutes();
        sec = gameDuration.getSeconds();
        mm = (min >= 10) ? "" + min : "0" + min; 
        ss = (sec >= 10) ? "" + sec : "0" + sec; 
        console.drawText((int)(LEFT_BOUNDARY + GRID_SIZE * 1.6), NEW_TILE_TOP - 22,
                            mm + ":" + ss, new Font(null,2,26), Color.BLACK);
    }
    private void showLoseMsg()
    {
        askIfRetry("Your score is: " + scorer.getCurrScore());
    }
    
    private void showWinMsg()
    {
        askIfRetry("Congratulation! You just WON the game!!!:D\n" +
                    "Your score is: " + scorer.getCurrScore());
    }
    
    private boolean askIfRetry(String winLoseMsg)
    {
        int retryAns = JOptionPane.showConfirmDialog(null, winLoseMsg + "\nRetry?", "Tetris2048", 
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (retryAns == JOptionPane.YES_OPTION)
        {
            tileList.cleanToReuse();
            scorer.cleanToReuse();
            return true;
        }
        else
        {
            JOptionPane.showMessageDialog(null, "Thanks for playing :)", "Tetris2048", 
                                        JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
        return false;
    }
}
