/*
 * Copyright (c) 2009-2018, Peter Abeles. All Rights Reserved.
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

package org.org2.ejml.dense.row.linsol.qr;

import org.org2.ejml.data.DMatrixRMaj;
import org.org2.ejml.dense.row.CommonOps_DDRM;
import org.org2.ejml.dense.row.NormOps_DDRM;
import org.org2.ejml.dense.row.decomposition.qr.QRColPivDecompositionHouseholderColumn_DDRM;
import org.org2.ejml.interfaces.SolveNullSpace;

/**
 * <p>Uses QR decomposition to find the null-space for a matrix of any shape if the number of
 * singular values is known.=</p>
 *
 * Solves for A<sup>T</sup>=QR and the last column in Q is the null space.
 *
 * @author Peter Abeles
 */
public class SolveNullSpaceQRP_DDRM implements SolveNullSpace<DMatrixRMaj> {
    CustomizedQRP decomposition = new CustomizedQRP();

    // Storage for Q matrix
    DMatrixRMaj Q = new DMatrixRMaj(1,1);

    /**
     * Finds the null space of A
     * @param A (Input) Matrix. Modified
     * @param numSingularValues Number of singular values
     * @param nullspace Storage for null-space
     * @return true if successful or false if it failed
     */
    public boolean process(DMatrixRMaj A , int numSingularValues, DMatrixRMaj nullspace ) {
        decomposition.decompose(A);

        if( A.numRows > A.numCols ) {
            Q.reshape(A.numCols,Math.min(A.numRows,A.numCols));
            decomposition.getQ(Q, true);
        } else {
            Q.reshape(A.numCols, A.numCols);
            decomposition.getQ(Q, false);
        }

        nullspace.reshape(Q.numRows,numSingularValues);
        CommonOps_DDRM.extract(Q,0,Q.numRows,Q.numCols-numSingularValues,Q.numCols,nullspace,0,0);

        return true;
    }

    private double check(DMatrixRMaj A, DMatrixRMaj nullspace ) {
        DMatrixRMaj r = new DMatrixRMaj(A.numRows,nullspace.numCols);
        CommonOps_DDRM.mult(A,nullspace,r);

        return NormOps_DDRM.normF(r);
    }

    @Override
    public boolean inputModified() {
        return decomposition.inputModified();
    }

    /**
     * Special/Hack version of QR decomposition to avoid copying memory and pointless transposes
     */
    private static class CustomizedQRP extends QRColPivDecompositionHouseholderColumn_DDRM {

        protected void convertToColumnMajor(DMatrixRMaj A) {
            for( int x = 0; x < numCols; x++ ) {
                System.arraycopy(A.data,x*A.numCols,dataQR[x],0,numRows);
            }
        }

        /**
         * Modified decomposition which assumes the input is a transpose of the matrix.
         * The decomposition needs to be applied to the transpose of A not A. This will do that adjustment
         * inplace
         */
        @Override
        public boolean decompose( DMatrixRMaj A ) {
            // Unlike the QR decomposition the entire matrix has to be considered because any of the columns
            // could be pivoted in
            setExpectedMaxSize(A.numCols,A.numRows);

            convertToColumnMajor(A);

            // initialize pivot variables
            setupPivotInfo();

            // go through each column and perform the decomposition
            for (int j = 0; j < minLength; j++) {
                if (j > 0)
                    updateNorms(j);
                swapColumns(j);
                // if its degenerate stop processing
                if (!householderPivot(j))
                    break;
                updateA(j);
                rank = j + 1;
            }

            return true;
        }

    }

    public DMatrixRMaj getQ() {
        return Q;
    }
}
