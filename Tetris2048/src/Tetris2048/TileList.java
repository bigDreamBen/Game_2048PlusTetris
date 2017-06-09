/**
 * Dictionary.
 * 
 * "pplMv" refers to the movement required by players, e.g. a left movement
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
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
* Main usage of TileList Class:
*    1.1) get tileBeingCtrled obj by calling:
*            getLatestTile()
*    1.2) use functions of the tileBeingCtrled obj to modify its path:
*            setLeftMvOfAGrid(), setRightMvOfAGrid(), setDownMvOfGrids()
*    1.3) call below function to follow the path to move & display all the tiles:
*            updateAndDisplay()
*    2)   pause or resume by calling:
*            pauseOrContinue().
**/
class TileList
{
    //**  identiers declaration  **//
    //
    //**mode
    static final int EASY_MODE = 0;
    static final int HARD_MODE = 1;
    private int mode;
    private static final int[][] value248Ratio = {{1,3,6}, {6,3,1}};
    private static final double[] fallTimeGap_sec = {1, 0.5}; //time difference between adjacent free falls in sec
    
    //**time related
    private int frameCount, //as timeline.
                            //each time updateAndDisplay() runs, frameCount++
                            //but it won't be used to to determine when to free fall.
                            //due to the potentially unstable frequency of call of updateAndDisplay().
                            //it is only used in generating animation
                settledTilesFallFrame; //the frame at which settled tiles should fall
    private Calendar tileBingCtrledNextFallTime; //expected time of next tileBeingCtrled's free fall
    private boolean paused;
    private Calendar lastPauseTime;

    //**tiles
    private static final int FALL__FRAME_PER_GRID = 11  ; //number of frames to complete a free fall across a grid
    private static final int PPLMV__FRAME_PER_GRID = FALL__FRAME_PER_GRID;
    private static final int FALLDIST__PER_FRAME = GRID_SIZE / FALL__FRAME_PER_GRID; //dist for distance
    private static final int PPLMVDIST__PER_FRAME = GRID_SIZE / PPLMV__FRAME_PER_GRID; 
    
    private final Random randomer; //serve to generate random values for tiles
    //"tilesImgMap" as a container of the images of tiles.
    //running Console.loadImage(String imagePath) consumes a lot of resource, which may slow down fps.
    //hence, build a map and reuse loaded Image obj
    private static final HashMap<Integer, Image> tilesImgMap = new HashMap<Integer, Image>(11); 
    private boolean exceededUpperBoundary; //true when any one of Tile exceeds the upper boundary
    private int highestValue; //the highest value that has been made
    private List<Tile> tileList;
    private Tile latestTile;    //roughly speaking, it is tileBeingCtrled.
                                //However, sometimes, a player may have no control power over the latest tile.
                                //Considering this situation.
                                //tileBeingCtrled falls during a horizontal move over the lower boundary.
                                //as the fall is blocked, it is suggested to create next tile.
                                //therefore, at this moment, the player immediately loses his control power over
                                //the tile just tried to fall.
                                //Nevertheless, the next tile will not be created until the completion of the horizontal
                                //move.
                                //as a result, during this period, latestTile is not equivalent to tileBeingCtrled.
    private int nextValue;
    
    private Console console;
    private Scorer scorer;
    
    //**test use
    boolean nextValueListOn;
    private int[] nextValueList;
    int nextValueListCounter;
    //
    //**  END of identiers declaration  **//

    /**
    * Important functions of Tile Class:
    *   1) set path of a Tile obj:
    *       setLeftMvOfAGrid(), setRightMvOfAGrid() , setDownMvOfGrids()
    *   2) firstly,check if the path problematic, & if yes, modify it;
    *      secondly, update the position by following the modified path to move:
    *       updateCurrPosi().
    **/ 
    class Tile implements Comparable<Tile>{
        //**  identiers declaration  **//
        //
        private int value;                  
        //a Tile obj's coordinate & motion
        private final Position  currPosi, //curr for current, posi for position
                                predictedNextFramePosi;
        private boolean inLeftPplMv, inRightPplMv ,inDownPplMv, //true if in process of the pplMv
                        inFall,
                        fallHeldOn, //sometimes a tile is moving horizontally over other tile(s),
                                    //free falling is then held on until the completion of the horizontal move
                        absorbing, beingAbsorbed, //e.g. A moves & collides with B, then A absorbs B
                        beingCtrled, //true when this obj is under a player's control
                        createdNewerTile;   //when a Tile obj is going to settle down,
                                            //it will call a function of TileList to suggest to create the next Tile obj.
                                            //afterwards, this boolean is set true
        private int     remainFallDist, remainPplMvDist; //remaining distance needed to finish a certain move
                                                         //fall & pplMv distances are stored separately
                                                         //since they may occur simutaneously
        private Tile    tileAbsorbingThis, tileBeingAbsorbed;
        //
        //**  END of identiers declaration  **//

