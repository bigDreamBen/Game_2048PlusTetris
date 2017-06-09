/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tetris2048;

/**
 *
 * @author ILoveIdunna
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

/**
 *
 * @author USER
 */



public class Sound extends Thread {

    private AudioStream as;
    private AudioPlayer ap;
    public boolean playback;
    private String songPath;
    public static int BGM = 0, SOUND_EFFECT = 1;
    private int type;

    public boolean setSong(String songPath, int type) {
        try {
            this.songPath = songPath;
            this.type = type;
            File a = new File(System.getProperty("user.dir") + "/src/assets/" + songPath);
            if (a.exists()) {
                // HERE THERE IS THE IMPLEMENTATION WHEN RUN FROM NETBEANS
                as = new AudioStream(new FileInputStream(a));
                return true;
            } else { // HERE SHOULD BE THE JAR FILE IMPLEMENTATION
                as = new AudioStream(this.getClass().getResourceAsStream(songPath));
                return true;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error in loading\n" + ex.getMessage());
        }
        return false;
    }
    public boolean setSong() {
        try {
            File a = new File(System.getProperty("user.dir") + "/src/assets/" + songPath);
            if (a.exists()) {
                // HERE THERE IS THE IMPLEMENTATION WHEN RUN FROM NETBEANS
                as = new AudioStream(new FileInputStream(a));
                return true;
            } else { // HERE SHOULD BE THE JAR FILE IMPLEMENTATION
                as = new AudioStream(this.getClass().getResourceAsStream(songPath));
                return true;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error in loading\n" + ex.getMessage());
        }
        return false;
    }

    public void startLoopPlayback() {
        playback = true;
        ap.player.start(as);
        try {
            do {
            } while (as.available() > 0 && playback);
            if (playback) {
                setSong();
                startLoopPlayback();
            }
        } catch (IOException ex) {
            Logger.getLogger(Sound.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void playSoundEffect()
    {
        setSong();
        ap.player.start(as);
    }
    
    public void stopPlayback() {
        playback = false;
        AudioPlayer.player.stop(as);
    }
    
    public void run(){
        if (type == BGM)
            startLoopPlayback();
    }
}
