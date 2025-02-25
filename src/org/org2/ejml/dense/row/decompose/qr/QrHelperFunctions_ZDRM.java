/*
 * Copyright (c) 2009-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.org2.ejml.dense.row.decompose.qr;

import org.org2.ejml.data.Complex_F64;
import org.org2.ejml.data.ZMatrixRMaj;


/**
 * <p>
 * Contains different functions that are useful for computing the QR decomposition of a matrix.
 * </p>
 *
 * <p>
 * Two different families of functions are provided for help in computing reflectors.  Internally
 * both of these functions switch between normalization by division or multiplication.  Multiplication
 * is most often significantly faster than division (2 or 3 times) but produces less accurate results
 * on very small numbers.  It checks to see if round off error is significant and decides which
 * one it should do.
 * </p>
 *
 * <p>
 * Tests were done using the stability benchmark in jmatbench and there doesn't seem to be
 * any advantage to always dividing by the max instead of checking and deciding.  The most
 * noticeable difference between the two methods is with very small numbers.
 * </p>
 *
 * @author Peter Abeles
 */
public class QrHelperFunctions_ZDRM {

    /**
     * Returns the maximum magnitude of the complex numbers
     * @param u Array of complex numbers
     * @param startU first index to consider in u
     * @param length Number of complex numebrs to consider
     * @return magnitude
     */
    public static double findMax( double[] u, int startU , int length ) {
        double max = -1;

        int index = startU*2;
        int stopIndex = (startU + length)*2;
        for( ; index < stopIndex;) {
            double real = u[index++];
            double img = u[index++];

            double val = real*real + img*img;

            if( val > max ) {
                max = val;
            }
        }

        return Math.sqrt(max);
    }

    /**
     * Performs the following operation:<br>
     * u[(startU+j):(startU+numRows)] /= A<br>
     * were u and A are a complex
     */
    public static void divideElements(final int j, final int numRows , final double[] u,
                                      final int startU ,
                                      final double realA, final double imagA ) {

        double mag2 = realA*realA + imagA*imagA;

        int index = (startU+j)*2;

        for( int i = j; i < numRows; i++ ) {
            double realU = u[index];
            double imagU = u[index+1];

            // u[i+startU] /= u_0;
            u[index++] = (realU*realA + imagU*imagA)/mag2;
            u[index++] = (imagU*realA - realU*imagA)/mag2;
        }
    }

    /**
     * Performs the following operations:
     * <pre>
     * x = x / max
     * tau = x0*|x|/|xo|   adjust sign to avoid cancellation
     * u = x; u0 = x0 + tau; u = u/u0  (x is not divided by x0)
     * gamma = 2/|u|^2
     * </pre>
     * Note that u is not explicitly computed here.
     *
     * @param start Element in 'u' that it starts at.
     * @param stop Element in 'u' that it stops at.
     * @param x Array
     * @param max Max value in 'u' that is used to normalize it.
     * @param tau Storage for tau
     * @return Returns gamma
     */
    public static double computeTauGammaAndDivide(final int start, final int stop,
                                                  final double[] x, final double max,
                                                  Complex_F64 tau) {

        int index = start*2;
        double nx = 0;
        for (int i = start; i < stop; i++) {
            double realX = x[index++] /= max;
            double imagX = x[index++] /= max;

            nx += realX * realX + imagX * imagX;
        }

        nx = Math.sqrt(nx);

        double real_x0 = x[2*start];
        double imag_x0 = x[2*start+1];
        double mag_x0 = Math.sqrt(real_x0*real_x0 + imag_x0*imag_x0);

        // TODO Could stability be improved by computing theta so that this
        // special case doesn't need to be handled?
        if( mag_x0 == 0 ) {
            tau.real = nx;
            tau.imaginary = 0;
        } else {
            tau.real = real_x0 / mag_x0 * nx;
            tau.imaginary = imag_x0 / mag_x0 * nx;
        }

        double top,bottom;

        // if there is a chance they can cancel swap the sign
        // TODO not sure if this is right...
        double m0 = mag(real_x0+tau.real , imag_x0+tau.imaginary);
        double m1 = mag(real_x0-tau.real , imag_x0-tau.imaginary);

        // if ( real_x0*tau.real<0) { // This is the previous rule until the m0 m1 code came into play
        if ( m1 > m0 ) {
            tau.real = -tau.real;
            tau.imaginary = -tau.imaginary;
            top = nx * nx - nx *mag_x0;
            bottom = mag_x0*mag_x0 - 2.0* nx *mag_x0 + nx * nx;
        } else {
            top = nx * nx + nx *mag_x0;
            bottom = mag_x0*mag_x0 + 2.0* nx *mag_x0 + nx * nx;
        }

        return bottom/top; // gamma
    }