        class Position
        {
            private int top,left;
            private Position setTop(int top)    { this.top = top; return this; }
            private Position setLeft(int left)  { this.left = left; return this; }
            public int getTop()    { return top; }
            public int getLeft()   { return left; }
            @Override
            public int hashCode() { return top*10000 + left; }
            @Override
            public boolean equals(Object o)
            {
                if (o == null)
                    return false;
                return (this.hashCode() == ((Position)o).hashCode());
            }
        }

        Tile ()
        {
            currPosi = new Position();
            predictedNextFramePosi = null;
        }
        Tile (int value, int top, int left)
        {
            this.value = value;
            currPosi = new Position();
            currPosi.setTop(top).setLeft(left);
            predictedNextFramePosi = new Position();
            beingCtrled = true;
        }

        @Override
        public int hashCode() { return currPosi.hashCode(); } //it is expected no more than one tile located in the same position
        @Override
        public boolean equals(Object o)
        {
            if (o == null)
                return false;
            return (this.hashCode() == ((Tile)o).hashCode()); //it is expected no more than one tile located in the same position
                                                              //this definition of Tile obj comparsion will be used by
                                                              //tileList.get(..), .contains(..) functions,
                                                              //where tileList is an ArrayList<Tile> obj
        }
        @Override
        public int compareTo(Tile another)
        {
            //by redefining compareTo(..) in this way,
            //Arrays.sort(Object[] array) can then be used to sort the list in a way
            //that "tiles with lower position in the game field"
            //would be placed to "a frontier location of the list"
            return another.getCurrPosi().getTop() - this.getCurrPosi().getTop();
        }

        public int      getValue() { return value; }
        public Position getCurrPosi()  { return currPosi; }
        public boolean isAbsorbing() { return absorbing; }
        public boolean isBeingAbsorbed() { return beingAbsorbed; }

        private void doubleValue() 
        {
            value += value;
            addCurrScore(value); //a function of TileList.
        }
        private void settleDown() { beingCtrled = false; }
        private void beAbsorbedBy(Tile tileAbsorbingThis)
        { 
            tileAbsorbingThis = this.tileAbsorbingThis;
            beingAbsorbed = true;
        }
        private void absorbs(Tile tileBeingAbsorbed) 
        { 
            if (absorbing || beingAbsorbed)
                return;
            this.tileBeingAbsorbed = tileBeingAbsorbed;
            absorbing = true; 
            doubleValue();
        }
        //p.s.  an absorption is finished, when one entirely overlaps another.
        //      the tile being abosrbed is gone when the end of absorption.
        void endAbsorbing() 
        {
            if (!absorbing)
                return;
            //remove the tileBeingAbsorbed obj
            int tileBeingAbsorbedIdx; //the index in the ArrayList object, "tileList"                
            tileBeingAbsorbedIdx = tileList.indexOf(tileBeingAbsorbed); //indexOf(Object o) will only return the first occurence
                                                                        //of the object.
                                                                        //Here, the index returned may be for the tile which is absorbing
                                                                        //or the tile which is being absorbed.
            if (tileBeingAbsorbed != tileList.get(tileBeingAbsorbedIdx))
                tileBeingAbsorbedIdx = tileList.lastIndexOf(tileBeingAbsorbed);
            tileList.remove(tileBeingAbsorbedIdx);

            //update the status of this Tile obj
            absorbing = false;
            tileBeingAbsorbed = null;
        }

        //****  bewlow are functions related to movement  ****//
        //****  some are to:    1)set path;  ****//
        //****                  2)check collision & hence modify path;  ****//
        //****                  3)follow path to move  ****//

