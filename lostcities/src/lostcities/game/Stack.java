package lostcities.game;

public class Stack {
    private static int N_C_CARDS = 13;
    private int[] stack = new int[N_C_CARDS];   // There are 13 cards per color
    private int index = -1;

    public boolean isEmpty() {
        return index < 0;
    }

    public void addCard(int card) {
        stack[++index] = card;
    }

    public int takeCard() {
        return stack[index--];
    }

    public int peek() {
        return stack[index];
    }

    public Stack copy() {
        Stack newStack = new Stack();
        if (index > 0) {
            System.arraycopy(stack, 0, newStack.stack, 0, index);
            newStack.index = index;
        }
        return newStack;
    }
}
