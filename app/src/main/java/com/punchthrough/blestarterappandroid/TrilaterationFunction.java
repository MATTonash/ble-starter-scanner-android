package com.punchthrough.blestarterappandroid;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresFactory;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;


public class TrilaterationFunction {
    // 2D trilateration just uses 2 functions

    int numEquations = 2;
    int numColumns = 3;
    double precision = 1E-5;
    double[][] fMatrix = new double[numEquations][1];
    double[][] jacobianMatrix = new double[numEquations][numColumns];

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

    double[] solve() {
        return solveNewtonRaphson();
    }

    /**
     * 3D trilateration with 3 beacons at least, assumed
     * @return DoubleArray of length n (x,y,z coordinate)
     */
    double[] solveLevenbergMarquardt() {
        int n = this.numEquations;
        int maxIter = 10000;
        double[][] anchors = beaconCoordinates;
        double[] ranges = beaconDistances;

        MultivariateJacobianFunction model = point -> {
            double x = point.getEntry(0);
            double y = point.getEntry(1);
            double z = point.getEntry(2);

            double[] values = new double[n];
            double[][] jacobian = new double[n][3];

            for (int i = 0; i < n; i++) {
                double dx = x - (beaconCoordinates[i].length > 0 ? beaconCoordinates[i][0] : 0);
                double dy = y - (beaconCoordinates[i].length > 1 ? beaconCoordinates[i][1] : 0);
                double dz = z - (beaconCoordinates[i].length > 2 ? beaconCoordinates[i][2] : 0);
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (dist < 1e-8) dist = 1e-8;

                values[i] = dist;

                jacobian[i][0] = dx / dist;
                jacobian[i][1] = dy / dist;
                jacobian[i][2] = dz / dist;
            }

            RealVector value = MatrixUtils.createRealVector(values);
            RealMatrix jac = MatrixUtils.createRealMatrix(jacobian);
            return new Pair<>(value, jac);
        };

        RealVector target = MatrixUtils.createRealVector(ranges);

        // Initial guess = centroid of anchors
        double sx = 0, sy = 0, sz = 0;
        for (int i = 0; i < numEquations; i ++) {
            sx += beaconCoordinates[i].length > 0 ? beaconCoordinates[i][0] : 0;
            sy += beaconCoordinates[i].length > 1 ? beaconCoordinates[i][1] : 0;
            sz += beaconCoordinates[i].length > 2 ? beaconCoordinates[i][2] : 0;
        }
        sx /= n; sy /= n; sz /= n;

        RealVector start = MatrixUtils.createRealVector(new double[]{sx, sy, sz});

        LeastSquaresProblem problem = LeastSquaresFactory.create(
                model, target, start, null, Integer.MAX_VALUE, Integer.MAX_VALUE
        );

        Optimum optimum = new LevenbergMarquardtOptimizer().optimize(problem);
        return optimum.getPoint().toArray();
    }

    double[] solveNewtonRaphson() {
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