        //**  functions to set path  **//
        //
        public boolean readyForNewPplMv() 
        { 
            return !(inLeftPplMv || inRightPplMv || inDownPplMv || absorbing || beingAbsorbed || !beingCtrled);
        }
        public boolean setRightMvOfAGrid()
        {
            if (!readyForNewPplMv())
                return false;
            inRightPplMv = true;
            remainPplMvDist = GRID_SIZE;
            return true;
        }
        public boolean setLeftMvOfAGrid()
        {
            if (!readyForNewPplMv())
                return false;
            inLeftPplMv = true;
            remainPplMvDist = GRID_SIZE;
            return true;
        }
        public boolean setDownMvOfGrids()
        {
            //**  basic checking about if the execution of this downPplMv possible  **//
            //
            if (!readyForNewPplMv())
                return false;

            //when player moves tileBeingCtrled down, it will go as far as it can,
            //then stop immediately, & then it will try to fall down after 1s(EASY_MODE)/ 0.5s(HARD_MODE).
            //this is to ensure time for horizontal move after a donwPplMv.
            //however, there will be no free falling if the player keeps requiring downPplMv.
            //hence, refuse downPplMv if tileBeingCtrled is just above another tile/the lower boundary
            int currTop = currPosi.getTop();
            int currLeft = currPosi.getLeft();
            if ((currTop - NEW_TILE_TOP) % GRID_SIZE == 0)  //true when it is not laying over 2 grids
            {
                Tile imagObstacle;  //imaginary obstacle tile.
                                    //tileList.contains(imagObstacle) can be used to check if
                                    //this obstacle is really present
                imagObstacle = new Tile();
                imagObstacle.getCurrPosi().setTop(currTop + GRID_SIZE).setLeft(currLeft); 

                //refuse the downPplMv when
                //this Tile obj reaches the lower boundary
                //or this Tile obj is just right above another Tile obj with different value
                if (currTop == LOWER_BOUNDARY - GRID_SIZE || 
                    (tileList.contains(imagObstacle) &&
                    tileList.get(tileList.indexOf(imagObstacle)).getValue() != value))
                    return false;
            }
            //
            //**  END OF basic checking about if the execution of this downPplMv possible  **//


            inDownPplMv = true;
            remainPplMvDist = GRID_SIZE * NO_ROW; //destination is set below the lower boundary.
                                                  //once tileBeingCtrled tries to go beyond the boundary
                                                  //or collides with tile during downPplMv,
                                                  //its path will be modified to stop its dropping.
            //disable the free fall of tileBeingCtrled during downPplMv
            if (inFall) 
            {
                inFall = false;
                remainFallDist = 0;
            }
            return true;
        }
        /**A function to set the next fall time of the tileBeingCtrled.
         * please to be reminded, when it's time for one's free fall, it will only set a free falling path
         * for it. if the path would bring the tile collision, the free fall may then be canceled.
         * 
         * 3 situations to call setTileBeingCtrledNextFallTime():
         *  1) when it is confirmed that tileBeingCtrled now begins a fall
         *  2) when a downPplMv is just finished (calling this function to ensure time for horizontal move)
         *  3) when the next tile is just created
        **/
        private boolean setTileBeingCtrledNextFallTime()
        {
            if (!beingCtrled)
                return false;

            tileBingCtrledNextFallTime = Calendar.getInstance();
            tileBingCtrledNextFallTime.add(Calendar.MILLISECOND, (int)(fallTimeGap_sec[mode] * Math.pow(10,3)));
            return true;
        }
        //set free falling path when it's time to begin a fall
        private void getReadyIfTimeToBeginFall()
        {
            //reject to begin free falling if this Tile obj is in process of combination,
            //but free falling which begins before combination will not be stopped
            if (absorbing || beingAbsorbed)
                return; 

            //for tileBeingCtrled
            if (beingCtrled)
            {
                if ( tileBingCtrledNextFallTime.after(Calendar.getInstance())|| inFall)
                    return;

                //set free falling path
                if (!inDownPplMv)
                {
                    inFall = true; //the free fall of tileBeingCtrled is disabled during downPplMv
                                    //so that downPpl's speed looks constant
                    remainFallDist = GRID_SIZE;
                }
                setTileBeingCtrledNextFallTime();
            }
            //for settled tiles
            else
            {
                if (frameCount != settledTilesFallFrame ||
                    inFall)
                    return;
                inFall = true;
                remainFallDist = GRID_SIZE;
            }
        }
        //
        //**  END of functions to set path  **//


