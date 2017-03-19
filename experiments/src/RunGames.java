import experiments.SimGame;
import framework.util.StatCounter;

import java.text.DecimalFormat;
import java.util.Scanner;

/**
 * Created by Tom on 5-4-2014.
 */
public class RunGames {
    private static final DecimalFormat df2 = new DecimalFormat("###,##0.000");

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("Enter no of games");
        int games = in.nextInt();
        in.nextLine();
        System.out.println("Enter time/sim limit");
        String timelimit = in.nextLine();
        System.out.println("Enter game");
        String game = in.nextLine();
        System.out.println("ai1 param");
        String ai1Param = in.nextLine();
        System.out.println("ai2 param");
        String ai2Param = in.nextLine();
        in.close();
        StatCounter sc = new StatCounter();
        // --game chinesecheckers --p1 srmcts_h_sl_d1_s_rc2_sr --p2 mcts_h_sl_s --seed 74950326 --timelimit 25000
        for (int i = 0; i < games; i++) {
            String ai1String = (i % 2 == 0) ? ai1Param : ai2Param;
            String ai2String = (i % 2 == 0) ? ai2Param : ai1Param;

            SimGame sim = new SimGame();
            sim.parseArgs(String.format("--game %s --p1 %s --p2 %s --seed %s --timelimit %s", game, ai1String, ai2String, System.currentTimeMillis(), timelimit).split(" "));
            sim.run();
            int winner;
            if (i % 2 == 0) {
                winner = sim.lastWinner == 1 ? 1 : 2;
            } else {
                winner = sim.lastWinner == 1 ? 2 : 1;
            }
            sc.push(winner == 1 ? 1 : 0);
            System.out.println("AI " + winner + " won");
            System.out.println(String.format("%s\t%s ± %s\tn:%s", ai1Param, df2.format(sc.mean()), df2.format(sc.ci95()), sc.visits()));
        }
    }
}
