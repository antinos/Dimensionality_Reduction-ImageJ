/*
 * Copyright (c) 2009-2019, Peter Abeles. All Rights Reserved.
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

package org.org2.ejml.data;

import org.org2.ejml.ops.MatrixIO;

/**
 * Fixed sized vector with 6 elements.  Can represent a 6 x 1 or 1 x 6 matrix, context dependent.
 * <p>DO NOT MODIFY.  Automatically generated code created by GenerateMatrixFixedN</p>
 *
 * @author Peter Abeles
 */
public class DMatrix6 implements DMatrixFixed {
    public double a1,a2,a3,a4,a5,a6;

    public DMatrix6() {
    }

    public DMatrix6(double a1, double a2, double a3, double a4, double a5, double a6)
    {
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.a4 = a4;
        this.a5 = a5;
        this.a6 = a6;
    }

    public DMatrix6(DMatrix6 o) {
        this.a1 = o.a1;
        this.a2 = o.a2;
        this.a3 = o.a3;
        this.a4 = o.a4;
        this.a5 = o.a5;
        this.a6 = o.a6;
    }

    @Override
    public void zero() {
        a1 = 0.0;
        a2 = 0.0;
        a3 = 0.0;
        a4 = 0.0;
        a5 = 0.0;
        a6 = 0.0;
    }

    public void set(double a1, double a2, double a3, double a4, double a5, double a6)
    {
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.a4 = a4;
        this.a5 = a5;
        this.a6 = a6;
    }

    public void set( int offset , double array[] ) {
        this.a1 = array[offset+0];
        this.a2 = array[offset+1];
        this.a3 = array[offset+2];
        this.a4 = array[offset+3];
        this.a5 = array[offset+4];
        this.a6 = array[offset+5];
    }

    @Override
    public double get(int row, int col) {
        return unsafe_get(row,col);
    }

    @Override
    public double unsafe_get(int row, int col) {
        if( row != 0 && col != 0 )
            throw new IllegalArgumentException("Row or column must be zero since this is a vector");

        int w = Math.max(row,col);

        if( w == 0 ) {
            return a1;
        } else if( w == 1 ) {
            return a2;
        } else if( w == 2 ) {
            return a3;
        } else if( w == 3 ) {
            return a4;
        } else if( w == 4 ) {
            return a5;
        } else if( w == 5 ) {
            return a6;
        } else {
            throw new IllegalArgumentException("Out of range.  "+w);
        }
    }

    @Override
    public void set(int row, int col, double val) {
        unsafe_set(row,col,val);
    }

    @Override
    public void unsafe_set(int row, int col, double val) {
        if( row != 0 && col != 0 )
            throw new IllegalArgumentException("Row or column must be zero since this is a vector");

        int w = Math.max(row,col);

        if( w == 0 ) {
            a1 = val;
        } else if( w == 1 ) {
            a2 = val;
        } else if( w == 2 ) {
            a3 = val;
        } else if( w == 3 ) {
            a4 = val;
        } else if( w == 4 ) {
            a5 = val;
        } else if( w == 5 ) {
            a6 = val;
        } else {
            throw new IllegalArgumentException("Out of range.  "+w);
        }
    }

    @Override
    public void set(Matrix original) {
        DMatrix m = (DMatrix)original;

        if( m.getNumCols() == 1 && m.getNumRows() == 6 ) {
            a1 = m.get(0,0);
            a2 = m.get(1,0);
            a3 = m.get(2,0);
            a4 = m.get(3,0);
            a5 = m.get(4,0);
            a6 = m.get(5,0);
        } else if( m.getNumRows() == 1 && m.getNumCols() == 6 ){
            a1 = m.get(0,0);
            a2 = m.get(0,1);
            a3 = m.get(0,2);
            a4 = m.get(0,3);
            a5 = m.get(0,4);
            a6 = m.get(0,5);
        } else {
            throw new IllegalArgumentException("Incompatible shape");
        }
    }

    @Override
    public int getNumRows() {
        return 6;
    }

    @Override
    public int getNumCols() {
        return 1;
    }

    @Override
    public int getNumElements() {
        return 6;
    }

    @Override
    public <T extends Matrix> T copy() {
        return (T)new DMatrix6(this);
    }

    @Override
    public void print() {
       MatrixIO.printFancy(System.out, this, MatrixIO.DEFAULT_LENGTH);
    }

    @Override
    public void print( String format ) {
        MatrixIO.print(System.out, this, format);
    }

    @Override
    public <T extends Matrix> T createLike() {
        return (T)new DMatrix6();
    }

    @Override
    public MatrixType getType() {
        return MatrixType.UNSPECIFIED;
    }}

