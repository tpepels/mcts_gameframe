package experiments;

import ai.FastSigm;
import ai.FastLog;
import ai.FastTanh;
import ai.framework.AIPlayer;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.mcts.MCTSOptions;
import ai.mcts.MCTSPlayer;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

public class AITests {
    private static final DecimalFormat df2 = new DecimalFormat("###,#00.000");
    //
    private AIPlayer aiPlayer1, aiPlayer2;
    private int games = 0, totalGames = 0;
    private double ai1Wins = 0, ai2Wins = 0, draws = 0;
    private int ai1Color = IBoard.P1, ai2Color = IBoard.P2;
    private IMove lastMove = null;
    //
    private StringBuilder finalString = new StringBuilder();
    private String outFile, game;
    private PrintWriter out;

    public static void main(String[] args) {
        AITests aitests = new AITests();
        int test;
        if (args.length == 0) {
            Scanner in = new Scanner(System.in);
            System.out.println("Enter test number");
            test = in.nextInt();
            System.out.println("Enter no of games");
            aitests.games = in.nextInt();
            in.nextLine();
            System.out.println("Enter output file");
            aitests.outFile = in.nextLine();
            System.out.println("Enter game");
            aitests.game = in.nextLine();
            in.close();
        } else {
            test = Integer.parseInt(args[0]);
            aitests.games = Integer.parseInt(args[1]);
            aitests.outFile = args[2];
            aitests.game = args[3];
        }
        aitests.runTests(test);
    }

    public void runTests(int which) {
        createOutputFile();
        // Record the date, so the version of the program can be traced back
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date today = Calendar.getInstance().getTime();
        writeOutput(df.format(today), true);
        games = 1000;
        writeOutput("Running test # " + which + ", # of runs: " + games + ", on game: " + game, true);
        // AI 1
        MCTSOptions options1 = new MCTSOptions();
        options1.debug = false;
        options1.swUCT = true;
        options1.timeInterval = 1500;
        options1.setGame(game);
        aiPlayer1 = new MCTSPlayer();
        aiPlayer1.setOptions(options1);
        // AI 2
        MCTSOptions options2 = new MCTSOptions();
        options2.debug = false;
        options2.timeInterval = 1500;
        options2.setGame(game);
        aiPlayer2 = new MCTSPlayer();
        aiPlayer2.setOptions(options2);

        // Run one of the defined experiments
        if (which == 1) {
            options1.switches = 4;
            runGames("SW UCT 4 | MCTS");

            options1.switches = 12;
            runGames("SW UCT 12 | MCTS");
        } else if (which == 2) {
            options1.switches = 6;
            runGames("SW UCT 6 | MCTS");

            options1.switches = 14;
            runGames("SW UCT 14 | MCTS");
        } else if (which == 3) {
            options1.switches = 8;
            runGames("SW UCT 8 | MCTS");

            options1.switches = 16;
            runGames("SW UCT 16 | MCTS");
        } else if (which == 4) {
            options1.switches = 10;
            runGames("SW UCT 10 | MCTS");

            options1.switches = 20;
            runGames("SW UCT 20 | MCTS");
        }
    }

    private void runGames(String testMessage) {
        writeOutput(testMessage, true);
        totalGames = 0;
        ai1Color = IBoard.P1;
        ai2Color = IBoard.P2;
        ai1Wins = 0;
        ai2Wins = 0;
        draws = 0;

        // Initialize the fast... stuff
        FastTanh.tanh(1.);
        FastSigm.sigm(1.);
        FastLog.log(1.);

        while (totalGames < games) {
            runGame();
            // Switch the colors so 50% is played as black/white
            ai1Color = ai1Color == IBoard.P1 ? IBoard.P2 : IBoard.P1;
            ai2Color = ai2Color == IBoard.P1 ? IBoard.P2 : IBoard.P1;
        }
        // Print the final statistics and the win/loss rates
        printStatistics(true);
        writeOutput("AI1: Wins " + ai1Wins, true);
        writeOutput("AI2: Wins " + ai2Wins, true);
        writeFinalOutput();
    }

    private void writeOutput(String output, boolean isFinal) {
        if (out != null) { // Write output to file
            out.println(output);
            out.flush();
        }
        // Also write it to default output in case writing to file fails.
        System.out.println(output);
        if (isFinal) {
            finalString.append(output);
            finalString.append("\n");
        }
    }

    private void writeFinalOutput() {
        createOutputFile();
        writeOutput(finalString.toString(), false);
    }

    private void createOutputFile() {
        try {
            out = new PrintWriter(outFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private IBoard getBoard() {
        IBoard board = null;

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
        } else if (game.equals("checkers")) {
            board = new checkers.game.Board();
        } else {
            throw new RuntimeException("Unrecognized game: " + game);
        }
        return board;
    }

    public void runGame() {
        IBoard board = getBoard();
        board.initialize();
        int winner = IBoard.NONE_WIN;
        aiPlayer1.newGame(ai1Color, game);
        aiPlayer2.newGame(ai2Color, game);

        while (winner == IBoard.NONE_WIN) {
            //
            IMove move;
            if (ai1Color == board.getPlayerToMove()) {
                aiPlayer1.getMove(board.copy(), null, ai1Color, false, lastMove);
                move = aiPlayer1.getBestMove();
            } else {
                aiPlayer2.getMove(board.copy(), null, ai2Color, false, lastMove);
                move = aiPlayer2.getBestMove();
            }
            if (board.doAIMove(move, board.getPlayerToMove())) {
                lastMove = move;
                winner = board.checkWin();
            } else {
                System.err.println("Error, invalid move!");
            }
            // Run the GC in between moves, to limit the runs during search
            System.gc();
        }
        lastMove = null;
        // Bookkeeping
        totalGames++;
        if (winner == ai1Color) {
            ai1Wins++;
        } else if (winner == ai2Color) {
            ai2Wins++;
        } else {
            draws++;
        }
        printStatistics(false);
    }

    private void printStatistics(boolean isFinal) {
        double total = ai1Wins + ai2Wins + draws;
        if (total > 0) {
            double mean = (ai1Wins + (0.5 * draws)) / total;
            // Calculate the variance
            double variance = 0.;
            variance += ai1Wins * Math.pow(1.0 - mean, 2);
            variance += draws * Math.pow(0.5 - mean, 2);
            variance += ai2Wins * Math.pow(0. - mean, 2);
            // Std dev and 95% conf. int.
            variance /= total;
            double ci95 = (1.96 * Math.sqrt(variance)) / Math.sqrt(total);
            double ai2WinRate = (ai2Wins + 0.5 * draws) / total;
            writeOutput(df2.format(mean * 100.0) + "% \t " + df2.format(ai2WinRate * 100.0) + "% \t ±" + df2.format(ci95 * 100.0) + "%", isFinal);
        }
    }
}
