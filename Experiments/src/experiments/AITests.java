package experiments;

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
        writeOutput("Running test # " + which + ", # of runs: " + games + ", on game: " + game, true);
        // Run one of the defined experiments
        if (which == 1) {
            // AI 1
            MCTSOptions options1 = new MCTSOptions();
            options1.debug = false;
            options1.qualityBonus = true;
            options1.setGame(game);
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(options1);
            // AI 2
            MCTSOptions options2 = new MCTSOptions();
            options2.debug = false;
            options2.setGame(game);
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(options2);
            //
            double[] values = {3., 2.25};
            for (double i : values) {
                options1.k = i;
                runGames("AI 1 QB k = " + i + " || AI 2 MCTS");
            }
        } else if (which == 2) {
            // AI 1
            MCTSOptions options1 = new MCTSOptions();
            options1.debug = false;
            options1.qualityBonus = true;
            options1.setGame(game);
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(options1);
            // AI 2
            MCTSOptions options2 = new MCTSOptions();
            options2.debug = false;
            options2.setGame(game);
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(options2);
            //
            double[] values = {.8, 1.};
            for (double i : values) {
                options1.k = i;
                runGames("AI 1 QB k = " + i + " || AI 2 MCTS");
            }
        } else if (which == 3) {
            // AI 1
            MCTSOptions options1 = new MCTSOptions();
            options1.debug = false;
            options1.qualityBonus = true;
            options1.setGame(game);
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(options1);
            // AI 2
            MCTSOptions options2 = new MCTSOptions();
            options2.debug = false;
            options2.setGame(game);
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(options2);
            //
            double[] values = {1.25, 1.5};
            for (double i : values) {
                options1.k = i;
                runGames("AI 1 QB k = " + i + " || AI 2 MCTS");
            }
        } else if (which == 4) {
            // AI 1
            MCTSOptions options1 = new MCTSOptions();
            options1.debug = false;
            options1.qualityBonus = true;
            options1.setGame(game);
            aiPlayer1 = new MCTSPlayer();
            aiPlayer1.setOptions(options1);
            // AI 2
            MCTSOptions options2 = new MCTSOptions();
            options2.debug = false;
            options2.setGame(game);
            aiPlayer2 = new MCTSPlayer();
            aiPlayer2.setOptions(options2);
            //
            double[] values = {2., 2.5};
            for (double i : values) {
                options1.k = i;
                runGames("AI 1 QB k = " + i + " || AI 2 MCTS");
            }
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
        Class<?> clss = null;
        try {
            if (game.equals("cannon"))
                clss = Class.forName("cannon.game.Board");
            else if (game.equals("chinesecheckers"))
                clss = Class.forName("chinesecheckers.game.Board");
            else if (game.equals("lostcities"))
                clss = Class.forName("lostcities.game.Table");
            else if (game.equals("pentalath"))
                clss = Class.forName("pentalath.game.Board");
            else if (game.equals("amazons"))
                clss = Class.forName("amazons.game.Board");
            else if (game.equals("breakthrough"))
                clss = Class.forName("breakthrough.game.Board");

            // Instantiate the board for the chosen game and GO
            if (clss != null)
                return (IBoard) clss.newInstance();
            else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