    private static double mag( double r , double i ) {
        return r*r + i*i;
    }

    /**
     * <p>
     * Performs a rank-1 update operation on the submatrix specified by w with the multiply on the right.<br>
     * <br>
     * A = (I - &gamma;*u*u<sup>H</sup>)*A<br>
     * </p>
     *
     * @param A matrix
     * @param u vector
     * @param offsetU offset added to w0 when indexing u.  Multiplied by 2 since complex.
     * @param gamma real component of gamma
     * @param colA0 first column in A sub-matrix.
     * @param w0 first index in sub-array in u and row sub-matrix in A
     * @param w1 last index + 1 in sub-array in u and row sub-matrix in A
     * @param _temp temporary storage.  Same size as u.
     */
    public static void rank1UpdateMultR(ZMatrixRMaj A,
                                        double u[], int offsetU,
                                        double gamma ,
                                        int colA0,
                                        int w0, int w1,
                                        double _temp[])
    {
//        for( int i = colA0; i < A.numCols; i++ ) {
//            double val = 0;
//
//            for( int k = w0; k < w1; k++ ) {
//                val += u[k+offsetU]*A.data[k*A.numCols +i];
//            }
//            _temp[i] = gamma*val;
//        }

        // reordered to reduce cpu cache issues
        int indexU = (w0+offsetU)*2;
        double realU = u[indexU];
        double imagU = -u[indexU+1];

        int indexA = (w0*A.numCols + colA0)*2;
        int indexTmp = colA0*2;

        for( int i = colA0; i < A.numCols; i++ ) {
            double realA = A.data[indexA++];
            double imagA = A.data[indexA++];

            _temp[indexTmp++] = realU*realA - imagU*imagA;
            _temp[indexTmp++] = realU*imagA + imagU*realA;
        }

        for( int k = w0+1; k < w1; k++ ) {
            indexA = (k*A.numCols + colA0)*2;
            indexU = (k+offsetU)*2;
            indexTmp = colA0*2;

            realU = u[indexU];
            imagU = -u[indexU+1];

            for( int i = colA0; i < A.numCols; i++ ) {
                double realA = A.data[indexA++];
                double imagA = A.data[indexA++];

                _temp[indexTmp++] += realU*realA - imagU*imagA;
                _temp[indexTmp++] += realU*imagA + imagU*realA;
            }
        }

        indexTmp = colA0*2;
        for( int i = colA0; i < A.numCols; i++ ) {
            _temp[indexTmp++] *= gamma;
            _temp[indexTmp++] *= gamma;
        }

        // end of reorder

        for( int i = w0; i < w1; i++ ) {
            indexA = (i*A.numCols + colA0)*2;
            indexU = (i+offsetU)*2;
            indexTmp = colA0*2;

            realU = u[indexU];
            imagU = u[indexU+1];

            for( int j = colA0; j < A.numCols; j++ ) {
                double realTmp = _temp[indexTmp++];
                double imagTmp = _temp[indexTmp++];

                A.data[indexA++] -= realU*realTmp - imagU*imagTmp;
                A.data[indexA++] -= realU*imagTmp + imagU*realTmp;
            }
        }
    }

    /**
     * <p>
     * Performs a rank-1 update operation on the submatrix specified by w with the multiply on the left.<br>
     * <br>
     * A = A(I - &gamma;*u*u<sup>H</sup>)<br>
     * </p>
     * <p>
     * The order that matrix multiplies are performed has been carefully selected
     * to minimize the number of operations.
     * </p>
     *
     * <p>
     * Before this can become a truly generic operation the submatrix specification needs
     * to be made more generic.
     * </p>
     */
    public static void rank1UpdateMultL(ZMatrixRMaj A , double u[] , int offsetU,
                                        double gammaR ,
                                        int colA0,
                                        int w0 , int w1 )
    {
        for( int i = colA0; i < A.numRows; i++ ) {
            int startIndex = (i*A.numCols+w0)*2;
            double realSum = 0,imagSum=0;
            int rowIndex = startIndex;
            int indexU = (offsetU+w0)*2;
            for( int j = w0; j < w1; j++ ) {
                double realA = A.data[rowIndex++];
                double imajA = A.data[rowIndex++];

                double realU = u[indexU++];
                double imajU = u[indexU++];

                realSum += realA*realU - imajA*imajU;
                imagSum += realA*imajU + imajA*realU;
            }
            double realTmp = -gammaR*realSum;
            double imagTmp = -gammaR*imagSum;

            rowIndex = startIndex;
            indexU = (offsetU+w0)*2;
            for( int j = w0; j < w1; j++ ) {
                double realU = u[indexU++];
                double imagU = -u[indexU++];

                A.data[rowIndex++] += realTmp*realU - imagTmp*imagU;
                A.data[rowIndex++] += realTmp*imagU + imagTmp*realU;
            }
        }
    }

