package breakthrough.gui;

import ai.framework.IMove;
import ai.framework.MoveCallback;
import ai.framework.MoveList;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;
import breakthrough.game.Board;
import breakthrough.game.Move;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class BreakthroughPanel extends JPanel implements MouseListener, MouseMotionListener, MoveCallback {
    private static final long serialVersionUID = 1L;
    private final JFrame frame;
    private MoveList moves;
    private int squareSize = 40, boardCol = -1, boardRow = -1, clickNum = 0;
    private int[] clickPos = {-1, -1, -1};
    //
    private Board board;
    private MCTSPlayer aiPlayer1, aiPlayer2;
    private IMove lastMove;

    public BreakthroughPanel(int squareSize, JFrame frame) {
        this.squareSize = squareSize;
        this.frame = frame;
        board = new Board();
        board.initialize();
        //
        if (board.getPlayerToMove() == Board.P2) {
            frame.setTitle("Breaktrhough - Black's move.");
        } else {
            frame.setTitle("Breakthrough - White's move.");
        }
        //
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        // Moves need to be generated
        moves = board.getExpandMoves();
        //System.out.println(moves.toArray());
        for (int i = 0; i < moves.size(); i++) 
            System.out.println(moves.get(i));
        // Definition for player 1
       
        aiPlayer1 = null;
        aiPlayer1 = new MCTSPlayer();
        MCTSOptions options1 = new MCTSOptions();
        options1.swUCT = true;
        options1.windowSize = 1000;
        options1.setGame("breakthrough");
        aiPlayer1.setOptions(options1);

        // Definition for player 2
        aiPlayer2 = new MCTSPlayer();
        MCTSOptions options2 = new MCTSOptions();
        options2.setGame("breakthrough");
//        options2.epsGreedyEval = true;
//        options2.egeEpsilon = 0.1;
//        options2.implicitMM = true;
//        options2.imAlpha = 0.5;
//        options2.timeInterval = 5000;
        aiPlayer2.setOptions(options2);
        
        //
        if (aiPlayer1 != null)
            aiPlayer1.getMove(board.copy(), this, Board.P1, true, null);
    }

    public void paint(Graphics g) {
        super.paint(g);
        int row, col, x, y, boardPos;
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int NUM_SQUARES = 8;
        for (row = 0; row < NUM_SQUARES; row++) {
            for (col = 0; col < NUM_SQUARES; col++) {
                boardPos = row * NUM_SQUARES + col;
                //
                x = col * squareSize;
                y = row * squareSize;
                //
                if (boardPos == clickPos[0]) {
                    g.setColor(Color.decode("#FFF482"));  
                } else if (boardPos == clickPos[1]) {
                    g.setColor(Color.decode("#BDFF60"));
                } else if (boardPos == clickPos[2]) {
                    g.setColor(Color.decode("#FF4762"));
                } else if (col == boardCol && row == boardRow) {
                    g.setColor(Color.GRAY);
                } else if ((row % 2) == (col % 2)) {
                    g.setColor(Color.decode("#8B4500"));
                } else {
                    g.setColor(Color.decode("#FFEC8B"));
                }

                if (clickNum > 0) {
                    if (isAvailMove(clickPos[0] / 8, clickPos[0] % 8, row, col)) {
                        g.setColor(g.getColor().brighter());
                    }
                }

                g.fillRect(x, y, squareSize, squareSize);
                g.setColor(Color.white);
                g.drawString(Integer.toString(boardPos), x + 1, y + 11);

                char boardPiece = board.board[row][col];
                if (boardPiece != '.') {
                    if (boardPiece == 'w') {
                        g.setColor(Color.WHITE);
                        g.fillOval(x + 5, y + 5, squareSize - 10, squareSize - 10);
                    } else if (boardPiece == 'b') {
                        g.setColor(Color.BLACK);
                        g.fillOval(x + 5, y + 5, squareSize - 10, squareSize - 10);
                    }
                }
            }

        }
    }

    public boolean isAvailMove(int frow, int fcol, int row, int col) {
        for (int i = 0; i < moves.size(); i++) {
            //System.out.println("from : " + fcol + " " + frow + " to: " + col + " " + row);
            if (moves.get(i).getMove()[0] == frow &&
                    moves.get(i).getMove()[1] == fcol &&
                    moves.get(i).getMove()[2] == row &&
                    moves.get(i).getMove()[3] == col) {
                return true;
            }
        }
        return false;
    }

    public Move getMove(int frow, int fcol, int row, int col) {
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i).getMove()[0] == frow &&
                    moves.get(i).getMove()[1] == fcol &&
                    moves.get(i).getMove()[2] == row &&
                    moves.get(i).getMove()[3] == col) {
                // System.out.println("from : " + fcol + " " + frow + " to: " + col + " " + row);
                return (Move) moves.get(i);
            }
        }
        return null;
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {

    }

    @Override
    public void mouseMoved(MouseEvent arg0) {
        boardCol = arg0.getX() / squareSize;
        boardRow = arg0.getY() / squareSize;
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        int winner = board.checkWin();
        //
        if (winner == Board.P2_WIN) {
            frame.setTitle("Breakthrough - Black wins");
            System.out.println("P2 wins");
            board = new Board();
            board.initialize();
        } else if (winner == Board.P1_WIN) {
            frame.setTitle("Breakthrough - White wins.");
            System.out.println("P1 wins");
            board = new Board();
            board.initialize();
        }
        // clickPos[0] / 8  is the old row
        // clickPos[0] % 8  is the old col
        // boardRow; the row we're going to, set in mouseover
        // boardCol; the col we're going to, set in mouseover
        int boardPos = boardRow * 8 + boardCol;
        System.out.println("clicked " + boardRow + " " + boardCol);
        char playerToMoveChar = board.getPlayerToMove() == 1 ? 'w' : 'b'; 
        if (clickNum == 0) {
            if (board.board[boardRow][boardCol] != playerToMoveChar) {
                // System.out.println("fail 1");
                return;
            }
        } else if (clickNum == 1) {
            if (board.board[boardRow][boardCol] == playerToMoveChar || !isAvailMove(clickPos[0] / 8, clickPos[0] % 8, boardRow, boardCol)) {
                // System.out.println(board.board[boardRow][boardCol] + " " + (clickPos[0] / 8) + " " + (clickPos[0] % 8));
                // System.out.println("fail 2: " + (clickPos[0] / 8) + " " + (clickPos[0] % 8) + " " + boardRow + " " + boardCol);
                clickNum--;
                return;
            }
        }
        System.out.println("clicked here");
        //
        clickPos[clickNum] = boardPos;
        clickNum++;
        if (clickNum == 2) {
            System.out.println("Executing move!!!");
            lastMove = getMove(clickPos[0] / 8, clickPos[0] % 8, clickPos[1] / 8, clickPos[1] % 8);

            clickNum = 0;
            clickPos = new int[]{-1, -1, -1};
            
            makeMove(lastMove); 

            /*board.doAIMove(lastMove, board.getPlayerToMove());
            //
            if (board.getPlayerToMove() == Board.P2) {
                frame.setTitle("Breakthrough - Black's move.");
            } else {
                frame.setTitle("Breakthrough - White's move.");
            }
            moves = board.getExpandMoves();
            repaint();
            clickNum = 0;
            clickPos = new int[]{-1, -1, -1};*/
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {

    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        boardCol = -1;
        boardRow = -1;
    }

    @Override
    public void mousePressed(MouseEvent arg0) {

    }

    @Override
    public void mouseReleased(MouseEvent arg0) {

    }

    @Override
    public void makeMove(IMove move) {
        lastMove = move;
        board.doAIMove(move, board.getPlayerToMove());
        //
        int winner = board.checkWin();
        if (winner == Board.P2_WIN) {
            frame.setTitle("Breakthrough - Black wins");
            return;
        } else if (winner == Board.P1_WIN) {
            frame.setTitle("Breakthrough - White wins.");
            return;
        } else if (winner == Board.DRAW) {
            frame.setTitle("Breakthrough - Draw!");
            return;
        }

        repaint();
        clickNum = 0;
        clickPos = new int[]{-1, -1, -1};
        moves = board.getExpandMoves();

        // Run the GC in between moves, to limit the runs during search
        System.gc();
        //
        if (board.getPlayerToMove() == Board.P2) {
            frame.setTitle("Breakthrough - Black's move");
            if (aiPlayer2 != null)
                aiPlayer2.getMove(board.copy(), this, Board.P2, true, lastMove);
            //aiPlayer2.getMove(board, this, Board.P2, true, lastMove);
        } else {
            frame.setTitle("Breakthrough - White's move");
            if (aiPlayer1 != null)
                aiPlayer1.getMove(board.copy(), this, Board.P1, true, lastMove);
            //aiPlayer1.getMove(board, this, Board.P1, true, lastMove);
        }
    }
}
