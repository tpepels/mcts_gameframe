package chinesecheckers.gui;

import chinesecheckers.game.Board;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CCGui extends JFrame {

    private static final long serialVersionUID = -1921481286866231418L;
    public static CCPanel ccPanel;
    private final ButtonGroup player2Group = new ButtonGroup();
    private final ButtonGroup player1Group = new ButtonGroup();
    private Board currentBoard;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    CCGui frame = new CCGui();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public CCGui() {
        setResizable(false);
        setBackground(Color.BLACK);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 568, 695);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);
        currentBoard = new Board();
        currentBoard.initialize();
        JMenuItem mntmNewGame = new JMenuItem("New game");
        mntmNewGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                currentBoard.initialize();
                ccPanel.setBoard(currentBoard);
            }
        });
        mnFile.add(mntmNewGame);

        JMenu mnPlayer = new JMenu("Player 1");
        menuBar.add(mnPlayer);

        final JRadioButtonMenuItem player1Human = new JRadioButtonMenuItem("Human");
        player1Human.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ccPanel.setPlayer(1, player1Human.isSelected());
            }
        });
        player1Group.add(player1Human);
        player1Human.setSelected(true);
        mnPlayer.add(player1Human);

        final JRadioButtonMenuItem player1AI = new JRadioButtonMenuItem("A.I.");
        player1AI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ccPanel.setPlayer(1, !player1AI.isSelected());
            }
        });
        player1Group.add(player1AI);
        mnPlayer.add(player1AI);

        JMenu mnPlayer_1 = new JMenu("Player 2");
        menuBar.add(mnPlayer_1);

        final JRadioButtonMenuItem player2Human = new JRadioButtonMenuItem("Human");
        player2Human.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ccPanel.setPlayer(2, player2Human.isSelected());
            }
        });
        player2Group.add(player2Human);
        mnPlayer_1.add(player2Human);

        final JRadioButtonMenuItem player2AI = new JRadioButtonMenuItem("A.I.");
        player2AI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ccPanel.setPlayer(2, !player2AI.isSelected());
            }
        });
        player2AI.setSelected(true);
        player2Group.add(player2AI);
        mnPlayer_1.add(player2AI);

        JMenuItem mntmNewMenuItem = new JMenuItem("Undo");
        mntmNewMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ccPanel.undoMove();
            }
        });

        menuBar.add(mntmNewMenuItem);

        JMenuItem mntmNewMenuItem_1 = new JMenuItem("AI Move");
        mntmNewMenuItem_1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ccPanel.makeAIMove();
            }
        });
        menuBar.add(mntmNewMenuItem_1);

        ccPanel = new CCPanel(currentBoard, true, false);
        ccPanel.setBackground(Color.BLACK);
        ccPanel.setLayout(null);
        setContentPane(ccPanel);

    }

    public static void showBoard(Board board) {
        ccPanel.setBoard(board);
        ccPanel.repaint();
    }
}
