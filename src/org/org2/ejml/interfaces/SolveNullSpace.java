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

package org.org2.ejml.interfaces;

import org.org2.ejml.data.Matrix;

/**
 * Finds the nullspace for a matrix given the number of singular values
 *
 * @author Peter Abeles
 */
public interface SolveNullSpace<T extends Matrix> {
    /**
     * Finds the nullspace inside of input
     *
     * @param input (Input) input matrix. Maybe modified
     * @param numberOfSingular Number of singular values in the input
     * @param nullspace (Output) storage for null space
     * @return true if successful or false if it failed
     */
    boolean process( T input , int numberOfSingular , T nullspace );

    /**
     * Returns true if the input matrix is modified
     */
    boolean inputModified();
}