    /**
     * Extracts a house holder vector from the column of A and stores it in u
     * @param A Complex matrix with householder vectors stored in the lower left triangle
     * @param row0 first row in A (implicitly assumed to be r + i0)
     * @param row1 last row + 1 in A
     * @param col Column in A
     * @param u Output array storage
     * @param offsetU first index in U
     */
    public static void extractHouseholderColumn( ZMatrixRMaj A ,
                                                 int row0 , int row1 ,
                                                 int col , double u[], int offsetU )
    {
        int indexU = (row0+offsetU)*2;
        u[indexU++] = 1;
        u[indexU++] = 0;

        for (int row = row0+1; row < row1; row++) {
            int indexA = A.getIndex(row,col);
            u[indexU++] = A.data[indexA];
            u[indexU++] = A.data[indexA+1];
        }
    }

    /**
     * Extracts a house holder vector from the rows of A and stores it in u
     * @param A Complex matrix with householder vectors stored in the upper right triangle
     * @param row Row in A
     * @param col0 first row in A (implicitly assumed to be r + i0)
     * @param col1 last row +1 in A
     * @param u Output array storage
     * @param offsetU first index in U
     */
    public static void extractHouseholderRow( ZMatrixRMaj A ,
                                              int row ,
                                              int col0, int col1 , double u[], int offsetU )
    {
        int indexU = (offsetU+col0)*2;
        u[indexU]   = 1;
        u[indexU+1] = 0;

        int indexA = (row*A.numCols + (col0+1))*2;
        System.arraycopy(A.data,indexA,u,indexU+2,(col1-col0-1)*2);
    }

    /**
     * Extracts the column of A and copies it into u while computing the magnitude of the
     * largest element and returning it.
     *
     * <pre>
     * u[ (offsetU+row0+i)*2    ] = A.getReal(row0+i,col)
     * u[ (offsetU+row0+i)*2 + 1] = A.getImag(row0+i,col)
     * </pre>
     *
     * @param A Complex matrix
     * @param row0 First row in A to be copied
     * @param row1 Last row in A + 1 to be copied
     * @param col Column in A
     * @param u Output array storage
     * @param offsetU first index in U
     * @return magnitude of largest element
     */
    public static double extractColumnAndMax( ZMatrixRMaj A ,
                                              int row0 , int row1 ,
                                              int col , double u[], int offsetU) {
        int indexU = (offsetU+row0)*2;

        // find the largest value in this column
        // this is used to normalize the column and mitigate overflow/underflow
        double max = 0;

        int indexA = A.getIndex(row0,col);
        double h[] = A.data;

        for( int i = row0; i < row1; i++, indexA += A.numCols*2 ) {
            // copy the householder vector to an array to reduce cache misses
            // big improvement on larger matrices and a relatively small performance hit on small matrices.
            double realVal = u[indexU++] = h[indexA];
            double imagVal = u[indexU++] = h[indexA+1];

            double magVal = realVal*realVal + imagVal*imagVal;
            if( max < magVal ) {
                max = magVal;
            }
        }
        return Math.sqrt(max);
    }

    /**
     * Finds the magnitude of the largest element in the row
     * @param A Complex matrix
     * @param row Row in A
     * @param col0 First column in A to be copied
     * @param col1 Last column in A + 1 to be copied
     * @return magnitude of largest element
     */
    public static double computeRowMax( ZMatrixRMaj A ,
                                        int row , int col0 , int col1 ) {
        double max = 0;

        int indexA = A.getIndex(row,col0);
        double h[] = A.data;

        for (int i = col0; i < col1; i++) {
            double realVal = h[indexA++];
            double imagVal = h[indexA++];

            double magVal = realVal*realVal + imagVal*imagVal;
            if( max < magVal ) {
                max = magVal;
            }
        }
        return Math.sqrt(max);
    }
}
