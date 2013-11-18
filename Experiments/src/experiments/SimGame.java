package experiments;

import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;

import breakthrough.game.Board;


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

    SimGame() { 
        game = "none specified"; 
        p1label = "none specified";
        p2label = "none specified"; 
        player1 = null;
        player2 = null;
        timeLimit = 1000; 
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
            }
            else if (args[i].equals("--p1")) { 
                i++;
                p1label = args[i];
            }
            else if (args[i].equals("--p2")) { 
                i++;
                p2label = args[i]; 
            }
            else if (args[i].equals("--timelimit")) { 
                i++;
                timeLimit = Integer.parseInt(args[i]); 
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
         *    _h   = enable heuristics
         *    _im  = enable implicit minimax
         *    _pdX = enable early playout termination, pdedpth = X, where X is an integer         
         */ 
        String label = (player == 1 ? p1label : p2label); 
        AIPlayer playerRef = null; 

        String[] parts = label.split("_"); 

        if (parts[0].equals("mcts")) { 
            playerRef = new MCTSPlayer();
            MCTSOptions options = new MCTSOptions(); 
            options.timeInterval = timeLimit;

            // now, parse the tags
            for (int i = 1; i < parts.length; i++) { 
                String tag = parts[i];

                if (tag.startsWith("pd")) { 
                    options.earlyEval = true; 
                    options.pdepth = Integer.parseInt(tag.substring(2));  
                }
                else if (tag.equals("h")) { 
                    options.useHeuristics = true;
                }
                else if (tag.equals("im")) { 
                    options.implicitMM = true; 
                }
                else { 
                    throw new RuntimeException("Unrecognized MCTS tag: " + tag); 
                }
            }

            // and set the options for this player
            playerRef.setOptions(options); 
        } 
        else { 
            throw new RuntimeException("Unrecognized player: " + label); 
        }

        // Now, set the player

        if (player == 1) 
            player1 = playerRef; 
        else if (player == 2)
            player2 = playerRef;
    }

    public void loadGame() {
        if (game.equals("breakthrough")) {
            board = new breakthrough.game.Board(); 
        }
        else { 
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

        while (board.checkWin() == Board.NONE_WIN) {
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

        System.out.println("Winner is " + board.checkWin());
    }

}

