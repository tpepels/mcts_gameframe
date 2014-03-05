package chinesecheckers.gui;

import ai.framework.*;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import chinesecheckers.game.Board;
import chinesecheckers.game.Move;
import chinesecheckers.game.Piece;
import rush.HexGridCell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class CCPanel extends JPanel implements MouseListener, MoveCallback {
    private static final long serialVersionUID = -7255477935485381647L;
    private static final int CELL_R = 25;
    public AIPlayer aiPlayer1, aiPlayer2;
    private MCTSOptions p1Options, p2Options;
    //
    private int[] cornersY = new int[6], cornersX = new int[6];
    private HexGridCell hexagons = new HexGridCell(CELL_R);
    private Board board;
    //
    private boolean p1Human = false, p2Human = false;
    private IMove lastMove;
    private int selected = -1;

    public CCPanel(Board board, boolean p1Human, boolean p2Human) {
        this.board = board;
        this.p1Human = p1Human;
        this.p2Human = p2Human;
        //
        p1Options = new MCTSOptions();
        p1Options.setGame("chinesecheckers");

        p2Options = new MCTSOptions();
        p2Options.setGame("chinesecheckers");

        //
        if (!p1Human) {
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(p1Options);
        }
        if (!p2Human) {
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(p2Options);
        }
        addMouseListener(this);
        makeAIMove();
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
            aiPlayer1.setOptions(p1Options);
        }
        if (!p2Human) {
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(p2Options);
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
                    Piece occ = board.board[i * Board.WIDTH + j].occupant;
                    if (occ != null) {
                        if (occ.colour == Board.P2) {
                            g2d.setColor(Color.black);
                            g2d.fillOval(cornersX[0] + 7, cornersY[0] - 3, 30, 30);
                            g2d.setColor(Color.decode("#FFFFDD"));
                        }
                        //
                        if (occ.colour == Board.P1) {
                            g2d.setColor(Color.decode("#FFFFDD"));
                            g2d.fillOval(cornersX[0] + 7, cornersY[0] - 3, 30, 30);
                            g2d.setColor(Color.black);
                        }
                    }

                    g2d.drawString(Integer.toString(i * Board.WIDTH + j),
                            cornersX[0] + 10, cornersY[0] + 10);
                }
            }
        }
        //
        if (board.getPlayerToMove() == Board.P1)
            g2d.setColor(Color.black);
        else
            g2d.setColor(Color.decode("#FFFFDD"));
        //
        if (selected != -1) {
            MoveList moves = Board.getMoves();
            Color green = new Color(0, 100, 0, 100);
            for (int i = 0; i < moves.size(); i++) {
                int move = moves.get(i).getMove()[1];
                int x = move / Board.WIDTH, y = move % Board.WIDTH;
                System.out.println(move);
                hexagons.setCellIndex(x, y);
                hexagons.computeCorners(cornersY, cornersX);
                g2d.setColor(green);
                g2d.fillOval(cornersX[0] + 10, cornersY[0], 25, 25);
            }
            System.out.println("-----");
        }
    }

    public void setPlayer(int player, boolean human) {
        if (player == 1) {
            this.p1Human = human;
            //
            if (!human) {
                aiPlayer1 = new MCTSPlayer();
                p1Options.setGame("chinesecheckers");
                aiPlayer1.setOptions(p1Options);
            }
        } else {
            this.p2Human = human;
            //
            if (!human) {
                aiPlayer2 = new MCTSPlayer();
                p2Options.setGame("chinesecheckers");
                aiPlayer2.setOptions(p2Options);
            }
        }
        makeAIMove();
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
        if (isInsideBoard(clickI, clickJ) && selected != -1 && board.board[position].occupant == null) {
            makeMove(new Move(selected, position, 0));
            selected = -1;
            Board.getMoves().clear();
        } else if (selected == -1 || board.board[position].occupant.colour == board.getPlayerToMove()) {
            selected = position;
            Board.getMoves().clear();
            board.generateMovesForPiece(position, true);
            repaint();
        }
    }

    @Override
    public void makeMove(IMove move) {
        board.doMove(move);
        lastMove = move;
        // Run the GC in between moves, to limit the runs during search
        System.gc();
        int winner = board.checkWin();
        if (winner != Board.NONE_WIN) {
            if (winner == Board.P2_WIN) {
                System.out.println("Black wins!");
            } else {
                System.out.println("White wins!");
            }
        } else {
            // Check if the AI should make a move
            if (board.getPlayerToMove() == IBoard.P1 && !p1Human) {

                aiPlayer1.getMove(board.copy(), this, Board.P1, true, lastMove);
                System.out.println("Player 1, thinking ...");
            } else if (board.getPlayerToMove() == IBoard.P2 && !p2Human) {

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
