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

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 *
 * @author Josh Jordan
 */
public class IntObjectMap<T> {

    private static final int MAX_SEGEMENT_LENGTH = Integer.valueOf(System.getProperty("org.primitive.max.segment.length", "1000"));
    private static final int MAX_NUM_SEGEMENTS = 32;
    private final AtomicSegement<T>[] segments;
    private final AtomicInteger size;

    public IntObjectMap(int segcount) {
        segments = new AtomicSegement[segcount];
        size = new AtomicInteger();
    }

    private AtomicSegement<T> findSegement(int key){
        int index = key / segments.length;
        if (index >= 0) {
            return segments[index];
        }
        return null;
    }
    
    public boolean put(int key, T value) {
        return findSegement(key).put(key, value);
    }

    public T get(int key) {
        return findSegement(key).get(key);
    }

    public T remove(int key) {
        return findSegement(key).remove(key);
    }

    public int[] getKeys(){
        int[] keyset = new int[0];
        for(AtomicSegement segement : segments){
            
        }
        return keyset;
    }

    protected class AtomicSegement<T> {

        AtomicIntegerArray _keys;
        AtomicReferenceArray<T> _values;
        boolean[] _occupied;
        AtomicSegement<T> next;
        int length;

        AtomicSegement(int capacity) {
            _keys = new AtomicIntegerArray(capacity);
            _values = new AtomicReferenceArray<T>(capacity);
            _occupied = new boolean[capacity];
            length = capacity;
        }

        boolean put(int key, T value) {
            int i;
            int vacant = -1;
            for (i = 0; i < _keys.length(); i++) {
                if (_occupied[i]) {
                    int compare = _keys.get(i);
                    if (compare == key) {
                        _keys.getAndSet(i, key);
                        _values.getAndSet(i, value);
                        return true;
                    }
                } else {
                    vacant = i;
                }
            }

            if (i < length) {
                _keys.getAndSet(i, key);
                _values.getAndSet(i, value);
                _occupied[i] = true;
            } else {
                if (next == null) {
                    int newcapacity = length * 2;

                    if (newcapacity > MAX_SEGEMENT_LENGTH) {
                        newcapacity = MAX_SEGEMENT_LENGTH;
                    }
                    next = new AtomicSegement<T>(newcapacity);
                }
                if (!next.put(key, value)) {
                    if (vacant > -1) {
                        _keys.getAndSet(i, key);
                        _values.getAndSet(i, value);
                        _occupied[i] = true;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
            size.incrementAndGet();
            return true;
        }

        T get(int key) {
            return readOrDelete(key, false);
        }

        T remove(int key) {
            return readOrDelete(key, true);
        }

        T readOrDelete(int key, boolean delete) {
            for (int i = 0; i < _keys.length() && _occupied[i]; i++) {
                int compare = _keys.get(i);
                if (compare == key) {
                    _occupied[i] = false;
                    if (delete) {
                        size.decrementAndGet();
                        return _values.getAndSet(i, null);
                    } else {
                        return _values.get(i);
                    }
                }
            }
            if (next != null) {
                return next.remove(key);
            }
            return null;
        }

        int[] keys() {
            int[] keyset = new int[length];
            return keyset;
        }
    }
}
