/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tetris2048;

import static Tetris2048.GameFieldData.*;
import game.v2.Console;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author ILoveIdunna
 */
class Scorer
{
    private int currScore, bestScore; //curr for current
    private String bestScoreFileAbsolutePath;
    private RandomAccessFile bestScoreRAF;
    private Console console;
    private Sound soundEffect;
    
    Scorer(Sound soundEffect) throws FileNotFoundException, IOException
    { 
        this.soundEffect = soundEffect;
        console = Console.getInstance();
        this.bestScoreFileAbsolutePath = new File("").getAbsolutePath() + BEST_SCORE_FILE_RELATIVE_PATH;
        try
        {
            bestScoreRAF = new RandomAccessFile(bestScoreFileAbsolutePath,"rws");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        bestScore = getBestScoreFromFile();
    }

    public int getCurrScore() { return currScore; }
    public int getBestScore() { return bestScore; }
    private int getBestScoreFromFile() throws IOException
    {   
        int bestScoreFromFile = 0;
        try
        {
            bestScoreRAF.seek(0);
            bestScoreFromFile = bestScoreRAF.readInt();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            return bestScoreFromFile;
        }
    }

    public Scorer addCurrScore(int add)
    {
        if (add <= 0)
            return this;
        currScore += add;
        playSoundEffect();
        bestScore = (bestScore >= currScore) ? bestScore : currScore;
        updateBestScoreInFile();
        return this;
    }
    private void playSoundEffect()
    {
        soundEffect.playSoundEffect();
    }
    private void updateBestScoreInFile()
    {
        int bestScoreFromFile = 0;
        try
        {
            bestScoreFromFile = getBestScoreFromFile();
            if (bestScore > bestScoreFromFile)
            {
                bestScoreRAF.seek(0);
                bestScoreRAF.writeInt(bestScore);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    public void displayScores()
    {
        int noOfDigitOfCurrScore = 0, noOfDigitOfBestScore = 0, //used to adjust the position
                                                            //of scores according to their numbers of digits
            fontSize;
        for (int i = 1; i < currScore; i *= 10)
            noOfDigitOfCurrScore++;
        for (int i = 1; i < bestScore; i *= 10)
            noOfDigitOfBestScore++;
        fontSize = 24;
        console.drawText(CURR_SCORE_LEFT - noOfDigitOfCurrScore*(fontSize/4),
                        CURR_SCORE_BOTTOM,String.valueOf(currScore), 
                        new Font(null,1,fontSize), new Color(0x00,0x00,0x00,0xFF));
        console.drawText(BEST_SCORE_LEFT - noOfDigitOfBestScore*(fontSize/4),
                        BEST_SCORE_BOTTOM,String.valueOf(bestScore), 
                        new Font(null,3,fontSize), new Color(0xBB,0x00,0x00,0xFF));
    }
    public void cleanToReuse()
    {
        currScore = 0;
    }
}