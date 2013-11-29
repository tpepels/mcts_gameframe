package breakthrough.game;

import ai.FastTanh;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Board implements IBoard {
    private static final int N_PIECES = 16;
    private static final MoveList tempList = new MoveList(3);   // Temp move store for heuristic evaluation
    private static final ArrayList<IMove> poMoves = new ArrayList<IMove>(384);
    private static final MoveList static_moves = new MoveList(384);   // 64*6
    //
    public char[][] board;
    public int nMoves, winner, curPlayer;
    private int pieces1, pieces2;
    private int progress1, progress2;
    private Stack<IMove> pastMoves;

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

    private void recomputeProgress(int player) { 
        if (player == 1) { 
            // white, start from top
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) { 
                    if (board[r][c] == 'w') { 
                        progress1 = 7-r; 
                        return;
                    }
                }
            }
        }
        else if (player == 2) { 
            // black, start from bottom
            for (int r = 7; r >= 0; r--) {
                for (int c = 0; c < 8; c++) { 
                    if (board[r][c] == 'b') { 
                        progress2 = r; 
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        int[] movearr = move.getMove();
        int r = movearr[0], c = movearr[1], rp = movearr[2], cp = movearr[3];

        board[rp][cp] = board[r][c];
        board[r][c] = '.';

        // check for a capture
        if (move.getType() == Move.CAPTURE) {
            if (player == 1) { 
              pieces2--;
              // wiping out this piece could reduce the player's progress
              if (progress2 == rp && pieces2 > 0) 
                recomputeProgress(2); 
            }
            else if (player == 2) {
              pieces1--;
              if (progress1 == 7 - rp && pieces1 > 0) 
                recomputeProgress(1); 
            }
        }

        // check for a win
        if (player == 1 && (rp == 0 || pieces2 == 0)) winner = 1;
        else if (player == 2 && (rp == 7 || pieces1 == 0)) winner = 2;

        // check for progress (furthest pawn)
        if (player == 1 && (7 - rp) > progress1) progress1 = 7 - rp;
        else if (player == 2 && rp > progress2) progress2 = rp;

        nMoves++;
        pastMoves.push(move);
        curPlayer = 3 - curPlayer;

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
        progress1 = move.getOldProgress1();
        progress2 = move.getOldProgress2();
    }

    @Override
    public MoveList getExpandMoves() {
        static_moves.clear();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (curPlayer == 1 && board[r][c] == 'w') {
                    if (inBounds(r - 1, c - 1)) {
                        // northwest
                        if (board[r - 1][c - 1] == 'b')
                            static_moves.add(new Move(r, c, r - 1, c - 1, Move.CAPTURE, progress1, progress2));
                        else if (board[r - 1][c - 1] == '.')
                            static_moves.add(new Move(r, c, r - 1, c - 1, Move.MOVE, progress1, progress2));
                    }
                    if (inBounds(r - 1, c + 1)) {
                        // northeast
                        if (board[r - 1][c + 1] == 'b')
                            static_moves.add(new Move(r, c, r - 1, c + 1, Move.CAPTURE, progress1, progress2));
                        else if (board[r - 1][c + 1] == '.')
                            static_moves.add(new Move(r, c, r - 1, c + 1, Move.MOVE, progress1, progress2));
                    }
                    if (inBounds(r - 1, c) && board[r - 1][c] == '.') {
                        // north
                        static_moves.add(new Move(r, c, r - 1, c, Move.MOVE, progress1, progress2));
                    }
                } else if (curPlayer == 2 && board[r][c] == 'b') {
                    if (inBounds(r + 1, c - 1)) {
                        // southwest
                        if (board[r + 1][c - 1] == 'w')
                            static_moves.add(new Move(r, c, r + 1, c - 1, Move.CAPTURE, progress1, progress2));
                        else if (board[r + 1][c - 1] == '.')
                            static_moves.add(new Move(r, c, r + 1, c - 1, Move.MOVE, progress1, progress2));
                    }
                    if (inBounds(r + 1, c + 1)) {
                        // southeast
                        if (board[r + 1][c + 1] == 'w')
                            static_moves.add(new Move(r, c, r + 1, c + 1, Move.CAPTURE, progress1, progress2));
                        else if (board[r + 1][c + 1] == '.')
                            static_moves.add(new Move(r, c, r + 1, c + 1, Move.MOVE, progress1, progress2));
                    }
                    if (inBounds(r + 1, c) && board[r + 1][c] == '.') {
                        // south
                        static_moves.add(new Move(r, c, r + 1, c, Move.MOVE, progress1, progress2));
                    }
                }
            }
        }
        return static_moves.copy();
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        //ArrayList<IMove> forced = new ArrayList<IMove>(); 
        ArrayList<IMove> forced = null;

        poMoves.clear();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                tempList.clear();
                if (curPlayer == 1 && board[r][c] == 'w') {
                    if (inBounds(r - 1, c - 1)) {
                        // northwest
                        if (board[r - 1][c - 1] == 'b')
                            tempList.add(new Move(r, c, r - 1, c - 1, Move.CAPTURE, progress1, progress2));
                        else if (board[r - 1][c - 1] == '.')
                            tempList.add(new Move(r, c, r - 1, c - 1, Move.MOVE, progress1, progress2));
                    }
                    if (inBounds(r - 1, c + 1)) {
                        // northeast
                        if (board[r - 1][c + 1] == 'b')
                            tempList.add(new Move(r, c, r - 1, c + 1, Move.CAPTURE, progress1, progress2));
                        else if (board[r - 1][c + 1] == '.')
                            tempList.add(new Move(r, c, r - 1, c + 1, Move.MOVE, progress1, progress2));
                    }
                    if (inBounds(r - 1, c) && board[r - 1][c] == '.') {
                        // north
                        tempList.add(new Move(r, c, r - 1, c, Move.MOVE, progress1, progress2));
                    }
                } else if (curPlayer == 2 && board[r][c] == 'b') {
                    if (inBounds(r + 1, c - 1)) {
                        // southwest
                        if (board[r + 1][c - 1] == 'w')
                            tempList.add(new Move(r, c, r + 1, c - 1, Move.CAPTURE, progress1, progress2));
                        else if (board[r + 1][c - 1] == '.')
                            tempList.add(new Move(r, c, r + 1, c - 1, Move.MOVE, progress1, progress2));
                    }
                    if (inBounds(r + 1, c + 1)) {
                        // southeast
                        if (board[r + 1][c + 1] == 'w')
                            tempList.add(new Move(r, c, r + 1, c + 1, Move.CAPTURE, progress1, progress2));
                        else if (board[r + 1][c + 1] == '.')
                            tempList.add(new Move(r, c, r + 1, c + 1, Move.MOVE, progress1, progress2));
                    }
                    if (inBounds(r + 1, c) && board[r + 1][c] == '.') {
                        // south
                        tempList.add(new Move(r, c, r + 1, c, Move.MOVE, progress1, progress2));
                    }
                }
                if (tempList.size() == 0)
                    continue;
                //
                if (heuristics) {
                    for (int i = 0; i < tempList.size(); i++) {
                        IMove move = tempList.get(i);
                        poMoves.add(move);
                        // Prefer defenseless capture moves
                        if (move.getType() == Move.CAPTURE) {
                            int mr = move.getMove()[0]; int mc = move.getMove()[1];
                            int mrp = move.getMove()[2]; int mcp = move.getMove()[3]; 
                            int pl = board[mr][mc] == 'w' ? 1 : 2; 

                            if (   pl == 1 
                                && (!inBounds(mrp-1, mcp-1) || board[mrp-1][mcp-1] == '.')
                                && (!inBounds(mrp-1, mcp+1) || board[mrp-1][mcp+1] == '.') )
                            {
                                poMoves.add(move);
                                poMoves.add(move);
                                poMoves.add(move);
                                poMoves.add(move);
                            }
                            else if (   pl == 2
                                     && (!inBounds(mrp+1, mcp-1) || board[mrp+1][mcp-1] == '.')
                                     && (!inBounds(mrp+1, mcp+1) || board[mrp+1][mcp+1] == '.') )
                            {
                                poMoves.add(move);
                                poMoves.add(move);
                                poMoves.add(move);
                                poMoves.add(move);
                            } else {
                                poMoves.add(move);
                            }
                        }
                        // Decisive / anti-decisive moves
                        if (curPlayer == 1 && (move.getMove()[2] == 0)) {
                            poMoves.clear();
                            poMoves.add(move);
                            return poMoves;
                        } else if (curPlayer == 2 && (move.getMove()[2] == 7)) {
                            poMoves.clear();
                            poMoves.add(move);
                            return poMoves;
                        } else if (move.getType() == Move.CAPTURE && (move.getMove()[0] == 7 || move.getMove()[0] == 0)) {
                            if (forced == null) 
                                forced = new ArrayList<IMove>();
                            forced.add(move);
                        }
                    }
                } else {
                    for (int i = 0; i < tempList.size(); i++) {
                        IMove move = tempList.get(i);
                        poMoves.add(move);
                    }
                }
            }
        }
        if (forced != null && forced.size() > 0) return forced;
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

        pieces1 = pieces2 = N_PIECES;
        progress1 = progress2 = 1;
        nMoves = 0;
        winner = NONE_WIN;
        curPlayer = 1;
        pastMoves = new Stack<IMove>();
    }

    @Override
    public double evaluate(int player) {
        // inspired by evaluation function in Maarten's thesis
        double p1eval = 0;
        if (progress1 == 7 || pieces2 == 0) p1eval = 1;
        else if (progress2 == 7 || pieces1 == 0) p1eval = -1;
        else {
            double delta = (pieces1 * 10 + progress1 * 2.5) - (pieces2 * 10 + progress2 * 2.5);
            if (delta < -100) delta = -100;
            if (delta > 100) delta = 100;
            // now pass it through tanh;
            p1eval = FastTanh.tanh(delta / 60.0);
        }
        return (player == 1 ? p1eval : -p1eval);
    }

    @Override
    public double getQuality() {
        if(winner == P1_WIN)
            return ((double)(N_PIECES - pieces2 - pieces1) / (double)(2 * N_PIECES));
        else if (winner == P2_WIN)
            return ((double)(N_PIECES - pieces1 - pieces2) / (double)(2 * N_PIECES));
        return 1;
    }

    public String toString() {
        String str = "";
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) str += board[r][c];
            str += "\n";
        }
        str +=  "\nPieces: " + pieces1 + " " + pieces2 + ", " 
               + "Progresses: " + progress1 + " " + progress2 + "\n";
        return str;
    }

    private boolean inBounds(int r, int c) {
        return (r >= 0 && c >= 0 && r < 8 && c < 8);
    }

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
        // TODO I think a draw is possible, if the only two pieces left are facing eachother
        return false;
    }
}
