package com.punchthrough.blestarterappandroid;

import java.util.Scanner;
/*
 * Author: Sebastian Amara
 * Date: January 8, 2015
 * Description: representation of an augmented matriz, along with a method that
 * brings the matrix to reduced row echelon form, with helper functions that are
 * elementry row operations
 **/

public class AugmentedMatrix
{
    private double[][] matrix; //the augmented matrix
    private int numEquations; //number of rows in the matrix
    private int numVariables; //number of columns-1 in the matrix, the last
    //column is the constants from rigt sides of the equaiton
    private int numColumns;
    private static final double percision = 1E-10;

    // constructor for the augmented matrix
    public AugmentedMatrix(int equations, int columns)
    {
        numEquations = equations;
        numVariables = columns-1;
        numColumns = columns;

        matrix = new double[equations][numColumns];
        this.clear();
    }

    // function to allow the setting of matrix values
    public void set(int row, int col, double val)
    {
        if (row < 0 || row >= numEquations || col < 0 || col >= numColumns)
        {
            System.err.println("attemting to set invalid matrix position");
        }

        matrix[row][col] = val;
    }

    public double get(int row, int col)
    {
        return matrix[row][col];
    }

    /** clears the board that calls it by setting each position in the matrix
     * 2D array to '0'
     * */
    public void clear()
    {
        for (int i = 0; i < numEquations; i++)
        {
            for (int j = 0; j < numColumns; j++)
                matrix[i][j] = 0;
        }
    }

    // multiply all entries in a row by a nonzero constant
    public void scale(int equation, double scalar)
    {
        if (equation < 0 || equation >= numEquations)
            System.err.println("invalid equation attempting to be scaled");

        for (int i = 0; i< numColumns; i++)
        {
            if ( Math.abs(matrix[equation][i] = matrix[equation][i]*scalar) < percision)
                matrix[equation][i] = 0;
        }
    }

    // interchange two rows
    public void interchange(int row1, int row2)
    {
        if (row1 < 0 || row1 >= numEquations || row2 < 0 || row2 >= numEquations)
            System.err.println("one or more invalid equations attempting to be inte" +
                    "rchanged");

        // if row1 == row2 do nothing
        if( row1 == row2 )
            return;

        // swap individual coefficents/constants of the two rows
        double temp;
        for (int i = 0; i< numColumns; i++)
        {
            temp = matrix[row1][i];
            matrix[row1][i] = matrix[row2][i];
            matrix[row2][i] = temp;
        }
    }

    // replace one row by the sum of itself and a multiple of another row
    // row1 = row1 + scalar*row2
    public void replace(int row1, double scalar, int row2)
    {
        if (row1 < 0 || row1 >= numEquations || row2 < 0 || row2 >= numEquations)
            System.err.println("one or more invalid equations attempting to used in" +
                    "replace");
        for (int i = 0; i< numColumns; i++)
        {
            matrix[row1][i] += matrix[row2][i]*scalar;
            if( Math.abs(matrix[row1][i]) < percision)
                matrix[row1][i] = 0;
        }
    }

