package com.punchthrough.blestarterappandroid;


public class TrilaterationFunction {

    int numEquations;
    int numUnknowns;
    double precision = 1E-3;
    double[][] fMatrix;
    double[][] jacobianMatrix;

    double[][] beaconCoordinates;
    double[] beaconDistances;

    double[] initial = {1,1,1,1};

    // 3D!!! but w variable number of beacons
    TrilaterationFunction(double[] initial, double[][] beaconCoordinates, double[] beaconDistances) {
        this.initial = initial;
        this.beaconCoordinates = beaconCoordinates;
        this.beaconDistances = beaconDistances;
        numEquations = beaconDistances.length;
        this.numUnknowns = 4;
        this.fMatrix = new double[numEquations][1];
        this.jacobianMatrix = new double[numEquations][numUnknowns];
    }

    double[] solve() {
        double[] next = this.iterate(initial);
        while (!this.calcError(initial, next)) {
            initial = next;
            next = this.iterate(initial);
        }
        return new double[]{next[0], next[1], next[2]};
    }

    double[] iterate(double[] initial) {

        buildFMatrix();
        buildJacobian();
        double[][] transposedMatrix = transposeJacobMatrix();
        double[][] ATb = matrixMultiply(transposedMatrix, fMatrix);
        double[][] ATA = matrixMultiply(transposedMatrix, jacobianMatrix);

        AugmentedMatrix reducedMatrix = new AugmentedMatrix(ATA.length, numUnknowns+1);
        // for (int i = 0; i < numEquations; i ++) {
        for (int i = 0; i < ATA.length; i ++) {
            for (int a = 0; a < numUnknowns; a ++) {
                reducedMatrix.set(i, a, ATA[i][a]);
            }
        }
        for (int i = 0; i < ATb.length; i ++) {
            reducedMatrix.set(i, numUnknowns, ATb[i][0]);
        }

        reducedMatrix.rowReduce();

        double[] nextGuess = new double[reducedMatrix.matrix.length];

        for (int i = 0; i < reducedMatrix.matrix.length-1; i ++) {
            // NOTE: this.numUnknowns used to be this.numEquations (transposition method changes stuff)
            nextGuess[i] = reducedMatrix.matrix[i][this.numUnknowns] + initial[i];
        }

        return nextGuess; // return the last column = coordinates
    }


    public boolean calcError(double[] initial, double[] next) {
        boolean sufficientError = false;
        for (int i = 0; i <= this.numUnknowns-1; i++) {
            double error = Math.abs((initial[i] - next[i])/initial[i]);
            if (error < precision) {
                return sufficientError = true;
            }
            sufficientError = false;
        }
        return sufficientError;
    }

    private void buildJacobian() {
        for (int i = 0; i < numEquations; i ++) {
            // 3D partial diff
            jacobianMatrix[i][0] = -(initial[0] - beaconCoordinates[i][0])/Math.sqrt(Math.pow((initial[0] - beaconCoordinates[i][0]),2) + Math.pow((initial[1] - beaconCoordinates[i][1]),2) + Math.pow((initial[2] - beaconCoordinates[i][2]),2));
            jacobianMatrix[i][1] = -(initial[1] - beaconCoordinates[i][1])/Math.sqrt(Math.pow((initial[0] - beaconCoordinates[i][0]),2) + Math.pow((initial[1] - beaconCoordinates[i][1]),2) + Math.pow((initial[2] - beaconCoordinates[i][2]),2));
            jacobianMatrix[i][2] = -(initial[2] - beaconCoordinates[i][2])/Math.sqrt(Math.pow((initial[0] - beaconCoordinates[i][0]),2) + Math.pow((initial[1] - beaconCoordinates[i][1]),2) + Math.pow((initial[2] - beaconCoordinates[i][2]),2));
            jacobianMatrix[i][3] = -initial[3]*299792458; //hmmmhmhmhmmhmh
        }
    }
    // 'b' Matrix
    private void buildFMatrix() {
        for (int i = 0; i < numEquations; i ++) {
            fMatrix[i][0] = beaconDistances[i] - Math.sqrt(Math.pow((initial[0]-beaconCoordinates[i][0]), 2) + Math.pow((initial[1]-beaconCoordinates[i][1]), 2) + Math.pow((initial[2]-beaconCoordinates[i][2]), 2)) - 299792458*initial[3];
        }
    }

    // 'A' matrix
    private double[][] transposeJacobMatrix() {
        double[][] transposedFMatrix = new double[jacobianMatrix[0].length][jacobianMatrix.length];
        for (int i = 0; i < jacobianMatrix.length; i ++) {
            for (int u = 0; u < jacobianMatrix[0].length; u ++) {
                transposedFMatrix[u][i] = jacobianMatrix[i][u];
            }
        }
        return transposedFMatrix;
    }

    // This function multiplies mat1[][]
    // and mat2[][], and stores the result
    // in res[][]
    public double[][] matrixMultiply(double[][] mat1, double[][] mat2) {
        int N = mat1.length;
        int M = mat2[0].length;
        double[][] res = new double[N][M];
        int i, j, k;
//        for (i = 0; i < N; i++) {
//            for (j = 0; j < N; j++) {
//                res[i][j] = 0;
//                for (k = 0; k < M; k++)
//                    res[i][j] += mat1[i][k]
//                            * mat2[k][j];
//            }
//        }
        if (mat2.length != mat1[0].length) {
            System.out.println("matrices not compatible: make sure no. rows in matrix 1 = no. columns in matrix 2");
            return res;
        }

        for (i=0; i < N; i ++) {
            for (j=0; j < mat1[0].length; j ++) {
                // k is index used just for mat2 - keeping it to column 1 while i and j can be used for mat 1
                for (k=0; k < M; k ++)
                    res[i][k] += mat1[i][j] * mat2[j][k];
            }
        }
        return res;
    }
}