        //**  functions to check if any need to modify path  **//
        //
        //this function is called to check if the path would bring this Tile obj
        //collision with another Tile obj/boundaries of the game field, &
        //modify the path if any collision found.
        private void modifyPathIfColl()
        {
            updatePredictedNextFramePosi();
            checkCollAgainstBoundary();
            checkCollAgainstTile();
        }
        private void updatePredictedNextFramePosi()
        {
            predictedNextFramePosi.setTop(currPosi.getTop()).setLeft(currPosi.getLeft());
            int nextTop = predictedNextFramePosi.getTop();
            int nextLeft = predictedNextFramePosi.getLeft();

            if (inFall)
                predictedNextFramePosi.setTop(nextTop + FALLDIST__PER_FRAME);
            if (inLeftPplMv)
                predictedNextFramePosi.setLeft(nextLeft - PPLMVDIST__PER_FRAME);
            else if (inRightPplMv)
                predictedNextFramePosi.setLeft(nextLeft + PPLMVDIST__PER_FRAME);
            else if (inDownPplMv)
                predictedNextFramePosi.setTop(nextTop + PPLMVDIST__PER_FRAME);
        }
        //check if the path will bring this Tile obj collision with boundaries of game field
        private void checkCollAgainstBoundary()
        {
            int nextTop = predictedNextFramePosi.getTop();
            int nextLeft = predictedNextFramePosi.getLeft();
            int nextBottom = nextTop + GRID_SIZE;
            int nextRight = nextLeft + GRID_SIZE;

            if (nextBottom > LOWER_BOUNDARY)
            {    
                if (inFall)
                {
                    inFall = false;
                    remainFallDist = 0;
                    fallHeldOn = false;
                    beingCtrled = false; //prohibit further pplMv
                                         //& as signal to tell this tile is no longer under players' control,
                                        //i.e. a signal suggesting to create next tile
                }
                if (inDownPplMv)
                {
                    //if a downPplMv is performed during free falling,
                    //then in the end of the downPplMv.
                    //"remainPplMvDist" will be needed to adjust to ensure tileBeingCtrled won't go beyond the lower boundary.
                    remainPplMvDist = LOWER_BOUNDARY - (currPosi.getTop() + GRID_SIZE);
                }
            }
            if (nextLeft < LEFT_BOUNDARY)
            {
                inLeftPplMv = false;
                remainPplMvDist = 0;
            }
            else if (nextRight > RIGHT_BOUNDARY)
            {
                inRightPplMv = false;
                remainPplMvDist = 0;
            }
        }
        //check if the path will bring this Tile obj collision with another Tile obj
        private void checkCollAgainstTile()
        {
            int currTop = currPosi.getTop();
            int currLeft = currPosi.getLeft();
            int nextTop = predictedNextFramePosi.getTop();
            int nextLeft = predictedNextFramePosi.getLeft();
            int imagObstacleTop, imagObstacleLeft;  //imaginary obstacle's coordinate.
                                                    //tryStartAbsorb(..) will be used to verify
                                                    //if this obstacle is really present.

            //Please to be reminded that,
            //when absorption begins, tiles would maintain their original movement until the end of absorption.
            //therefore, not only when there is no collision between tiles found,
            //but also when this Tile obj is in an absorption process, this function mostly does nothing.

            //**check if the free falling path will bring this Tile obj collision
                //if yes, then depending on situation, this program may:
                    //1) hold on the free fall
                    //2) let this Tile obj absorbs another
                    //3) remove player's control power over the tile, & create the next tile
            if (inFall)
            {
                //get the maximum no of obstacles, in a range of 0 - 2, which hinder free falling
                int maxNoOfImagObstacle;
                maxNoOfImagObstacle = ((nextLeft - NEW_TILE_LEFT) % GRID_SIZE == 0) //true if this Tile obj will lay over 2 grids
                                        ? 1 : 2;

                for (int i = 0; i < maxNoOfImagObstacle; i++)
                {
                    //additional explanation for below calculation.
                    //In the calculation of "imagObstacleTop", "currTop" is used instead of "nextTop" when inDownPplMv is false,
                    //but why? Considering below example.
                    //e.g. there are grids: A B
                    //                      C D
                    //                      E F
                    //Assume:   there are 2 tiles in C,D.
                    //          tileBeingCtrled is falling & moving horizontally over A,B.
                    //Now, tileBeingCtrled is going to reach the lower edges of A,B.
                    //Then, if "nextTop" is used instead of "currTop",
                    //according to how this piece of code is written, the free falling will then be held on/ blocked
                    //right before reaching A,B's bottom edges.
                    imagObstacleTop =  ((((inDownPplMv) ? nextTop : currTop) - NEW_TILE_TOP) / GRID_SIZE + 1)
                                        * GRID_SIZE + NEW_TILE_TOP;
                    imagObstacleLeft = ((nextLeft - NEW_TILE_LEFT) / GRID_SIZE + i) * GRID_SIZE + NEW_TILE_LEFT;

                    if (maxNoOfImagObstacle == 1) //true means no horizontal move
                                                //or a horizontal move is finishing in this frame
                    {
                        fallHeldOn = false;
                        if (tryStartAbsorb(imagObstacleTop, imagObstacleLeft) == 0) //true when the free falling is blocked
                        {
                            inFall = false;
                            remainFallDist = 0;
                            beingCtrled = false; //remove player's control power over the tile
                                                //& as a signal to suggest to create the next tile
                        }
                        else if (tryStartAbsorb(imagObstacleTop, imagObstacleLeft) == -1) //true when the imagObstacle is not really present
                        {
                            setTileBeingCtrledNextFallTime();
                            if (beingCtrled)
                                settledTilesFall(); //a function of tileList
                                                    //which sets up a signal to tell settled tiles to fall
                        }
                    }
                    else //when a horizontal move is in process
                    {
                        if (tryStartAbsorb(imagObstacleTop, imagObstacleLeft) == 0)
                        {
                            fallHeldOn = true; //if any obstacle hindering the free falling,
                                                //hold on the free fall to ensure that
                                                //the horizontal move would not be disturbed by the blocking of free fall,
                                                //i.e. a complete horizontal move is ensured.
                                                //(free fall held on will be released when the horizontal move is finished)
                        }
                    }
                }
                if (maxNoOfImagObstacle == 2 || !fallHeldOn)
                {
                    setTileBeingCtrledNextFallTime();
                    settledTilesFall();
                }
            }

            //**determine when to stop a downPplMv
            if (inDownPplMv)
            {
                imagObstacleTop = (((nextTop - NEW_TILE_TOP) / GRID_SIZE) + 1) * GRID_SIZE
                                + NEW_TILE_TOP; //by setting imagObstacleTop in this way,
                                                //the obstacle will only be detected just right before collision
                imagObstacleLeft = nextLeft;
                if (tryStartAbsorb(imagObstacleTop, imagObstacleLeft) == 0)
                {
                    remainPplMvDist = imagObstacleTop - (currTop + GRID_SIZE);
                    setTileBeingCtrledNextFallTime();
                }
            }


            //**check if any need to cancel a horizontal pplMv
            //if the beginning of a horizontal movement is alright, then the rest of the horizontal move must be fine too.
            //hence, here we only need to check if it is problematic to allow a horizontal move to begin
            if (inLeftPplMv || inRightPplMv)
            {
                if ((currLeft - NEW_TILE_LEFT) % GRID_SIZE == 0) //true means this Tile obj isn't laying over 2 grids,
                                                                //i.e. not already in horizontal movement,
                                                                    //& a horizontal move about to begin
                {
                    if (inFall)
                    {
                        if ((currTop - NEW_TILE_TOP) % GRID_SIZE > 0)//true if already in a free falling process
                        {
                            //check if any obstacle
                            //just left/right to
                            //the lower grid over which this Tile obj is laying
                            imagObstacleTop = ((currTop - NEW_TILE_TOP) / GRID_SIZE + 1) * GRID_SIZE + NEW_TILE_TOP;
                            imagObstacleLeft = currLeft + ((inLeftPplMv) ? -GRID_SIZE : GRID_SIZE);
                        }
                        else //if horizontal move & free falling begin simultaneously
                        {
                            //check if any obstacle 
                            //that's pointed by
                            //the left/right, bottom corner of this Tile obj
                            imagObstacleTop = currTop + GRID_SIZE;
                            imagObstacleLeft = currLeft + ((inLeftPplMv) ? -GRID_SIZE : GRID_SIZE);
                        }
                    }
                    else //if about to begin horizontal move but no falling
                    {
                        //check if any adjacent obstacle
                        //left/right to this Tile obj
                        imagObstacleTop = currTop;
                        imagObstacleLeft = currLeft + ((inLeftPplMv) ? -GRID_SIZE : GRID_SIZE);
                    }

                    if (tryStartAbsorb(imagObstacleTop, imagObstacleLeft) == 0)
                    {
                        inLeftPplMv = false;
                        inRightPplMv = false;
                        remainPplMvDist = 0;
                    }
                }
            }
        }
        /**
         * This function examines the possibility of letting this Tile obj absorb another Tile obj,
         * whose position data is passed to this function as parameter, & let this Tile obj absorb
         * it if possible.
         * This does not check if absorption still in process .
         * To check & handle the end of absorption, use tryEndAbsorbing().
         * @param imagObstacle      it is not a real tile, but just
         *                          an imaginary tile which contains only position data.
         *                          This function will get a real tile, if any, based on the 
         *                          position data, & check if this Tile obj may absorb it.
         *                          Please to be reminded that the IMAGOBSTACLE IS ASSUMED TO BE LOCATED
         *                          IN SOMEWHERE COVERED BY THIS TILE OBJ'S CURRENT PATH,
         *                          i.e. ACCORDING TO THE CURRENT PATH, THIS TILE OBJ IS MOVING TOWARDS
         *                               THE IMAGOBSTACLE.
         * @return                  -1, if no real tile found based on the position data of "imagObstacle";
         *                          0, if real tile found but absorption not available;
         *                          1, absorption in process/ if real tile found & absorption is available
         */
        private int tryStartAbsorb(int imagObstacleTop, int imagObstacleLeft)
        {                
            if (absorbing) 
                return 1;

            Tile imagObstacle = new Tile();
            imagObstacle.getCurrPosi().setTop(imagObstacleTop).setLeft(imagObstacleLeft);
            if (tileList.contains(imagObstacle)) //true when real tile found
            {
                Tile obstacle;
                int obstacleIdx, obstacleValue;

                //retrieve the value of the obstacle to see if absorption possible
                obstacleIdx = tileList.indexOf(imagObstacle);
                obstacle = tileList.get(obstacleIdx);
                obstacleValue = obstacle.getValue();

                //check if the position of this Tile obj & the obstacle fit for absorption
                int nextTop, nextLeft;
                boolean bothPosiFitForabsorption;
                nextTop = predictedNextFramePosi.getTop();
                nextLeft = predictedNextFramePosi.getLeft();
                bothPosiFitForabsorption = (   (nextTop == imagObstacleTop
                                                && Math.abs(nextLeft-imagObstacleLeft) <= GRID_SIZE) 
                                            ||
                                                (nextLeft == imagObstacleLeft
                                                 && Math.abs(nextTop-imagObstacleTop) <= GRID_SIZE)
                                            ); //true when the tiles are adjacent to each other

                if (value != obstacleValue || obstacle.isBeingAbsorbed() || !bothPosiFitForabsorption) //true when absorption not available
                {
                    return 0;
                }
                else //when absorption available
                {
                    absorbs(obstacle);
                    obstacle.beAbsorbedBy(this);
                    return 1;
                }
            }
            else
                return -1;
        }
        //check if it's the end of the absorbing, BASED ON THIS TILE OBJ's CURRENT POSITION
        //& check if it's needed to modify the path, BASED ON THIS TILE OBJ's CURRENT PATH.
        //p.s. an absorption is finished, when one entirely overlaps/passes via another.
        private boolean tryEndAbsorbing()
        {
            if (!absorbing)
                return false;
            //in general, when it's the end of an absorption, the tile which absorbs another would
            //be located in the same position as the tile being absorbed,
            if (this.equals(tileBeingAbsorbed)) //true when they are at the same position
            {
                endAbsorbing();
            }
            //if tileBeingCtrled takes a downPplMv during free falling,
            //then the above general situation will not be held.
            //instead, the tile which absorbs another will just pass via the tileBeingAbsorbed.
            else if (inDownPplMv)
            {
                int currTop, nextTop, tileBeingAbsorbedTop;
                currTop = currPosi.getTop();
                nextTop = currTop + PPLMVDIST__PER_FRAME;
                tileBeingAbsorbedTop = tileBeingAbsorbed.getCurrPosi().getTop();
                if (nextTop  > tileBeingAbsorbedTop) //true when this Tile obj is going to pass through tileBeingAbsorbed.
                {
                    //check if any obstacle right below the tileBeingAbsorbed for this Tile obj 
                    Tile imagObstacle, realObstacle;
                    int imagObstacleTop, imagObstacleLeft;
                    boolean imagObstacleIsReal; //true when imagObstacle is really present

                    imagObstacleTop = tileBeingAbsorbedTop + GRID_SIZE;
                    imagObstacleLeft = currPosi.getLeft();
                    imagObstacle = new Tile();
                    imagObstacle.getCurrPosi().setTop(imagObstacleTop).setLeft(imagObstacleLeft);

                    imagObstacleIsReal = tileList.contains(imagObstacle);
                    if (imagObstacleIsReal)
                    {
                        realObstacle = tileList.get(tileList.indexOf(imagObstacle));
                        if (value != realObstacle.getValue())
                        {
                            endAbsorbing();
                            remainPplMvDist = imagObstacleTop - (currTop + GRID_SIZE); //modify path to make this Tile obj
                                                                                //not overlaping another Tile obj with different value
                            setTileBeingCtrledNextFallTime();
                        }
                        else
                        {
                            endAbsorbing();
                            absorbs(realObstacle);
                            realObstacle.beAbsorbedBy(this);
                        }
                    }
                    else
                    {
                        endAbsorbing();
                    }
                }
            }
            return true;
        }
        //
        //**  END of functions to check if any need to modify path  **//


