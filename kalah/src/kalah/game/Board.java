package kalah.game;

import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.FastTanh;
import framework.util.StatCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Board implements IBoard {

    /*
    private static final int N_PIECES = 16;
    private static final MoveList tempList = new MoveList(3);   // Temp move store for heuristic evaluation
    //
    private int pieces1, pieces2;
    private int progress1, progress2;
    */

    /**
     * Player 2
     * 11 10 9  8  7  6
     * S2                  S1
     * 0  1  2  3  4  5
     * Player 1
     */

    // How many holes per side
    protected static final int N_HOUSES = 6;

    // How many starting pieces per hole
    protected static final int N_SPIECES = 4;

    public final int[] oppHouse1 = {11, 10, 9, 8, 7, 6};
    public final int[] oppHouse2 = {0, 0, 0, 0, 0, 0, 5, 4, 3, 2, 1, 0};

    private static final ArrayList<IMove> poMoves = new ArrayList<IMove>(10);
    private static final MoveList static_moves = new MoveList(10);

    public static long[] zbnums = null;
    public long zbHash = 0;

    private Stack<IMove> pastMoves;
    public int nMoves, winner, curPlayer;
    public int[] board;
    public int store1, store2;

    private int getZbId(int house) {
        int maxStones = N_HOUSES * 2 * N_SPIECES;
        int id = maxStones * house;

        if (house == (N_HOUSES * 2))
            id += store1;
        else if (house == (N_HOUSES * 2) + 1)
            id += store2;
        else
            id += board[house];

        return id;
    }

    @Override
    public long hash() {
        long zbHash = 0;
        for (int i = 0; i < (N_HOUSES * 2 + 2); i++)
            zbHash ^= zbnums[getZbId(i)];
        return zbHash;
    }

    @Override
    public boolean poMoves() {
        return false;
    }

    @Override
    public int getNPlayers() {
        return 2;
    }

    public void initialize(int setboard[]) {
        board = new int[N_HOUSES * 2];
        store1 = 0;
        store2 = 0;

        for (int i = 0; i < N_HOUSES * 2; i++)
            board[i] = setboard[i];

        pastMoves = new Stack<IMove>();
        nMoves = 0;
        winner = NONE_WIN;
        curPlayer = 1;

        if (zbnums == null) {
            Random rng = new Random();
            // max number per spot = N_HOUSES*2*N_SPIECES
            int maxStones = N_HOUSES * 2 * N_SPIECES;
            zbnums = new long[maxStones * (N_HOUSES * 2 + 2)];
            for (int i = 0; i < maxStones * (N_HOUSES * 2 + 2); i++)
                zbnums[i] = rng.nextLong();
        }
    }

    @Override
    public void initialize() {

        board = new int[N_HOUSES * 2];
        store1 = 0;
        store2 = 0;

        for (int i = 0; i < N_HOUSES * 2; i++)
            board[i] = N_SPIECES;

        pastMoves = new Stack<IMove>();
        nMoves = 0;
        winner = NONE_WIN;
        curPlayer = 1;

    }

    @Override
    public IBoard copy() {

        Board b = new Board();

        b.board = new int[N_HOUSES * 2];
        for (int i = 0; i < 2 * N_HOUSES; i++)
            b.board[i] = this.board[i];

        b.store1 = this.store1;
        b.store2 = this.store2;

        b.nMoves = this.nMoves;
        b.winner = this.winner;
        b.curPlayer = this.curPlayer;

        // no need to copy the move stack, but need to initialize it
        b.pastMoves = new Stack<IMove>();

        return b;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        /*
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
            } else if (player == 2) {
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
        */

        int[] values = move.getMove();
        int house = values[0];
        int sow = values[1];
        int numCaptured = values[2];

        //System.out.println("START OF DOAIMOVE for move: " + move + ", current board: "); 
        //System.out.println(toString()); 

        if (values[3] != player)
            throw new RuntimeException("player mismatch");

        if (player != curPlayer)
            throw new RuntimeException("player mismatch");


        if (board[house] != sow)
            throw new RuntimeException("board[house] != sow, house = " + house + ", sow = " + sow);

        // take them out
        board[house] -= sow;

        boolean justPlayedStoreMove = false;
        boolean extraTurn = false;

        // distribute them
        while (sow > 0) {
            extraTurn = false;

            if (curPlayer == 1 && house == 5) {
                sow--;
                store1++;
                extraTurn = true;
                house = 6;
                justPlayedStoreMove = true;
            } else if (curPlayer == 2 && house == 11) {
                sow--;
                store2++;
                extraTurn = true;
                house = 0;
                justPlayedStoreMove = true;
            } else {
                if (!justPlayedStoreMove) {
                    house++;
                    if (house >= 12)
                        house = 0;
                }

                justPlayedStoreMove = false;

                sow--;
                board[house]++;
            }
        }

        int nextPlayer = -1;

        // check move type
        if (move.getType() == Move.CAPTURE) {
            int opphouse = (player == 1 ? oppHouse1[house] : oppHouse2[house]);

            if (numCaptured != board[opphouse])
                throw new RuntimeException("capturedPieces != board[opphouse], move: " +
                        move + ", houses = " + house + " " + opphouse + ", board[opphouse] = " + board[opphouse]);
            else if (board[house] != 1)
                throw new RuntimeException("board[house] != 1, move: " + move);


            int totalPieceGain = 1 + numCaptured;
            board[house] = 0;
            board[opphouse] = 0;

            if (curPlayer == 1)
                store1 += totalPieceGain;
            else
                store2 += totalPieceGain;

            if (extraTurn)
                throw new RuntimeException("extra turn in capture");

            // change the player
            nextPlayer = 3 - curPlayer;
        } else if (move.getType() == Move.STORE) {

            if (numCaptured != 0)
                throw new RuntimeException("num captures != 0 in store move");
            if (!extraTurn)
                throw new RuntimeException("no extra turn in store move");

            nextPlayer = curPlayer;
        } else {
            if (extraTurn)
                throw new RuntimeException("extra turn in capture");

            nextPlayer = 3 - curPlayer;
        }

        // check win
        int sumRow1 = 0;
        int sumRow2 = 0;
        for (int h = 0; h < 6; h++)
            sumRow1 += board[h];
        for (int h = 6; h < 11; h++)
            sumRow2 += board[h];

        if (sumRow1 == 0) {
            int finalstore2 = store2 + sumRow2;

            if (store1 > finalstore2)
                winner = 1;
            else if (finalstore2 > store1)
                winner = 2;
            else
                winner = DRAW;
        } else if (sumRow2 == 0) {
            int finalstore1 = store1 + sumRow1;

            if (finalstore1 > store2)
                winner = 1;
            else if (store2 > finalstore1)
                winner = 2;
            else
                winner = DRAW;
        } else {
            winner = NONE_WIN;
        }

        nMoves++;
        pastMoves.push(move);
        curPlayer = nextPlayer;

        //System.out.println("END OF DOAIMOVE for move: " + move + ", current board: "); 
        //System.out.println(toString()); 

        // check no negatives and magic pieces
        int totalPieces = 0;

        for (int i = 0; i < 6; i++) {
            if (board[i] < 0)
                throw new RuntimeException("board[" + i + "] = " + board[i]);
            if (board[6 + i] < 0)
                throw new RuntimeException("board[" + (6 + i) + "] = " + board[6 + i]);

            totalPieces += board[i];
            totalPieces += board[6 + i];
        }

        if (totalPieces + store1 + store2 != (4 * 12))
            throw new RuntimeException("Total piece count!");

        return true;
    }

    @Override
    public void undoMove() {
        /*
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
        */

        Move move = (Move) pastMoves.pop();
        nMoves--;

        //System.out.println("START OF UNDO for move: " + move + ", current board: "); 
        //System.out.println(toString()); 

        int[] values = move.getMove();
        int startHouse = values[0];
        int sow = values[1];
        int numCaptured = values[2];
        int player = values[3];

        curPlayer = player;

        int landingPos = -1;

        if (player == 1) {
            landingPos = (startHouse + sow) % 13;
            if (landingPos >= 7) landingPos--;
        } else if (player == 2) {
            landingPos = (startHouse + sow) % 13;
        }

        int house = landingPos;

        // check for a capture
        if (move.getType() == Move.CAPTURE) {
            int opphouse = (player == 1 ? oppHouse1[house] : oppHouse2[house]);

            if (board[house] != 0)
                throw new RuntimeException("board[house] != 0 in undo");
            if (board[opphouse] != 0)
                throw new RuntimeException("board[opphouse] != 0 in undo");

            board[opphouse] = numCaptured;

            if (player == 1)
                store1 -= (numCaptured + 1);
            else
                store2 -= (numCaptured + 1);
        }

        int sowCounter = 0;

        // check for store move
        if (move.getType() == Move.STORE) {
            if (player == 1) {
                store1--;
                house = 5;
                sowCounter = 1;
            } else if (player == 2) {
                store2--;
                house = 11;
                sowCounter = 1;
            }
        }

        // re-distribute them back onto the board
        while (sowCounter < sow) {

            boolean remove = true;

            //System.out.println("putting back sow, house = " + house); 
            if (move.getType() == Move.CAPTURE && sowCounter == 0) {
                // keep this spot at 0, but still increase the sow counter
                remove = false;
            }

            boolean storeMove = false;

            if (player == 1 && house == 6) {
                sowCounter++;
                if (remove)
                    board[house]--;
                storeMove = true;
            } else if (player == 2 && house == 0) {
                sowCounter++;
                if (remove)
                    board[house]--;
                storeMove = true;
            }

            if (storeMove) {
                if (sowCounter >= sow)
                    throw new RuntimeException("under sowCounter >= sow after storeMove");

                if (player == 1) {
                    sowCounter++;
                    store1--;
                    house = 5;
                } else if (player == 2) {
                    sowCounter++;
                    store2--;
                    house = 11;
                }
            } else {
                sowCounter++;
                if (remove)
                    board[house]--;

                house--;
                if (house < 0)
                    house = 11;
            }
        }

        // might have done many full revolutions
        board[startHouse] += sowCounter;

        winner = NONE_WIN;

        //System.out.println("END OF UNDO for move: " + move + ", current board: "); 
        //System.out.println(toString()); 

        // check no negatives and magic pieces
        int totalPieces = 0;

        for (int i = 0; i < 6; i++) {
            if (board[i] < 0)
                throw new RuntimeException("board[" + i + "] = " + board[i]);
            if (board[6 + i] < 0)
                throw new RuntimeException("board[" + (6 + i) + "] = " + board[6 + i]);

            totalPieces += board[i];
            totalPieces += board[6 + i];
        }

        if (totalPieces + store1 + store2 != (4 * 12))
            throw new RuntimeException("Total piece count!");

    }

    @Override
    public MoveList getExpandMoves() {
        static_moves.clear();

        int startHouse = (curPlayer == 1 ? 0 : 6);
        int endHouse = (curPlayer == 1 ? 5 : 11);
        int store = endHouse + 1;

        /**
         *   From p1's point of view:
         *
         *      12 11 10  9  8  7 
         *                         6
         *       0  1  2  3  4  5 
         *
         *   From p2's point of view:
         *
         *         11 10  9  8  7  6
         *      12
         *          0  1  2  3  4  5
         */


        for (int house = startHouse; house <= endHouse; house++) {
            int sow = board[house];

            if (sow == 0)
                continue;

            if ((house + sow) % 13 == store) {
                static_moves.add(new Move(Move.STORE, house, sow, 0, curPlayer));
            } else if (curPlayer == 1) {

                int landingPos = (house + sow) % 13;
                if (landingPos >= 7) landingPos--;

                // check for capture
                if (landingPos >= startHouse && landingPos <= endHouse
                        && board[landingPos] == 0 && sow <= 13) {

                    int opphouse = oppHouse1[landingPos];
                    int piecesCaptured = board[opphouse];

                    // might be more if you wrapped around                    
                    if (sow >= 8) {
                        int fullrevs = sow / 13;
                        int halfrevs = (sow - fullrevs) >= 8 ? 1 : 0;
                        piecesCaptured += (fullrevs + halfrevs);
                    }

                    static_moves.add(new Move(Move.CAPTURE, house, sow, piecesCaptured, curPlayer));
                } else {
                    // regular move
                    static_moves.add(new Move(Move.MOVE, house, sow, 0, curPlayer));
                }
            } else if (curPlayer == 2) {
                int landingPos = (house + sow) % 13;

                // check for capture
                if (landingPos >= startHouse && landingPos <= endHouse
                        && board[landingPos] == 0 && sow <= 13) {

                    int opphouse = oppHouse2[landingPos];
                    int piecesCaptured = board[opphouse];

                    // might be more if you wrapped around                    
                    if (sow >= 8) {
                        int fullrevs = sow / 13;
                        int halfrevs = (sow - fullrevs) >= 8 ? 1 : 0;
                        piecesCaptured += (fullrevs + halfrevs);
                    }

                    static_moves.add(new Move(Move.CAPTURE, house, sow, piecesCaptured, curPlayer));
                } else {
                    // regular move
                    static_moves.add(new Move(Move.MOVE, house, sow, 0, curPlayer));
                }
            }
        }

        return static_moves.copy();
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {

        poMoves.clear();

        int startHouse = (curPlayer == 1 ? 0 : 6);
        int endHouse = (curPlayer == 1 ? 5 : 11);
        int store = endHouse + 1;

        /**
         *   From p1's point of view:
         *
         *      12 11 10  9  8  7 
         *                         6
         *       0  1  2  3  4  5 
         *
         *   From p2's point of view:
         *
         *         11 10  9  8  7  6
         *      12
         *          0  1  2  3  4  5
         */


        for (int house = startHouse; house <= endHouse; house++) {
            int sow = board[house];

            if (sow == 0)
                continue;

            if ((house + sow) % 13 == store) {
                poMoves.add(new Move(Move.STORE, house, sow, 0, curPlayer));
            } else if (curPlayer == 1) {

                int landingPos = (house + sow) % 13;
                if (landingPos >= 7) landingPos--;

                // check for capture
                if (landingPos >= startHouse && landingPos <= endHouse
                        && board[landingPos] == 0 && sow <= 13) {

                    int opphouse = oppHouse1[landingPos];
                    int piecesCaptured = board[opphouse];

                    // might be more if you wrapped around                    
                    if (sow >= 8) {
                        int fullrevs = sow / 13;
                        int halfrevs = (sow - fullrevs) >= 8 ? 1 : 0;
                        piecesCaptured += (fullrevs + halfrevs);
                    }

                    poMoves.add(new Move(Move.CAPTURE, house, sow, piecesCaptured, curPlayer));
                } else {
                    // regular move
                    poMoves.add(new Move(Move.MOVE, house, sow, 0, curPlayer));
                }
            } else if (curPlayer == 2) {
                int landingPos = (house + sow) % 13;

                // check for capture
                if (landingPos >= startHouse && landingPos <= endHouse
                        && board[landingPos] == 0 && sow <= 13) {

                    int opphouse = oppHouse2[landingPos];
                    int piecesCaptured = board[opphouse];

                    // might be more if you wrapped around                    
                    if (sow >= 8) {
                        int fullrevs = sow / 13;
                        int halfrevs = (sow - fullrevs) >= 8 ? 1 : 0;
                        piecesCaptured += (fullrevs + halfrevs);
                    }

                    poMoves.add(new Move(Move.CAPTURE, house, sow, piecesCaptured, curPlayer));
                } else {
                    // regular move
                    poMoves.add(new Move(Move.MOVE, house, sow, 0, curPlayer));
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

    public int getEndScore(int player) {
        int score = 0;
        if (player == 1) {
            score = store1;
            for (int i = 0; i < 6; i++)
                score += board[i];
        } else if (player == 2) {
            score = store2;
            for (int i = 0; i < 6; i++)
                score += board[6 + i];
        }
        return score;
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
        return 6;  // really, it's just 6
    }

    @Override
    public double evaluate(int player, int version) {
        // use the one from the ramanujan paper
        double score1 = store1;
        double score2 = store2; 

        /*for (int i = 0; i < 6; i++) { 
            score1 += board[i];
            score2 += board[6+i];
        }*/

        double diff = score1 - score2;

        //System.out.println("diff = " + diff);
        double p1eval = FastTanh.tanh(diff / 10.0);
        //double p1eval = diff;

        if (player == 1)
            return p1eval;
        else
            return -p1eval;
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

    }

    @Override
    public double getQuality() {
        
        /*
        if (winner == P1_WIN)
            return ((double) (pieces1 - pieces2)) / (double) (N_PIECES);
        else if (winner == P2_WIN)
            return ((double) (pieces2 - pieces1)) / (double) (N_PIECES);
        return 1;
        */
        return 0;
    }

    @Override
    public MoveList getOrderedMoves() {
        return getExpandMoves();
    }

    private String formatInt(int x) {
        if (x >= 10)
            return ("" + x);
        else
            return (" " + x);

    }

    public String toString() {
        String str = "\n";

        /**
         *          Player 2
         *      11 10 9  8  7  6
         *  S2                    S1
         *      0  1  2  3  4  5 
         *          Player 1
         */

        str += "Current player: " + curPlayer + ", nMoves = " + nMoves + "\n\n";

        str += "   ";

        for (int house = 11; house >= 6; house--)
            str += formatInt(board[house]) + " ";

        str += "\n";
        str += formatInt(store2);
        str += "                   ";
        str += formatInt(store1);
        str += "\n";

        str += "   ";

        for (int house = 0; house < 6; house++)
            str += formatInt(board[house]) + " ";

        str += "\n";

        return str;
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