    /* The Row Reduction Algorithm
     * 1: Begin with the leftmost nonzero column this is a pivot column.
     * The pivot position is at the top.
     * 2: Select a nonzero entry in the pivot column as a pivot. If necessary,
     * interchange rows to move this entry into the pivot position.
     * 3: Use row replacement operations to create all zeros in all positions below pivot
     * 4: Cover or ignore the row containing the pivot position and coler all rows
     * if any above it. Apply steps 1-3 to the submatrix that remains. Repeat the
     * process until there are no more nonzero rows to modify
     * 5: Begining with the right most pivot and working upward and to the left
     * create zeros above each pivot
     **/
    public void rowReduce()
    {
        System.out.println("Solving the following augmented matrix:");
        System.out.print(this);

        //1:
        reduceToTriangleForm:
        for(int pivotColumn = 0, pivotRow = 0; pivotRow < numEquations; pivotRow++)
        {
            int pivotRowCheck = pivotRow; // pivotRowCheck is used to check each row
            // until a pivot point is found in the column
            while ( matrix[pivotRowCheck][pivotColumn] == 0 )
            {
                pivotRowCheck++;
                // if checks all of equations and finds no potential pivot points in the
                // current pivot column, the seach continues in the next pivot column
                if ( pivotRowCheck == numEquations )
                {
                    pivotColumn++;
                    pivotRowCheck = pivotRow;
                }
                // should no more pivot columns points be found, exit search
                if ( pivotColumn == numColumns)
                {
                    System.out.println("Matrix is now in triangle form");
                    break reduceToTriangleForm;
                }
            }
            // a new pivot poition has been found in pivotRowCheck
            // move this equaiton to the pivotRow
            if ( pivotRow != pivotRowCheck )
            {
                System.out.println("interchanging row " + pivotRow + " with row " + pivotRowCheck);
                interchange(pivotRow,pivotRowCheck);
                System.out.print(this);
            }

//3: row replacement algorithm 
            // reduce row so that pivot point is a 1
            if ( Math.abs( matrix[pivotRow][pivotColumn] ) > percision)
            {
                System.out.println("scale row " + (pivotRow+1) + " by " +  (1/matrix[pivotRow][pivotColumn]));
                scale(pivotRow, 1/matrix[pivotRow][pivotColumn]);
                System.out.print(this);
            }

//interate through rest of equaitons
            for ( int row = pivotRow+1; row < numEquations; row++)
            {
                // replace all other rows such that pivot column in other rows = 0
                if ( matrix[row][pivotColumn] != 0 )
                {
                    System.out.println("add " + ( -1*matrix[row][pivotColumn] ) + " times row " + (pivotRow+1) + " to row " + (row+1));
                    replace(row, -1*matrix[row][pivotColumn], pivotRow);
                    System.out.print(this);
                }

            }

            //4: itterate through loop and try to find another pivot point in next column
            if ( pivotRow == numEquations-1)
            {
                System.out.println("Matrix is now in triangle form");
            }
        }
        //reached end of itterating though loop
        //5: Begining with the rightmost pivot and working upward and to the left
        // create zeros above each pivot.
        boolean solved = true;
        reduceToRREF:
        for ( int pivotRow = numEquations-1; pivotRow >= 0; pivotRow-- )
        {
            for (int pivotColumn = 0; pivotColumn < numColumns; pivotColumn++)
            {
                if ( matrix[pivotRow][pivotColumn] != 0 )
                {
// if a pivot is found and is not in last column
                    if ( pivotColumn != numColumns-1 )
                    {
                        // add to the rows above the pivot
                        for ( int row = pivotRow-1; row >= 0; row--)
                        {
                            System.out.println("add " + ( -1*matrix[row][pivotColumn] ) + " times row " + (pivotRow+1) + " to row " + (row+1));
                            replace(row, -1*matrix[row][pivotColumn], pivotRow);
                            System.out.print(this);
                        }
                        break;
                    }
                    // if a pivot is found but is in the last column -> matrix inconsistent
                    else if ( pivotColumn == numColumns-1 )
                    {
                        solved = false;
                        break reduceToRREF;
                    }
                }
            }
        }
        if (solved)
        {
            System.out.println("Matrix is now in Reduced Row Echelon Form");
            System.out.println( getSolutions() );
        }
        else
            System.out.println("Matrix is inconsistent, no solution can be found");
    }

    private String getSolutions()
    {
        int[] freeVariables = new int[numVariables];
        String toReturn = "";
        for (int row = 0; row < numEquations; row++)
        {
            int col = 0;
            for (; col < numColumns-1; col++)
            {
                if ( Math.abs(matrix[row][col]) > percision )
                {
                    toReturn += ("\nx" + (col+1) + " = ");
                    break;
                }
            }
            if (col == numColumns-1)
                break;
            toReturn += String.format("%.5f ", matrix[row][numColumns-1]);
            // free variables
            for (col++; col < numColumns-1; col++)
            {
                if ( Math.abs(matrix[row][col]) > percision )
                {
                    freeVariables[col] = 1;
                    if (matrix[row][col] > 0)
                        toReturn += "+ ";
                    else
                        toReturn += "- ";
                    toReturn += String.format("%.5f ", Math.abs(matrix[row][col])) + "(x" + (col+1) + ") ";
                }
            }
        }
        for (int i = 0; i < numVariables; i++ )
        {
            if (freeVariables[i] == 1)
                toReturn += "\nx" + (i+1) + " is free";
        }
        return toReturn;
    }

    //prints the augmented matrix to stdout
    public String toString()
    {
        String toReturn = new String();;
        toReturn += "--";
        for (int s = 0; s <= 11*(numColumns)-2; s++)
            toReturn += " ";
        toReturn += "--\n";
        for (int i = 0; i < numEquations; i++)
        {
            toReturn += "| ";
            for (int j = 0; j < numColumns; j++)
            {
                toReturn += String.format("%10.5f ", matrix[i][j]);

                if ( j == numColumns-1 )
                    toReturn += "|\n";
            }
        }
        toReturn += "--";
        for (int s = 0; s <= 11*(numColumns)-2; s++)
            toReturn += " ";
        toReturn += "--\n";

        return toReturn;
    }



    /** hosts interactions between user and program
     * */
    public static void userinterface()
    {
        Scanner input = new Scanner( System.in );
        System.out.println("Welcome to the augmented matrix solver!");
        boolean done = false;
        while(!done)
        {
            System.out.println("\nEnter the dimensions of an augmented matrix seperated by a space. eg \"3 4\"");
            int rows = input.nextInt();
            int cols = input.nextInt();

            AugmentedMatrix aMatrix = new AugmentedMatrix(rows, cols);

            for (int r = 0; r<rows; r++)
            {
                System.out.println("Enter row " + (r+1) +"'s values of the matrix seperated by spaces");
                for (int c = 0; c<cols; c++)
                {
                    aMatrix.set(r,c, input.nextDouble());
                }
            }

            aMatrix.rowReduce();

            System.out.println("\nWould you like to enter another matrix? (y/n)");

            if (input.next().charAt(0) != 'y')
                done = true;

        }


    }
}