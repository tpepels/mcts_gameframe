package cannon.gui;

import cannon.game.Board;

import javax.swing.*;
import java.awt.*;

public class CannonGui extends JFrame {
    private static final long serialVersionUID = 1L;

    public CannonGui() {
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(100, 100, 550, 550);
        //
        Board b = new Board();
        b.initialize();
        //
        CannonPanel panel = CannonPanel.getInstance();
        panel.setBackground(Color.LIGHT_GRAY);
        setContentPane(panel);
        panel.setBoard(b);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    CannonGui frame = new CannonGui();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
