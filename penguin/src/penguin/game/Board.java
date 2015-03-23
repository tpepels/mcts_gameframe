package penguin.game;

import ai.MCTSOptions;
import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.FastTanh;
import framework.util.StatCounter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class Board implements IBoard {
    private static final MoveList tempList = new MoveList(3);   // Temp move store for heuristic evaluation
    private static final ArrayList<IMove> poMoves = new ArrayList<IMove>(5000);
    private static final MoveList static_moves = new MoveList(5000);   // 64*6

    //
    public int[][] board;
    public int nMoves, winner, curPlayer;

    private static int[] rowSizes = {8, 7, 8, 7, 8, 7, 8, 7};
    private int score1, score2;
    private int placed1, placed2;
    private int passes1, passes2;
    private int floes1, floes2;

    private Stack<IMove> pastMoves;

    @Override
    public IBoard copy() {
        Board b = new Board();

        b.board = new int[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < rowSizes[r]; c++)
                b.board[r][c] = this.board[r][c];

        b.score1 = this.score1;
        b.score2 = this.score2;
        b.placed1 = this.placed1;
        b.placed2 = this.placed2;
        b.passes1 = this.passes1;
        b.passes2 = this.passes2;
        b.floes1 = this.floes1;
        b.floes2 = this.floes2;

        b.curPlayer = this.curPlayer;
        b.nMoves = this.nMoves;
        b.winner = this.winner;

        // no need to copy the move stack, but need to initialize it
        b.pastMoves = new Stack<IMove>();

        return b;
    }


    @Override
    public boolean doAIMove(IMove move, int player) {
        int type = move.getType();

        // do stuff

        if (type == Move.PASS) {
            // don't modify the board, but check if all playes have passed

            if (curPlayer == 1)
                passes1++;
            else if (curPlayer == 2)
                passes2++;

            // now, check if all players passed. if so: we're done!
            if (passes1 > 0 && passes2 > 0) {
                // give endgame points
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < rowSizes[r]; c++) {
                        if (board[r][c] >= 100) {
                            int pl = board[r][c] / 100;
                            int points = board[r][c] % 100;

                            if (pl == 1) {
                                score1 += points;
                                floes1++;
                            } else {
                                score2 += points;
                                floes2++;
                            }
                        }
                    }
                }

                // check score
                if (score1 > score2)
                    winner = 1;
                else if (score2 > score1)
                    winner = 2;
                else if (score1 == score2) {
                    // tiebreaker?

                    if (floes1 > floes2)
                        winner = 1;
                    else if (floes2 > floes1)
                        winner = 2;
                    else
                        winner = DRAW;
                }
            }
        } else if (type == Move.PLACE) {
            int[] movearr = move.getMove();
            int r = movearr[0], c = movearr[1];

            board[r][c] += (player * 100);

            if (player == 1)
                placed1++;
            else
                placed2++;
        } else if (type == Move.MOVE) {
            int[] movearr = move.getMove();
            int scoreInc = ((Move) move).getScoreInc();
            int r = movearr[0], c = movearr[1], rp = movearr[2], cp = movearr[3];

            if (player == 1) {
                score1 += scoreInc;
                floes1++;
            } else if (player == 2) {
                score2 += scoreInc;
                floes2++;
            }

            board[r][c] = 0;
            board[rp][cp] += (player * 100);
        }


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
        int type = move.getType();

        // undo stuff
        if (type == Move.PASS) {
            // now, check if all players had passed, if so must undo endgame points

            if (passes1 > 0 && passes2 > 0) {
                // undo endgame points
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < rowSizes[r]; c++) {
                        if (board[r][c] >= 100) {
                            int pl = board[r][c] / 100;
                            int points = board[r][c] % 100;

                            if (pl == 1) {
                                score1 -= points;
                                floes1--;
                            } else {
                                score2 -= points;
                                floes2--;
                            }
                        }
                    }
                }

                // winner reset below  
            }

            if (curPlayer == 1)
                passes1--;
            else if (curPlayer == 2)
                passes2--;
        } else if (type == Move.PLACE) {
            int[] movearr = move.getMove();
            int r = movearr[0], c = movearr[1];

            if (curPlayer == 1)
                placed1--;
            else
                placed2--;

            board[r][c] -= (curPlayer * 100);
        } else if (type == Move.MOVE) {
            int[] movearr = move.getMove();
            int scoreInc = ((Move) move).getScoreInc();
            int r = movearr[0], c = movearr[1], rp = movearr[2], cp = movearr[3];

            board[rp][cp] -= (curPlayer * 100);
            board[r][c] = (curPlayer * 100 + scoreInc);

            if (curPlayer == 1) {
                score1 -= scoreInc;
                floes1--;
            } else if (curPlayer == 2) {
                score2 -= scoreInc;
                floes2--;
            }

        }

        // remove the win, if there was one
        winner = NONE_WIN;
    }


    @Override
    public MoveList getExpandMoves() {
        static_moves.clear();

        //System.out.println("HERE + " + curPlayer + " " + placed1 + " " + placed2);

        // add all moves using static_moves.add(new Move(...))
        if ((curPlayer == 1 && placed1 != 4) || (curPlayer == 2 && placed2 != 4)) {
            // player not done placing; search for all 1-fish floes            

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < rowSizes[r]; c++) {
                    // can only place on 1-fish floes
                    if (board[r][c] == 1)
                        static_moves.add(new Move(r, c, 0, 0, Move.PLACE, 0));
                }
            }
        } else {
            // enumerate all moves
            int base = curPlayer * 100;
            boolean nomoves = true;

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < rowSizes[r]; c++) {
                    // check for a piece belonging to curPlayer (e.g for player 1: 101, 102, 103)
                    if (board[r][c] >= base && board[r][c] <= base + 3) {
                        // now enumerate all the moves for this guy

                        for (int dir = 1; dir <= 6; dir++) {
                            int rp = r;
                            int cp = c;

                            while (true) {
                                int newRow = nextRow(rp, cp, dir);
                                int newCol = nextCol(rp, cp, dir);
                                rp = newRow;
                                cp = newCol;

                                if (!inBounds(rp, cp) || isBlocked(rp, cp))
                                    break;

                                static_moves.add(new Move(r, c, rp, cp, Move.MOVE, board[r][c] % 100));
                                nomoves = false;
                            }
                        }
                    }
                }
            }

            if (nomoves)
                static_moves.add(new Move(0, 0, 0, 0, Move.PASS, 0));
        }

        return static_moves.copy();
    }

    private int nextRow(int r, int c, int dir) {
        switch (dir) {
            case 1:
                return r - 1;  // northeast
            case 2:
                return r;    // east
            case 3:
                return r + 1;  // southeast
            case 4:
                return r + 1;  // southwest
            case 5:
                return r;    // west
            case 6:
                return r - 1;  // northwest
        }

        throw new RuntimeException("bad dir in nextRow");
    }

    private int nextCol(int r, int c, int dir) {
        switch (dir) {
            case 1:
                return (r % 2 == 1 ? (c + 1) : (c));   // northeast
            case 2:
                return c + 1;                          // east
            case 3:
                return (r % 2 == 1 ? (c + 1) : (c));   // southeast
            case 4:
                return (r % 2 == 1 ? (c) : (c - 1));   // southwest
            case 5:
                return c - 1;                          // west
            case 6:
                return (r % 2 == 1 ? (c) : (c - 1));   // northwest
        }

        throw new RuntimeException("bad dir in nextCol");
    }

    private boolean isBlocked(int r, int c) {
        // if the ice floe is gone or there's another penguin
        return (board[r][c] == 0 || board[r][c] >= 100);
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        //ArrayList<IMove> forced = new ArrayList<IMove>(); 
        //ArrayList<IMove> forced = null;

        // at the moment, this is the same as getExpandMoves

        poMoves.clear();

        // add all moves using static_moves.add(new Move(...))
        if ((curPlayer == 1 && placed1 != 4) || (curPlayer == 2 && placed2 != 4)) {
            // player not done placing; search for all 1-fish floes

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < rowSizes[r]; c++) {
                    // can only place on 1-fish floes
                    if (board[r][c] == 1)
                        poMoves.add(new Move(r, c, 0, 0, Move.PLACE, 0));
                }
            }
        } else {
            // enumerate all moves
            int base = curPlayer * 100;
            boolean nomoves = true;

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < rowSizes[r]; c++) {
                    // check for a piece belonging to curPlayer (e.g for player 1: 101, 102, 103)
                    if (board[r][c] >= base && board[r][c] <= base + 3) {
                        // now enumerate all the moves for this guy

                        for (int dir = 1; dir <= 6; dir++) {
                            int rp = r;
                            int cp = c;

                            while (true) {
                                int newRow = nextRow(rp, cp, dir);
                                int newCol = nextCol(rp, cp, dir);
                                rp = newRow;
                                cp = newCol;

                                if (!inBounds(rp, cp) || isBlocked(rp, cp))
                                    break;

                                poMoves.add(new Move(r, c, rp, cp, Move.MOVE, board[r][c] % 100));
                                nomoves = false;
                            }
                        }
                    }
                }
            }

            if (nomoves)
                poMoves.add(new Move(0, 0, 0, 0, Move.PASS, 0));
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
        board = new int[8][8];
        score1 = score2 = 0;
        placed1 = placed2 = 0;
        passes1 = passes2 = 0;
        floes1 = floes2 = 0;

        // initial placement:
        //
        // 60 Hexagon Ice Floes
        //   30 one fish ice floes
        //   20 two fish ice floes
        //   10 three fish ice floes
        ArrayList<Integer> floes = new ArrayList<Integer>();

        for (int i = 0; i < 30; i++)
            floes.add(1);

        for (int i = 0; i < 20; i++)
            floes.add(2);

        for (int i = 0; i < 10; i++)
            floes.add(3);

        int row = 0;
        int col = 0;

        while (floes.size() > 0) {
            int idx = MCTSOptions.r.nextInt(floes.size());
            int num = floes.remove(idx);

            board[row][col] = num;

            col++;
            if (col == rowSizes[row]) {
                row++;
                col = 0;
            }
        }

        curPlayer = 1;
        nMoves = 0;
        winner = NONE_WIN;
        pastMoves = new Stack<IMove>();
    }

    class Coord {
        int r, c;

        Coord(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }

    private int floodDiff(int player) {
        int[][] bcopy = new int[8][8];

        LinkedList<Coord> p1cur = new LinkedList<Coord>();
        LinkedList<Coord> p1next = new LinkedList<Coord>();
        LinkedList<Coord> p2cur = new LinkedList<Coord>();
        LinkedList<Coord> p2next = new LinkedList<Coord>();

        int p1area = 0;
        int p2area = 0;

        for (int r = 0; r < 8; r++)
            for (int c = 0; c < rowSizes[r]; c++) {
                bcopy[r][c] = board[r][c];

                if (board[r][c] >= 100 && board[r][c] <= 103) {
                    p1cur.add(new Coord(r, c));
                    p1area += board[r][c] % 100;
                } else if (board[r][c] >= 200 && board[r][c] <= 203) {
                    p2cur.add(new Coord(r, c));
                    p2area += board[r][c] % 100;
                }
            }

        int curplayer = player;
        boolean keepGoing = true;

        while (p1cur.size() > 0 && p2cur.size() > 0) {

            LinkedList<Coord> cur = (curplayer == 1 ? p1cur : p2cur);
            LinkedList<Coord> next = (curplayer == 1 ? p1next : p2next);

            while (cur.size() > 0) {
                Coord coord = cur.removeFirst();
                int r = coord.r;
                int c = coord.c;

                for (int dir = 1; dir <= 6; dir++) {
                    int rp = nextRow(r, c, dir);
                    int cp = nextCol(r, c, dir);

                    if (inBounds(rp, cp) && bcopy[rp][cp] >= 1 && bcopy[rp][cp] <= 3) {
                        if (curplayer == 1)
                            p1area += bcopy[rp][cp];
                        else
                            p2area += bcopy[rp][cp];

                        bcopy[rp][cp] = curplayer * 100;
                        next.add(new Coord(rp, cp));
                    }
                }
            }

            if (curplayer == 1) {
                LinkedList<Coord> tmp = p1cur;
                p1cur = p1next;
                p1next = tmp;
                p1next.clear();
            } else {
                LinkedList<Coord> tmp = p2cur;
                p2cur = p2next;
                p2next = tmp;
                p2next.clear();
            }

            curplayer = 3 - curplayer;
        }

        return (p1area - p2area);
    }

    @Override
    public double evaluate(int player, int version) {
        // points-based; this is actually a bad ev. func for this game

        double p1eval = 0;
        if (winner == 1) p1eval = 1;
        else if (winner == 2) p1eval = -1;
        else if (winner == DRAW) p1eval = 0;
        else {
            //double delta = (pieces1 * 10 + progress1 * 2.5 + capBonus1) - (pieces2 * 10 + progress2 * 2.5 + capBonus2);
            int augscore1 = score1;
            int augscore2 = score2;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < rowSizes[r]; c++) {
                    if (board[r][c] >= 101 && board[r][c] <= 103)
                        augscore1 += board[r][c] % 100;
                    else if (board[r][c] >= 201 && board[r][c] <= 203)
                        augscore2 += board[r][c] % 100;
                }
            }

            double sdelta = (augscore1 * 10) - (augscore2 * 10);
            double fdelta = floodDiff(curPlayer) * 10;
            double delta = sdelta + fdelta;
            //if (delta < -100) delta = -100;
            //if (delta > 100) delta = 100;
            // now pass it through tanh;
            p1eval = FastTanh.tanh(delta / 60.0);
        }
        return (player == 1 ? p1eval : -p1eval);
    }

    @Override
    public void initNodePriors(int parentPlayer, StatCounter stats, IMove move, int npvisits) {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public double getQuality() {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public MoveList getOrderedMoves() {
        throw new RuntimeException("unimplemented");
        //static_moves.clear();
        //return static_moves.copy();
    }

    @Override
    public long hash() {
        return 0;
    }

    public String toString() {
        String str = "Scores: " + score1 + " " + score2 +
                ", Floes: " + floes1 + " " + floes2 + "\n\n";

        for (int r = 0; r < 8; r++) {
            if (r % 2 == 1)
                str += "  ";

            for (int c = 0; c < rowSizes[r]; c++) {

                if (board[r][c] == 0) {
                    str += "    ";
                    continue;
                }

                String pl = " ";
                if ((board[r][c] / 100) == 1)
                    pl = "r";
                else if ((board[r][c] / 100) == 2)
                    pl = "g";

                str += (pl + (board[r][c] % 100) + "  ");
            }

            str += "\n";
        }

        return str;
    }

    private boolean inBounds(int r, int c) {
        return (r >= 0 && c >= 0 && r < 8 && c < rowSizes[r]);
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
