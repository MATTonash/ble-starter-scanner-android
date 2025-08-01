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

public class TrilaterationFunction {
    // 2D trilateration just uses 2 functions
//    Expr rearrangedCircleFormula = SymMath.sqrt(SymMath.pow(x.subtract(a), 2)
//            .add(SymMath.pow(y.subtract(b), 2))).subtract(SymMath.pow(d,2));

    int numEquations = 2;
    int numColumns = 3;
    double precision = 1E-3;
    double[][] fMatrix = new double[numEquations][1];
    double[][] jacobianMatrix = new double[numEquations][numColumns];
    double[] beacon1;
    double[] beacon2;
    double[] beacon3;
    double beacon1dist;
    double beacon2dist;

    TrilaterationFunction(double[] coordinates1, double[] coordinates2,  double dist1, double dist2) {
        this.beacon1 = coordinates1;
        this.beacon2 = coordinates2;
        this.beacon1dist = dist1;
        this.beacon2dist = dist2;
    }

    void setBeacon1Dist(double dist) {
        this.beacon1dist = dist;
    }

    void setBeacon2Dist(double dist) {
        this.beacon2dist = dist;
    }

    double[] solve() {
        double[] initial = {1,1};
        double[] next = this.iterate(beacon1dist, beacon2dist, initial);
        while (!this.calcError(initial, next)) {
            initial = next;
            next = this.iterate(beacon1dist, beacon2dist, initial);
        }
        return next;
    }

    double[] iterate(double dist1, double dist2, double[] initial) {

//        // Initial function matrix F(X)
//        Expr f1 = rearrangedCircleFormula.subs(a, initial[0]);
//        f1.subs(b, initial[1]);
//        f1.subs(d, dist1);
//
//        Expr f2 = rearrangedCircleFormula.subs(a, initial[0]);
//        f2.subs(b, initial[1]);
//        f2.subs(d, dist2);
//
//        // jacobian matrix is partial diff
//        SymMatrix jacobian = new SymMatrix();
//        Expr diff1 = f1.diff(x);
//        diff1.subs(x, initial[0]);
//        diff1.subs(y, initial[1]);
//        diff1.simplify();
//        jacobian.set(0,0, diff1);
//
//        Expr diff2 = f1.diff(x);
//        diff2.subs(x, initial[0]);
//        diff2.subs(y, initial[1]);
//        diff2.simplify();
//        jacobian.set(0,1, diff2);
//
//        Expr diff3 = f1.diff(x);
//        diff3.subs(x, initial[0]);
//        diff3.subs(y, initial[1]);
//        diff3.simplify();
//        jacobian.set(1,0, diff3);
//
//        Expr diff4 = f1.diff(x);
//        diff4.subs(x, initial[0]);
//        diff4.subs(y, initial[1]);
//        diff4.simplify();
//        jacobian.set(1,1, diff4);
//
//        f1.subs(x, initial[0]);
//        f2.subs(x, initial[0]);
//        f1.subs(y, initial[1]);
//        f2.subs(y, initial[1]);
//        jacobian.set(0, 2, f1.multiply(-1));
//        jacobian.set(1, 2, f2.multiply(-1));
//
//        NumMatrix numJacobian = new NumMatrix(jacobian, f1.args());

        fMatrix[0][0] = (Math.pow((initial[0]-beacon1[0]), 2) + Math.pow((initial[1]-beacon1[1]), 2) - Math.pow(dist1, 2));
        fMatrix[1][0] = (Math.pow((initial[0]-beacon2[0]), 2) + Math.pow((initial[1]-beacon2[1]), 2) - Math.pow(dist2, 2));

        jacobianMatrix[0][0] = 2*(initial[0]-beacon1[0]);
        jacobianMatrix[0][1] = 2*(initial[1]-beacon1[1]);
        jacobianMatrix[0][2] = -1*fMatrix[0][0];

        jacobianMatrix[1][0] = 2*(initial[0]-beacon2[0]);
        jacobianMatrix[1][1] = 2*(initial[1]-beacon2[1]);
        jacobianMatrix[1][2] = -1*fMatrix[1][0];

        double[][] reducedMatrix = rowReduce(jacobianMatrix);

        double[] nextGuess = new double[reducedMatrix.length];

        for (int i = 0; i < reducedMatrix.length; i ++) {
            nextGuess[i] = reducedMatrix[i][this.numEquations] + initial[i];
        }

        return nextGuess; // return the last column = coordinates
    }

