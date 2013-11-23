package experiments;

import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;

/*
FYI: can't do this due to naming conflicts. Below, you can specify which ones you want
     by using their fully-qualified names. 
import amazons.game.Board;
import cannon.game.Board;
import chinesecheckers.game.Board;
import breakthrough.game.Board;
import lostcities.game.Table;
import pentalath.game.Board;
*/


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
    private int timeLimit;
    private long seed;

    SimGame() {
        game = "none specified";
        p1label = "none specified";
        p2label = "none specified";
        player1 = null;
        player2 = null;
        timeLimit = 1000;

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
            }
        }
    }

    public void loadPlayer(int player) {
        /**
         * Label of the player determines options. This is needed by the 
         * post-processing scripts. 
         *
         *  "mcts" is MCTSPlayer with default options
         *  For other options, add tags separated by underscores:
         *
         *  i.e. "mcts_pd4_im" = mcts player, early playout enabled (pdepth 4), implicit minimax enabled
         *
         *  Index of tags:
         *
         *    _egeX  = epsilon-greedy playouts using the eval func, where X is a double
         *    _h     = enable heuristics
         *    _im    = enable implicit minimax
         *    _pdX   = enable early playout termination, pdedpth = X, where X is an integer
         *    _wX    = enable sliding window UCT with Wc = X, where X is a double
         *    _rbX   = enable the relative bonus with K = X, where X is a double
         *    _uctX  = sets the UCT constant to X, where X is a double
         *    _ucb1t = enables UCB1-Tuned
         */
        String label = (player == 1 ? p1label : p2label);
        AIPlayer playerRef = null;

        String[] parts = label.split("_");

        if (parts[0].equals("mcts")) {
            playerRef = new MCTSPlayer();
            MCTSOptions options = new MCTSOptions();
            options.useHeuristics = false;
            options.timeInterval = timeLimit;
            MCTSOptions.r.setSeed(seed);

            // now, parse the tags
            for (int i = 1; i < parts.length; i++) {
                String tag = parts[i];

                if (tag.startsWith("pd")) {
                    options.earlyEval = true;
                    options.pdepth = Integer.parseInt(tag.substring(2));
                } else if (tag.equals("h")) {
                    options.useHeuristics = true;
                } else if (tag.equals("im")) {
                    options.implicitMM = true;
                } else if (tag.startsWith("ege")) {
                    options.epsGreedyEval = true;
                    options.egeEpsilon = Double.parseDouble(tag.substring(3));
                } else if (tag.startsWith("w")) {
                    options.swUCT = true;
                    options.windowC = Double.parseDouble(tag.substring(1));
                } else if (tag.startsWith("rb")) {
                    options.relativeBonus = true;
                    options.k = Double.parseDouble(tag.substring(2));
                } else if (tag.startsWith("ucb1t")) {
                    options.ucbTuned = true;
                } else if (tag.startsWith("uct")) {
                    options.uctC = options.k = Double.parseDouble(tag.substring(3));
                } else {
                    throw new RuntimeException("Unrecognized MCTS tag: " + tag);
                }
            }

            // and set the options for this player
            playerRef.setOptions(options);
        } else {
            throw new RuntimeException("Unrecognized player: " + label);
        }
        // Now, set the player
        if (player == 1) {
            player1 = playerRef;
            player1.newGame(1, game);
        } else if (player == 2) {
            player2 = playerRef;
            player2.newGame(2, game);
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
        } else if (game.equals("lostcities")) {
            board = new lostcities.game.Table();
        } else if (game.equals("pentalath")) {
            board = new pentalath.game.Board();
        } else {
            throw new RuntimeException("Unrecognized game: " + game);
        }

        board.initialize();

    }

    public void run() {
        System.out.println("Starting game simulation...");

        System.out.println("Game: " + game);
        System.out.println("P1: " + p1label);
        System.out.println("P2: " + p2label);
        System.out.println("");

        loadGame();
        loadPlayer(1);
        loadPlayer(2);

        while (board.checkWin() == IBoard.NONE_WIN) {
            int player = board.getPlayerToMove();
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
        System.out.println("Game over. Winner is " + board.checkWin());
    }

}

