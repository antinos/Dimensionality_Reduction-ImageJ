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

import org.org2.ejml.data.FMatrixRMaj;
import org.org2.ejml.data.FMatrixSparseCSC;
import org.org2.ejml.interfaces.linsol.LinearSolverSparse;
import org.org2.ejml.sparse.ComputePermutation;
import org.org2.ejml.sparse.FillReducing;
import org.org2.ejml.sparse.csc.decomposition.chol.CholeskyUpLooking_FSCC;
import org.org2.ejml.sparse.csc.decomposition.lu.LuUpLooking_FSCC;
import org.org2.ejml.sparse.csc.decomposition.qr.QrLeftLookingDecomposition_FSCC;
import org.org2.ejml.sparse.csc.linsol.chol.LinearSolverCholesky_FSCC;
import org.org2.ejml.sparse.csc.linsol.lu.LinearSolverLu_FSCC;
import org.org2.ejml.sparse.csc.linsol.qr.LinearSolverQrLeftLooking_FSCC;

/**
 * Factory for sparse linear solvers
 *
 * @author Peter Abeles
 */
public class LinearSolverFactory_FSCC {
    public static LinearSolverSparse<FMatrixSparseCSC,FMatrixRMaj> cholesky(FillReducing permutation) {
        ComputePermutation<FMatrixSparseCSC> cp = FillReductionFactory_FSCC.create(permutation);
        CholeskyUpLooking_FSCC chol = (CholeskyUpLooking_FSCC)DecompositionFactory_FSCC.cholesky();
        return new LinearSolverCholesky_FSCC(chol,cp);
    }

    public static LinearSolverSparse<FMatrixSparseCSC,FMatrixRMaj> qr(FillReducing permutation) {
        ComputePermutation<FMatrixSparseCSC> cp = FillReductionFactory_FSCC.create(permutation);
        QrLeftLookingDecomposition_FSCC qr = new QrLeftLookingDecomposition_FSCC(cp);
        return new LinearSolverQrLeftLooking_FSCC(qr);
    }

    public static LinearSolverSparse<FMatrixSparseCSC,FMatrixRMaj> lu(FillReducing permutation) {
        ComputePermutation<FMatrixSparseCSC> cp = FillReductionFactory_FSCC.create(permutation);
        LuUpLooking_FSCC lu = new LuUpLooking_FSCC(cp);
        return new LinearSolverLu_FSCC(lu);
    }
}
