/*
 * Copyright 2026 Punch Through Design LLC
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

public class DistanceRegression {

    int n;
    double[] y_vals;
    double[] x_vals;
    double[] lin_y;
    double[] lin_x;
    double sum_x;
    double sum_y;
    double sum_xy;
    double sum_x_pow_2;
    double av_y;
    double av_x;
    double a_0;
    double a_1;
    double[] coefficients;

    public DistanceRegression(double[] y_vals, double[] x_vals) {
        this.n = y_vals.length;
        this.y_vals = y_vals;
        this.x_vals = x_vals;
        this.lin_y = new double[n];
        this.lin_x = new double[n];
        this.sum_x = 0;
        this.sum_y = 0;
        this.av_x = 0;
        this.av_y = 0;
        // PowerModelCurveFit();
        ExponentialModelCurveFit();
        // call get coefficients
        // TODO: Implement the comparison of R Squared calculations to choose which is best fit
        // TODO: Implement polyfit function
    }

    // returns a double array of coefficients
    public void PowerModelCurveFit() {
        for (int i = 0; i < n; i++) {
            lin_y[i] = Math.log10(-y_vals[i]);
            sum_y += lin_y[i];
        }
        for (int i = 0; i < n; i++) {
            lin_x[i] = Math.log10(x_vals[i]);
            sum_x += lin_x[i];
            sum_xy += lin_y[i]*lin_x[i];
            sum_x_pow_2 += Math.pow(lin_x[i],2);
        }
        a_1 = ((n*sum_xy) - (sum_x*sum_y))/((n*sum_x_pow_2) - Math.pow((sum_x),2));
        a_0 = sum_y/n - (a_1*(sum_x/n));
        double alpha = Math.pow(10, a_0);
        double beta = a_1;
        double[][] curve_vals = new double[2][n];
        curve_vals[0] = lin_x;
        for (int i = 0; i < n; i++) {
            curve_vals[1][i] = alpha*Math.pow(curve_vals[0][i], beta);
        }
        // RSquaredValue(curve_vals, (sum_y/n), a_0, a_1);
        this.coefficients = new double[2];
        coefficients[0] = alpha;
        coefficients[1] = beta;
    }

    public void ExponentialModelCurveFit() {
        for (int i = 0; i < n; i++) {
            lin_y[i] = Math.log(-y_vals[i]);
            sum_y += lin_y[i];
        }
        for (int i = 0; i < n; i++) {
            lin_x[i] = x_vals[i];
            sum_x += lin_x[i];
            sum_xy += lin_y[i]*lin_x[i];
            sum_x_pow_2 += Math.pow(lin_x[i],2);
        }
        a_1 = ((n*sum_xy) - (sum_x*sum_y))/((n*sum_x_pow_2) - Math.pow((sum_x),2));
        a_0 = sum_y/n - (a_1*(sum_x/n));
        double alpha = Math.exp(a_0);
        double beta = a_1;
        this.coefficients = new double[2];
        coefficients[0] = alpha;
        coefficients[1] = beta;
    }

//
//    public void LogarithmicModelCurveFit() {
//        for (int i = 0; i < n; i++) {
//            lin_y[i] = -y_vals[i];
//            sum_y += lin_y[i];
//        }
//        for (int i = 0; i < n; i++) {
//            lin_x[i] = Math.log(x_vals[i]);
//            sum_x += lin_x[i];
//            sum_xy += lin_y[i]*lin_x[i];
//            sum_x_pow_2 += Math.pow(lin_x[i],2);
//        }
//        a_1 = ((n*sum_xy) - (sum_x*sum_y))/((n*sum_x_pow_2) - Math.pow((sum_x),2));
//        a_0 = sum_y/n - (a_1*(sum_x/n));
//        double alpha = Math.exp(a_0);
//        double beta = a_1;
//        this.coefficients = new double[2];
//        coefficients[0] = alpha;
//        coefficients[1] = beta;
//    }

    public double[] getCoefficients() {
        return coefficients;
    }
}
