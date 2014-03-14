package ai;

/**
 * Created by Tom on 11-3-14.
 */
public class Test {
    public static void main(String[] args)
    {
        int vertical;
        int horizontal;
        int diagonal;
        String firstS= "ACATGTACGT";
        String secondS="AGTCTACCGT";
        char[] first=firstS.toCharArray();
        char[] second=secondS.toCharArray();
        int[][] table=new int[first.length+1][second.length+1];
        int[] tableSingle=new int[first.length+1];

        //Single array implementation
        for(int i=0;i<tableSingle.length;i++)
            tableSingle[i]=i;
        System.out.print("\n\t");
        for (int i=1;i<second.length+1;i++,System.out.print("\n\t"))
        {
            if(first[0]==second[i-1])
                diagonal=tableSingle[0];
            else
                diagonal=tableSingle[0]+1;
            horizontal=tableSingle[0];
            tableSingle[0]=horizontal+1;

            for (int j=1;j<first.length+1;j++)
            {
                horizontal+=1;
                vertical=tableSingle[j]+1;
                if(first[j-1]==second[i-1])
                    horizontal=Math.min(Math.min(vertical,horizontal),diagonal);
                else
                    horizontal=Math.min(Math.min(vertical,horizontal),diagonal+1);
                diagonal=tableSingle[j];
                tableSingle[j]=horizontal;
                System.out.print(tableSingle[j]+"\t");
            }
        }

        System.out.println("\n");





        //2D array implementation
        for (int i=0;i<table.length;i++)
            table[i][0]=i;
        for (int i=0;i<table[0].length;i++)
            table[0][i]=i;
        for (int i=1;i<table.length;i++)
            for(int j=1;j<table.length;j++)
            {
                vertical=table[i-1][j]+1;
                horizontal=table[i][j-1]+1;
                if(first[i-1]!=second[j-1])
                    diagonal=table[i-1][j-1]+1;
                else diagonal=table[i-1][j-1];
                int min=Math.min(Math.min(vertical,horizontal),diagonal);
                table[i][j]=min;
            }

        for (int i=0;i<table.length;i++,System.out.println())
            for(int j=0;j<table.length;j++)
                System.out.print(table[i][j]+"\t");


        //Optimal Path
        int x=table.length-1;
        int y=table[0].length-1;
        String one="";
        String two="";
        while(table[x][y]!=0)
        {
            if(x>1)
                vertical=table[x-1][y];
            else
                vertical=Integer.MAX_VALUE;
            if(y>1)
                horizontal=table[x][y-1];
            else
                horizontal=Integer.MAX_VALUE;
            if(x>1&&y>1)
                diagonal=table[x-1][y-1];
            else
                diagonal=Integer.MAX_VALUE;

            int min=Math.min(Math.min(vertical,horizontal),diagonal);
            if(min==diagonal)
            {
                x-=1;
                y-=1;
                one+=first[x];
                two+=second[y];
            }
            else if(min==vertical)
            {
                x-=1;
                one+=first[x];
                two+="-"+second[y];
            }
            else if (min==horizontal)
            {
                y-=1;
                one+="-"+first[x];
                two+=second[y];
            }
        }
        //Printing perfect path solution
        char[] help1=one.toCharArray();
        char[] help2=two.toCharArray();
        char[] help3=new char[help1.length];
        char[] help4=new char[help2.length];
        for(int i=0;i<help1.length;i++)
            help3[i]=help1[help1.length-i-1];
        for(int i=0;i<help1.length;i++)
            help4[i]=help2[help2.length-i-1];
        for(int i=0;i<help1.length;i++)
            System.out.print(help3[i]);
        System.out.println();
        for(int i=0;i<help2.length;i++)
            System.out.print(help4[i]);
        System.out.println();



    }
}
