/**
 * 
 * 
 * As a data container.
 * 
 * 
 * 
 */
package Tetris2048;

/**
 *
 * @author ILoveIdunna
 */
public class GameFieldData {
    //**time related
    static final int FPS = 85;
    
    //**game field
    static final int GRID_SIZE = 110;
    static final int NO_ROW = 5;
    static final int NO_COL = 4;
    static final int MAX_NO_TILES = 1 + (NO_ROW-1) * NO_COL;
    static final int LEFT_BOUNDARY = 10; //left boundary of the game field
    static final int RIGHT_BOUNDARY = LEFT_BOUNDARY + NO_COL*GRID_SIZE;
    static final int UPPER_BOUNDARY = 150 + GRID_SIZE;
    static final int LOWER_BOUNDARY = 150 + NO_ROW*GRID_SIZE;
    static final int NEW_TILE_TOP = 150; //the beginning top coordinate for newly created tile
    static final int NEW_TILE_LEFT = LEFT_BOUNDARY;
    static final int NEXT_VALUE_BOTTOM = 124; //the bottom coordinate for the display of next value
    static final int NEXT_VALUE_LEFT = LEFT_BOUNDARY + (NO_COL-1)*GRID_SIZE + 32;
    static final int CURR_SCORE_BOTTOM = 63; //curr for current
    static final int CURR_SCORE_LEFT = 183;
    static final int BEST_SCORE_BOTTOM = CURR_SCORE_BOTTOM;
    static final int BEST_SCORE_LEFT = CURR_SCORE_LEFT +170;
    static final int MODE_BOTTOM = 50;
    static final int MODE_LEFT = 10;
    
    //**asset
    static final String BOARD_IMG_RELATIVE_PATH = "/assets/board.png"; //img for image
    static final String BEST_SCORE_FILE_RELATIVE_PATH = "/src/assets/bestScore.txt";
    static final String TILE_IMG_DIR_RELATIVE_PATH = "/assets/tiles/"; //dir for directory
    static final String TILE_IMG_FILE_EXTENSION = ".png";
    static final String BGM_NAME = "bgm.wav";
    static final String SOUND_EFFECT_NAME = "sound_effect.wav";
    
}