        //**  function related to path following  **//
        //
        //updates "currPosi" to be next frame's
        private void updateCurrPosi()
        {   
            getReadyIfTimeToBeginFall();
            modifyPathIfColl();

            //update currPosi
            if (inFall && (!beingCtrled || !fallHeldOn))
            {
                currPosi.setTop(currPosi.getTop() + FALLDIST__PER_FRAME);
                remainFallDist -= FALLDIST__PER_FRAME;
            }
            if (inLeftPplMv)
            {
                currPosi.setLeft(currPosi.getLeft() - PPLMVDIST__PER_FRAME);
                remainPplMvDist -= PPLMVDIST__PER_FRAME;
            }
            else if (inRightPplMv)
            {
                currPosi.setLeft(currPosi.getLeft() + PPLMVDIST__PER_FRAME);
                remainPplMvDist -= PPLMVDIST__PER_FRAME;
            }
            else if (inDownPplMv)
            {
                if (remainPplMvDist - PPLMVDIST__PER_FRAME >= 0)
                {
                    currPosi.setTop(currPosi.getTop() + PPLMVDIST__PER_FRAME);
                    remainPplMvDist -= PPLMVDIST__PER_FRAME;
                }
                else
                {
                    //sometimes, the last frame movement of a downPplMv is not
                    //of distance equal to PPLMVDIST__PER_FRAME due to free fall
                    currPosi.setTop(currPosi.getTop() + remainPplMvDist);
                    remainPplMvDist -= remainPplMvDist;
                }
            }
            tryEndAbsorbing();

            //check if this is the end of a certain move
            if (remainFallDist <= 0)
            {
                inFall = false;
            }
            if (remainPplMvDist <= 0)
            {
                if (inDownPplMv)
                {
                    inDownPplMv = false;
                    setTileBeingCtrledNextFallTime(); //ensure time for player to perform horizontal move after down pplMv
                }
                inLeftPplMv = false;
                inRightPplMv = false;
            }

            updateHighestValue(this); ///a function of TileList

            //check if it's time to create next tile for players to play with
            //& check if the player loses this game
            if (!beingCtrled && !createdNewerTile)
            {
                //ensure a complete horizontal move before letting next tile come
                //& let next tile get into play
                if (!(inLeftPplMv || inRightPplMv))
                {
                    checkIfExceedUpperBoundary(this); //a function of TileList                
                    createNextTile(); //a function of tileList. called to suggest to create next tile
                    createdNewerTile = true; //restrict the amount of tile one Tile obj suggests to create
                }
            }
        }
        //
        //**  END of function related to path following  **//
    } //end of Tile class

