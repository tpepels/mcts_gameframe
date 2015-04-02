package experiments;

import ai.H_ISMCTS.HISMCTSPlayer;
import ai.ISMCTS.ISMCTSPlayer;
import ai.MCTSOptions;
import ai.mcts.MCTSPlayer;
import alphabeta.AlphaBeta;
import alphabeta.AlphaBetaOptions;
import framework.AIPlayer;
import framework.IBoard;
import framework.IMove;
import framework.util.*;
import mcts2e.BRUE.MCTS2ePlayer;
import mcts_tt.H_MCTS.HybridPlayer;
import mcts_tt.uct.UCTPlayer;

/**
 * Runs a single experiment. Options are sent by command-line.
 */

public class SimGame {

    private String game;
    private String p1label;
    private String p2label;
    private IBoard board;
    private IMove move;
    private AIPlayer player1;
    private AIPlayer player2;
    private MCTSOptions options1, options2;
    private int timeLimit;
    private long seed;
    private boolean printBoard;
    private boolean mctsDebug;
    // Fopr the time-based experiments in SHOT and H-MCTS
    private boolean timed = false;
    private int timedPlayer = 0;

    SimGame() {
        game = "none specified";
        p1label = "none specified";
        p2label = "none specified";
        player1 = null;
        player2 = null;
        timeLimit = 1000;
        printBoard = false;
        mctsDebug = false;

        seed = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SimGame sim = new SimGame();
        sim.parseArgs(args);
        sim.run();
    }