    public double[][] rowReduce(double[][] matrix)
    {
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
                    // System.out.println("Matrix is now in triangle form");
                    break reduceToTriangleForm;
                }
            }
            // a new pivot poition has been found in pivotRowCheck
            // move this equaiton to the pivotRow
            if ( pivotRow != pivotRowCheck )
            {
                // System.out.println("interchanging row " + pivotRow + " with row " + pivotRowCheck);
                interchange(pivotRow,pivotRowCheck, matrix);
                // System.out.print(this);
            }

//3: row replacement algorithm
            // reduce row so that pivot point is a 1
            if ( Math.abs( matrix[pivotRow][pivotColumn] ) > precision)
            {
                // System.out.println("scale row " + (pivotRow+1) + " by " +  (1/matrix[pivotRow][pivotColumn]));
                scale(pivotRow, 1/matrix[pivotRow][pivotColumn], matrix);
                // System.out.print(this);
            }

//interate through rest of equaitons
            for ( int row = pivotRow+1; row < numEquations; row++)
            {
                // replace all other rows such that pivot column in other rows = 0
                if ( matrix[row][pivotColumn] != 0 )
                {
                    // System.out.println("add " + ( -1*matrix[row][pivotColumn] ) + " times row " + (pivotRow+1) + " to row " + (row+1));
                    replace(row, -1*matrix[row][pivotColumn], pivotRow, matrix);
                    // System.out.print(this);
                }

            }

            //4: itterate through loop and try to find another pivot point in next column
            if ( pivotRow == numEquations-1)
            {
                // System.out.println("Matrix is now in triangle form");
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
                            // System.out.println("add " + ( -1*matrix[row][pivotColumn] ) + " times row " + (pivotRow+1) + " to row " + (row+1));
                            replace(row, -1*matrix[row][pivotColumn], pivotRow, matrix);
                            // System.out.print(this);
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
        return matrix;
    }
    // interchange two rows
    public void interchange(int row1, int row2, double[][] matrix)
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

    // multiply all entries in a row by a nonzero constant
    public void scale(int equation, double scalar, double[][] matrix)
    {
        if (equation < 0 || equation >= numEquations)
            System.err.println("invalid equation attempting to be scaled");

        for (int i = 0; i< numColumns; i++)
        {
            if ( Math.abs(matrix[equation][i] = matrix[equation][i]*scalar) < precision)
                matrix[equation][i] = 0;
        }
    }

    // replace one row by the sum of itself and a multiple of another row
    // row1 = row1 + scalar*row2
    public void replace(int row1, double scalar, int row2, double[][] matrix)
    {
        if (row1 < 0 || row1 >= numEquations || row2 < 0 || row2 >= numEquations)
            System.err.println("one or more invalid equations attempting to used in" +
                    "replace");
        for (int i = 0; i< numColumns; i++)
        {
            matrix[row1][i] += matrix[row2][i]*scalar;
            if( Math.abs(matrix[row1][i]) < precision)
                matrix[row1][i] = 0;
        }
    }

    public boolean calcError(double[] initial, double[] next) {
        boolean sufficientError = false;
        for (int i = 0; i <= this.numEquations-1; i++) {
            double error = Math.abs((initial[i] - next[i])/initial[i]);
            if (error < this.precision) {
                return sufficientError = true;
            }
            sufficientError = false;
        }
        return sufficientError;
    }
}

