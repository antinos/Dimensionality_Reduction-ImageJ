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

import org.org2.ejml.data.DMatrix1Row;
import org.org2.ejml.data.DMatrixRMaj;
import org.org2.ejml.dense.row.CommonOps_DDRM;
import org.org2.ejml.interfaces.linsol.LinearSolverDense;


/**
 * A matrix can be easily inverted by solving a system with an identify matrix.  The only
 * disadvantage of this approach is that additional computations are required compared to
 * a specialized solution.
 *
 * @author Peter Abeles
 */
public class InvertUsingSolve_DDRM {

    public static void invert(LinearSolverDense<DMatrixRMaj> solver , DMatrix1Row A , DMatrixRMaj A_inv , DMatrixRMaj storage) {

        if( A.numRows != A_inv.numRows || A.numCols != A_inv.numCols) {
            throw new IllegalArgumentException("A and A_inv must have the same dimensions");
        }

        CommonOps_DDRM.setIdentity(storage);

        solver.solve(storage,A_inv);
    }

    public static void invert(LinearSolverDense<DMatrixRMaj> solver , DMatrix1Row A , DMatrixRMaj A_inv ) {

        if( A.numRows != A_inv.numRows || A.numCols != A_inv.numCols) {
            throw new IllegalArgumentException("A and A_inv must have the same dimensions");
        }

        CommonOps_DDRM.setIdentity(A_inv);

        solver.solve(A_inv,A_inv);
    }
}
