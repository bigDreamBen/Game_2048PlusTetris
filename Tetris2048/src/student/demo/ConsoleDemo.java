/**
 * Copyright (c) 2012, Van Ting <vanting@gmail.com>
 * All rights reserved. Unauthorized use and redistribution of this file is
 * strictly prohibited.
 */
package student.demo;

import game.v2.Console;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import javax.swing.JOptionPane;

/**
 * Examples to demonstrate the use of Console.
 * The Console class represents a game window and serves as a canvas to draw game elements.
 * 
 * @author vanting
 */
public class ConsoleDemo {

    public static void main(String[] args) throws IOException {

        /*
         The Console class uses singleton design pattern.
         There is one and only one instance in the system. The getInstance() method always returns the same instance. 
         This will be useful as you may want to get a reference to the same Console instance somewhere else in your program. 
         */
        Console thisConsole = Console.getInstance();
        Console thatConsole = Console.getInstance();
        if (thisConsole == thatConsole) {
            System.out.println("Console.getInstance() always returns the same instance");
        }

        /*
         The Console class uses fluent interface design pattern.
         The Console instance represents a game window which can be customized by its setters. These setters can be chained together 
         in subsequent calls and ended by calling show() which makes the window visible.
         */
        Console.getInstance()
                .setTitle("Console Demo")
                .setWidth(1000)
                .setHeight(600)
                .setTheme(Console.Theme.DARK)
                .show();

        System.out.println("Title: " + thisConsole.getTitle());
        System.out.println("Width x Height: " + thisConsole.getWidth() + " x " + thatConsole.getHeight());

        /*
         It is very easy to load an image from class path by calling the static method loadImage().
         Note that these two samples images are shipped with Console.jar
         */
        Image img1 = Console.loadImage("/demo/img/invader_1.png");
        Image img2 = Console.loadImage("/demo/img/invader_2.png");

        /*
         The Console instance provides a number of drawXXX() methods to paint elements on the game window. These methods also
         support method chaining.
         */
        Console.getInstance().drawImage(100, 200, img1);                        // x=100, y=200
        Console.getInstance().drawImage(100, 400, img2);
        Console.getInstance().drawCircle(500, 400, 128, 128, Color.GREEN)
                .drawRectangle(500, 200, 128, 128, Color.RED)               // x=500, y=200, width=128, height=128
                .drawRectangle(700, 200, 128, 128, Color.CYAN, 20)          // rounded rectangle with arc radius 20
                .drawText(50, 100, "Draw text at (50,100) with default color and font")
                .drawText(50, 150, "Draw text at (50,150) with different color and font", new Font("Arial", Font.BOLD, 16), Color.YELLOW);

        /*
         To reduce screen flicker, all drawXXX() methods write to an off-screen buffer. You must call update() to flush the painting 
         from buffer to screen.
         */
        Console.getInstance().update();

        /*
         You may want to introduce some time delay between the operations in the game loop. The pause() method below will suspend 
         the program for 5 seconds.
         */
        Console.pause(1000 * 5);

        /*
         Finally, you can call the close() method to programmatically close the window.
         */
        if (JOptionPane.showConfirmDialog(null, "Confirm to close this window?", "", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            Console.getInstance().close();
        }

        /*
         You can also call clear() to erase the screen with background color. 
         */
        Console.getInstance().clear().update();

    }

}
