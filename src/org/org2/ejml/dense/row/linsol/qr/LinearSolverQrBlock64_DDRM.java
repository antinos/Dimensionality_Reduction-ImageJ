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

package org.org2.ejml.dense.row.linsol.qr;

import org.org2.ejml.data.DMatrixRMaj;
import org.org2.ejml.dense.block.linsol.qr.QrHouseHolderSolver_DDRB;
import org.org2.ejml.dense.row.linsol.LinearSolver_DDRB_to_DDRM;


/**
 * Wrapper around {@link QrHouseHolderSolver_DDRB} that allows it to process
 * {@link DMatrixRMaj}.
 *
 * @author Peter Abeles
 */
public class LinearSolverQrBlock64_DDRM extends LinearSolver_DDRB_to_DDRM {

    public LinearSolverQrBlock64_DDRM() {
        super(new QrHouseHolderSolver_DDRB());
    }
}