    TileList(int mode, Scorer scorer) throws FileNotFoundException, IOException
    {
        setMode(mode);
        this.scorer = scorer;
        console = Console.getInstance();
        randomer= new Random(System.currentTimeMillis());
        tileList = new ArrayList<Tile>(MAX_NO_TILES);
        nextValue = getRandomValue();
        createNextTile();
        //initialize the images of tiles of various values
        for (int i = 2; i <= 2048; i*=2)
        {
            String imgPath = TILE_IMG_DIR_RELATIVE_PATH + i + TILE_IMG_FILE_EXTENSION;
            Image img = Console.loadImage(imgPath);
            tilesImgMap.put(i, img);
        }
    }

    //**  private methods  **//
    //
    private int getRandomValue()
    {
        //**  code for test use  **//
        //
        if (nextValueListOn && nextValueListCounter <= nextValueList.length - 1)
        {
            return nextValueList[nextValueListCounter++];
        }
        //
        //**  NED of code for test use  **//
            
        
        int value2Ratio, value4Ratio, value8Ratio;
        value2Ratio = value248Ratio[mode][0];
        value4Ratio = value248Ratio[mode][1];
        value8Ratio = value248Ratio[mode][2];
        
        int randomNum = randomer.nextInt(value2Ratio + value4Ratio + value8Ratio);        
        if (randomNum < value2Ratio)
            return 2;
        else if (randomNum < value2Ratio + value4Ratio)
            return 4;
        else
            return 8;
    }
    private boolean createNextTile()
    {
        if (tileList.size() >= MAX_NO_TILES)
            return false;

        //create new Tile based on pre-generated random value
        Tile newTile = new Tile(nextValue, NEW_TILE_TOP, NEW_TILE_LEFT);
        latestTile = newTile;
        tileList.add(latestTile);
        settledTilesFall();
        latestTile.setTileBeingCtrledNextFallTime();

        //generate another random value for next tile
        nextValue = getRandomValue();

        return true;
    }
    //set up a signal to tell settled tiles to fall
    private void settledTilesFall()
    {
        settledTilesFallFrame = frameCount; 
    }
    private void addCurrScore(int add)
    {
        scorer.addCurrScore((add >= 0) ? add : 0);
    }
    private void updateHighestValue(Tile tile)
    {
        if (tile.isAbsorbing() || tile.isBeingAbsorbed())
            return;
        highestValue = (highestValue >= tile.getValue()) ? highestValue : tile.getValue();
    }
    private void checkIfExceedUpperBoundary(Tile tile)
    {
        if (tile.getCurrPosi().getTop() < UPPER_BOUNDARY)    
            exceededUpperBoundary = true;
    }
    //
    //**  END of private methods  **//


