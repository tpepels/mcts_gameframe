package chinesecheckers.gui;

import ai.framework.AIPlayer;
import ai.framework.IMove;
import ai.framework.MoveCallback;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import chinesecheckers.game.Board;
import chinesecheckers.game.Move;
import rush.HexGridCell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;

public class CCPanel extends JPanel implements MouseListener, MoveCallback {
    private static final long serialVersionUID = -7255477935485381647L;
    private static final int CELL_R = 25;
    public AIPlayer aiPlayer1, aiPlayer2;
    //
    private int[] cornersY = new int[6], cornersX = new int[6];
    private HexGridCell hexagons = new HexGridCell(CELL_R);
    private Board board;
    //
    private boolean p1Human = true, p2Human = true;
    private IMove lastMove;
    private int selected = -1;

    public CCPanel(Board board, boolean p1Human, boolean p2Human) {
        this.board = board;
        this.p1Human = p1Human;
        this.p2Human = p2Human;
        //
        if (!p1Human) {
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(new MCTSOptions());
        }
        if (!p2Human) {
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(new MCTSOptions());
        }
        addMouseListener(this);
    }

    public void undoMove() {
        board.undoMove();
        if (aiPlayer1 != null) {
            aiPlayer1.stop();
        }
        if (aiPlayer2 != null) {
            aiPlayer2.stop();
        }
        repaint();
    }

    public void makeAIMove() {
        if (board.getPlayerToMove() == 1 && !p1Human) {
            aiPlayer1.getMove(board.copy(), this, Board.P1, true, lastMove);
        } else if (board.getPlayerToMove() == 2 && !p2Human) {
            aiPlayer2.getMove(board.copy(), this, Board.P2, true, lastMove);
        }
    }

    public void setBoard(Board board) {
        this.board = board;
        //
        if (!p1Human) {
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(new MCTSOptions());
        }
        if (!p2Human) {
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(new MCTSOptions());
        }
        //
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        if (board.getPlayerToMove() == Board.P2)
            g2d.setColor(Color.black);
        else
            g2d.setColor(Color.decode("#FFFFDD"));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //
        for (int i = 0; i < Board.HEIGHT; i++) {
            for (int j = 0; j < Board.WIDTH; j++) {
                hexagons.setCellIndex(i, j);
                //
                if (Board.occupancy[i * Board.WIDTH + j] == 1) {
                    hexagons.computeCorners(cornersY, cornersX);
                    g2d.setColor(Color.decode("#FFE47A"));
                    g2d.fillPolygon(cornersX, cornersY, 6);
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawPolygon(cornersX, cornersY, 6);
                    //
                    g2d.setColor(Color.DARK_GRAY);
                    if (board.board[i * Board.WIDTH + j].occupant == Board.P2) {
                        g2d.setColor(Color.black);
                        g2d.fillOval(cornersX[0] + 5, cornersY[0] - 2, 25, 25);
                        g2d.setColor(Color.decode("#FFFFDD"));
                    }
                    //
                    if (board.board[i * Board.WIDTH + j].occupant == Board.P1) {
                        g2d.setColor(Color.decode("#FFFFDD"));
                        g2d.fillOval(cornersX[0] + 5, cornersY[0] - 2, 25, 25);
                        g2d.setColor(Color.black);
                    }

                    g2d.drawString(Integer.toString(i * Board.WIDTH + j),
                            cornersX[0] + 17, cornersY[0] + 17);
                }
            }
        }
        //
        if (board.getPlayerToMove() == Board.P1)
            g2d.setColor(Color.black);
        else
            g2d.setColor(Color.decode("#FFFFDD"));
        //
        Color green = new Color(0, 100, 0, 100);
        for (int i = 0; i < board.moves.size(); i++) {
            int x = board.moves.get(i).getMove()[1] / Board.WIDTH, y = board.moves.get(i).getMove()[1] % Board.WIDTH;
            hexagons.setCellIndex(x, y);
            hexagons.computeCorners(cornersY, cornersX);
            g2d.setColor(green);
            g2d.fillOval(cornersX[0] + 5, cornersY[0] - 2, 25, 25);
        }
    }

    public void setPlayer(int player, boolean human) {
        if (player == 1) {
            this.p1Human = human;
            //
            if (!human) {
                aiPlayer1 = new MCTSPlayer();
                aiPlayer1.setOptions(new MCTSOptions());
            }
        } else {
            this.p2Human = human;
            //
            if (!human) {
                aiPlayer2 = new MCTSPlayer();
                aiPlayer2.setOptions(new MCTSOptions());
            }
        }
    }

    private boolean isInsideBoard(int i, int j) {
        return i >= 0 && i < Board.WIDTH && j >= 0 && j < Board.HEIGHT
                && Board.occupancy[i * Board.WIDTH + j] == 1;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Check if human player.
        if (board.getPlayerToMove() == 1 && !p1Human) {
            return;
        } else if (board.getPlayerToMove() == 2 && !p2Human) {
            return;
        }
        //
        hexagons.setCellByPoint(e.getY(), e.getX());
        int clickI = hexagons.getIndexI();
        int clickJ = hexagons.getIndexJ();
        int position = clickI * Board.WIDTH + clickJ;
        //
        if (isInsideBoard(clickI, clickJ) && selected != -1 && board.board[position].occupant == Board.EMPTY) {
            makeMove(new Move(selected, position, 0));
            selected = -1;
            board.moves.clear();
        } else if (selected == -1 || board.board[position].occupant == board.getPlayerToMove()) {
            selected = position;
            board.moves.clear();
            board.generateMovesForPiece(position, true);
            repaint();
        }
    }

    @Override
    public void makeMove(IMove move) {
        board.doMove(move);
        lastMove = move;
        int winner = board.checkWin();
        if (winner != Board.NONE_WIN) {
            if (winner == Board.P2_WIN) {
                System.out.println("Black wins!");
            } else {
                System.out.println("White wins!");
            }
        } else {
            // Check if the AI should make a move
            if (board.getPlayerToMove() == 1 && !p1Human) {
                aiPlayer1.getMove(board.copy(), this, Board.P1, true, lastMove);
                System.out.println("Player 1, thinking ...");
            } else if (board.getPlayerToMove() == 2 && !p2Human) {
                aiPlayer2.getMove(board.copy(), this, Board.P2, true, lastMove);
                System.out.println("Player 2, thinking ...");
            }
        }
        repaint();
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }
}
