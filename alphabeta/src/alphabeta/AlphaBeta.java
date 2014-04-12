package alphabeta;

import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveCallback;
import ai.framework.MoveList;
import alphabeta.Transposition;

// need this for now to implement AIPlayer. should eventually rename to AIOptions
import ai.mcts.MCTSOptions; 

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

public class AlphaBeta implements AIPlayer {

    private final double N_INF = -2000000, P_INF = 2000000;
    // The base win value, decreased with D_DECR per depth
    private final double WIN_VAL = 1000000, D_DECR = 5000;
    // Some contant values (TT_size = 2^25)
    private final int TT_SIZE = 33554432, TIME_CHECK_INT = 1000, BASE_TIME = 15000;
    private final DecimalFormat decForm = new DecimalFormat("#,###,###,###,##0");
    //
    double DELTA = 60, DEFAULT_DELTA = 60; 
    public int R = 2, MAX_DEPTH = 1000;
    public boolean nullmoves = false, historyHeuristic = false,
            killermoves = false, aspiration = false;
    boolean interupted = false;
    int[] captures = new int[2];

    //
    private Random r = new Random();
    //private MoveCallback callback;
    private IBoard initBoard;
    private IMove bestMove, prevBestMove;
    private IMove finalBestMove;

    // Statistics
    private double totalNodes;
    private double totalDepth, numMoves, researches;
    // Counters etc.
    private int maxDepth, nodes, collisions, timeCheck, myPlayer, opponent, //bestMove, prevBestMove,
            tt_lookups;
    private long endTime;
    private boolean forceHalt = false, parallel = true;
    // Transposition table, history, killer moves, butterfly board
    private Transposition[] tt;
    private int[][] history, bfboard;
    private int[][] killermove;
    private Thread t;
    private AlphaBetaOptions options;

    public AlphaBeta() {
        // Assuming we never go deeper than the size of the board.
        //history = new int[2][Board.SIZE];
        //bfboard = new int[2][Board.SIZE];
    }

    public void newGame(int myPlayer, String game) {
    }

    private long MASK = TT_SIZE - 1;



    private int getHashPos(long hash) {
        return (int) (hash & MASK);
    }

    private void resetTT() {
        for (int i = 0; i < TT_SIZE; i++)
            tt = null;
    }

    public void resetStats() {
        totalDepth = 0;
        numMoves = 0;
        totalNodes = 0;
        researches = 0;
        // Assuming we never go deeper than the size of the board.
        //history = new int[2][Board.SIZE];
        //bfboard = new int[2][Board.SIZE];
    }

    public double averageDepth() {
        return totalDepth / numMoves;
    }

    public double totalnodes() {
        return totalNodes;
    }

    public double averageNodes() {
        return totalNodes / numMoves;
    }

    public double averageResearches() {
        return researches / numMoves;
    }

    public IMove getBestMove() {
        //return new Move(bestMove);
        return finalBestMove;
    }