    //**  public methods  **//
    //
    /**
     * effect of setMode(int mode) reasonably appears at & after:
     *  next next tile (random value)
     *  next next free fall (time gap between adjacent free falls, 0.5s & 1s)
     */
    public boolean setMode(int mode)
    {
        switch(mode)
        { 
            case EASY_MODE:
            case HARD_MODE:
                this.mode = mode;
                return true;

            //auto set to easy mode if invalid mode input
            default:
                setMode(EASY_MODE);
                return false;
        }
    }
    public int getMode(){ return mode; }
    public Tile getLatestTile() { return latestTile; }
    public boolean pauseOrContinue() { 
        if (!paused)
        {
            lastPauseTime = Calendar.getInstance(); //used to update nextFallTime when continuing game
        }
        else
        {
            //update nextFallTime
            long now_ms = Calendar.getInstance().getTimeInMillis();
            long lastPauseTime_ms = lastPauseTime.getTimeInMillis();
            tileBingCtrledNextFallTime.add(Calendar.MILLISECOND, (int) (now_ms - lastPauseTime_ms));
        }
        paused = !paused; 
        return true;
    }
    /**
    * Using functions to set path for the tileBeingControlled
    * before calling updateAndDisplay() to follow the path & display all tiles.
    **/
    public void updateAndDisplay()
    {
        boolean pausedWhenCallThis = paused;    //to ensure consistency that all tiles displayed are out of the same frame.
                                                //if player pauses game during the running of this function,
                                                //this boolean variable will ensure the consistency.

        //update latestTile's position first for 2 reasons:
        //others' free falling depends on it.
        //it has the priority to cause/join an absorption in case 3 tiles collide simultaneously.
        if (!pausedWhenCallThis)
            latestTile.updateCurrPosi();

        /**Handle the rest of tiles.
         * 
         * By considering the problem about free falling of the rest of tiles,
         * we would find that the lower tiles in a column should be handled first.
         * Considering this example:
         * A player moves tileBeingCtrled to left & the tile combines with another to form a new tile.
         * Assume there are 2 tiles above the newly made tile.
         * Soon the player has the tile moved left again, & therefore the above 2 tiles should then fall down.
         * In case, the upper one is handled first. 
         * Then when it tries to fall, it finds there is a tile below it which blocks its free fall.
         * As a consequence, the 2 tiles do not fall simultaneously.
         * Therefore, tiles in a column should be handled from low to high.
         * 
         * To handle the tiles in a proper sequence, compareTo(Object obj) is overrided in Tile class
         * so that Arrays.sort(Object[] array) would then be able to sort the tiles to be in a 
         * proper sequence. 
         */
        //Reason why using Arrays.sort(Object[] array) instead of Collection.sort(List list):
        //1)Collection.sort(..) may provoke ConcurrentModificationException
        //2)Collection.sort(..) may affect the running of endAbsorbing() by shuffling the indices of tileList
        Object[] tileArray = tileList.toArray();
        Arrays.sort(tileArray);
        for (Object tileObj : tileArray)
        {
            Tile tile = (Tile)tileObj;
            if (tile == latestTile)
                continue;
            if (!pausedWhenCallThis)
                tile.updateCurrPosi();
            Integer value = tile.getValue();
            Image tileImg = tilesImgMap.get(value);
            console.drawImage(tile.getCurrPosi().getLeft(),tile.getCurrPosi().getTop(),tileImg); //display tiles
        }
        //display next tile's value
        console.drawText(NEXT_VALUE_LEFT,NEXT_VALUE_BOTTOM,String.valueOf(nextValue), 
                        new Font(null,3,26), new Color(0x00,0x00,0x00,0xFF));
        //display mode
        if (mode == EASY_MODE) 
            console.drawText(MODE_LEFT,MODE_BOTTOM,"Easy", 
                            new Font(null,2,26), new Color(0x00,0x88,0x88,0xFF));
        else if (mode == HARD_MODE)
            console.drawText(MODE_LEFT,MODE_BOTTOM,"HARD", 
                            new Font(null,3,28), new Color(0xFF,0x00,0x00,0xFF));
        //having the latestTile's image drawn in the end so that it won't be covered by the others.
        console.drawImage(  latestTile.getCurrPosi().getLeft(),
                            latestTile.getCurrPosi().getTop(),
                            tilesImgMap.get(latestTile.getValue())); //display tiles

        if (!pausedWhenCallThis)
            frameCount++;
    }
    public int getHighestValue()
    {
        return highestValue;
    }
    public boolean exceededUpperBoundary()
    {
        return exceededUpperBoundary;
    }
    public void cleanToReuse()
    {
        setMode(EASY_MODE);
        tileList.clear();
        tileList = new ArrayList<Tile>(MAX_NO_TILES);
        nextValue = getRandomValue();
        createNextTile();
        scorer.cleanToReuse();
        frameCount = 0;
        settledTilesFallFrame = 0;
        paused = false;
        highestValue = 0;
        exceededUpperBoundary = false;
    }
    //for test only
    public boolean testBySetNextValue(int i)
    {
        nextValue = i;
        return true;
    }
    public void nextValueListOn(int[] nextValueList)
    {
        nextValueListOn = true;
        this.nextValueList = nextValueList;
        nextValue = nextValueList[nextValueListCounter++];
        tileList.remove(tileList.indexOf(latestTile));
        createNextTile();
    }
    //
    //**  END of public methods  **//
    
    //for class, AIPlayer, only
    ArrayList<Tile> getTileList()
    {
        return new ArrayList(tileList); //return a clone to avoid concurrent modification to "tileList"
    }
}