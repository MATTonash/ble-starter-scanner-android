/*
 * Copyright 2024 Punch Through Design LLC
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

/**
 * Solves the trilateration problem using nonlinear least squares optimization
 * (Levenberg-Marquardt algorithm) with a Huber robust loss function via
 * Iteratively Reweighted Least Squares (IRLS).
 *
 * <p>The Huber loss behaves like L2 (squared) for small residuals and like L1
 * (absolute value) for large ones. This prevents noisy beacon measurements from
 * pulling the solution off-course, since outliers are down-weighted each iteration
 * rather than penalized quadratically.
 *
 * <p>HUBER_DELTA controls the threshold between L2 and L1 behaviour. Set it to
 * roughly the expected noise level in your distance measurements (in metres).
 */
public class TrilaterationFunction {

    private static final int MAX_ITERATIONS = 1000;
    private static final double CONVERGENCE_THRESHOLD = 1e-10;
    private static final double INITIAL_LAMBDA = 1e-3;

    /**
     * Huber loss threshold (metres). Residuals smaller than this are penalised
     * quadratically; larger ones are penalised linearly, capping their influence.
     * Tune this to match your expected per-beacon distance noise floor.
     */
    private static final double HUBER_DELTA = 0.5;

    private final double[] prev;
    private final double[][] beaconCoords;
    private final double[] distances;
    private final int numBeacons;

    /**
     * @param prev        Initial guess for the position [x, y, z]
     * @param coordinates Beacon positions, each row is [x, y, z]
     * @param distances   Measured distances from each beacon
     */
    public TrilaterationFunction(double[] prev, double[][] coordinates, double[] distances) {
        this.prev = prev.clone();
        this.beaconCoords = coordinates;
        this.distances = distances;
        this.numBeacons = coordinates.length;
    }

    /**
     * Solves for the position using Levenberg-Marquardt with IRLS (Huber loss).
     *
     * <p>Each iteration:
     * <ol>
     *   <li>Compute residuals r_i = dist(pos, beacon_i) - measured_i</li>
     *   <li>Compute per-beacon Huber weights w_i = huberWeight(r_i)</li>
     *   <li>Solve the weighted normal equations: (JᵀWJ + λI)Δ = −JᵀWr</li>
     *   <li>Accept or reject the step using the weighted Huber cost</li>
     * </ol>
     *
     * @return Estimated position as [x, y, z]
     */
    public double[] solve() {
        double[] pos = prev.clone();
        double lambda = INITIAL_LAMBDA;

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            // Compute residuals: r_i = dist(pos, beacon_i) - measured_i
            double[] residuals = computeResiduals(pos);

            // IRLS: compute per-beacon Huber weights based on current residuals.
            // Beacons with large residuals (likely noisy outliers) get a lower weight,
            // capping their influence on the solution.
            double[] weights = huberWeights(residuals);

            // Compute Jacobian J (numBeacons x 3)
            double[][] J = computeJacobian(pos);

            // Compute weighted J^T*W*J and J^T*W*r
            double[][] JtWJ = multiplyJtWJ(J, weights);
            double[] JtWr = multiplyJtWr(J, weights, residuals);

            // Levenberg-Marquardt damping: (J^T*W*J + lambda*I) * delta = -J^T*W*r
            double[][] A = addDamping(JtWJ, lambda);
            double[] b = negate(JtWr);

            double[] delta = solveLinear3x3(A, b);
            if (delta == null) {
                // Singular matrix — increase damping and retry
                lambda *= 10.0;
                continue;
            }

            double[] newPos = add(pos, delta);
            double oldCost = huberCost(residuals);
            double newCost = huberCost(computeResiduals(newPos));

            if (newCost < oldCost) {
                pos = newPos;
                lambda = Math.max(lambda / 10.0, 1e-15);
                if (norm(delta) < CONVERGENCE_THRESHOLD) {
                    break;
                }
            } else {
                lambda = Math.min(lambda * 10.0, 1e10);
            }
        }