    public void stop() {
        if (t != null) {
            interupted = true;
        }
    }

    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel, IMove lastMove) { 
        this.initBoard = board;
        //this.callback = callback;
        this.myPlayer = myPlayer;
        this.parallel = parallel;
        
        //this.opponent = (myPlayer == Board.P2) ? Board.P1 : Board.P2;
        this.opponent = 3-this.opponent;

        //
        interupted = false;
        collisions = 0;
        nodes = 0;
        finalBestMove = null;
        bestMove = null;
        maxDepth = 0;
        tt_lookups = 0;
        timeCheck = TIME_CHECK_INT;
        forceHalt = false;
        double prevVal = 0;

        if (options.transpositions && tt == null)
            tt = new Transposition[TT_SIZE];

        // endTime = 15000; // for testing
        endTime =  System.currentTimeMillis() + options.timeLimit;

        //
        // long lastItStartTime = 0, lastItTime = 0;
        double val = 0, alpha = N_INF, beta = P_INF;
        boolean wonlost = false;

        while (maxDepth < MAX_DEPTH && !forceHalt && !interupted) {
            maxDepth += 1;
            if (options.debugInfoAB)
                System.out.println(":: Max depth: " + maxDepth);
            prevBestMove = bestMove;
            prevVal = val;
            // lastItStartTime = System.currentTimeMillis();
            //
            val = alphaBeta(initBoard, maxDepth, alpha, beta, myPlayer, null, false);
            if (aspiration) {
                if (val >= beta) {
                    System.out.println(":: Re-search required, val(" + val + ") >= beta.");
                    researches++;
                    alpha = val;
                    beta = P_INF;
                    val = alphaBeta(initBoard, maxDepth, alpha, beta, myPlayer, null, false);
                } else if (val <= alpha) {
                    researches++;
                    System.out.println(":: Re-search required, val(" + val + ") <= alpha.");
                    alpha = N_INF;
                    beta = val;
                    val = alphaBeta(initBoard, maxDepth, alpha, beta, myPlayer, null, false);
                }
                DELTA = Math.max(Math.abs(val / 2) - 1, DEFAULT_DELTA);
                //
                alpha = val - DELTA;
                beta = val + DELTA;
            }
            //
            if (options.debugInfoAB) {
                System.out.println(" - Best value so far: " + val);
                System.out.println(" - Best move so far: " + bestMove);
                System.out.println(" - Nodes visited: " + decForm.format(nodes));
            }
            // We win/lose
            /*if (Math.abs(val) > FW1_VAL) {
                wonlost = true;
                break;
            }*/
        }
        // We can still use the current val if the result is better in vase of forced halt
        if (forceHalt || interupted) {
            bestMove = prevBestMove;
            val = prevVal;
            maxDepth--;
        }
        //
        if (!wonlost) {
            numMoves++;
            totalNodes += nodes;
            totalDepth += maxDepth;
        }

        finalBestMove = bestMove;

        //
        if (options.debugInfoMove) {
            System.out.println(" - MaxDepth: " + maxDepth);
            System.out.println(" - Final best value: " + val);
            System.out.println(" - Final best move : " + finalBestMove);
            System.out.println(" - Nodes visited: " + decForm.format(nodes));

            System.out.println(":: Forced halt: " + forceHalt);
            System.out.println(":: TT Lookups: " + decForm.format(tt_lookups));
            System.out.println(":: Collisions: " + decForm.format(collisions));
            System.out.println(":: Nodes visited: " + decForm.format(nodes));
            System.out.println("--------------------------------");
        }
        // Free the transposition table for the gc.
        //tt = null;
        if (options.transpositions)
            resetTT();

        if (!interupted && parallel)
            callback.makeMove(finalBestMove);
    }
  
    private int getOpponent(int player) {
        /*if (player == Board.P1)
            return Board.P2;
        else
            return Board.P1;*/
        return 3-player;
    }

    private double alphaBeta(IBoard board, int depth, double alpha, double beta, int player, IMove move,
                          boolean nullMove) {
        if (forceHalt || interupted)
            return 0;
        if (timeCheck == 0) {
            // Check if still time left.
            if (System.currentTimeMillis() >= endTime) {
                forceHalt = true;
                return 0;
            }
            timeCheck = TIME_CHECK_INT;
        }
        timeCheck--;
        nodes++;
        // For win/loss depth
        int inv_depth = maxDepth - depth; 
        double value = N_INF, bestValue = N_INF;
        double capsw, capsb, olda = alpha, oldb = beta;
        int  hashPos = 0, color = (player == myPlayer) ? 1 : -1;
        IMove plyBestMove = null;
        boolean valuefound = false, collision = false;
        int curBestMoveIndex = -1;
        IMove curBestMove = null;

        //int[] currentMoves;
        MoveList currentMoves;
        int tpBestMoveIndex = -1;
        //
        Transposition tp = null;
        if (options.transpositions) {
            hashPos = getHashPos(board.hash());
            tp = tt[hashPos];
            // Check if present in transposition table
            if (tp != null) {
                tt_lookups++;
                // Position was evaluated previously
                // Check for a collision
                if (tp.hash != board.hash()) {
                    collisions++;
                    collision = true;
                }
                else if (depth <= tp.depth) {
                    if (tp.flag == Transposition.REAL)
                        return tp.value;
                    if (tp.flag == Transposition.L_BOUND && tp.value > alpha)
                        alpha = tp.value;
                    else if (tp.flag == Transposition.U_BOUND && tp.value < beta)
                        beta = tp.value;
                    if (alpha >= beta)
                        return tp.value;
                }

                tpBestMoveIndex = tp.bestMoveIndex;
            }
        }
        // Check if position is terminal.
        if (move != null) {
            int winstate = board.checkWin();
            if (winstate != IBoard.NONE_WIN) {
                if (winstate == player) {
                    // Prefer shallow wins!
                    bestValue = (WIN_VAL - (D_DECR * inv_depth));
                    valuefound = true;
                } else if (winstate == IBoard.DRAW) {
                    return 0;
                } else {
                    // Deeper losses are "less worse" :) than shallow losses
                    bestValue = -(WIN_VAL - (D_DECR * inv_depth));
                    return bestValue;
                }
            }
        }
        // Leaf-node, evaluate the node
        if (depth == 0 && !valuefound) {
            bestValue = color * board.evaluate(myPlayer, options.evVer);
            valuefound = true;
        } else if (!valuefound) {
            /*
            if (tp == null || collision) {
                currentMoves = board.getAvailableMoves(killermove[inv_depth]);
            } else {
                currentMoves = board.getAvailableMoves(killermove[inv_depth][0],
                        killermove[inv_depth][1], tp.bestMove);
            }*/
            // get the moves
            //currentMoves = board.getExpandMoves();
            currentMoves = board.getOrderedMoves();
            // 
            //int startindex = board.startindex, currentmove;
            IMove currentmove = null;

            double maxHistVal = 1.;
            for (int i = 0; i < currentMoves.size(); i++) {
                // Try the killer and transposition moves first, then try the hh moves
                //if (i >= startindex && maxHistVal > 0. && historyHeuristic) {
                //    board.getNextMove(history[player - 1], bfboard[player - 1], currentMoves, i);
                //    // If the previous max history value was 0, we can just follow the indexed list
                //    maxHistVal = board.maxHistVal;
                // }

                // take the move suggestes by the transpos table first -- did not seem to work
                // must be interacting with the other stuff somehow
                //if (!tookTPmove && tpBestMoveIndex > 0 && tpBestMoveIndex < currentMoves.size()) {
                //    currentmove = currentMoves.get(tpBestMoveIndex);
                //    tookTPmove = true;
                //}
                //else if (tookTPmove && i == tpBestMoveIndex)
                //    continue;
                //else
                //    currentmove = currentMoves.get(i);
                currentmove = currentMoves.get(i);

                if (board.doAIMove(currentmove, player)) {
                    // Returns false if suicide
                    //
                    //
                    value = -alphaBeta(board, depth - 1, -beta, -alpha, getOpponent(player),
                            currentmove, nullMove);
                    //
                    if (value > bestValue) {
                        // for detemining the move to return
                        if (depth == maxDepth && value > bestValue) {
                            bestMove = currentmove;
                        }
                        //
                        curBestMove = currentmove;
                        curBestMoveIndex = i;
                        bestValue = value;
                        plyBestMove = currentmove;
                    }
                    //
                    alpha = Math.max(alpha, bestValue);
                    board.undoMove();
                    // Update the butterfly board for the relative history heuristic
                    //bfboard[player - 1][currentmove]++;

                    if (alpha >= beta) {
                        /*if (killermoves && currentmove != killermove[inv_depth][0]) {
                            killermove[inv_depth][1] = killermove[inv_depth][0];
                            killermove[inv_depth][0] = currentmove;
                        }*/
                        break;
                    }
                } else {
                    System.err.println("error making move!");
                }
            }

        }
        // Update the history useHeuristics for move-ordering
        //if (plyBestMove > -1)
        //    history[player - 1][plyBestMove]++;
        // Replace if deeper or doesn't exist
        if (options.transpositions && (tp == null || (collision && depth > tp.depth))) {
            tp = new Transposition();
            tt[hashPos] = tp;
            tp.bestMove = plyBestMove;
            tp.depth = depth;
            tp.hash = board.hash();
            //
            if (bestValue <= olda) {
                tp.flag = Transposition.U_BOUND;
            } else if (bestValue >= oldb) {
                tp.flag = Transposition.L_BOUND;
            } else {
                tp.flag = Transposition.REAL;
            }
            tp.value = bestValue;

            tp.bestMove = curBestMove;
            tp.bestMoveIndex = curBestMoveIndex;
        }
        return bestValue;
    }

    //private int evaluate(Board board, int inv_depth) {
    // use the one from board

    @Override
    public void setOptions(MCTSOptions options) {
        // TODO Auto-generated method stub
        this.options = (AlphaBetaOptions)options;
    }


}