    public void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--game")) {
                i++;
                game = args[i];
            } else if (args[i].equals("--p1")) {
                i++;
                p1label = args[i];
            } else if (args[i].equals("--p2")) {
                i++;
                p2label = args[i];
            } else if (args[i].equals("--timelimit")) {
                i++;
                timeLimit = Integer.parseInt(args[i]);
            } else if (args[i].equals("--seed")) {
                i++;
                seed = Long.parseLong(args[i]);
                MCTSOptions.r.setSeed(seed);
            } else if (args[i].equals("--printboard")) {
                printBoard = true;
            } else if (args[i].equals("--mctsdebug")) {
                mctsDebug = true;
            } else {
                throw new RuntimeException("Unknown option: " + args[i]);
            }
        }
    }

    public void loadPlayer(int player, String label) {
        /**
         * Label of the player determines options. This is needed by the 
         * post-processing scripts. 
         *
         *  "mcts" is MCTSPlayer with default options
         *  For other options, add tags separated by underscores:
         *
         *  i.e. "mcts_pd4_imX" = mcts player, early playout enabled (pdepth 4), implicit minimax enabled
         *
         *  Index of tags (please keep alphabetically ordered):
         *
         *    _dfX   = depth frquency check for dynamic early terminations
         *    _detX  = dynamic eart terminations, with X = threshold value (a double)
         *    _efvX  = evaluation function version
         *    _egeX  = epsilon-greedy playouts using the eval func, where X is a double
         *    _h     = enable heuristics
         *    _imX   = enable implicit minimax
         *    _ip    = enable implicit pruning
         *    _mastX = Plays highest MAST move with probability X, X is double
         *    _mbpX  = Maximum backpropagation (X is int, trhreshold for when to switch)
         *    _npX   = Node priors using the evaluation function, X = number of initialized visits 
         *    _pbX   = progressive bias, weight = X (a double)
         *    _pbd   = progressive bias with decay
         *    _pdX   = enable early playout termination, pdedpth = X, where X is an integer
         *    _rbX   = enable the relative bonus with K = X, where X is a double, X is optional
         *    _rbqX  = enable the relative bonus with quality bonus, K = X, where X is a double, X is optional
         *    _sl    = Use a fixed simulation limit as opposed to time
         *    _swX   = enable sliding window UCT, with Wc = X, where X is double
         *    _uctX  = sets the UCT constant to X, where X is a double
         *    _ucb1t = enables UCB1-Tuned
         *    _wX    = enable sliding window UCT with Wc = X, where X is a double
         */
        AIPlayer playerRef = null;

        String[] parts = label.split("_");
        MCTSOptions options = new MCTSOptions();
        if (parts[0].equals("mcts") || parts[0].equals("mctstt")) {
            if (parts[0].equals("mctstt")) {
                playerRef = new UCTPlayer();
            } else {
                playerRef = new MCTSPlayer();
            }
            options.debug = mctsDebug; // false by default
            options.useHeuristics = false;
            options.solver = false;
            options.timeInterval = timeLimit;
            options.simulations = timeLimit;
            options.setGame(game);

            // now, parse the tags
            for (int i = 1; i < parts.length; i++) {
                String tag = parts[i];

                if (tag.startsWith("pd")) {
                    options.earlyEval = true;
                    options.pdepth = Integer.parseInt(tag.substring(2));
                } else if (tag.equals("h")) {
                    options.useHeuristics = true;
                } else if (tag.startsWith("im")) {
                    options.implicitMM = true;
                    if (tryParseDouble(tag.substring(2)))
                        options.imAlpha = Double.parseDouble(tag.substring(2));
                    else
                        throw new RuntimeException("IM: problem parsing alpha");
                } else if (tag.startsWith("det")) {
                    options.detEnabled = true;
                    if (tryParseDouble(tag.substring(3)))
                        options.detThreshold = Double.parseDouble(tag.substring(3));
                    else
                        throw new RuntimeException("IM: problem parsing det threshold");
                } else if (tag.startsWith("df")) {
                    options.detFreq = Integer.parseInt(tag.substring(2));
                } else if (tag.startsWith("ege")) {
                    options.epsGreedyEval = true;
                    options.egeEpsilon = Double.parseDouble(tag.substring(3));
                } else if (tag.startsWith("rb")) {
                    options.relativeBonus = true;
                    if (tryParseDouble(tag.substring(2)))
                        options.kr = Double.parseDouble(tag.substring(2));
                } else if (tag.startsWith("qb")) {
                    options.qualityBonus = true;
                    if (tryParseDouble(tag.substring(2)))
                        options.kq = Double.parseDouble(tag.substring(2));
                } else if (tag.startsWith("ucb1t")) {
                    options.ucbTuned = true;
                } else if (tag.startsWith("uct")) {
                    options.uctC = Double.parseDouble(tag.substring(3));
                } else if (tag.startsWith("mast")) {
                    options.MAST = true;
                    options.history = true;
                    if (tryParseDouble(tag.substring(4)))
                        options.mastEps = Double.parseDouble(tag.substring(4));
                } else if (tag.startsWith("sw")) {
                    options.swUCT = true;
                    if (tryParseDouble(tag.substring(2)))
                        options.windowSize = Double.parseDouble(tag.substring(2));
                } else if (tag.equals("sl")) {
                    options.fixedSimulations = true;
                } else if (tag.equals("s")) {
                    options.solver = true;
                } else if (tag.equals("pbd")) {
                    options.progBias = true;
                    options.pbDecay = true;
                    options.progBiasWeight = 0.66;
                } else if (tag.startsWith("pb")) {
                    options.progBias = true;
                    if (tryParseDouble(tag.substring(2)))
                        options.progBiasWeight = Double.parseDouble(tag.substring(2));
                    else
                        throw new RuntimeException("Unable to parse prog bias weight");
                } else if (tag.equals("ip")) {
                    options.imPruning = true;
                } else if (tag.startsWith("mbp")) {
                    options.maxBackprop = true;
                    options.maxBackpropT = Integer.parseInt(tag.substring(3));
                } else if (tag.startsWith("np")) {
                    options.nodePriors = true;
                    options.nodePriorsVisits = Integer.parseInt(tag.substring(2));
                } else if (tag.startsWith("efv")) {
                    options.efVer = Integer.parseInt(tag.substring(3));
                } else if (tag.startsWith("ph")) {
                    options.history = true;
                    options.progHistory = true;
                    if (tryParseDouble(tag.substring(2)))
                        options.phW = Double.parseDouble(tag.substring(2));
                } else if (tag.startsWith("dc")) {
                    if (tag.charAt(2) == 'n')
                        options.uctC -= Double.parseDouble(tag.substring(3));
                    else
                        options.uctC += Double.parseDouble(tag.substring(2));
                } else {
                    throw new RuntimeException("Unrecognized MCTS tag: " + tag);
                }
            }

            // and set the options for this player
            playerRef.setOptions(options);
        } else if (parts[0].equals("brue")) {
            //
            playerRef = new MCTS2ePlayer();
            options.debug = mctsDebug; // false by default
            options.useHeuristics = false;
            options.timeInterval = timeLimit;
            options.simulations = timeLimit;
            options.setGame(game);
            MCTSOptions.r.setSeed(seed);

            // now, parse the tags
            for (int i = 1; i < parts.length; i++) {
                String tag = parts[i];
                if (tag.startsWith("sl")) {
                    options.fixedSimulations = true;
                } else {
                    throw new RuntimeException("Unrecognized MCTS tag: " + tag);
                }
            }
            // and set the options for this player
            playerRef.setOptions(options);
        } else if (parts[0].equals("srmcts") || parts[0].equals("srmctstt")) {
            if (parts[0].equals("srmctstt")) {
                playerRef = new HybridPlayer();
            } else {
                playerRef = new HybridPlayer();
            }
            options.hybrid = true;
            options.debug = mctsDebug; // false by default
            options.useHeuristics = false;
            options.solver = false;
            options.remove = false;
            options.rec_halving = false;
            options.stat_reset = false;
            options.max_back = false;
            options.timeInterval = timeLimit;
            options.simulations = timeLimit;
            options.setGame(game);

            MCTSOptions.r.setSeed(seed);
            // now, parse the tags
            for (int i = 1; i < parts.length; i++) {
                String tag = parts[i];
                if (tag.equals("sl")) {
                    options.fixedSimulations = true;
                } else if (tag.equals("h")) {
                    options.useHeuristics = true;
                    options.setGame(game);
                } else if (tag.startsWith("uct")) {
                    options.uctC = Double.parseDouble(tag.substring(3));
                } else if (tag.equals("s")) {
                    options.solver = true;
                    options.setGame(game);
                } else if (tag.startsWith("rc")) {
                    options.rc = Integer.parseInt(tag.substring(2));
                } else if (tag.startsWith("bl")) {
                    options.bl = Integer.parseInt(tag.substring(2));
                } else if (tag.startsWith("rb")) {
                    options.range_back = true;
                    options.bp_range = Double.parseDouble(tag.substring(2));
                } else if (tag.equals("max")) {
                    options.max_back = true;
                } else if (tag.startsWith("mast")) {
                    options.MAST = true;
                    options.history = true;
                    options.mastEps = Double.parseDouble(tag.substring(4));
                } else if (tag.startsWith("shot")) {
                    options.enableShot();
                } else if (tag.startsWith("ph")) {
                    options.history = true;
                    options.progHistory = true;
                    if (tryParseDouble(tag.substring(2)))
                        options.phW = Double.parseDouble(tag.substring(2));
                } else if (tag.startsWith("srph")) {
                    options.history = true;
                    options.progHistory = true;
                    if (tryParseDouble(tag.substring(4)))
                        options.sr_phW = Double.parseDouble(tag.substring(4));
                } else if (tag.startsWith("mast")) {
                    options.MAST = true;
                    options.history = true;
                    if (tryParseDouble(tag.substring(4)))
                        options.mastEps = Double.parseDouble(tag.substring(4));
                } else if (tag.startsWith("ublb")) {
                    options.UBLB = true;
                    if (tryParseDouble(tag.substring(4)))
                        options.uctCC = Double.parseDouble(tag.substring(4));
                } else if (tag.startsWith("dc")) {
                    if (tag.charAt(2) == 'n')
                        options.uctC -= Double.parseDouble(tag.substring(3));
                    else
                        options.uctC += Double.parseDouble(tag.substring(2));
                } else if (tag.equals("t")) {
                    timed = true;
                    timedPlayer = player;
                } else {
                    throw new RuntimeException("Unrecognized MCTS tag: " + tag);
                }
            }
            // and set the options for this player
            playerRef.setOptions(options);
        } else if (parts[0].equals("ab")) {
            AlphaBetaOptions alphaBetaOptions = new AlphaBetaOptions();
            playerRef = new AlphaBeta();
            alphaBetaOptions.timeLimit = timeLimit;

            for (int i = 1; i < parts.length; i++) {
                String tag = parts[i];
                if (tag.equals("tt")) {
                    alphaBetaOptions.transpositions = true;
                } else if (tag.startsWith("ev")) {
                    alphaBetaOptions.evVer = Integer.parseInt(tag.substring(2));
                }
            }
            playerRef.setOptions(alphaBetaOptions);
        } else if (parts[0].equals("ismcts") || parts[0].equals("hismcts")) {
            //
            if(parts[0].equals("ismcts"))
                playerRef = new ISMCTSPlayer();
            else
                playerRef = new HISMCTSPlayer();

            options.useHeuristics = false;
            options.timeInterval = timeLimit;
            options.simulations = timeLimit;
            options.debug = mctsDebug; // false by default
            options.setGame(game);
            MCTSOptions.r.setSeed(seed);
            //
            for (int i = 1; i < parts.length; i++) {
                String tag = parts[i];
                if (tag.equals("sl")) {
                    options.fixedSimulations = true;
                } else if (tag.equals("h")) {
                    options.useHeuristics = true;
                    options.setGame(game);
                } else if (tag.startsWith("uct")) {
                    options.uctC = Double.parseDouble(tag.substring(3));
                } else if (tag.startsWith("bd")) {
                    options.banditD = true;
                    options.nDeterminizations = Integer.parseInt(tag.substring(2));
                }
            }
            playerRef.setOptions(options);
        } else if (parts[0].equals("random")) {
            playerRef = new RandomPlayer();
        } else if (parts[0].equals("keyboard")) {
            playerRef = new KeyboardPlayer();
        } else {
            throw new RuntimeException("Unrecognized player: " + label);
        }
        // Now, set the player
        if (player == 1) {
            player1 = playerRef;
            player1.newGame(1, game);
            options1 = options;
        } else if (player == 2) {
            player2 = playerRef;
            player2.newGame(2, game);
            options2 = options;
        }
    }

    boolean tryParseDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    public void loadGame() {
        if (game.equals("amazons")) {
            board = new amazons.game.Board();
        } else if (game.equals("breakthrough")) {
            board = new breakthrough.game.Board();
        } else if (game.equals("cannon")) {
            board = new cannon.game.Board();
        } else if (game.equals("chinesecheckers")) {
            board = new chinesecheckers.game.Board();
        } else if (game.equals("kalah")) {
            board = new kalah.game.Board();
        } else if (game.equals("lostcities")) {
            board = new lostcities.game.Table();
        } else if (game.equals("pentalath")) {
            board = new pentalath.game.Board();
        } else if (game.equals("checkers")) {
            board = new checkers.game.Board();
        } else if (game.startsWith("domineering")) {
            int size = Integer.parseInt(game.substring(11));
            board = new domineering.game.Board(size);
            domineering.game.Board.CRAM = false;
        } else if (game.startsWith("cram")) {
            int size = Integer.parseInt(game.substring(4));
            board = new domineering.game.Board(size);
            domineering.game.Board.CRAM = true;
        } else if (game.startsWith("nogo")) {
            int size = Integer.parseInt(game.substring(4));
            nogo.game.Board.SIZE = size;
            board = new nogo.game.Board();
        } else if (game.equals("penguin")) {
            board = new penguin.game.Board();
        } else if (game.equals("gofish")) {
            board = new gofish.game.Board();
        } else {
            throw new RuntimeException("Unrecognized game: " + game);
        }

        board.initialize();
    }

    public void run() {

        if (game.equals("kalah")) {
            run_kalah();
            return;
        }

        System.out.println("Starting game simulation...");

        System.out.println("Game: " + game);
        System.out.println("P1: " + p1label);
        System.out.println("P2: " + p2label);
        System.out.println("");

        loadGame();
        loadPlayer(1, p1label);
        loadPlayer(2, p2label);

        // Initialize the fast... stuff
        FastTanh.tanh(1.);
        FastSigm.sigm(1.);
        FastLog.log(1.);

        int budget = timeLimit;
        if (timed) {
            // Initially, both players are assigned the same budget
            options1.fixedSimulations = true;
            options1.simulations = budget;
            options2.fixedSimulations = true;
            options2.simulations = budget;
        }

        IMove m = null;
        while (board.checkWin() == IBoard.NONE_WIN) {
            int player = board.getPlayerToMove();

            if (printBoard)
                System.out.println(board.toString());
            int p = board.getPlayerToMove();
            AIPlayer aiPlayer = (p == 1 ? player1 : player2);
            System.gc();

            long startTime = System.currentTimeMillis();
            aiPlayer.getMove(board.copy(), null, board.getPlayerToMove(), false, m);
            int time = (int) (System.currentTimeMillis() - startTime);
            m = aiPlayer.getBestMove();
            board.doAIMove(m, player);

            if (m != null)
                // System.out.println("Player " + player + " played " + m + ", time " + time + "ms");
                //
                if (timed && p == timedPlayer) {
                    // Allocate the time spent to the non-fixed player
                    MCTSOptions opt = (timedPlayer == 1) ? options2 : options1;
                    opt.fixedSimulations = false;
                    opt.timeInterval = Math.max(100, time);
                    //
                    // System.out.println("new time set: " + time);
                }
        }

//        AIPlayer fixedPlayer = timedPlayer == 1 ? player1 : player2;
//        AIPlayer timedPl = timedPlayer == 1 ? player2 : player1;
//        long fixedTime = ((SRPlayer) fixedPlayer).total / (((SRPlayer) fixedPlayer).totalTime / 1000);
//        long timedTime = ((UCTPlayer) timedPl).total / ((((UCTPlayer) timedPl).totalT) / 1000);
//
//        System.out.println("Fixed: " + fixedTime);
//        System.out.println("Timed: " + timedTime);

        // Do not change the format of this line. Used by results aggregator scripts/parseres.perl
        System.out.println("Game over. Winner is " + board.checkWin());
    }

    public void run_kalah() {

        System.out.println("Starting kalah game simulation...");

        System.out.println("P1: " + p1label);
        System.out.println("P2: " + p2label);
        System.out.println("");

        // generate a random board. 
        int[] initb = new int[12];
        for (int i = 0; i < 12; i++)
            initb[i] = 0;

        for (int i = 0; i < 48; i++) {
            int index = (int) (MCTSOptions.r.nextDouble() * 12);
            initb[index]++;
        }

        kalah.game.Board initBoard = new kalah.game.Board();
        initBoard.initialize(initb);

        System.out.println("Generated board: \n" + initBoard);

        loadPlayer(1, p1label);
        loadPlayer(2, p2label);

        kalah.game.Board board = (kalah.game.Board) initBoard.copy();

        // Initialize the fast... stuff
        FastTanh.tanh(1.);
        FastSigm.sigm(1.);
        FastLog.log(1.);

        while (board.checkWin() == IBoard.NONE_WIN) {
            int player = board.getPlayerToMove();

            if (printBoard)
                System.out.println(board.toString());

            AIPlayer aiPlayer = (board.getPlayerToMove() == 1 ? player1 : player2);
            System.gc();

            IMove m = null;
            aiPlayer.getMove(board.copy(), null, board.getPlayerToMove(), false, m);
            m = aiPlayer.getBestMove();
            board.doAIMove(m, player);

            if (m != null)
                System.out.println("Player " + player + " played " + m);
        }

        // Do not change the format of this line. Used by results aggregator scripts/parseres.perl
        //System.out.println("Game over. Winner is " + board.checkWin());

        int firstgame_p1score = board.getEndScore(1);
        int firstgame_p2score = board.getEndScore(2);

        System.out.println("*** First game done. Scores are: " + firstgame_p1score + " " + firstgame_p2score);
        System.out.println("");

        System.out.println("Reloading initial board and swaping players...");

        // swap!
        loadPlayer(1, p2label);
        loadPlayer(2, p1label);

        board = (kalah.game.Board) initBoard.copy();

        System.out.println(board);

        //try { Thread.sleep(5000); } catch(Exception e) { } 

        // Initialize the fast... stuff
        FastTanh.tanh(1.);
        FastSigm.sigm(1.);
        FastLog.log(1.);

        while (board.checkWin() == IBoard.NONE_WIN) {
            int player = board.getPlayerToMove();

            if (printBoard)
                System.out.println(board.toString());

            AIPlayer aiPlayer = (board.getPlayerToMove() == 1 ? player1 : player2);
            System.gc();

            IMove m = null;
            aiPlayer.getMove(board.copy(), null, board.getPlayerToMove(), false, m);
            m = aiPlayer.getBestMove();
            board.doAIMove(m, player);

            if (m != null)
                System.out.println("Player " + player + " played " + m);
        }

        // Do not change the format of this line. Used by results aggregator scripts/parseres.perl
        //System.out.println("Game over. Winner is " + board.checkWin());

        int secondgame_p1score = board.getEndScore(1);
        int secondgame_p2score = board.getEndScore(2);

        System.out.println("***  game done. Scores are: " + secondgame_p1score + " " + secondgame_p2score);
        System.out.println("");

        System.out.println("First game scores, " + p1label + ": " + firstgame_p1score + " " + p2label + ": " + firstgame_p2score);
        System.out.println("Second game scores, " + p2label + ": " + secondgame_p1score + " " + p1label + ": " + secondgame_p2score);

        // Check for a p1label win
        if ((firstgame_p1score > firstgame_p2score && secondgame_p2score >= secondgame_p1score)
                || (secondgame_p2score > secondgame_p1score && firstgame_p1score >= firstgame_p2score))
            System.out.println("Game over. Winner is 1");
            // Check for a p2label win
        else if ((firstgame_p2score > firstgame_p1score && secondgame_p1score >= secondgame_p2score)
                || (secondgame_p1score > secondgame_p2score && firstgame_p2score >= firstgame_p1score))
            System.out.println("Game over. Winner is 2");
            // else, discard
        else
            System.out.println("Game over. Winner is 3");

    }

}

