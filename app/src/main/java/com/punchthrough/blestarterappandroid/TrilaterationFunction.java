package com.punchthrough.blestarterappandroid;

import com.punchthrough.blestarterappandroid.AugmentedMatrix;

public class TrilaterationFunction {
    // 2D trilateration just uses 2 functions

    int numEquations = 2;
    int numColumns = 3;
    double precision = 1E-5;
    double[][] fMatrix = new double[numEquations][1];
    double[][] jacobianMatrix = new double[numEquations][numColumns];
//    double[] beacon1;
//    double[] beacon2;
//    double[] beacon3;
//    double beacon1dist;
//    double beacon2dist;
//    double beacon3dist;

    double[][] beaconCoordinates;
    double[] beaconDistances;

    double[] initial = {1,1,1};

    // 3D with variable number of beacons (Z assumed 0 for 2D inputs)
    TrilaterationFunction(double[][] beaconCoordinates) {
        this.beaconCoordinates = beaconCoordinates;
        // Equations count is the number of beacons provided
        this.numEquations = beaconCoordinates != null ? beaconCoordinates.length : 0;
        // Columns: x, y, z and the function value column
        this.numColumns = 4;
        this.fMatrix = new double[numEquations][1];
        this.jacobianMatrix = new double[numEquations][numColumns];
        this.initial = new double[]{1, 1, 1};
    }

    public void setBeaconDistances(double[] beaconDistances) {
        this.beaconDistances = beaconDistances;
    }


//    // For 3D!!!!!!!!! only takes 3 beacons manually
//    // NOTE: ALL COORDINATES WILL NEED 3 VALUES [X,Y,Z]
//    TrilaterationFunction(double[] coordinates1, double[] coordinates2, double[] coordinates3, double dist1, double dist2, double dist3) {
//        this.beacon1 = coordinates1;
//        this.beacon2 = coordinates2;
//        this.beacon3 = coordinates3;
//        this.beacon1dist = dist1;
//        this.beacon2dist = dist2;
//        this.beacon3dist = dist3;
//        this.numEquations = 3;
//        this.numColumns = 4;
//        this.fMatrix = new double[numEquations][1];
//        this.jacobianMatrix = new double[numEquations][numColumns];
//        this.initial = new double[]{1, 1, 1};
//    }
//
//    void setBeacon1Dist(double dist) {
//        this.beacon1dist = dist;
//    }
//
//    void setBeacon2Dist(double dist) {
//        this.beacon2dist = dist;
//    }
//
//    void setBeacon3Dist(double dist) {
//        this.beacon3dist = dist;
//    }
//
//    void setBeacon1(double[] coordinates) {
//        this.beacon1 = coordinates;
//    }
//
//    void setBeacon2(double[] coordinates) {
//        this.beacon2 = coordinates;
//    }
//
//    void setBeacon3(double[] coordinates) {
//        this.beacon3 = coordinates;
//    }

    double[] solve() {
        double[] next = this.iterate(initial);
        while (!this.calcError(initial, next)) {
            initial = next;
            next = this.iterate(initial);
        }
        return next;
    }

    double[] iterate(double[] initial) {
        buildFMatrix();
        buildJacobian();

        AugmentedMatrix reducedMatrix = new AugmentedMatrix(numEquations, numColumns);
        for (int i = 0; i < numEquations; i ++) {
            for (int a = 0; a < numColumns; a ++) {
                reducedMatrix.set(i, a, jacobianMatrix[i][a]);
            }
        }

        reducedMatrix.rowReduce();

        double[] nextGuess = new double[reducedMatrix.matrix.length];

        for (int i = 0; i < reducedMatrix.matrix.length; i ++) {
            nextGuess[i] = reducedMatrix.matrix[i][this.numEquations] + initial[i];
        }

        return nextGuess; // return the last column = coordinates
    }


    public boolean calcError(double[] initial, double[] next) {
        for (int i = 0; i <= this.numEquations-1; i++) {
            double error = Math.abs((initial[i] - next[i])/initial[i]);
            if (error < precision) {
                return true;
            }
        }
        return false;
    }

    private void buildJacobian() {
        for (int i = 0; i < numEquations; i ++) {
            // 3D partial derivatives; if coordinates provided are 2D, assume z=0
            double bx = beaconCoordinates[i].length > 0 ? beaconCoordinates[i][0] : 0;
            double by = beaconCoordinates[i].length > 1 ? beaconCoordinates[i][1] : 0;
            double bz = beaconCoordinates[i].length > 2 ? beaconCoordinates[i][2] : 0;

            jacobianMatrix[i][0] = 2*(initial[0]-bx);
            jacobianMatrix[i][1] = 2*(initial[1]-by);
            jacobianMatrix[i][2] = 2*(initial[2]-bz);
            jacobianMatrix[i][3] = -1*fMatrix[i][0];
        }
    }

    private void buildFMatrix() {
        if (beaconDistances == null || beaconDistances.length != numEquations) {
            throw new IllegalStateException("beaconDistances must be set and match number of beacons before solving");
        }
        for (int i = 0; i < numEquations; i ++) {
            double bx = beaconCoordinates[i].length > 0 ? beaconCoordinates[i][0] : 0;
            double by = beaconCoordinates[i].length > 1 ? beaconCoordinates[i][1] : 0;
            double bz = beaconCoordinates[i].length > 2 ? beaconCoordinates[i][2] : 0;

            fMatrix[i][0] = (Math.pow((initial[0]-bx), 2)
                    + Math.pow((initial[1]-by), 2)
                    + Math.pow((initial[2]-bz), 2)
                    - Math.pow(beaconDistances[i], 2));
        }
    }
}
