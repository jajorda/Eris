/*
 * Copyright (c) 2012 Contributors.
 *
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 *
 *      http://eclipse.org/legal/epl-v10.html
 *
 * 
 * Contributors:
 * 
 * Josh Jordan
 *
 */
package org.eris.primitive.collections.map;

/**
 *
 * @author Josh Jordan
 */
public class IntObjectEntry<V> {

    private int key;
    private V value;
    
    public IntObjectEntry(int key, V value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IntObjectEntry<V> other = (IntObjectEntry<V>) obj;
        if (this.key != other.key) {
            return false;
        }
        if (this.value != other.value && (this.value == null || !this.value.equals(other.value))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return key;
    }
}
