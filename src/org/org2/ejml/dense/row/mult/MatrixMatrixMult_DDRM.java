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

package org.org2.ejml.dense.row.mult;

import org.org2.ejml.MatrixDimensionException;
import org.org2.ejml.data.DMatrix1Row;
import org.org2.ejml.dense.row.CommonOps_DDRM;

/**
 * <p>
 * This class contains various types of matrix matrix multiplication operations for {@link DMatrix1Row}.
 * </p>
 * <p>
 * Two algorithms that are equivalent can often have very different runtime performance.
 * This is because of how modern computers uses fast memory caches to speed up reading/writing to data.
 * Depending on the order in which variables are processed different algorithms can run much faster than others,
 * even if the number of operations is the same.
 * </p>
 *
 * <p>
 * Algorithms that are labeled as 'reorder' are designed to avoid caching jumping issues, some times at the cost
 * of increasing the number of operations.  This is important for large matrices.  The straight forward 
 * implementation seems to be faster for small matrices.
 * </p>
 * 
 * <p>
 * Algorithms that are labeled as 'aux' use an auxiliary array of length n.  This array is used to create
 * a copy of an out of sequence column vector that is referenced several times.  This reduces the number
 * of cache misses.  If the 'aux' parameter passed in is null then the array is declared internally.
 * </p>
 *
 * <p>
 * Typically the straight forward implementation runs about 30% faster on smaller matrices and
 * about 5 times slower on larger matrices.  This is all computer architecture and matrix shape/size specific.
 * </p>
 * 
 * <center>******** IMPORTANT **********</center>
 * This class was auto generated using org.ejml.dense.row.mult.GeneratorMatrixMatrixMult_DDRM
 * 
 * @author Peter Abeles
 */
