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

package org.org2.ejml.dense.row.decomposition.hessenberg;

import org.org2.ejml.data.FMatrixRMaj;
import org.org2.ejml.dense.row.decomposition.UtilDecompositons_FDRM;
import org.org2.ejml.dense.row.decomposition.qr.QrHelperFunctions_FDRM;
import org.org2.ejml.interfaces.decomposition.TridiagonalSimilarDecomposition_F32;

/**
 * <p>
 * Performs a {@link TridiagonalSimilarDecomposition_F32 similar tridiagonal decomposition} on a square symmetric input matrix.
 * Householder vectors perform the similar operation and the symmetry is taken advantage
 * of for good performance.
 * </p>
 * <p>
 * Finds the decomposition of a matrix in the form of:<br>
 * <br>
 * A = O*T*O<sup>T</sup><br>
 * <br>
 * where A is a symmetric m by m matrix, O is an orthogonal matrix, and T is a tridiagonal matrix.
 * </p>
 * <p>
 * This implementation is based off of the algorithm described in:<br>
 * <br>
 * David S. Watkins, "Fundamentals of Matrix Computations," Second Edition.  Page 349-355
 * </p>
 *
 * @author Peter Abeles
 */
public class TridiagonalDecompositionHouseholder_FDRM
        implements TridiagonalSimilarDecomposition_F32<FMatrixRMaj> {

    /**
     * Only the upper right triangle is used.  The Tridiagonal portion stores
     * the tridiagonal matrix.  The rows store householder vectors.
     */
    private FMatrixRMaj QT;

    // The size of the matrix
    private int N;

    // temporary storage
    private float w[];
    // gammas for the householder operations
    private float gammas[];
    // temporary storage
    private float b[];

    public TridiagonalDecompositionHouseholder_FDRM() {
        N = 1;
        w = new float[N];
        b = new float[N];
        gammas = new float[N];
    }

    /**
     * Returns the internal matrix where the decomposed results are stored.
     * @return
     */
    public FMatrixRMaj getQT() {
        return QT;
    }

    @Override
    public void getDiagonal(float[] diag, float[] off) {
        for( int i = 0; i < N; i++ ) {
            diag[i] = QT.data[i*N+i];

            if( i+1 < N ) {
                off[i] = QT.data[i*N+i+1];
            }
        }
    }

    /**
     * Extracts the tridiagonal matrix found in the decomposition.
     *
     * @param T If not null then the results will be stored here.  Otherwise a new matrix will be created.
     * @return The extracted T matrix.
     */
    @Override
    public FMatrixRMaj getT(FMatrixRMaj T ) {
        T = UtilDecompositons_FDRM.checkZeros(T,N,N);

        T.data[0] = QT.data[0];

        for( int i = 1; i < N; i++ ) {
            T.set(i,i, QT.get(i,i));
            float a = QT.get(i-1,i);
            T.set(i-1,i,a);
            T.set(i,i-1,a);
        }

        if( N > 1 ) {
            T.data[(N-1)*N+N-1] = QT.data[(N-1)*N+N-1];
            T.data[(N-1)*N+N-2] = QT.data[(N-2)*N+N-1];
        }
            
        return T;
    }

    /**
     * An orthogonal matrix that has the following property: T = Q<sup>T</sup>AQ
     *
     * @param Q If not null then the results will be stored here.  Otherwise a new matrix will be created.
     * @return The extracted Q matrix.
     */
    @Override
    public FMatrixRMaj getQ(FMatrixRMaj Q , boolean transposed ) {
        Q = UtilDecompositons_FDRM.checkIdentity(Q,N,N);

        for( int i = 0; i < N; i++ ) w[i] = 0;

        if( transposed ) {
            for( int j = N-2; j >= 0; j-- ) {
                w[j+1] = 1;
                for( int i = j+2; i < N; i++ ) {
                    w[i] = QT.data[j*N+i];
                }
                QrHelperFunctions_FDRM.rank1UpdateMultL(Q, w, gammas[j + 1], j + 1, j + 1, N);
            }
        } else {
            for( int j = N-2; j >= 0; j-- ) {
                w[j+1] = 1;
                for( int i = j+2; i < N; i++ ) {
                    w[i] = QT.get(j,i);
                }
                QrHelperFunctions_FDRM.rank1UpdateMultR(Q, w, gammas[j + 1], j + 1, j + 1, N, b);
            }
        }

        return Q;
    }

    /**
     * Decomposes the provided symmetric matrix.
     *
     * @param A Symmetric matrix that is going to be decomposed.  Not modified.
     */
    @Override
    public boolean decompose( FMatrixRMaj A ) {
        init(A);

        for( int k = 1; k < N; k++ ) {
            similarTransform(k);
        }

        return true;
    }

    /**
     * Computes and performs the similar a transform for submatrix k.
     */
    private void similarTransform( int k) {
        float t[] = QT.data;

        // find the largest value in this column
        // this is used to normalize the column and mitigate overflow/underflow
        float max = 0;

        int rowU = (k-1)*N;

        for( int i = k; i < N; i++ ) {
            float val = Math.abs(t[rowU+i]);
            if( val > max )
                max = val;
        }

        if( max > 0 ) {
            // -------- set up the reflector Q_k

            float tau = QrHelperFunctions_FDRM.computeTauAndDivide(k, N, t, rowU, max);

            // write the reflector into the lower left column of the matrix
            float nu = t[rowU+k] + tau;
            QrHelperFunctions_FDRM.divideElements(k + 1, N, t, rowU, nu);
            t[rowU+k] = 1.0f;

            float gamma = nu/tau;
            gammas[k] = gamma;

            // ---------- Specialized householder that takes advantage of the symmetry
            householderSymmetric(k,gamma);

            // since the first element in the householder vector is known to be 1
            // store the full upper hessenberg
            t[rowU+k] = -tau*max;
        } else {
            gammas[k] = 0;
        }
    }

    /**
     * Performs the householder operations on left and right and side of the matrix.  Q<sup>T</sup>AQ
     * @param row Specifies the submatrix.
     *
     * @param gamma The gamma for the householder operation
     */
    public void householderSymmetric( int row , float gamma )
    {
        int startU = (row-1)*N;

        // compute v = -gamma*A*u
        for( int i = row; i < N; i++ ) {
            float total = 0;
            // the lower triangle is not written to so it needs to traverse upwards
            // to get the information.  Reduces the number of matrix writes need
            // improving large matrix performance
            for( int j = row; j < i; j++ ) {
                total += QT.data[j*N+i]*QT.data[startU+j];
            }
            for( int j = i; j < N; j++ ) {
                total += QT.data[i*N+j]*QT.data[startU+j];
            }
            w[i] = -gamma*total;
        }
        // alpha = -0.5f*gamma*u^T*v
        float alpha = 0;

        for( int i = row; i < N; i++ ) {
            alpha += QT.data[startU+i]*w[i];
        }
        alpha *= -0.5f*gamma;

        // w = v + alpha*u
        for( int i = row; i < N; i++ ) {
            w[i] += alpha*QT.data[startU+i];
        }
        // A = A + w*u^T + u*w^T
        for( int i = row; i < N; i++ ) {

            float ww = w[i];
            float uu = QT.data[startU+i];

            int rowA = i*N;
            for( int j = i; j < N; j++ ) {
                // only write to the upper portion of the matrix
                // this reduces the number of cache misses
                QT.data[rowA+j] += ww*QT.data[startU+j] + w[j]*uu;
            }
        }
    }


    /**
     * If needed declares and sets up internal data structures.
     *
     * @param A Matrix being decomposed.
     */
    public void init( FMatrixRMaj A ) {
        if( A.numRows != A.numCols)
            throw new IllegalArgumentException("Must be square");

        if( A.numCols != N ) {
            N = A.numCols;

            if( w.length < N ) {
                w = new float[ N ];
                gammas = new float[N];
                b = new float[N];
            }
        }

        QT = A;
    }

    @Override
    public boolean inputModified() {
        return true;
    }
}
