/**
 * Our algorithm for AIPlayer:
 * Game field(4x4 grids):
 *       col0  col1 col2 col3
 * rowD    0D   1D   2D    3D
 * rowC    0C   1C   2C    3C
 * rowB    0B   1B   2B    3B
 * rowA    0A   1A   2A    3A
 * 1)Try to have all tiles placed into columns that already has/have been allocated tiles
 *   In case it is impossible to have the values of row B > C > D in the “used columns”, use the “unused column”
 * 2)In case the combination of tiles on row A is available, having them joint together
 * 3)Use downward movement to make itself able to achieve 64 value tile in hard mode within 30s.
 */


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tetris2048;

import Tetris2048.TileList.Tile;
import Tetris2048.TileList.Tile.Position;
import static Tetris2048.GameFieldData.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 *
 * @author ILoveIdunna
 */
public class AIPlayer {
    private static int[] colVisitOrder = {0,2,3};
    private Tile latestTile;
    private ArrayList<ArrayList<Tile>> colList; //the latestTile is not included
    private TileList tileListSource;
    private LinkedList<Integer> keyCodeInputForGame;
    
    public AIPlayer(TileList tileListSource)
    {
        colList = new ArrayList<ArrayList<Tile>>(NO_COL);
        for (int i = 0; i < NO_COL; i++)
        {
            colList.add(new ArrayList<Tile>(NO_ROW - 1));
        }
        keyCodeInputForGame = new LinkedList<Integer>();
        this.tileListSource = tileListSource;
    }
    
    //**  public methods  **//
    //
    public int getAINextInput()
    {
        try
        {
            //**determine a path for latest Tile
            if (latestTile == null)
            {
                latestTile = tileListSource.getLatestTile();
                keyCodeInputForGame.addLast(KeyEvent.VK_DOWN);
            }
            else
            {
                //handle path before reaching the lower boundary
                if (updateIfLatestTileChanged())
                {
                    keyCodeInputForGame.clear();
                    refreshColList();
                    setPathBeforeLowerBoundary();
                }
                
                //handle path when this Tile is on the lower boundary
                if (latestTileOnLowerBoundary() && latestTile.readyForNewPplMv())
                {
                    keyCodeInputForGame.clear();
                    if (!absorbTileOnLowerBoundary(0))
                        absorbTileOnLowerBoundary(2);
                }
            }
            
            //**return the determined path for the latest Tile
            if (latestTile.readyForNewPplMv())
                return keyCodeInputForGame.getFirst();
            
            //**return -1 if no movement command to be returned
            return -1;
        }
        catch (Exception e)
        {
            return -2;
        }
    }
    public void nextInputTaken()
    {
        keyCodeInputForGame.removeFirst();
    }
    //
    //**  END of public methods  **//
    
    
    //**  private methods  **//
    //
    private boolean updateIfLatestTileChanged()
    {
        Tile latestTile = tileListSource.getLatestTile();
        if (this.latestTile == latestTile)
            return false;
        else
        {
            this.latestTile = tileListSource.getLatestTile();
            return true;
        }
    }
    private void cleanColList()
    {
        for (ArrayList<Tile> col : colList)
            col.clear();
    }
    private void refreshColList()
    {
        cleanColList();
        ArrayList<Tile> tileList = tileListSource.getTileList();
        Collections.sort(tileList);
        for (Tile tile : tileList)
        {
            if (tile == latestTile)
                continue;
            int left = tile.getCurrPosi().getLeft();
            int col = (left - NEW_TILE_LEFT) / GRID_SIZE;
            colList.get(col).add(tile);
        }
    }
    private void setPathBeforeLowerBoundary()
    {
        for (int i = 0; i < colVisitOrder.length; i++)
        {
            if (visitCol(colVisitOrder[i]))
                return;
        }
        generateKeyCodeInput(3,true);
    }
    
