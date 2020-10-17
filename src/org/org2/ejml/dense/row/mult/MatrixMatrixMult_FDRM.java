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
import org.org2.ejml.data.FMatrix1Row;
import org.org2.ejml.dense.row.CommonOps_FDRM;

/**
 * <p>
 * This class contains various types of matrix matrix multiplication operations for {@link FMatrix1Row}.
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
 * This class was auto generated using org.ejml.dense.row.mult.GeneratorMatrixMatrixMult_FDRM
 * 
 * @author Peter Abeles
 */
public class MatrixMatrixMult_FDRM {
    /**
     * @see CommonOps_FDRM#mult( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void mult_reorder( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_FDRM.fill(c,0);
            return;
        }
        float valA;
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
     * @see CommonOps_FDRM#mult( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void mult_small( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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
                float total = 0;

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
     * @see CommonOps_FDRM#mult( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void mult_aux( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c , float []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( aux == null ) aux = new float[ b.numRows ];

        for( int j = 0; j < b.numCols; j++ ) {
            // create a copy of the column in B to avoid cache issues
            for( int k = 0; k < b.numRows; k++ ) {
                aux[k] = b.unsafe_get(k,j);
            }

            int indexA = 0;
            for( int i = 0; i < a.numRows; i++ ) {
                float total = 0;
                for( int k = 0; k < b.numRows; ) {
                    total += a.get(indexA++)*aux[k++];
                }
                c.set( i*c.numCols+j , total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multTransA( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multTransA_reorder( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_FDRM.fill(c,0);
            return;
        }
        float valA;

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
     * @see CommonOps_FDRM#multTransA( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multTransA_small( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

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
     * @see CommonOps_FDRM#multTransAB( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multTransAB( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

                for( ;indexB<end; ) {
                    total += a.get(indexA) * b.get(indexB++);
                    indexA += a.numCols;
                }

                c.set( cIndex++ , total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multTransAB( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multTransAB_aux( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c , float []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        if( aux == null ) aux = new float[ a.numRows ];

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_FDRM.fill(c,0);
            return;
        }
        int indexC = 0;
        for( int i = 0; i < a.numCols; i++ ) {
            for( int k = 0; k < b.numCols; k++ ) {
                aux[k] = a.unsafe_get(k,i);
            }

            for( int j = 0; j < b.numRows; j++ ) {
                float total = 0;

                for( int k = 0; k < b.numCols; k++ ) {
                    total += aux[k] * b.unsafe_get(j,k);
                }
                c.set( indexC++ , total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multTransB( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multTransB( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

                while( indexA<end ) {
                    total += a.get(indexA++) * b.get(indexB++);
                }

                c.set( cIndex++ , total );
            }
            aIndexStart += a.numCols;
        }
    }

    /**
     * @see CommonOps_FDRM#multAdd( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAdd_reorder( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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
        float valA;
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
     * @see CommonOps_FDRM#multAdd( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAdd_small( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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
                float total = 0;

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
     * @see CommonOps_FDRM#multAdd( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAdd_aux( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c , float []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( aux == null ) aux = new float[ b.numRows ];

        for( int j = 0; j < b.numCols; j++ ) {
            // create a copy of the column in B to avoid cache issues
            for( int k = 0; k < b.numRows; k++ ) {
                aux[k] = b.unsafe_get(k,j);
            }

            int indexA = 0;
            for( int i = 0; i < a.numRows; i++ ) {
                float total = 0;
                for( int k = 0; k < b.numRows; ) {
                    total += a.get(indexA++)*aux[k++];
                }
                c.plus( i*c.numCols+j , total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multAddTransA( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAddTransA_reorder( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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
        float valA;

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
     * @see CommonOps_FDRM#multAddTransA( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAddTransA_small( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

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
     * @see CommonOps_FDRM#multAddTransAB( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAddTransAB( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

                for( ;indexB<end; ) {
                    total += a.get(indexA) * b.get(indexB++);
                    indexA += a.numCols;
                }

                c.plus( cIndex++ , total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multAddTransAB( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAddTransAB_aux( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c , float []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        if( aux == null ) aux = new float[ a.numRows ];

        if( a.numCols == 0 || a.numRows == 0 ) {
            return;
        }
        int indexC = 0;
        for( int i = 0; i < a.numCols; i++ ) {
            for( int k = 0; k < b.numCols; k++ ) {
                aux[k] = a.unsafe_get(k,i);
            }

            for( int j = 0; j < b.numRows; j++ ) {
                float total = 0;

                for( int k = 0; k < b.numCols; k++ ) {
                    total += aux[k] * b.unsafe_get(j,k);
                }
                c.plus( indexC++ , total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multAddTransB( org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAddTransB( FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

                while( indexA<end ) {
                    total += a.get(indexA++) * b.get(indexB++);
                }

                c.plus( cIndex++ , total );
            }
            aIndexStart += a.numCols;
        }
    }

    /**
     * @see CommonOps_FDRM#mult(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void mult_reorder( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_FDRM.fill(c,0);
            return;
        }
        float valA;
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
     * @see CommonOps_FDRM#mult(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void mult_small( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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
                float total = 0;

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
     * @see CommonOps_FDRM#mult(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void mult_aux( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c , float []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( aux == null ) aux = new float[ b.numRows ];

        for( int j = 0; j < b.numCols; j++ ) {
            // create a copy of the column in B to avoid cache issues
            for( int k = 0; k < b.numRows; k++ ) {
                aux[k] = b.unsafe_get(k,j);
            }

            int indexA = 0;
            for( int i = 0; i < a.numRows; i++ ) {
                float total = 0;
                for( int k = 0; k < b.numRows; ) {
                    total += a.get(indexA++)*aux[k++];
                }
                c.set( i*c.numCols+j , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multTransA(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multTransA_reorder( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numCols);

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_FDRM.fill(c,0);
            return;
        }
        float valA;

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
     * @see CommonOps_FDRM#multTransA(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multTransA_small( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

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
     * @see CommonOps_FDRM#multTransAB(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multTransAB( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

                for( ;indexB<end; ) {
                    total += a.get(indexA) * b.get(indexB++);
                    indexA += a.numCols;
                }

                c.set( cIndex++ , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multTransAB(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multTransAB_aux( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c , float []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        if( aux == null ) aux = new float[ a.numRows ];

        if( a.numCols == 0 || a.numRows == 0 ) {
            CommonOps_FDRM.fill(c,0);
            return;
        }
        int indexC = 0;
        for( int i = 0; i < a.numCols; i++ ) {
            for( int k = 0; k < b.numCols; k++ ) {
                aux[k] = a.unsafe_get(k,i);
            }

            for( int j = 0; j < b.numRows; j++ ) {
                float total = 0;

                for( int k = 0; k < b.numCols; k++ ) {
                    total += aux[k] * b.unsafe_get(j,k);
                }
                c.set( indexC++ , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multTransB(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multTransB( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

                while( indexA<end ) {
                    total += a.get(indexA++) * b.get(indexB++);
                }

                c.set( cIndex++ , alpha*total );
            }
            aIndexStart += a.numCols;
        }
    }

    /**
     * @see CommonOps_FDRM#multAdd(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAdd_reorder( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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
        float valA;
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
     * @see CommonOps_FDRM#multAdd(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAdd_small( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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
                float total = 0;

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
     * @see CommonOps_FDRM#multAdd(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAdd_aux( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c , float []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numCols != b.numRows ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numRows,b.numCols);

        if( aux == null ) aux = new float[ b.numRows ];

        for( int j = 0; j < b.numCols; j++ ) {
            // create a copy of the column in B to avoid cache issues
            for( int k = 0; k < b.numRows; k++ ) {
                aux[k] = b.unsafe_get(k,j);
            }

            int indexA = 0;
            for( int i = 0; i < a.numRows; i++ ) {
                float total = 0;
                for( int k = 0; k < b.numRows; ) {
                    total += a.get(indexA++)*aux[k++];
                }
                c.plus( i*c.numCols+j , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multAddTransA(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAddTransA_reorder( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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
        float valA;

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
     * @see CommonOps_FDRM#multAddTransA(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAddTransA_small( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

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
     * @see CommonOps_FDRM#multAddTransAB(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAddTransAB( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

                for( ;indexB<end; ) {
                    total += a.get(indexA) * b.get(indexB++);
                    indexA += a.numCols;
                }

                c.plus( cIndex++ , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multAddTransAB(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAddTransAB_aux( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c , float []aux )
    {
        if( a == c || b == c )
            throw new IllegalArgumentException("Neither 'a' or 'b' can be the same matrix as 'c'");
        else if( a.numRows != b.numCols ) {
            throw new MatrixDimensionException("The 'a' and 'b' matrices do not have compatible dimensions");
        }
        c.reshape(a.numCols,b.numRows);

        if( aux == null ) aux = new float[ a.numRows ];

        if( a.numCols == 0 || a.numRows == 0 ) {
            return;
        }
        int indexC = 0;
        for( int i = 0; i < a.numCols; i++ ) {
            for( int k = 0; k < b.numCols; k++ ) {
                aux[k] = a.unsafe_get(k,i);
            }

            for( int j = 0; j < b.numRows; j++ ) {
                float total = 0;

                for( int k = 0; k < b.numCols; k++ ) {
                    total += aux[k] * b.unsafe_get(j,k);
                }
                c.plus( indexC++ , alpha*total );
            }
        }
    }

    /**
     * @see CommonOps_FDRM#multAddTransB(float,  org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row, org.org2.ejml.data.FMatrix1Row)
     */
    public static void multAddTransB( float alpha , FMatrix1Row a , FMatrix1Row b , FMatrix1Row c )
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

                float total = 0;

                while( indexA<end ) {
                    total += a.get(indexA++) * b.get(indexB++);
                }

                c.plus( cIndex++ , alpha*total );
            }
            aIndexStart += a.numCols;
        }
    }

}