        return pos;
    }

    /**
     * Computes the Huber weight for each beacon.
     *
     * <p>For |r| <= delta: weight = 1  (full L2 influence, residual is small/trustworthy)
     * <p>For |r| >  delta: weight = delta / |r|  (down-weights the outlier beacon)
     *
     * <p>This is the standard IRLS weight derived from the Huber loss derivative:
     * multiplying J and r by sqrt(w) and then forming JᵀWJ is equivalent to
     * minimizing sum_i rho(r_i) where rho is the Huber function.
     */
    private double[] huberWeights(double[] residuals) {
        double[] weights = new double[numBeacons];
        for (int i = 0; i < numBeacons; i++) {
            double absR = Math.abs(residuals[i]);
            weights[i] = (absR <= HUBER_DELTA) ? 1.0 : HUBER_DELTA / absR;
        }
        return weights;
    }

    /**
     * Evaluates the total Huber cost for a set of residuals.
     * Used instead of sum-of-squares to correctly judge step acceptance.
     *
     * <p>rho(r) = 0.5*r^2            if |r| <= delta
     * <p>rho(r) = delta*(|r| - 0.5*delta)  if |r| >  delta
     */
    private double huberCost(double[] residuals) {
        double cost = 0.0;
        for (double r : residuals) {
            double absR = Math.abs(r);
            if (absR <= HUBER_DELTA) {
                cost += 0.5 * r * r;
            } else {
                cost += HUBER_DELTA * (absR - 0.5 * HUBER_DELTA);
            }
        }
        return cost;
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private double[] computeResiduals(double[] pos) {
        double[] r = new double[numBeacons];
        for (int i = 0; i < numBeacons; i++) {
            r[i] = dist(pos, beaconCoords[i]) - distances[i];
        }
        return r;
    }

    /**
     * Jacobian row i = d(r_i)/d(pos) = (pos - beacon_i) / dist(pos, beacon_i)
     */
    private double[][] computeJacobian(double[] pos) {
        double[][] J = new double[numBeacons][3];
        for (int i = 0; i < numBeacons; i++) {
            double d = dist(pos, beaconCoords[i]);
            if (d < 1e-12) d = 1e-12; // avoid division by zero
            for (int k = 0; k < 3; k++) {
                J[i][k] = (pos[k] - beaconCoords[i][k]) / d;
            }
        }
        return J;
    }

    /** Computes J^T * W * J (3x3 matrix), where W = diag(weights) */
    private double[][] multiplyJtWJ(double[][] J, double[] weights) {
        double[][] result = new double[3][3];
        for (int a = 0; a < 3; a++) {
            for (int b = 0; b < 3; b++) {
                double sum = 0.0;
                for (int i = 0; i < numBeacons; i++) {
                    sum += weights[i] * J[i][a] * J[i][b];
                }
                result[a][b] = sum;
            }
        }
        return result;
    }

    /** Computes J^T * W * r (3-vector), where W = diag(weights) */
    private double[] multiplyJtWr(double[][] J, double[] weights, double[] r) {
        double[] result = new double[3];
        for (int a = 0; a < 3; a++) {
            double sum = 0.0;
            for (int i = 0; i < numBeacons; i++) {
                sum += weights[i] * J[i][a] * r[i];
            }
            result[a] = sum;
        }
        return result;
    }

    /** Adds lambda * I to a 3x3 matrix (in-place copy) */
    private double[][] addDamping(double[][] M, double lambda) {
        double[][] A = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                A[i][j] = M[i][j];
            }
            A[i][i] += lambda;
        }
        return A;
    }

    /**
     * Solves A * x = b for a 3x3 matrix using Cramer's rule.
     * Returns null if the matrix is singular.
     */
    private double[] solveLinear3x3(double[][] A, double[] b) {
        double det = det3x3(A);
        if (Math.abs(det) < 1e-15) return null;

        double[] x = new double[3];
        for (int col = 0; col < 3; col++) {
            double[][] M = new double[3][3];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    M[i][j] = (j == col) ? b[i] : A[i][j];
                }
            }
            x[col] = det3x3(M) / det;
        }
        return x;
    }

    private double det3x3(double[][] M) {
        return M[0][0] * (M[1][1] * M[2][2] - M[1][2] * M[2][1])
                - M[0][1] * (M[1][0] * M[2][2] - M[1][2] * M[2][0])
                + M[0][2] * (M[1][0] * M[2][1] - M[1][1] * M[2][0]);
    }

    private double dist(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private double[] add(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) result[i] = a[i] + b[i];
        return result;
    }

    private double[] negate(double[] a) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) result[i] = -a[i];
        return result;
    }

    private double norm(double[] a) {
        double sum = 0.0;
        for (double v : a) sum += v * v;
        return Math.sqrt(sum);
    }
}