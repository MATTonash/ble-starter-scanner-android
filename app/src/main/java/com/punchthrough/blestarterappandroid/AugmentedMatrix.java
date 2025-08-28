/*
 * Copyright 2025 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid;

public class AugmentedMatrix {
    public double[][] matrix; //the augmented matrix
    private int numEquations; //number of rows in the matrix
    private int numVariables; //number of columns-1 in the matrix, the last
    //column is the constants from rigt sides of the equaiton
    private int numColumns;
    private static final double percision = 0.05;

    // constructor for the augmented matrix
    public AugmentedMatrix(int equations, int columns) {
        numEquations = equations;
        numVariables = columns - 1;
        numColumns = columns;

        matrix = new double[equations][numColumns];
        this.clear();
    }

    // function to allow the setting of matrix values
    public void set(int row, int col, double val) {

        matrix[row][col] = val;
    }

    public double get(int row, int col) {
        return matrix[row][col];
    }

    /**
     * clears the board that calls it by setting each position in the matrix
     * 2D array to '0'
     */
    public void clear() {
        for (int i = 0; i < numEquations; i++) {
            for (int j = 0; j < numColumns; j++)
                matrix[i][j] = 0;
        }
    }

    // multiply all entries in a row by a nonzero constant
    public void scale(int equation, double scalar) {

        for (int i = 0; i < numColumns; i++) {
            if (Math.abs(matrix[equation][i] = matrix[equation][i] * scalar) < percision)
                matrix[equation][i] = 0;
        }
    }

    // interchange two rows
    public void interchange(int row1, int row2) {

        // if row1 == row2 do nothing
        if (row1 == row2)
            return;

        // swap individual coefficents/constants of the two rows
        double temp;
        for (int i = 0; i < numColumns; i++) {
            temp = matrix[row1][i];
            matrix[row1][i] = matrix[row2][i];
            matrix[row2][i] = temp;
        }
    }

    // replace one row by the sum of itself and a multiple of another row
    // row1 = row1 + scalar*row2
    public void replace(int row1, double scalar, int row2) {
        for (int i = 0; i < numColumns; i++) {
            matrix[row1][i] += matrix[row2][i] * scalar;
            if (Math.abs(matrix[row1][i]) < percision)
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
    public void rowReduce() {
        System.out.print(this);

        //1:
        reduceToTriangleForm:
        for (int pivotColumn = 0, pivotRow = 0; pivotRow < numEquations; pivotRow++) {
            int pivotRowCheck = pivotRow; // pivotRowCheck is used to check each row
            // until a pivot point is found in the column
            while (matrix[pivotRowCheck][pivotColumn] == 0) {
                pivotRowCheck++;
                // if checks all of equations and finds no potential pivot points in the
                // current pivot column, the seach continues in the next pivot column
                if (pivotRowCheck == numEquations) {
                    pivotColumn++;
                    pivotRowCheck = pivotRow;
                }
                // should no more pivot columns points be found, exit search
                if (pivotColumn == numColumns) {
                    break reduceToTriangleForm;
                }
            }
            // a new pivot poition has been found in pivotRowCheck
            // move this equaiton to the pivotRow
            if (pivotRow != pivotRowCheck) {
                interchange(pivotRow, pivotRowCheck);
            }

//3: row replacement algorithm
            // reduce row so that pivot point is a 1
            if (Math.abs(matrix[pivotRow][pivotColumn]) > percision) {
                scale(pivotRow, 1 / matrix[pivotRow][pivotColumn]);
            }

//interate through rest of equaitons
            for (int row = pivotRow + 1; row < numEquations; row++) {
                // replace all other rows such that pivot column in other rows = 0
                if (matrix[row][pivotColumn] != 0) {
                    replace(row, -1 * matrix[row][pivotColumn], pivotRow);
                }

            }

            //4: itterate through loop and try to find another pivot point in next column
        }
        //reached end of itterating though loop
        //5: Begining with the rightmost pivot and working upward and to the left
        // create zeros above each pivot.
        boolean solved = true;
        reduceToRREF:
        for (int pivotRow = numEquations - 1; pivotRow >= 0; pivotRow--) {
            for (int pivotColumn = 0; pivotColumn < numColumns; pivotColumn++) {
                if (matrix[pivotRow][pivotColumn] != 0) {
// if a pivot is found and is not in last column
                    if (pivotColumn != numColumns - 1) {
                        // add to the rows above the pivot
                        for (int row = pivotRow - 1; row >= 0; row--) {
                            replace(row, -1 * matrix[row][pivotColumn], pivotRow);
                        }
                        break;
                    }
                    // if a pivot is found but is in the last column -> matrix inconsistent
                    else if (pivotColumn == numColumns - 1) {
                        solved = false;
                        break reduceToRREF;
                    }
                }
            }
        }
        if (solved) {
            // System.out.println(getSolutions());
        } else
            System.out.println("Matrix is inconsistent, no solution can be found");
    }

    private String getSolutions() {
        int[] freeVariables = new int[numVariables];
        String toReturn = "";
        for (int row = 0; row < numEquations; row++) {
            int col = 0;
            for (; col < numColumns - 1; col++) {
                if (Math.abs(matrix[row][col]) > percision) {
                    toReturn += ("\nx" + (col + 1) + " = ");
                    break;
                }
            }
            if (col == numColumns - 1)
                break;
            toReturn += String.format("%.5f ", matrix[row][numColumns - 1]);
            // free variables
            for (col++; col < numColumns - 1; col++) {
                if (Math.abs(matrix[row][col]) > percision) {
                    freeVariables[col] = 1;
                    if (matrix[row][col] > 0)
                        toReturn += "+ ";
                    else
                        toReturn += "- ";
                    toReturn += String.format("%.5f ", Math.abs(matrix[row][col])) + "(x" + (col + 1) + ") ";
                }
            }
        }
        for (int i = 0; i < numVariables; i++) {
            if (freeVariables[i] == 1)
                toReturn += "\nx" + (i + 1) + " is free";
        }
        return toReturn;
    }
}