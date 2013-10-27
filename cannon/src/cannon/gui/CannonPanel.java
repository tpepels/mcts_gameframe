package cannon.gui;

import ai.framework.AIPlayer;
import ai.framework.IMove;
import ai.framework.MoveCallback;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import cannon.game.Board;
import cannon.game.Move;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class CannonPanel extends JPanel implements MouseListener, MoveCallback {
    private static final long serialVersionUID = -7566336110896323508L;
    public static CannonPanel instance = new CannonPanel();
    private static Board board;
    //
    private final int SQUARE_SIZE = 50, OFFS = 50;
    private Color red = new Color(100, 0, 0, 100), green = new Color(0, 100, 0, 100);
    private int selectedPosition = -1;
    private AIPlayer aiPlayer1, aiPlayer2;
    private boolean allAi = true;
    private IMove lastMove = null;

    private CannonPanel() {
        addMouseListener(this);
        //
        aiPlayer1 = new MCTSPlayer();
        MCTSOptions options1 = new MCTSOptions();
        options1.debug = true;
        aiPlayer1.setOptions(options1);
        aiPlayer2 = new MCTSPlayer();
        MCTSOptions options2 = new MCTSOptions();
        options2.debug = true;
        aiPlayer2.setOptions(options2);
    }

    public static CannonPanel getInstance() {
        return instance;
    }

    public void setBoard(Board board) {
        CannonPanel.board = board;
        instance.repaint();
        if (allAi) {
            aiPlayer1.getMove(board.copy(), this, Board.P2, true, lastMove);
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = 10, y = 10, position;
        int PIECE_SIZE = 40;
        for (int i = 9; i >= 0; i--) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString(Integer.toString(i + 1), i * SQUARE_SIZE + OFFS, OFFS / 2);
            g2d.drawString(Integer.toString(i + 1), OFFS / 4, i * SQUARE_SIZE + OFFS + 5);
            for (int j = 9; j >= 0; j--) {
                g2d.setColor(Color.BLACK);
                x = OFFS + i * SQUARE_SIZE - SQUARE_SIZE;
                y = OFFS + j * SQUARE_SIZE - SQUARE_SIZE;
                if (i > 0 && j > 0)
                    g2d.drawRect(x, y, SQUARE_SIZE, SQUARE_SIZE);
                //
                position = board.board[i + j * Board.HEIGHT];
                if (position != Board.EMPTY) {
                    x = OFFS + i * SQUARE_SIZE - PIECE_SIZE / 2;
                    y = OFFS + j * SQUARE_SIZE - PIECE_SIZE / 2;
                    g2d.setFont(new Font("MS Gothic", Font.BOLD, 24));
                    switch (position) {
                        case Board.B_SOLDIER:
                            g2d.setColor(Color.BLACK);
                            g2d.fillOval(x, y, PIECE_SIZE, PIECE_SIZE);
                            g2d.setColor(Color.WHITE);
                            g2d.drawString("\u5175", x + 5, y + SQUARE_SIZE / 2 + 4);
                            break;
                        case Board.W_SOLDIER:
                            g2d.setColor(Color.WHITE);
                            g2d.fillOval(x, y, PIECE_SIZE, PIECE_SIZE);
                            g2d.setColor(Color.BLACK);
                            g2d.drawString("\u5175", x + 5, y + SQUARE_SIZE / 2 + 4);
                            break;
                        case Board.B_TOWN:
                            g2d.setColor(Color.BLACK);
                            g2d.fillOval(x, y, PIECE_SIZE, PIECE_SIZE);
                            g2d.setColor(Color.WHITE);
                            g2d.drawString("\u91CC", x + 5, y + SQUARE_SIZE / 2 + 4);
                            break;
                        case Board.W_TOWN:
                            g2d.setColor(Color.WHITE);
                            g2d.fillOval(x, y, PIECE_SIZE, PIECE_SIZE);
                            g2d.setColor(Color.BLACK);
                            g2d.drawString("\u91CC", x + 5, y + SQUARE_SIZE / 2 + 4);
                            break;
                    }
                }
            }
        }
        //
        if (selectedPosition != -1) {
            IMove move;
            for (int i = 0; i < board.moves.size(); i++) {
                move = board.moves.get(i);
                if (move.getMove()[0] != selectedPosition)
                    continue;
                x = move.getMove()[1] % Board.WIDTH;
                y = move.getMove()[1] / Board.HEIGHT;
                x = OFFS + x * SQUARE_SIZE - PIECE_SIZE / 2;
                y = OFFS + y * SQUARE_SIZE - PIECE_SIZE / 2;
                if (move.getType() == Move.MOVE || move.getType() == Move.RETREAT)
                    g2d.setColor(green);
                else
                    g2d.setColor(red);
                g2d.fillOval(x, y, PIECE_SIZE, PIECE_SIZE);
            }
        }
    }

    @Override
    public void makeMove(IMove move) {
        board.doMove(move, true);
        repaint();
        if (allAi) {
            if (board.getPlayerToMove() == Board.P2)
                aiPlayer2.getMove(board.copy(), this, Board.P2, true, lastMove);
            else
                aiPlayer1.getMove(board.copy(), this, Board.P2, true, lastMove);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            PopUpMenu menu = new PopUpMenu();
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            PopUpMenu menu = new PopUpMenu();
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        int clickX = (int) Math.round(((arg0.getX() - OFFS) / (double) SQUARE_SIZE));
        int clickY = (int) Math.round(((arg0.getY() - OFFS) / (double) SQUARE_SIZE));
        //
        int position = clickX + clickY * Board.WIDTH;
        int occ = board.board[position];
        int mySoldier = (board.getPlayerToMove() == Board.P1) ? Board.B_SOLDIER : Board.W_SOLDIER;
        int color = (occ == Board.B_SOLDIER) ? Board.P1 : Board.P2;
        // Do the move!
        if (selectedPosition != -1 && occ != mySoldier) {
            IMove move;
            for (int i = 0; i < board.moves.size(); i++) {
                move = board.moves.get(i);
                if (move.getMove()[0] == selectedPosition && move.getMove()[1] == position) {
                    board.doMove(move, true);
                    selectedPosition = -1;
                    if (board.checkWin() != Board.NONE_WIN) {
                        if (board.checkWin() == Board.P1_WIN) {
                            System.out.println("Black wins");
                        } else if (board.checkWin() == Board.P2_WIN) {
                            System.out.println("White wins");
                        }
                    } else if (board.getPlayerToMove() == Board.P2 || (board.getPlayerToMove() == Board.P1 && allAi)) {
                        if (board.getPlayerToMove() == Board.P2)
                            aiPlayer2.getMove(board.copy(), this, Board.P2, true, lastMove);
                        else
                            aiPlayer1.getMove(board.copy(), this, Board.P2, true, lastMove);
                    }
                    repaint();
                    break;
                }
            }
        } else {
            if (color == board.currentPlayer && board.blackTownPlaced && board.whiteTownPlaced)
                selectedPosition = position;
            else if (board.currentPlayer == Board.P1 && !board.blackTownPlaced) {
                board.doMove(new Move(Move.CASTLE, new int[]{-1, position}), false);
                selectedPosition = -1;
                repaint();
                aiPlayer2.getMove(board.copy(), this, Board.P2, true, lastMove);
                return;
            } else if (board.currentPlayer == Board.P2 & !board.whiteTownPlaced) {
                board.doMove(new Move(Move.CASTLE, new int[]{-1, position}), false);
                selectedPosition = -1;
                repaint();
                return;
            } else {
                return;
            }
            // Draw the valid moves
            if (occ == Board.B_SOLDIER || occ == Board.W_SOLDIER) {
                board.getValidMovesForSoldier(position, color, false);
            }
            repaint();
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    // Popup menu for control
    class PopUpMenu extends JPopupMenu implements ActionListener {
        private static final long serialVersionUID = 1L;

        public PopUpMenu() {
            JMenuItem undoItem = new JMenuItem("Undo move");
            undoItem.setActionCommand("undo");
            undoItem.addActionListener(this);
            JMenuItem newItem = new JMenuItem("New game");
            newItem.setActionCommand("new");
            newItem.addActionListener(this);
            add(newItem);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (arg0.getActionCommand().equals("undo")) {
                board.undoMove();
                if (aiPlayer1 != null) {
                    aiPlayer1.stop();
                }
                if (aiPlayer2 != null) {
                    aiPlayer2.stop();
                }
                repaint();
            } else if (arg0.getActionCommand().equals("new")) {
                board.initialize();
                repaint();
            }
        }
    }
}