    private boolean visitCol(int colNo)
    {
        //**examine the column from low to high
        ArrayList<Tile> col = colList.get(colNo);
        int colLength = col.size();
        if (colLength == 0) //when it is an empty column
        {
            generateKeyCodeInput(colNo, false);
            return true;
        }
        else
        {
            if (col.get(0).getValue() == latestTile.getValue()) //when the lowest one has a value equal to latestTile's
            {
                generateKeyCodeInput(colNo, true);
                return true;
            }
            else
            {
                if (colLength == 1)
                {
                    generateKeyCodeInput(colNo, false);
                    return true;
                }
                else
                {
                    if (col.get(colLength - 1).getValue() >= latestTile.getValue()) //when the uppermost one has a value >= latestTile's
                    {
                        if (colLength == NO_ROW - 1 && //when reaching upper boundary
                            col.get(colLength - 1).getValue() != latestTile.getValue())
                            return false;
                        generateKeyCodeInput(colNo, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void generateKeyCodeInput(int colNo, boolean bottom)
    {
        if (bottom && colNo !=3)
        {
            switch(colNo)
            {
                case 0:
                    keyCodeInputForGame.addLast(KeyEvent.VK_RIGHT);
                    keyCodeInputForGame.addLast(KeyEvent.VK_DOWN);
                    break;
                case 2:
                    keyCodeInputForGame.addLast(KeyEvent.VK_RIGHT);
                    keyCodeInputForGame.addLast(KeyEvent.VK_DOWN);
                    break;
            }
        }
        else if (!bottom && colNo!=3)
        {
            switch(colNo)
            {
                case 0:
                    keyCodeInputForGame.addLast(KeyEvent.VK_DOWN);
                    break;
                case 2:
                    keyCodeInputForGame.addLast(KeyEvent.VK_RIGHT);
                    keyCodeInputForGame.addLast(KeyEvent.VK_RIGHT);
                    keyCodeInputForGame.addLast(KeyEvent.VK_DOWN);
                    break;
            }
        }else if (colNo == 3)
        {
            keyCodeInputForGame.addLast(KeyEvent.VK_RIGHT);
            keyCodeInputForGame.addLast(KeyEvent.VK_RIGHT);
            keyCodeInputForGame.addLast(KeyEvent.VK_RIGHT);
            keyCodeInputForGame.addLast(KeyEvent.VK_DOWN);
        }
    }
    
    private boolean latestTileOnLowerBoundary()
    {
        Tile.Position posi = latestTile.getCurrPosi();
        int top = posi.getTop();
        int left = posi.getLeft();
        if (top + GRID_SIZE == LOWER_BOUNDARY)
            return true;
        return false;
    }
    //called when latestTile is on the lower boundary
    private boolean absorbTileOnLowerBoundary(int targetCol)
    {
        Tile tile;
        tile = getSettledTile(targetCol,0);
        
        if (tile != null)
        {
            if (tile.isAbsorbing() || tile.isBeingAbsorbed())
                return false;
            else
                if (tile.getValue() == latestTile.getValue())
                {
                    //try to let this Tile obj absorbs tile0A
                    int horiDist; //the horizontal distance between latestTile & tile0A
                    horiDist = latestTile.getCurrPosi().getLeft() - tile.getCurrPosi().getLeft();
                    for (int i = 0; i < Math.abs(horiDist/GRID_SIZE); i++)
                        keyCodeInputForGame.addLast(horiDist > 0 ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT);
                }
        }
        return false;
    }
    /**
     * @param col   the leftmost col is marked as 0
     * @param row   the lowest row is marked as 0
     * @return 
     */
    private Tile getSettledTile(int targetCol, int targetRow)
    {
        int targetTop;
        ArrayList<Tile> col;
        
        refreshColList();
        targetTop = LOWER_BOUNDARY - (targetRow + 1) * GRID_SIZE;
        col = colList.get(targetCol);
        for (Tile tile : col)
        {
            int topBeingChecked = tile.getCurrPosi().getTop();
            if (topBeingChecked == targetTop)
                return tile;
            else if (topBeingChecked < targetTop)
                return null;
        }
        return null;
    }
    //
    //**  END of private methods  **//
    

    
    
    
    
    
    public void testColList()
    {
        for (ArrayList<Tile> col: colList)
        {
            for (Tile tile : col)
                System.out.print(tile.getValue() + " ");
            System.out.println();
        }
    }
    public void testInputList()
    {
        for (Integer i: keyCodeInputForGame)
        {
            System.out.print(i + " ");
        }
        System.out.println();
    }
    
    
}

//handle bottom tiles combination
//handle bottom absorption!


//if big one above the lowest one of col2


//wait a frame to catch any absorption when a new tile is created