package breakthrough.game;

import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;
import ai.FastTanh;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Board implements IBoard {
    private static MoveList static_moves = new MoveList(384);  // 64*6
    private static ArrayList<IMove> poMoves = new ArrayList<IMove>(384);

    private char[][] board;
    private int pieces1, pieces2;
    private int progress1;
    private int progress2;
    private int nMoves, winner, curPlayer;
    private Stack<IMove> pastMoves;

    @Override
    public void newDeterminization(int myPlayer) {
        // only need this for imperfect information games
    }

    @Override
    public boolean isPartialObservable() {
        return false;
    }

    @Override
    public int getNMovesMade() {
        return nMoves;
    }

    @Override
    public boolean isLegal(IMove move) {
        // only used in imperfect information
        return true;
    }

    @Override
    public boolean drawPossible() {
        return false;
    }

    @Override
    public IBoard copy() {
        Board b = new Board();

        b.board = new char[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                b.board[r][c] = this.board[r][c];

        b.pieces1 = this.pieces1;
        b.pieces2 = this.pieces2;
        b.progress1 = this.progress1;
        b.progress2 = this.progress2;
        b.nMoves = this.nMoves;
        b.winner = this.winner;
        b.curPlayer = this.curPlayer;

        // no need to copy the move stack, but need to initialize it
        b.pastMoves = new Stack<IMove>();

        return b;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {

        int[] movearr = move.getMove();
        int r = movearr[0], c = movearr[1], rp = movearr[2], cp = movearr[3];

        board[rp][cp] = board[r][c];
        board[r][c] = '.';

        // check for a win
        if (player == 1 && (rp == 0 || pieces2 == 0)) winner = 1;
        else if (player == 2 && (rp == 7 || pieces1 == 0)) winner = 2;

        // check for a capture
        if (move.getType() == Move.CAPTURE) {
            if (player == 1) pieces2--;
            else if (player == 2) pieces1--;
        }

        // check for progress (furthest pawn)
        if      (player == 1 && (7-rp) > progress1) progress1 = 7-rp;
        else if (player == 2 && rp > progress2) progress2 = rp; 

        nMoves++;
        pastMoves.push(move);
        curPlayer = 3 - curPlayer;

        // FIXME: modify progress1 and progress2, will be needed for evaluation function

        return true;
    }

    @Override
    public void undoMove() {
        Move move = (Move) pastMoves.pop();
        nMoves--;
        curPlayer = 3 - curPlayer;

        int[] movearr = move.getMove();
        int r = movearr[0], c = movearr[1], rp = movearr[2], cp = movearr[3];

        board[r][c] = board[rp][cp];
        board[rp][cp] = '.';

        // remove the win, if there was one
        winner = NONE_WIN;

        // check if it was a capture
        if (move.getType() == Move.CAPTURE) {
            if (curPlayer == 1) {
                board[rp][cp] = 'b';
                pieces2++;
            } else if (curPlayer == 2) {
                board[rp][cp] = 'w';
                pieces1++;
            }
        }

        // remove back the progress
        if      (curPlayer == 1) progress1 = move.getOldProgress();
        else if (curPlayer == 2) progress2 = move.getOldProgress();
    }

    @Override
    public MoveList getExpandMoves() {
        static_moves.clear();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (curPlayer == 1 && board[r][c] == 'w') {
                    if (inBounds(r - 1, c - 1)) {
                        // northwest
                        if      (board[r-1][c-1] == 'b') static_moves.add(new Move(r, c, r-1, c-1, Move.CAPTURE, progress1));
                        else if (board[r-1][c-1] == '.') static_moves.add(new Move(r, c, r-1, c-1, Move.MOVE, progress1));
                    }
                    else if (inBounds(r-1,c+1)) {
                        // northeast
                        if      (board[r-1][c+1] == 'b') static_moves.add(new Move(r, c, r-1, c+1, Move.CAPTURE, progress1));
                        else if (board[r-1][c+1] == '.') static_moves.add(new Move(r, c, r-1, c+1, Move.MOVE, progress1));
                    }
                    else if (inBounds(r-1,c) && board[r-1][c] == '.') {
                        // north
                        static_moves.add(new Move(r, c, r-1, c, Move.MOVE, progress1));
                    }
                } else if (curPlayer == 2 && board[r][c] == 'b') {
                    if (inBounds(r + 1, c - 1)) {
                        // southwest
                        if      (board[r+1][c-1] == 'w') static_moves.add(new Move(r, c, r+1, c-1, Move.CAPTURE, progress2));
                        else if (board[r+1][c-1] == '.') static_moves.add(new Move(r, c, r+1, c-1, Move.MOVE, progress2));
                    }
                    else if (inBounds(r+1,c+1)) {
                        // southeast
                        if      (board[r+1][c+1] == 'w') static_moves.add(new Move(r, c, r+1, c+1, Move.CAPTURE, progress2));
                        else if (board[r+1][c+1] == '.') static_moves.add(new Move(r, c, r+1, c+1, Move.MOVE, progress2));
                    }
                    else if (inBounds(r+1,c) && board[r+1][c] == '.') {
                        // south
                        static_moves.add(new Move(r, c, r+1, c, Move.MOVE, progress2));
                    }
                }
            }
        }

        return static_moves.copy();
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        poMoves.clear();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (curPlayer == 1 && board[r][c] == 'w') {
                    if (inBounds(r - 1, c - 1)) {
                        // northwest
                        if (board[r - 1][c - 1] == 'b') poMoves.add(new Move(r, c, r - 1, c - 1, Move.CAPTURE, progress1));
                        else if (board[r - 1][c - 1] == '.') poMoves.add(new Move(r, c, r - 1, c - 1, Move.MOVE, progress1));
                    } else if (inBounds(r - 1, c + 1)) {
                        // northeast
                        if (board[r - 1][c + 1] == 'b') poMoves.add(new Move(r, c, r - 1, c + 1, Move.CAPTURE, progress1));
                        else if (board[r - 1][c + 1] == '.') poMoves.add(new Move(r, c, r - 1, c + 1, Move.MOVE, progress1));
                    } else if (inBounds(r - 1, c) && board[r - 1][c] == '.') {
                        // north
                        poMoves.add(new Move(r, c, r - 1, c, Move.MOVE, progress1));
                    }
                } else if (curPlayer == 2 && board[r][c] == 'b') {
                    if (inBounds(r + 1, c - 1)) {
                        // southwest
                        if (board[r + 1][c - 1] == 'w') poMoves.add(new Move(r, c, r + 1, c - 1, Move.CAPTURE, progress2));
                        else if (board[r + 1][c - 1] == '.') poMoves.add(new Move(r, c, r + 1, c - 1, Move.MOVE, progress2));
                    } else if (inBounds(r + 1, c + 1)) {
                        // southeast
                        if (board[r + 1][c + 1] == 'w') poMoves.add(new Move(r, c, r + 1, c + 1, Move.CAPTURE, progress2));
                        else if (board[r + 1][c + 1] == '.') poMoves.add(new Move(r, c, r + 1, c + 1, Move.MOVE, progress2));
                    } else if (inBounds(r + 1, c) && board[r + 1][c] == '.') {
                        // south
                        poMoves.add(new Move(r, c, r + 1, c, Move.MOVE, progress2));
                    }
                }
            }
        }
        return poMoves;
    }

    @Override
    public int getOpponent(int player) {
        return (3 - player);
    }

    @Override
    public int checkWin() {
        return winner;
    }

    @Override
    public int checkPlayoutWin() {
        return winner;
    }

    @Override
    public int getPlayerToMove() {
        return curPlayer;
    }

    @Override
    public int getMaxUniqueMoveId() {
        return 4095;  // 64*64 - 1
    }

    @Override
    public void initialize() {
        board = new char[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                if (r == 0 || r == 1) board[r][c] = 'b'; // player 2 is black
                else if (r == 6 || r == 7) board[r][c] = 'w'; // player 1 is white
                else board[r][c] = '.';
            }

        pieces1 = pieces2 = 16;
        progress1 = progress2 = 1; 
        nMoves = 0;
        winner = NONE_WIN;
        curPlayer = 1;

        pastMoves = new Stack<IMove>();
    }
    
    @Override
    public double evaluate(int player) { 
        // inspired by evaluation function in Maarten's thesis
        double delta = (pieces1*10 + progress1*3) - (pieces2*10 + progress2*3);
        if (delta < -100) delta = -100;
        if (delta > 100) delta = 100;
        // now pass it through tanh;  
        // I think this is killing the speed
        double p1eval = FastTanh.tanh(delta/60.0);
        return (player == 1 ? p1eval : -p1eval);
    }

    public String toString() {
        String str = "";
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) str += board[r][c];
            str += "\n";
        }
        str += "\nPieces: " + pieces1 + " " + pieces2 + "\n";
        return str;
    }

    private boolean inBounds(int r, int c) {
        return (r >= 0 && c >= 0 && r < 8 && c < 8);
    }

}
