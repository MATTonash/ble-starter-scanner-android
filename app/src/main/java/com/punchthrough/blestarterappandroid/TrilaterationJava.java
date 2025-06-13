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

/**
 * Class to trilaterate the same way satellites do
 * https://math.stackexchange.com/questions/1445808/newton-raphson-method-in-gps
 */
public class TrilaterationJava {
    // Java program for implementation of
// Newton Raphson Method for solving
// equations
    static final double EPSILON = 0.001;

    // An example function whose solution
    // is determined using Bisection Method.
    // The function is x^3 - x^2 + 2
    static double func(double x)
    {
        return x * x * x - x * x + 2;
    }

    // Derivative of the above function
    // which is 3*x^x - 2*x
    static double derivFunc(double x)
    {
        return 3 * x * x - 2 * x;
    }

    // Function to find the root
    static void newtonRaphson(double x)
    {
        double h = func(x) / derivFunc(x);
        while (Math.abs(h) >= EPSILON)
        {
            h = func(x) / derivFunc(x);

            // x(i+1) = x(i) - f(x) / f'(x)
            x = x - h;
        }

        System.out.print("The value of the"
                + " root is : "
                + Math.round(x * 100.0) / 100.0);
    }

    // Driver code
    public static void main (String[] args)
    {

        // Initial values assumed
        double x0 = -20;
        newtonRaphson(x0);
    }

// This code is contributed by Anant Agarwal.
}
