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

package org.org2.ejml.dense.row.linsol;

import org.org2.ejml.data.DMatrixRMaj;
import org.org2.ejml.dense.row.misc.UnrolledInverseFromMinor_DDRM;
import org.org2.ejml.interfaces.decomposition.DecompositionInterface;
import org.org2.ejml.interfaces.linsol.LinearSolverDense;


/**
 * Solver which uses an unrolled inverse to compute the inverse.  This can only invert matrices and not solve.
 * This is faster than LU inverse but only supports small matrices..
 *
 * @author Peter Abeles
 */
public class LinearSolverUnrolled_DDRM implements LinearSolverDense<DMatrixRMaj> {
    DMatrixRMaj A;

    @Override
    public boolean setA(DMatrixRMaj A) {
        if( A.numRows != A.numCols)
            return false;

        this.A = A;
        return A.numRows <= UnrolledInverseFromMinor_DDRM.MAX;
    }

    @Override
    public /**/double quality() {
        throw new IllegalArgumentException("Not supported by this solver.");
    }

    @Override
    public void solve(DMatrixRMaj B, DMatrixRMaj X) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void invert(DMatrixRMaj A_inv) {
        if( A.numRows == 1 )
            A_inv.set(0,  1.0/A.get(0));
        UnrolledInverseFromMinor_DDRM.inv(A,A_inv);
    }

    @Override
    public boolean modifiesA() {
        return false;
    }

    @Override
    public boolean modifiesB() {
        return false;
    }

    @Override
    public <D extends DecompositionInterface> D getDecomposition() {
        return null;
    }
}
