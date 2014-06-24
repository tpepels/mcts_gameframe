import experiments.SimGame;

import java.util.Scanner;

/**
 * Created by Tom on 5-4-2014.
 */
public class RunGames {
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
        // --game chinesecheckers --p1 srmcts_h_sl_d1_s_rc2_sr --p2 mcts_h_sl_s --seed 74950326 --timelimit 25000
        for (int i = 0; i < games; i++) {
            String ai1String = (i % 2 == 0) ? ai1Param : ai2Param;
            String ai2String = (i % 2 == 0) ? ai2Param : ai1Param;
            SimGame.main(String.format("--game %s --p1 %s --p2 %s --seed %s --timelimit %s", game, ai1String, ai2String, System.currentTimeMillis(), timelimit).split(" "));

        }
    }
}
