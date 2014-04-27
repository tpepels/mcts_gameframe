package breakthrough.game;

import ai.FastTanh;
import ai.StatCounter;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Board implements IBoard {
    private static final int N_PIECES = 16;
    private static final MoveList tempList = new MoveList(3);   // Temp move store for heuristic evaluation
    private static final ArrayList<IMove> poMoves = new ArrayList<IMove>(384);
    private static final MoveList static_moves = new MoveList(384);   // 64*6
    private static final MoveList cap1_moves = new MoveList(384);   // 64*6
    private static final MoveList cap2_moves = new MoveList(384);   // 64*6
    private static final MoveList cap3_moves = new MoveList(384);   // 64*6
    private static final MoveList reg_moves = new MoveList(384);   // 64*6
    private static final MoveList dec_moves = new MoveList(384);

    // these are the values for black (p2)
    private static final char[][] lorentzValues = {{5, 15, 15, 5, 5, 15, 15, 5},
            {2, 3, 3, 3, 3, 3, 3, 2},
            {4, 6, 6, 6, 6, 6, 6, 4},
            {7, 10, 10, 10, 10, 10, 10, 7},
            {11, 15, 15, 15, 15, 15, 15, 11},
            {16, 21, 21, 21, 21, 21, 21, 16},
            {20, 28, 28, 28, 28, 28, 28, 20},
            {36, 36, 36, 36, 36, 36, 36, 36}};

    private String startingBoard = "";
    private int startingPlayer = 0;

    /*private String startingBoard =
          "..bb.bb." +
          ".b....bb" +
          "...w.b.b" +
          "..w....w" +
          "......w." +
          "..ww...w" +
          ".ww.ww.." +
          "........" ;

    private int startingPlayer = 2;*/

    static long[] zbnums = null;
    static long blackHash, whiteHash;


    //
    public char[][] board;
    public int nMoves, winner, curPlayer;
    private int pieces1, pieces2;
    private int progress1, progress2;
    private int lorentzPV1, lorentzPV2;
    public int capBonus1, capBonus2;
    private long zbHash = 0;
    private Stack<IMove> pastMoves;

    static {
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
        b.lorentzPV1 = this.lorentzPV1;
        b.lorentzPV2 = this.lorentzPV2;
        b.progress1 = this.progress1;
        b.progress2 = this.progress2;
        b.capBonus1 = this.capBonus1;
        b.capBonus2 = this.capBonus2;
        b.nMoves = this.nMoves;
        b.winner = this.winner;
        b.curPlayer = this.curPlayer;

        // no need to copy the move stack, but need to initialize it
        b.pastMoves = new Stack<IMove>();
        b.zbHash = zbHash;

        return b;
    }

    private int getLorentzPV(int player, int row, int col) {
        if (player == 2)
            return lorentzValues[row][col];
        else
            return lorentzValues[7 - row][col];
    }

    private int getZbId(int r, int c) {
        int id = (r * 8 + c) * 3;
        if (board[r][c] == 'w')
            id += 1;
        else if (board[r][c] == 'b')
            id += 2;
        return id;
    }

    private void recomputeProgress(int player) {
        if (player == 1) {
            // white, start from top
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (board[r][c] == 'w') {
                        progress1 = 7 - r;
                        return;
                    }
                }
            }
        } else if (player == 2) {
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

        // remove zobrist nums from hash of the squares that are changing
        int before_from_zbId = getZbId(r, c);
        int before_to_zbId = getZbId(rp, cp);
        zbHash ^= zbnums[before_from_zbId];
        zbHash ^= zbnums[before_to_zbId];

        board[rp][cp] = board[r][c];
        board[r][c] = '.';

        // lorentz piece value updates:
        // subtract off from where you came, add where you ended up
        if (player == 1) {
            lorentzPV1 -= getLorentzPV(1, r, c);
            lorentzPV1 += getLorentzPV(1, rp, cp);
        } else if (player == 2) {
            lorentzPV2 -= getLorentzPV(2, r, c);
            lorentzPV2 += getLorentzPV(2, rp, cp);
        }

        // check for a capture
        if (move.getType() == Move.CAPTURE) {
            if (player == 1) {
                pieces2--;
                // wiping out this piece could reduce the player's progress
                if (progress2 == rp && pieces2 > 0)
                    recomputeProgress(2);
                // remove a lorentz piece value for that player
                lorentzPV2 -= getLorentzPV(2, rp, cp);
                // cap bonus for capturing on defending side
                if (rp >= 4 && rp <= 7)
                    capBonus1++;
            } else if (player == 2) {
                pieces1--;
                if (progress1 == 7 - rp && pieces1 > 0)
                    recomputeProgress(1);
                // remove a lorentz piece value for that player
                lorentzPV1 -= getLorentzPV(1, rp, cp);
                // cap bonus for capturing on defending side
                if (rp >= 0 && rp <= 3)
                    capBonus2++;
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

        // add zobrist nums for the new hash
        int after_from_zbId = getZbId(r, c);
        int after_to_zbId = getZbId(rp, cp);
        zbHash ^= zbnums[after_from_zbId];
        zbHash ^= zbnums[after_to_zbId];

        hashCurrentPlayer();

        return true;
    }

    @Override
    public void undoMove() {
        Move move = (Move) pastMoves.pop();
        nMoves--;
        curPlayer = 3 - curPlayer;

        int[] movearr = move.getMove();
        int r = movearr[0], c = movearr[1], rp = movearr[2], cp = movearr[3];

        // remove zobrist nums from hash of the squares that are changing
        int before_from_zbId = getZbId(r, c);
        int before_to_zbId = getZbId(rp, cp);
        zbHash = zbHash ^ zbnums[before_from_zbId];
        zbHash = zbHash ^ zbnums[before_to_zbId];

        board[r][c] = board[rp][cp];
        board[rp][cp] = '.';

        // lorentz piece value updates:
        if (curPlayer == 1) {
            lorentzPV1 -= getLorentzPV(1, rp, cp);
            lorentzPV1 += getLorentzPV(1, r, c);
        } else if (curPlayer == 2) {
            lorentzPV2 -= getLorentzPV(2, rp, cp);
            lorentzPV2 += getLorentzPV(2, r, c);
        }

        // remove the win, if there was one
        winner = NONE_WIN;

        // check if it was a capture
        if (move.getType() == Move.CAPTURE) {
            if (curPlayer == 1) {
                board[rp][cp] = 'b';
                pieces2++;
                lorentzPV2 += getLorentzPV(2, rp, cp);
            } else if (curPlayer == 2) {
                board[rp][cp] = 'w';
                pieces1++;
                lorentzPV1 += getLorentzPV(1, rp, cp);
            }
        }

        // remove back the progress
        progress1 = move.getOldProgress1();
        progress2 = move.getOldProgress2();

        // remove back the capture bonuses
        capBonus1 = move.getOldCapBonus1();
        capBonus2 = move.getOldCapBonus2();

        // add zobrist nums for the new hash
        int after_from_zbId = getZbId(r, c);
        int after_to_zbId = getZbId(rp, cp);
        zbHash = zbHash ^ zbnums[after_from_zbId];
        zbHash = zbHash ^ zbnums[after_to_zbId];
        //
        hashCurrentPlayer();
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
                            static_moves.add(new Move(r, c, r - 1, c - 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r - 1][c - 1] == '.')
                            static_moves.add(new Move(r, c, r - 1, c - 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r - 1, c + 1)) {
                        // northeast
                        if (board[r - 1][c + 1] == 'b')
                            static_moves.add(new Move(r, c, r - 1, c + 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r - 1][c + 1] == '.')
                            static_moves.add(new Move(r, c, r - 1, c + 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r - 1, c) && board[r - 1][c] == '.') {
                        // north
                        static_moves.add(new Move(r, c, r - 1, c, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                } else if (curPlayer == 2 && board[r][c] == 'b') {
                    if (inBounds(r + 1, c - 1)) {
                        // southwest
                        if (board[r + 1][c - 1] == 'w')
                            static_moves.add(new Move(r, c, r + 1, c - 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r + 1][c - 1] == '.')
                            static_moves.add(new Move(r, c, r + 1, c - 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r + 1, c + 1)) {
                        // southeast
                        if (board[r + 1][c + 1] == 'w')
                            static_moves.add(new Move(r, c, r + 1, c + 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r + 1][c + 1] == '.')
                            static_moves.add(new Move(r, c, r + 1, c + 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r + 1, c) && board[r + 1][c] == '.') {
                        // south
                        static_moves.add(new Move(r, c, r + 1, c, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
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
                            tempList.add(new Move(r, c, r - 1, c - 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r - 1][c - 1] == '.')
                            tempList.add(new Move(r, c, r - 1, c - 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r - 1, c + 1)) {
                        // northeast
                        if (board[r - 1][c + 1] == 'b')
                            tempList.add(new Move(r, c, r - 1, c + 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r - 1][c + 1] == '.')
                            tempList.add(new Move(r, c, r - 1, c + 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r - 1, c) && board[r - 1][c] == '.') {
                        // north
                        tempList.add(new Move(r, c, r - 1, c, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                } else if (curPlayer == 2 && board[r][c] == 'b') {
                    if (inBounds(r + 1, c - 1)) {
                        // southwest
                        if (board[r + 1][c - 1] == 'w')
                            tempList.add(new Move(r, c, r + 1, c - 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r + 1][c - 1] == '.')
                            tempList.add(new Move(r, c, r + 1, c - 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r + 1, c + 1)) {
                        // southeast
                        if (board[r + 1][c + 1] == 'w')
                            tempList.add(new Move(r, c, r + 1, c + 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r + 1][c + 1] == '.')
                            tempList.add(new Move(r, c, r + 1, c + 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r + 1, c) && board[r + 1][c] == '.') {
                        // south
                        tempList.add(new Move(r, c, r + 1, c, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
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
                            int mr = move.getMove()[0];
                            int mc = move.getMove()[1];
                            int mrp = move.getMove()[2];
                            int mcp = move.getMove()[3];
                            int pl = board[mr][mc] == 'w' ? 1 : 2;

                            if (pl == 1
                                    && (!inBounds(mrp - 1, mcp - 1) || board[mrp - 1][mcp - 1] == '.')
                                    && (!inBounds(mrp - 1, mcp + 1) || board[mrp - 1][mcp + 1] == '.')) {
                                poMoves.add(move);
                                poMoves.add(move);
                                poMoves.add(move);
                                poMoves.add(move);
                            } else if (pl == 2
                                    && (!inBounds(mrp + 1, mcp - 1) || board[mrp + 1][mcp - 1] == '.')
                                    && (!inBounds(mrp + 1, mcp + 1) || board[mrp + 1][mcp + 1] == '.')) {
                                poMoves.add(move);
                                poMoves.add(move);
                                poMoves.add(move);
                                poMoves.add(move);
                            } else if (curPlayer == 1 && mrp >= 4 && mrp <= 7) {
                                // prefer defensive captures
                                //poMoves.add(move);
                                poMoves.add(move);
                            } else if (curPlayer == 2 && mrp >= 0 && mrp <= 3) {
                                // prefer defensive captures
                                //poMoves.add(move);
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
        pieces1 = pieces2 = 0;

        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {

                if (startingBoard.equals("")) {
                    if (r == 0 || r == 1) {
                        board[r][c] = 'b'; // player 2 is black
                        lorentzPV2 += getLorentzPV(2, r, c);
                    } else if (r == 6 || r == 7) {
                        board[r][c] = 'w'; // player 1 is white
                        lorentzPV1 += getLorentzPV(1, r, c);
                    } else board[r][c] = '.';
                } else {
                    if (startingBoard.length() != 64)
                        throw new RuntimeException("Starting board length! " + startingBoard.length());

                    board[r][c] = startingBoard.charAt(r * 8 + c);

                    if (board[r][c] == 'b') {
                        pieces2++;
                        lorentzPV2 += getLorentzPV(2, r, c);
                    } else if (board[r][c] == 'w') {
                        pieces1++;
                        lorentzPV1 += getLorentzPV(1, r, c);
                    }
                }
            }

        if (startingBoard.length() != 64) {
            pieces1 = pieces2 = N_PIECES;
            progress1 = progress2 = 1;
            capBonus1 = capBonus2 = 0;
            curPlayer = 1;
        } else {
            recomputeProgress(1);
            recomputeProgress(2);
            curPlayer = startingPlayer;
        }

        nMoves = 0;
        winner = NONE_WIN;
        pastMoves = new Stack<IMove>();

        // initialize the zobrist numbers

        if (zbnums == null) {
            // init the zobrist numbers
            Random rng = new Random();

            // 64 locations, 3 states for each location = 192
            zbnums = new long[192];

            for (int i = 0; i < 192; i++)
                zbnums[i] = rng.nextLong();
            whiteHash = rng.nextLong();
            blackHash = rng.nextLong();
        }
        // now build the initial hash
        zbHash = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int id = getZbId(r, c);
                zbHash ^= zbnums[id];
            }
        }
        curPlayer = P1;
        zbHash ^= whiteHash;
    }

    public double evaluateSchadd(int player) {
        // inspired by ion function in Maarten's thesis
        double p1eval = 0;
        if (progress1 == 7 || pieces2 == 0) p1eval = 1;
        else if (progress2 == 7 || pieces1 == 0) p1eval = -1;
        else {
            //double delta = (pieces1 * 10 + progress1 * 2.5 + capBonus1) - (pieces2 * 10 + progress2 * 2.5 + capBonus2);
            double delta = (pieces1 * 10 + progress1 * 2.5) - (pieces2 * 10 + progress2 * 2.5);
            if (delta < -100) delta = -100;
            if (delta > 100) delta = 100;
            // now pass it through tanh;
            p1eval = FastTanh.tanh(delta / 60.0);
        }
        return (player == 1 ? p1eval : -p1eval);
    }

    public double evaluateLorentz(int player) {
        // inspired by evaluation function in Maarten's thesis
        double p1eval = 0;
        if (progress1 == 7 || pieces2 == 0) p1eval = 1;
        else if (progress2 == 7 || pieces1 == 0) p1eval = -1;
        else {
            //double delta = (pieces1 * 10 + progress1 * 2.5) - (pieces2 * 10 + progress2 * 2.5);
            double delta = lorentzPV1 - lorentzPV2;
            //System.out.println("delta = " + delta);
            //if (delta < -100) delta = -100;
            //if (delta > 100) delta = 100;

            // now pass it through tanh;
            p1eval = FastTanh.tanh(delta / 100.0);
        }
        return (player == 1 ? p1eval : -p1eval);
    }

    @Override
    public double evaluate(int player, int version) {
        if (version == 0)
            return evaluateSchadd(player);
        else if (version == 1)
            return evaluateLorentz(player);
        else {
            throw new RuntimeException("Evaluation function version unknown! " + version);
        }
    }

    @Override
    public void initNodePriors(int parentPlayer, StatCounter stats, IMove move, int npvisits) {
        
        /*
        Using the eval func for node priors is actually bad. 
        See results/npriors.txt. Consistent with Lorenz paper.

        double eval = evaluate(parentPlayer); 
        for (int i = 0; i < npvisits; i++)
          stats.push(eval);
        if (true) return;*/

        initNodePriorsLorenz(parentPlayer, stats, move, npvisits);
    }

    public void initNodePriorsLorenz(int parentPlayer, StatCounter stats, IMove move, int npvisits) {

        //if (true) return;
        /*double eval = evaluate(parentPlayer); 
        for (int i = 0; i < npvisits; i++)
          stats.push(eval);
        if (true) return;*/

        // implements prior values according to Rich Lorenz's paper on Breakthrough        

        Move bmove = (Move) move;
        int rp = bmove.getMove()[2];
        int cp = bmove.getMove()[3];

        //assert(inBounds(rp,cp)); 
        char parentPiece = board[rp][cp];

        //assert((parentPlayer == 1 && parentPiece == 'w') || (parentPlayer == 2 && parentPiece == 'b')); 
        char oppPiece = (parentPiece == 'w' ? 'b' : 'w');

        // count immediate attackers and defenders
        int attackers = 0, defenders = 0;

        int[] rowOffset = {-1, -1, +1, +1};
        int[] colOffset = {-1, +1, -1, +1};

        for (int oi = 0; oi < 4; oi++) {
            int rpp = rp + rowOffset[oi];
            int cpp = cp + colOffset[oi];

            if (inBounds(rpp, cpp) && (board[rpp][cpp] == 'w' || board[rpp][cpp] == 'b')) {
                if (parentPiece == 'w' && oi < 2 && board[rpp][cpp] == 'b')
                    attackers++;
                if (parentPiece == 'w' && oi >= 2 && board[rpp][cpp] == 'w')
                    defenders++;

                if (parentPiece == 'b' && oi < 2 && board[rpp][cpp] == 'b')
                    defenders++;
                if (parentPiece == 'b' && oi >= 2 && board[rpp][cpp] == 'w')
                    attackers++;
            }
        }

        //System.out.println("ad " + attackers + " " + defenders);
        boolean safeMove = (attackers <= defenders);

        int distToGoal = (parentPlayer == 1 ? rp : (7 - rp));

        double winrate = 0.30;

        if (safeMove) {
            if (distToGoal == 1)
                winrate = 1.0;
            else if (distToGoal == 2)
                winrate = 0.95;
            else if (distToGoal == 3)
                winrate = 0.85;
            else if (distToGoal == 4)
                winrate = 0.75;
            else if (distToGoal == 5)
                winrate = 0.60;
        } else {
            if (bmove.getType() == Move.CAPTURE)
                winrate = 0.60;
        }

        //if (!safeMove) { 
        //    System.out.println("unsafe move! " + bmove + "\n" + toString()); 
        // }
        //System.out.println("Node priors, wins = " + wins);

        // this causes a significant slowdown
        /*for (int i = 0; i < winrate*npvisits; i++) 
            stats.push(1.0); 
        for (int i = 0; i < (1.0-winrate)*npvisits; i++)
            stats.push(-1.0);*/

        stats.initWinsLosses(winrate, npvisits);
    }

    @Override
    public double getQuality() {
        if (winner == P1_WIN)
            return ((double) (pieces1 - pieces2)) / (double) (N_PIECES);
        else if (winner == P2_WIN)
            return ((double) (pieces2 - pieces1)) / (double) (N_PIECES);
        return 1;
    }

    @Override
    public MoveList getOrderedMoves() {
        static_moves.clear();
        reg_moves.clear();
        cap1_moves.clear();
        cap2_moves.clear();
        cap3_moves.clear();
        dec_moves.clear();


        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                tempList.clear();
                if (curPlayer == 1 && board[r][c] == 'w') {
                    if (inBounds(r - 1, c - 1)) {
                        // northwest
                        if (board[r - 1][c - 1] == 'b')
                            tempList.add(new Move(r, c, r - 1, c - 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r - 1][c - 1] == '.')
                            tempList.add(new Move(r, c, r - 1, c - 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r - 1, c + 1)) {
                        // northeast
                        if (board[r - 1][c + 1] == 'b')
                            tempList.add(new Move(r, c, r - 1, c + 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r - 1][c + 1] == '.')
                            tempList.add(new Move(r, c, r - 1, c + 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r - 1, c) && board[r - 1][c] == '.') {
                        // north
                        tempList.add(new Move(r, c, r - 1, c, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                } else if (curPlayer == 2 && board[r][c] == 'b') {
                    if (inBounds(r + 1, c - 1)) {
                        // southwest
                        if (board[r + 1][c - 1] == 'w')
                            tempList.add(new Move(r, c, r + 1, c - 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r + 1][c - 1] == '.')
                            tempList.add(new Move(r, c, r + 1, c - 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r + 1, c + 1)) {
                        // southeast
                        if (board[r + 1][c + 1] == 'w')
                            tempList.add(new Move(r, c, r + 1, c + 1, Move.CAPTURE, progress1, progress2, capBonus1, capBonus2));
                        else if (board[r + 1][c + 1] == '.')
                            tempList.add(new Move(r, c, r + 1, c + 1, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                    if (inBounds(r + 1, c) && board[r + 1][c] == '.') {
                        // south
                        tempList.add(new Move(r, c, r + 1, c, Move.MOVE, progress1, progress2, capBonus1, capBonus2));
                    }
                }
                if (tempList.size() == 0)
                    continue;
                //
                for (int i = 0; i < tempList.size(); i++) {
                    IMove move = tempList.get(i);

                    // Prefer defenseless capture moves
                    if (move.getType() == Move.CAPTURE) {
                        int mr = move.getMove()[0];
                        int mc = move.getMove()[1];
                        int mrp = move.getMove()[2];
                        int mcp = move.getMove()[3];
                        int pl = board[mr][mc] == 'w' ? 1 : 2;

                        if (pl == 1
                                && (!inBounds(mrp - 1, mcp - 1) || board[mrp - 1][mcp - 1] == '.')
                                && (!inBounds(mrp - 1, mcp + 1) || board[mrp - 1][mcp + 1] == '.')) {
                            cap1_moves.add(move);
                        } else if (pl == 2
                                && (!inBounds(mrp + 1, mcp - 1) || board[mrp + 1][mcp - 1] == '.')
                                && (!inBounds(mrp + 1, mcp + 1) || board[mrp + 1][mcp + 1] == '.')) {
                            cap1_moves.add(move);
                        } else if (curPlayer == 1 && mrp >= 4 && mrp <= 7) {
                            // prefer defensive captures
                            //poMoves.add(move);
                            cap2_moves.add(move);
                        } else if (curPlayer == 2 && mrp >= 0 && mrp <= 3) {
                            // prefer defensive captures
                            //poMoves.add(move);
                            cap2_moves.add(move);
                        } else {
                            cap3_moves.add(move);
                        }

                        continue;
                    }

                    // Decisive / anti-decisive moves
                    if (curPlayer == 1 && (move.getMove()[2] == 0)) {
                        dec_moves.add(move);
                    } else if (curPlayer == 2 && (move.getMove()[2] == 7)) {
                        dec_moves.add(move);
                    } else if (move.getType() == Move.CAPTURE && (move.getMove()[0] == 7 || move.getMove()[0] == 0)) {
                        dec_moves.add(move);
                    } else {
                        reg_moves.add(move);
                    }
                }
            }
        }

        // decisive
        for (int i = 0; i < dec_moves.size(); i++)
            static_moves.add(dec_moves.get(i));

        // capture moves
        for (int i = 0; i < cap1_moves.size(); i++)
            static_moves.add(cap1_moves.get(i));
        for (int i = 0; i < cap2_moves.size(); i++)
            static_moves.add(cap2_moves.get(i));
        for (int i = 0; i < cap3_moves.size(); i++)
            static_moves.add(cap3_moves.get(i));

        // regular
        for (int i = 0; i < reg_moves.size(); i++)
            static_moves.add(reg_moves.get(i));

        return static_moves.copy();
    }

    private void hashCurrentPlayer() {
        if (curPlayer == Board.P1) {
            zbHash ^= blackHash;
            zbHash ^= whiteHash;
        } else {
            zbHash ^= whiteHash;
            zbHash ^= blackHash;
        }
    }

    @Override
    public long hash() {
        return zbHash;
    }

    public String toString() {
        String rowLabels = "87654321";
        String colLabels = "abcdefgh";

        String str = "";
        for (int r = 0; r < 8; r++) {
            str += (rowLabels.charAt(r));
            for (int c = 0; c < 8; c++) {
                str += board[r][c];
            }
            str += "\n";
        }
        str += (" " + colLabels + "\n");
        str += "\nPieces: " + pieces1 + " " + pieces2 + ", "
                + "Progresses: " + progress1 + " " + progress2 + ", "
                + "nMoves = " + nMoves + "\n";
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
    public boolean noMovesIsDraw() {
        // TODO I think a draw is possible, if the only two pieces left are facing eachother
        return false;
    }
}
