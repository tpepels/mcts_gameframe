package framework.util;

import ai.MCTSOptions;
import framework.*;

import java.util.Scanner;
//import ai.breakthrough.Move;

public class KeyboardPlayer implements AIPlayer {

    private int player;
    private String game;
    private IMove theMove;

    @Override
    public void newGame(int myPlayer, String game) {
        this.player = myPlayer;
        this.game = game;
    }

    @Override
    public void getMove(IBoard board, MoveCallback callback, int myPlayer, boolean parallel,
                        IMove lastMove) {

        MoveList list = board.getExpandMoves();

        try {
            Scanner scanner = new Scanner(System.in);

            int c = -1, r = -1, rp = -1, cp = -1;
            IMove m = null;

            while (m == null) {
                System.out.print("Enter move: ");
                String line = scanner.nextLine();

                c = (int) line.charAt(0) - 97;
                cp = (int) line.charAt(2) - 97;

                r = (8 - Integer.parseInt("" + line.charAt(1)));
                rp = (8 - Integer.parseInt("" + line.charAt(3)));

                for (int i = 0; i < list.size(); i++) {
                    //ai.breakthrough.Move mref = (ai.breakthrough.Move)list.get(i); 
                    int[] values = list.get(i).getMove();
                    if (values[0] == r && values[1] == c && values[2] == rp && values[3] == cp) {
                        m = list.get(i);
                        break;
                    }
                }
            }

            theMove = m;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public void setOptions(MCTSOptions options) {
    }

    @Override
    public IMove getBestMove() {
        return theMove;
    }
}

