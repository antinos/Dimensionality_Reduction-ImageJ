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

package org.org2.ejml.interfaces.linsol;

import org.org2.ejml.data.Matrix;

/**
 * <p>Implementation of {@link ReducedRowEchelonForm} for 64-bit floats </p>
 *
 * @author Peter Abeles
 */
public interface ReducedRowEchelonForm_F64<T extends Matrix> extends ReducedRowEchelonForm<T> {

    /**
     * Specifies tolerance for determining if the system is singular and it should stop processing.
     * A reasonable value is: tol = EPS/max(||tol||).
     *
     * @param tol Tolerance for singular matrix. A reasonable value is: tol = EPS/max(||tol||). Or just set to zero.
     */
    void setTolerance(double tol);
}