public class MatrixMatrixMult_DDRM {
    /**
     * @see CommonOps_DDRM#mult( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void mult_reorder( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_DDRM.fill(c,0);
            return;
        }
        double valA;
        int indexCbase= 0;
        int endOfKLoop = b.numRows*b.numCols;

        for( int i = 0; i < a.numRows; i++ ) {
            int indexA = i*a.numCols;

            // need to assign c.data to a value initially
            int indexB = 0;
            int indexC = indexCbase;
            int end = indexB + b.numCols;

            valA = a.get(indexA++);

            while( indexB < end ) {
                c.set(indexC++ , valA*b.get(indexB++));
            }

            // now add to it
            while( indexB != endOfKLoop ) { // k loop
                indexC = indexCbase;
                end = indexB + b.numCols;

                valA = a.get(indexA++);

                while( indexB < end ) { // j loop
                    c.plus(indexC++ , valA*b.get(indexB++));
                }
            }
            indexCbase += c.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#mult( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void mult_small( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        int aIndexStart = 0;
        int cIndex = 0;

        for( int i = 0; i < a.numRows; i++ ) {
            for( int j = 0; j < b.numCols; j++ ) {
                double total = 0;

                int indexA = aIndexStart;
                int indexB = j;
                int end = indexA + b.numRows;
                while( indexA < end ) {
                    total += a.get(indexA++) * b.get(indexB);
                    indexB += b.numCols;
                }

                c.set( cIndex++ , total );
            }
            aIndexStart += a.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#mult( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void mult_aux( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c , double []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( aux == null ) aux = new double[ b.numRows ];

        for( int j = 0; j < b.numCols; j++ ) {
            // create a copy of the column in B to avoid cache issues
            for( int k = 0; k < b.numRows; k++ ) {
                aux[k] = b.unsafe_get(k,j);
            }

            int indexA = 0;
            for( int i = 0; i < a.numRows; i++ ) {
                double total = 0;
                for( int k = 0; k < b.numRows; ) {
                    total += a.get(indexA++)*aux[k++];
                }
                c.set( i*c.numCols+j , total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multTransA( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multTransA_reorder( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_DDRM.fill(c,0);
            return;
        }
        double valA;

        for( int i = 0; i < a.numCols; i++ ) {
            int indexC_start = i*c.numCols;

            // first assign R
            valA = a.get(i);
            int indexB = 0;
            int end = indexB+b.numCols;
            int indexC = indexC_start;
            while( indexB<end ) {
                c.set( indexC++ , valA*b.get(indexB++));
            }
            // now increment it
            for( int k = 1; k < a.numRows; k++ ) {
                valA = a.unsafe_get(k,i);
                end = indexB+b.numCols;
                indexC = indexC_start;
                // this is the loop for j
                while( indexB<end ) {
                    c.plus( indexC++ , valA*b.get(indexB++));
                }
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multTransA( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multTransA_small( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numCols);

        int cIndex = 0;

        for( int i = 0; i < a.numCols; i++ ) {
            for( int j = 0; j < b.numCols; j++ ) {
                int indexA = i;
                int indexB = j;
                int end = indexB + b.numRows*b.numCols;

                double total = 0;

                // loop for k
                for(; indexB < end; indexB += b.numCols ) {
                    total += a.get(indexA) * b.get(indexB);
                    indexA += a.numCols;
                }

                c.set( cIndex++ , total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multTransAB( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multTransAB( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        int cIndex = 0;

        for( int i = 0; i < a.numCols; i++ ) {
            int indexB = 0;
            for( int j = 0; j < b.numRows; j++ ) {
                int indexA = i;
                int end = indexB + b.numCols;

                double total = 0;

                for( ;indexB<end; ) {
                    total += a.get(indexA) * b.get(indexB++);
                    indexA += a.numCols;
                }

                c.set( cIndex++ , total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multTransAB( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multTransAB_aux( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c , double []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        if( aux == null ) aux = new double[ a.numRows ];

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_DDRM.fill(c,0);
            return;
        }
        int indexC = 0;
        for( int i = 0; i < a.numCols; i++ ) {
            for( int k = 0; k < b.numCols; k++ ) {
                aux[k] = a.unsafe_get(k,i);
            }

            for( int j = 0; j < b.numRows; j++ ) {
                double total = 0;

                for( int k = 0; k < b.numCols; k++ ) {
                    total += aux[k] * b.unsafe_get(j,k);
                }
                c.set( indexC++ , total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multTransB( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multTransB( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numRows);

        int cIndex = 0;
        int aIndexStart = 0;

        for( int xA = 0; xA < a.numRows; xA++ ) {
            int end = aIndexStart + b.numCols;
            int indexB = 0;
            for( int xB = 0; xB < b.numRows; xB++ ) {
                int indexA = aIndexStart;

                double total = 0;

                while( indexA<end ) {
                    total += a.get(indexA++) * b.get(indexB++);
                }

                c.set( cIndex++ , total );
            }
            aIndexStart += a.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#multAdd( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAdd_reorder( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            return;
        }
        double valA;
        int indexCbase= 0;
        int endOfKLoop = b.numRows*b.numCols;

        for( int i = 0; i < a.numRows; i++ ) {
            int indexA = i*a.numCols;

            // need to assign c.data to a value initially
            int indexB = 0;
            int indexC = indexCbase;
            int end = indexB + b.numCols;

            valA = a.get(indexA++);

            while( indexB < end ) {
                c.plus(indexC++ , valA*b.get(indexB++));
            }

            // now add to it
            while( indexB != endOfKLoop ) { // k loop
                indexC = indexCbase;
                end = indexB + b.numCols;

                valA = a.get(indexA++);

                while( indexB < end ) { // j loop
                    c.plus(indexC++ , valA*b.get(indexB++));
                }
            }
            indexCbase += c.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#multAdd( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAdd_small( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        int aIndexStart = 0;
        int cIndex = 0;

        for( int i = 0; i < a.numRows; i++ ) {
            for( int j = 0; j < b.numCols; j++ ) {
                double total = 0;

                int indexA = aIndexStart;
                int indexB = j;
                int end = indexA + b.numRows;
                while( indexA < end ) {
                    total += a.get(indexA++) * b.get(indexB);
                    indexB += b.numCols;
                }

                c.plus( cIndex++ , total );
            }
            aIndexStart += a.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#multAdd( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAdd_aux( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c , double []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( aux == null ) aux = new double[ b.numRows ];

        for( int j = 0; j < b.numCols; j++ ) {
            // create a copy of the column in B to avoid cache issues
            for( int k = 0; k < b.numRows; k++ ) {
                aux[k] = b.unsafe_get(k,j);
            }

            int indexA = 0;
            for( int i = 0; i < a.numRows; i++ ) {
                double total = 0;
                for( int k = 0; k < b.numRows; ) {
                    total += a.get(indexA++)*aux[k++];
                }
                c.plus( i*c.numCols+j , total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multAddTransA( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAddTransA_reorder( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            return;
        }
        double valA;

        for( int i = 0; i < a.numCols; i++ ) {
            int indexC_start = i*c.numCols;

            // first assign R
            valA = a.get(i);
            int indexB = 0;
            int end = indexB+b.numCols;
            int indexC = indexC_start;
            while( indexB<end ) {
                c.plus( indexC++ , valA*b.get(indexB++));
            }
            // now increment it
            for( int k = 1; k < a.numRows; k++ ) {
                valA = a.unsafe_get(k,i);
                end = indexB+b.numCols;
                indexC = indexC_start;
                // this is the loop for j
                while( indexB<end ) {
                    c.plus( indexC++ , valA*b.get(indexB++));
                }
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multAddTransA( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAddTransA_small( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numCols);

        int cIndex = 0;

        for( int i = 0; i < a.numCols; i++ ) {
            for( int j = 0; j < b.numCols; j++ ) {
                int indexA = i;
                int indexB = j;
                int end = indexB + b.numRows*b.numCols;

                double total = 0;

                // loop for k
                for(; indexB < end; indexB += b.numCols ) {
                    total += a.get(indexA) * b.get(indexB);
                    indexA += a.numCols;
                }

                c.plus( cIndex++ , total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multAddTransAB( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAddTransAB( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        int cIndex = 0;

        for( int i = 0; i < a.numCols; i++ ) {
            int indexB = 0;
            for( int j = 0; j < b.numRows; j++ ) {
                int indexA = i;
                int end = indexB + b.numCols;

                double total = 0;

                for( ;indexB<end; ) {
                    total += a.get(indexA) * b.get(indexB++);
                    indexA += a.numCols;
                }

                c.plus( cIndex++ , total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multAddTransAB( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAddTransAB_aux( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c , double []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        if( aux == null ) aux = new double[ a.numRows ];

        if( a.numCols == 0 || a.numRows == 0 ) {
            return;
        }
        int indexC = 0;
        for( int i = 0; i < a.numCols; i++ ) {
            for( int k = 0; k < b.numCols; k++ ) {
                aux[k] = a.unsafe_get(k,i);
            }

            for( int j = 0; j < b.numRows; j++ ) {
                double total = 0;

                for( int k = 0; k < b.numCols; k++ ) {
                    total += aux[k] * b.unsafe_get(j,k);
                }
                c.plus( indexC++ , total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multAddTransB( org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAddTransB( DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numRows);

        int cIndex = 0;
        int aIndexStart = 0;

        for( int xA = 0; xA < a.numRows; xA++ ) {
            int end = aIndexStart + b.numCols;
            int indexB = 0;
            for( int xB = 0; xB < b.numRows; xB++ ) {
                int indexA = aIndexStart;

                double total = 0;

                while( indexA<end ) {
                    total += a.get(indexA++) * b.get(indexB++);
                }

                c.plus( cIndex++ , total );
            }
            aIndexStart += a.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#mult(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void mult_reorder( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_DDRM.fill(c,0);
            return;
        }
        double valA;
        int indexCbase= 0;
        int endOfKLoop = b.numRows*b.numCols;

        for( int i = 0; i < a.numRows; i++ ) {
            int indexA = i*a.numCols;

            // need to assign c.data to a value initially
            int indexB = 0;
            int indexC = indexCbase;
            int end = indexB + b.numCols;

            valA = alpha*a.get(indexA++);

            while( indexB < end ) {
                c.set(indexC++ , valA*b.get(indexB++));
            }

            // now add to it
            while( indexB != endOfKLoop ) { // k loop
                indexC = indexCbase;
                end = indexB + b.numCols;

                valA = alpha*a.get(indexA++);

                while( indexB < end ) { // j loop
                    c.plus(indexC++ , valA*b.get(indexB++));
                }
            }
            indexCbase += c.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#mult(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void mult_small( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        int aIndexStart = 0;
        int cIndex = 0;

        for( int i = 0; i < a.numRows; i++ ) {
            for( int j = 0; j < b.numCols; j++ ) {
                double total = 0;

                int indexA = aIndexStart;
                int indexB = j;
                int end = indexA + b.numRows;
                while( indexA < end ) {
                    total += a.get(indexA++) * b.get(indexB);
                    indexB += b.numCols;
                }

                c.set( cIndex++ , alpha*total );
            }
            aIndexStart += a.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#mult(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void mult_aux( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c , double []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( aux == null ) aux = new double[ b.numRows ];

        for( int j = 0; j < b.numCols; j++ ) {
            // create a copy of the column in B to avoid cache issues
            for( int k = 0; k < b.numRows; k++ ) {
                aux[k] = b.unsafe_get(k,j);
            }

            int indexA = 0;
            for( int i = 0; i < a.numRows; i++ ) {
                double total = 0;
                for( int k = 0; k < b.numRows; ) {
                    total += a.get(indexA++)*aux[k++];
                }
                c.set( i*c.numCols+j , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multTransA(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multTransA_reorder( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_DDRM.fill(c,0);
            return;
        }
        double valA;

        for( int i = 0; i < a.numCols; i++ ) {
            int indexC_start = i*c.numCols;

            // first assign R
            valA = alpha*a.get(i);
            int indexB = 0;
            int end = indexB+b.numCols;
            int indexC = indexC_start;
            while( indexB<end ) {
                c.set( indexC++ , valA*b.get(indexB++));
            }
            // now increment it
            for( int k = 1; k < a.numRows; k++ ) {
                valA = alpha*a.unsafe_get(k,i);
                end = indexB+b.numCols;
                indexC = indexC_start;
                // this is the loop for j
                while( indexB<end ) {
                    c.plus( indexC++ , valA*b.get(indexB++));
                }
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multTransA(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multTransA_small( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numCols);

        int cIndex = 0;

        for( int i = 0; i < a.numCols; i++ ) {
            for( int j = 0; j < b.numCols; j++ ) {
                int indexA = i;
                int indexB = j;
                int end = indexB + b.numRows*b.numCols;

                double total = 0;

                // loop for k
                for(; indexB < end; indexB += b.numCols ) {
                    total += a.get(indexA) * b.get(indexB);
                    indexA += a.numCols;
                }

                c.set( cIndex++ , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multTransAB(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multTransAB( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        int cIndex = 0;

        for( int i = 0; i < a.numCols; i++ ) {
            int indexB = 0;
            for( int j = 0; j < b.numRows; j++ ) {
                int indexA = i;
                int end = indexB + b.numCols;

                double total = 0;

                for( ;indexB<end; ) {
                    total += a.get(indexA) * b.get(indexB++);
                    indexA += a.numCols;
                }

                c.set( cIndex++ , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multTransAB(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multTransAB_aux( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c , double []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        if( aux == null ) aux = new double[ a.numRows ];

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_DDRM.fill(c,0);
            return;
        }
        int indexC = 0;
        for( int i = 0; i < a.numCols; i++ ) {
            for( int k = 0; k < b.numCols; k++ ) {
                aux[k] = a.unsafe_get(k,i);
            }

            for( int j = 0; j < b.numRows; j++ ) {
                double total = 0;

                for( int k = 0; k < b.numCols; k++ ) {
                    total += aux[k] * b.unsafe_get(j,k);
                }
                c.set( indexC++ , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multTransB(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multTransB( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numRows);

        int cIndex = 0;
        int aIndexStart = 0;

        for( int xA = 0; xA < a.numRows; xA++ ) {
            int end = aIndexStart + b.numCols;
            int indexB = 0;
            for( int xB = 0; xB < b.numRows; xB++ ) {
                int indexA = aIndexStart;

                double total = 0;

                while( indexA<end ) {
                    total += a.get(indexA++) * b.get(indexB++);
                }

                c.set( cIndex++ , alpha*total );
            }
            aIndexStart += a.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#multAdd(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAdd_reorder( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            return;
        }
        double valA;
        int indexCbase= 0;
        int endOfKLoop = b.numRows*b.numCols;

        for( int i = 0; i < a.numRows; i++ ) {
            int indexA = i*a.numCols;

            // need to assign c.data to a value initially
            int indexB = 0;
            int indexC = indexCbase;
            int end = indexB + b.numCols;

            valA = alpha*a.get(indexA++);

            while( indexB < end ) {
                c.plus(indexC++ , valA*b.get(indexB++));
            }

            // now add to it
            while( indexB != endOfKLoop ) { // k loop
                indexC = indexCbase;
                end = indexB + b.numCols;

                valA = alpha*a.get(indexA++);

                while( indexB < end ) { // j loop
                    c.plus(indexC++ , valA*b.get(indexB++));
                }
            }
            indexCbase += c.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#multAdd(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAdd_small( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        int aIndexStart = 0;
        int cIndex = 0;

        for( int i = 0; i < a.numRows; i++ ) {
            for( int j = 0; j < b.numCols; j++ ) {
                double total = 0;

                int indexA = aIndexStart;
                int indexB = j;
                int end = indexA + b.numRows;
                while( indexA < end ) {
                    total += a.get(indexA++) * b.get(indexB);
                    indexB += b.numCols;
                }

                c.plus( cIndex++ , alpha*total );
            }
            aIndexStart += a.numCols;
        }
    }

    /**
     * @see CommonOps_DDRM#multAdd(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAdd_aux( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c , double []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( aux == null ) aux = new double[ b.numRows ];

        for( int j = 0; j < b.numCols; j++ ) {
            // create a copy of the column in B to avoid cache issues
            for( int k = 0; k < b.numRows; k++ ) {
                aux[k] = b.unsafe_get(k,j);
            }

            int indexA = 0;
            for( int i = 0; i < a.numRows; i++ ) {
                double total = 0;
                for( int k = 0; k < b.numRows; ) {
                    total += a.get(indexA++)*aux[k++];
                }
                c.plus( i*c.numCols+j , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multAddTransA(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAddTransA_reorder( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            return;
        }
        double valA;

        for( int i = 0; i < a.numCols; i++ ) {
            int indexC_start = i*c.numCols;

            // first assign R
            valA = alpha*a.get(i);
            int indexB = 0;
            int end = indexB+b.numCols;
            int indexC = indexC_start;
            while( indexB<end ) {
                c.plus( indexC++ , valA*b.get(indexB++));
            }
            // now increment it
            for( int k = 1; k < a.numRows; k++ ) {
                valA = alpha*a.unsafe_get(k,i);
                end = indexB+b.numCols;
                indexC = indexC_start;
                // this is the loop for j
                while( indexB<end ) {
                    c.plus( indexC++ , valA*b.get(indexB++));
                }
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multAddTransA(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAddTransA_small( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numCols);

        int cIndex = 0;

        for( int i = 0; i < a.numCols; i++ ) {
            for( int j = 0; j < b.numCols; j++ ) {
                int indexA = i;
                int indexB = j;
                int end = indexB + b.numRows*b.numCols;

                double total = 0;

                // loop for k
                for(; indexB < end; indexB += b.numCols ) {
                    total += a.get(indexA) * b.get(indexB);
                    indexA += a.numCols;
                }

                c.plus( cIndex++ , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multAddTransAB(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAddTransAB( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        int cIndex = 0;

        for( int i = 0; i < a.numCols; i++ ) {
            int indexB = 0;
            for( int j = 0; j < b.numRows; j++ ) {
                int indexA = i;
                int end = indexB + b.numCols;

                double total = 0;

                for( ;indexB<end; ) {
                    total += a.get(indexA) * b.get(indexB++);
                    indexA += a.numCols;
                }

                c.plus( cIndex++ , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multAddTransAB(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAddTransAB_aux( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c , double []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        if( aux == null ) aux = new double[ a.numRows ];

        if( a.numCols == 0 || a.numRows == 0 ) {
            return;
        }
        int indexC = 0;
        for( int i = 0; i < a.numCols; i++ ) {
            for( int k = 0; k < b.numCols; k++ ) {
                aux[k] = a.unsafe_get(k,i);
            }

            for( int j = 0; j < b.numRows; j++ ) {
                double total = 0;

                for( int k = 0; k < b.numCols; k++ ) {
                    total += aux[k] * b.unsafe_get(j,k);
                }
                c.plus( indexC++ , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_DDRM#multAddTransB(double,  org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row, org.org2.ejml.data.DMatrix1Row)
     */
    public static void multAddTransB( double alpha , DMatrix1Row a , DMatrix1Row b , DMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numRows);

        int cIndex = 0;
        int aIndexStart = 0;

        for( int xA = 0; xA < a.numRows; xA++ ) {
            int end = aIndexStart + b.numCols;
            int indexB = 0;
            for( int xB = 0; xB < b.numRows; xB++ ) {
                int indexA = aIndexStart;

                double total = 0;

                while( indexA<end ) {
                    total += a.get(indexA++) * b.get(indexB++);
                }

                c.plus( cIndex++ , alpha*total );
            }
            aIndexStart += a.numCols;
        }
    }

}

