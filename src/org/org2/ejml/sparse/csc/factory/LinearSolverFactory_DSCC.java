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

package org.org2.ejml.sparse.csc.factory;

import org.org2.ejml.data.DMatrixRMaj;
import org.org2.ejml.data.DMatrixSparseCSC;
import org.org2.ejml.interfaces.linsol.LinearSolverSparse;
import org.org2.ejml.sparse.ComputePermutation;
import org.org2.ejml.sparse.FillReducing;
import org.org2.ejml.sparse.csc.decomposition.chol.CholeskyUpLooking_DSCC;
import org.org2.ejml.sparse.csc.decomposition.lu.LuUpLooking_DSCC;
import org.org2.ejml.sparse.csc.decomposition.qr.QrLeftLookingDecomposition_DSCC;
import org.org2.ejml.sparse.csc.linsol.chol.LinearSolverCholesky_DSCC;
import org.org2.ejml.sparse.csc.linsol.lu.LinearSolverLu_DSCC;
import org.org2.ejml.sparse.csc.linsol.qr.LinearSolverQrLeftLooking_DSCC;

/**
 * Factory for sparse linear solvers
 *
 * @author Peter Abeles
 */
public class LinearSolverFactory_DSCC {
    public static LinearSolverSparse<DMatrixSparseCSC,DMatrixRMaj> cholesky(FillReducing permutation) {
        ComputePermutation<DMatrixSparseCSC> cp = FillReductionFactory_DSCC.create(permutation);
        CholeskyUpLooking_DSCC chol = (CholeskyUpLooking_DSCC)DecompositionFactory_DSCC.cholesky();
        return new LinearSolverCholesky_DSCC(chol,cp);
    }

    public static LinearSolverSparse<DMatrixSparseCSC,DMatrixRMaj> qr(FillReducing permutation) {
        ComputePermutation<DMatrixSparseCSC> cp = FillReductionFactory_DSCC.create(permutation);
        QrLeftLookingDecomposition_DSCC qr = new QrLeftLookingDecomposition_DSCC(cp);
        return new LinearSolverQrLeftLooking_DSCC(qr);
    }

    public static LinearSolverSparse<DMatrixSparseCSC,DMatrixRMaj> lu(FillReducing permutation) {
        ComputePermutation<DMatrixSparseCSC> cp = FillReductionFactory_DSCC.create(permutation);
        LuUpLooking_DSCC lu = new LuUpLooking_DSCC(cp);
        return new LinearSolverLu_DSCC(lu);
    }
}
