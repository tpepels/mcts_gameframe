package cannon.game;

import ai.FastRandom;
import ai.framework.IBoard;
import ai.framework.IMove;
import ai.framework.MoveList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class Board implements IBoard {
    // Board constants
    public static final int EMPTY = 0, W_SOLDIER = 1, W_TOWN = 2, B_SOLDIER = 3, B_TOWN = 4;
    public static final int WIDTH = 10, HEIGHT = 10;
    public static final MoveList moves = new MoveList(500);
    private static final List<IMove> simMoves = new ArrayList<IMove>(500), mateMoves = new ArrayList<IMove>(100);
    private static final FastRandom random = new FastRandom();
    public final int[] board = new int[WIDTH * HEIGHT];
    public final Stack<IMove> pastMoves = new Stack<IMove>();
    private final int[] capture = {-1, -11, -10, -9, 1};
    private final int[] move = {-11, -10, -9};
    private final int[] retreat = {18, 20, 22};
    private final int[] n = {-1, 0, 1};
    // Whether the towns have been placed or not determines the first moves
    public boolean whiteTownPlaced = false, blackTownPlaced = false;
    public int whiteTown, blackTown, numWhitePcs, numBlackPcs;
    public int currentPlayer = IBoard.P1, winningPlayer = NONE_WIN;
    private int allMovesForPlayer = 0; // All moves for this player (P1/P2) have been generated

    @Override
    public void initialize() {
        numWhitePcs = 0;
        numBlackPcs = 0;
        // Reset all squares
        for (int i = 0; i < board.length; i++) {
            board[i] = EMPTY;
        }
        // Place the white soldiers
        for (int i = 10; i <= 38; i += 2) {
            board[i] = W_SOLDIER;
            numWhitePcs++;
        }
        // Place the black soldiers
        for (int i = 61; i <= 89; i += 2) {
            board[i] = B_SOLDIER;
            numBlackPcs++;
        }
        // Make sure the towns will be replaced
        whiteTownPlaced = false;
        blackTownPlaced = false;
        // Reset the current player
        currentPlayer = IBoard.P1;
        winningPlayer = Board.NONE_WIN;
        pastMoves.clear();
        getAllMovesForPlayer(currentPlayer, true);
    }

    /**
     * Accept a move from player or AI to perform. For placement of the towns, only the to parameter is required.
     */
    public boolean doMove(IMove move, boolean genNextMoves) {
        if (winningPlayer != NONE_WIN)
            return false;
        boolean moveMade = false;
        // Check for town placement
        if (currentPlayer == P2 && !whiteTownPlaced) {
            whiteTownPlaced = placeTown(W_TOWN, move.getMove()[1]);
            if (whiteTownPlaced) {
                whiteTown = move.getMove()[1];
                pastMoves.push(move);
            }
            moveMade = whiteTownPlaced;
        } else if (currentPlayer == P1 && !blackTownPlaced) {
            blackTownPlaced = placeTown(B_TOWN, move.getMove()[1]);
            if (blackTownPlaced) {
                blackTown = move.getMove()[1];
                pastMoves.push(move);
            }
            moveMade = blackTownPlaced;
        } else {
            int mySoldier = (currentPlayer == P1) ? B_SOLDIER : W_SOLDIER;
            int from = move.getMove()[0], to = move.getMove()[1];
            // Town was previously placed, move a soldier
            if (board[from] == mySoldier) {
                if (move.getType() == Move.MOVE || move.getType() == Move.RETREAT) {
                    board[from] = EMPTY;
                    board[to] = mySoldier;
                    moveMade = true;
                }
                if (move.getType() == Move.FIRE) {
                    board[to] = EMPTY;
                    if (currentPlayer == P1) {
                        numWhitePcs--;
                    } else {
                        numBlackPcs--;
                    }
                    moveMade = true;
                } else if (move.getType() == Move.CAPTURE) {
                    board[from] = EMPTY;
                    board[to] = mySoldier;
                    if (currentPlayer == P1) {
                        numWhitePcs--;
                    } else {
                        numBlackPcs--;
                    }
                    moveMade = true;
                }
            }
        }
        if (moveMade) {
            currentPlayer = (currentPlayer == P1) ? P2 : P1;
            pastMoves.push(move);
            //
            if (genNextMoves) {
                getAllMovesForPlayer(currentPlayer, true);
            }
        } else {
            throw new RuntimeException("Illegal move!");
        }
        return moveMade;
    }

    private void getAllMovesForPlayer(int player, boolean determineMates) {
        int myPieces = (player == P1) ? numBlackPcs : numWhitePcs;
        int mySoldier = (player == P1) ? B_SOLDIER : W_SOLDIER;
        moves.clear();
        mateMoves.clear();
        int c = 0;
        // First, players must place the town
        if (player == P2 && !whiteTownPlaced) {
            for (int i = 0; i < 10; i++) {
                moves.add(new Move(Move.CASTLE, new int[]{-1, i}));
            }
        } else if (player == P1 && !blackTownPlaced) {
            for (int i = 90; i < board.length; i++) {
                moves.add(new Move(Move.CASTLE, new int[]{-1, i}));
            }
        } else {
            // Generate all the soldiers moves.
            for (int i = 0; i < board.length; i++) {
                if (board[i] == mySoldier) {
                    getValidMovesForSoldier(i, player, determineMates);
                    c++;
                    if (c == myPieces)
                        break;
                }
            }
        }
        allMovesForPlayer = player;
    }

    private boolean checkMates(int player) {
        // First, check around the town for capturable soldiers, this means the town can be captured
        int from = (player == P1) ? blackTown : whiteTown;
        int opp_soldier = (player == P1) ? W_SOLDIER : B_SOLDIER;
        // Reverse the moves for white
        int multipl = (player == P1) ? 1 : -1;
        int start = 0, end = capture.length, to;
        if (from % WIDTH == 0) {
            if (player == P1)
                start += 2;
            else
                end -= 2;
        } else if (from % WIDTH == 9) {
            if (player == P1)
                end -= 2;
            else
                start += 2;
        }
        for (int i = start; i < end; i++) {
            to = from + (multipl * capture[i]);
            // Not outside the board, and not occupied
            if (to > 0 && to < board.length && board[to] == opp_soldier) {
                // System.out.println("Town can be captured.");
                return true;
            }
        }
        // Now check for cannons in range
        if (player == P2) {
            // All other canons won't be in range
            start = 0; // TODO this can be 3?
            end = 40;
        } else {
            start = 60;
            end = board.length; // TODO this can be -3?
        }
        // Check for canons that can fire on the town
        int cx, cy, initX, initY;
        for (int i = start; i < end; i++) {
            if (board[i] == opp_soldier) {
                initX = i % WIDTH;
                initY = i / HEIGHT;
                // Check the cannons
                int sol, c, nextPos;
                for (int y = 0; y < n.length; y++) {
                    for (int x = 0; x < n.length; x++) {
                        if (n[x] == 0 && n[y] == 0)
                            continue;
                        sol = opp_soldier;
                        c = 0;
                        cx = initX;
                        cy = initY;
                        while (sol == opp_soldier && c < 3) {
                            c++;
                            cx += n[x];
                            cy += n[y];
                            // Check out of bounds
                            if (cx >= 0 && cx < WIDTH && cy >= 0 && cy < HEIGHT) {
                                nextPos = cx + (cy * HEIGHT);
                                sol = board[nextPos];
                            } else {
                                break;
                            }
                        }
                        // There is a canon in this direction, check for movement and fire
                        if (c == 3) {
                            // Firing direction (opposite)
                            cx = initX - n[x];
                            cy = initY - n[y];
                            int h = 0;
                            // Check out of bounds
                            while (cx >= 0 && cx < WIDTH && cy >= 0 && cy < HEIGHT && h < 3) {
                                nextPos = cx + (cy * HEIGHT);
                                // Firing is possible if the immediate square is empty
                                if (h == 0 && board[nextPos] != EMPTY) {
                                    break;
                                } else if (h > 0 && nextPos == from) {
                                    // System.out.println("Town under fire.");
                                    return true;
                                }
                                h++;
                                cx -= n[x];
                                cy -= n[y];
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void getValidMovesForSoldier(int from, int colour, boolean determineMates) {
        Move testMove;
        int start = 0, end = move.length;
        int cx, cy, initX = from % WIDTH, initY = from / HEIGHT;
        // Reverse the moves for white
        int multipl = (colour == P1) ? 1 : -1;
        int opp_soldier = (colour == P1) ? W_SOLDIER : B_SOLDIER;
        int opp = (colour == P1) ? P2 : P1;
        int opp_town = (colour == P1) ? W_TOWN : B_TOWN;
        // Simple move
        if (from % WIDTH == 0) {
            if (colour == P1)
                start++;
            else
                end--;
        } else if (from % WIDTH == 9) {
            if (colour == P1)
                end--;
            else
                start++;
        }
        int to;
        for (int i = start; i < end; i++) {
            to = from + (multipl * move[i]);
            // Not outside the board, and not occupied
            if (to > 0 && to < board.length && board[to] == EMPTY) {
                // System.out.println("move " + to + " start " + start + " end " + end);
                testMove = new Move(Move.MOVE, new int[]{from, to});
                doMove(testMove, false);
                if (!checkMates(colour)) // Check if this move results in a mate for my town
                    moves.add(testMove);
                // Check if the move leads to a mate for the opponent
                if (determineMates && checkMates(opp))
                    mateMoves.add(testMove);
                undoMove();
            }
        }
        // Captures
        start = 0;
        end = capture.length;
        if (from % WIDTH == 0) {
            if (colour == P1)
                start += 2;
            else
                end -= 2;
        } else if (from % WIDTH == 9) {
            if (colour == P1)
                end -= 2;
            else
                start += 2;
        }
        boolean canRetreat = false;
        for (int i = start; i < end; i++) {
            to = from + (multipl * capture[i]);
            // Not outside the board, and not occupied
            if (to > 0 && to < board.length && (board[to] == opp_soldier || board[to] == opp_town)) {
                // System.out.println("capture" + to);
                if (board[to] == opp_soldier) {
                    testMove = new Move(Move.CAPTURE, new int[]{from, to});
                    doMove(testMove, false);
                    if (!checkMates(colour)) // Check if this move results in a mate for my town
                        moves.add(testMove);
                    // Only movement can lead to mate positions
                    undoMove();
                    canRetreat = true;
                }
            }
        }
        // Retreat!
        if (canRetreat && ((colour == P2 && from / 10 > 2) || colour == P1 && from / 10 < 8)) {
            start = 0;
            end = retreat.length;
            // Retreat moves
            if (from % WIDTH == 0) {
                if (colour == P1)
                    start++;
                else
                    end--;
            } else if (from % WIDTH == 9) {
                if (colour == P1)
                    end--;
                else
                    start++;
            }
            to = -1;
            int free = -1, inv_multipl = (multipl == 1) ? -1 : 1;
            for (int i = start; i < end; i++) {
                to = from + (multipl * retreat[i]);
                // This square needs to be free
                free = from + inv_multipl * move[i];
                // Not outside the board, and not occupied
                if (to > 0 && to < board.length && board[to] == EMPTY && board[free] == EMPTY) {
                    // System.out.println("retreat " + to);
                    testMove = new Move(Move.RETREAT, new int[]{from, to});
                    doMove(testMove, false);
                    if (!checkMates(colour)) // Check if this move results in a mate for my town
                        moves.add(testMove);
                    // Check if the move leads to a mate for the opponent
                    if (determineMates && checkMates(opp))
                        mateMoves.add(testMove);
                    undoMove();
                }
            }
        }
        // Check the cannons
        int mySoldier = board[from], sol, c, nextPos = 0;
        for (int aN : n) {
            for (int aN1 : n) {
                if (aN1 == 0 && aN == 0)
                    continue;
                sol = mySoldier;
                c = 0;
                cx = initX;
                cy = initY;
                while (sol == mySoldier && c < 3) {
                    c++;
                    cx += aN1;
                    cy += aN;
                    // Check out of bounds
                    if (cx >= 0 && cx < WIDTH && cy >= 0 && cy < HEIGHT) {
                        nextPos = cx + (cy * HEIGHT);
                        sol = board[nextPos];
                    } else {
                        break;
                    }
                }
                // There is a canon in this direction, check for movement and fire
                if (c == 3) {
                    // Movement direction
                    // Check out of bounds
                    if (cx >= 0 && cx < WIDTH && cy >= 0 && cy < HEIGHT) {
                        // Movement is possible if the square is empty
                        if (board[nextPos] == EMPTY) {
                            // System.out.println("move cannon " + nextPos);
                            testMove = new Move(Move.MOVE, new int[]{from, nextPos});
                            doMove(testMove, false);
                            if (!checkMates(colour)) // Check if this move results in a mate for my town
                                moves.add(testMove);
                            // Check if the move leads to a mate for the opponent
                            if (determineMates && checkMates(opp))
                                mateMoves.add(testMove);
                            undoMove();
                        }
                    }
                    // Firing direction (opposite)
                    cx = initX - aN1;
                    cy = initY - aN;
                    int h = 0;
                    // Check out of bounds
                    while (cx >= 0 && cx < WIDTH && cy >= 0 && cy < HEIGHT && h < 3) {
                        nextPos = cx + (cy * HEIGHT);
                        // Firing is possible if the immediate square is empty
                        if (h == 0 && board[nextPos] != EMPTY) {
                            break;
                        } else if (h > 0 && (board[nextPos] == opp_soldier || board[nextPos] == opp_town)) {
                            // System.out.println("shoot cannon " + nextPos);
                            if (board[nextPos] == opp_soldier) {
                                testMove = new Move(Move.FIRE, new int[]{from, nextPos});
                                doMove(testMove, false);
                                if (!checkMates(colour)) { // Check if this move results in a mate for my town
                                    moves.add(testMove);
                                }
                                undoMove();
                            }
                        }
                        h++;
                        cx -= aN1;
                        cy -= aN;
                    }
                }
            }
        }
    }

    @Override
    public int checkWin() {
        // If a player has no pieces left, he lost the game
        if (numWhitePcs == 0) {
            winningPlayer = P1;
        } else if (numBlackPcs == 0) {
            winningPlayer = P2;
        }
        // Check for a checkmate position
        if (winningPlayer == NONE_WIN && allMovesForPlayer == currentPlayer) {
            if (moves.size() == 0)
                winningPlayer = getOpponent(currentPlayer);
        } else if (allMovesForPlayer != currentPlayer) {
            throw new RuntimeException("Wrong moves generated.");
        }
        return winningPlayer;
    }

    @Override
    public int checkPlayoutWin() {
        return checkWin();
    }

    /**
     * Place a player's town on a given square
     *
     * @param town     the town to place
     * @param position board coordinate of the town
     * @return true if town was placed, false if illegal
     */

    private boolean placeTown(int town, int position) {
        // Check the coordinates
        if (town == B_TOWN && (position < 90 || position >= board.length))
            return false;
        else if (town == W_TOWN && (position < 0 || position > 9))
            return false;
        // Place the appropriate town
        board[position] = town;
        return true;
    }

    @Override
    public IBoard copy() {
        Board newBoard = new Board();
        System.arraycopy(board, 0, newBoard.board, 0, board.length);
        newBoard.blackTown = blackTown;
        newBoard.whiteTown = whiteTown;
        newBoard.numWhitePcs = numWhitePcs;
        newBoard.numBlackPcs = numBlackPcs;
        newBoard.currentPlayer = currentPlayer;
        newBoard.whiteTownPlaced = whiteTownPlaced;
        newBoard.blackTownPlaced = blackTownPlaced;
        newBoard.winningPlayer = winningPlayer;
        newBoard.getAllMovesForPlayer(currentPlayer, false);
        return newBoard;
    }

    @Override
    public boolean doAIMove(IMove move, int player) {
        return doMove(move, true);
    }

    @Override
    public MoveList getExpandMoves() {
        if (allMovesForPlayer != currentPlayer) {
            //
            getAllMovesForPlayer(currentPlayer, false);
        }
        return moves.copy();
    }

    @Override
    public List<IMove> getPlayoutMoves(boolean heuristics) {
        if (allMovesForPlayer != currentPlayer) {
            //
            getAllMovesForPlayer(currentPlayer, true);
        }
        // Favour capturing and shooting :)
        simMoves.clear();
        if (mateMoves.isEmpty()) {
            IMove move;
            for (int i = 0; i < moves.size(); i++) {
                move = moves.get(i);
                // Heuristic: capture and fire whenever possible
                if (heuristics && random.nextDouble() < 0.95) {
                    if (move.getType() == Move.CAPTURE) {
                        simMoves.add(move);
                    } else if (move.getType() == Move.FIRE) {
                        simMoves.add(move);
                        simMoves.add(move);
                    }
                } else {
                    simMoves.add(move);
                }
            }
        } else {
            // Play mate-moves whenever you can
            return mateMoves;
        }
        //
        if (simMoves.size() > 1)
            return simMoves;
        else // This will not happen very often
            return Arrays.asList(moves.getArrayCopy());
    }

    @Override
    public void undoMove() {
        Move move = (Move) pastMoves.pop();
        if (move != null) {
            int player = getOpponent(currentPlayer);
            int soldier = (player == P1) ? B_SOLDIER : W_SOLDIER;
            int enemySoldier = (soldier == B_SOLDIER) ? W_SOLDIER : B_SOLDIER;
            switch (move.getType()) {
                case (Move.CASTLE):
                    if (player == P1) {
                        board[blackTown] = EMPTY;
                        blackTownPlaced = false;
                        blackTown = 0;
                    } else {
                        board[whiteTown] = EMPTY;
                        whiteTownPlaced = false;
                        whiteTown = 0;
                    }
                    break;
                case (Move.MOVE):
                    board[move.getMove()[0]] = soldier;
                    board[move.getMove()[1]] = EMPTY;
                    break;
                case (Move.RETREAT):
                    board[move.getMove()[0]] = soldier;
                    board[move.getMove()[1]] = EMPTY;
                    break;
                case (Move.CAPTURE):
                    board[move.getMove()[0]] = soldier;
                    board[move.getMove()[1]] = enemySoldier;
                    if (player == P1)
                        numWhitePcs++;
                    else
                        numBlackPcs++;
                    break;
                case (Move.FIRE):
                    board[move.getMove()[1]] = enemySoldier;
                    if (player == P1)
                        numWhitePcs++;
                    else
                        numBlackPcs++;
                    break;
            }
            winningPlayer = Board.NONE_WIN;
            currentPlayer = player;
        } else {
            System.err.println("Null in undomove");
        }
    }

    @Override
    public int getOpponent(int player) {
        return (currentPlayer == P1) ? Board.P2 : Board.P1;
    }

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    @Override
    public int getMaxUniqueMoveId() {
        return 59999;
    }

    @Override
    public void newDeterminization(int myPlayer) {
        // No implementation required
    }

    @Override
    public boolean isPartialObservable() {
        return false;
    }

    @Override
    public boolean isLegal(IMove move) {
        return true;
    }

    @Override
    public boolean drawPossible() {
        return false;
    }
}
