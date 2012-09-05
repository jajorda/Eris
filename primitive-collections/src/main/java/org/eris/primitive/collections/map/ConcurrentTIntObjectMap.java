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


import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Josh Jordan
 */
public class ConcurrentTIntObjectMap<T> {

    private static final int MAX_NUM_SEGEMENTS = 1 << 16;
    private static final int DEFAULT_NUM_SEGMENTS = 16;
    private ConcurrentTIntObjectMap.Segment<T>[] segments;

    public ConcurrentTIntObjectMap(int segcount) {
        segments = new ConcurrentTIntObjectMap.Segment[segcount];
    }

    /**
     * Applies a supplemental hash function to a given hashCode, which defends
     * against poor quality hash functions. This is critical because
     * ConcurrentHashMap uses power-of-two length hash tables, that otherwise
     * encounter collisions for hashCodes that do not differ in lower or upper
     * bits.
     */
    private static int hash(int h) {
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
//        h += (h << 15) ^ 0xffffcd7d;
//        h ^= (h >>> 10);
//        h += (h << 3);
//        h ^= (h >>> 6);
//        h += (h << 2) + (h << 14);
//        return h ^ (h >>> 16);
        return h;
    }

    /**
     * Get the segment for the given hash
     */
    @SuppressWarnings("unchecked")
    private ConcurrentTIntObjectMap.Segment<T> segmentForHash(int h) {
        int index = h & (segments.length - 1);
        ConcurrentTIntObjectMap.Segment<T> seg = segments[index];
        if (seg == null) {
            seg = new ConcurrentTIntObjectMap.Segment<T>();
            segments[index] = seg;
        }

        return seg;
    }

    public T put(int key, T value) {
        int hash = hash(key);
        return segmentForHash(hash).put(key, value);
    }

    public T get(int key) {
        int hash = hash(key);
        return segmentForHash(hash).get(key);
    }

    public T remove(int key) {
        int hash = hash(key);
        return segmentForHash(hash).remove(key);
    }

    public int[] getKeys() {
        int[] keyset = new int[0];
        for (ConcurrentTIntObjectMap.Segment segement : segments) {
        }
        return keyset;
    }

    public int size() {
        return 0;
    }

    protected static class Segment<T> extends ReentrantReadWriteLock {

        TIntObjectMap<T> _table;

        Segment() {
            _table = new TIntObjectHashMap();
        }

        T put(int key, T value) {
            try {
                writeLock().lock();
                return _table.put(key, value);
            } finally {
                writeLock().unlock();
            }
        }

        T get(int key) {
            try {
                readLock().lock();
                return _table.get(key);
            } finally {
                readLock().unlock();
            }            
        }

        T remove(int key) {
            try {
                writeLock().lock();
                return _table.remove(key);
            } finally {
                writeLock().unlock();
            } 
        }
    }
}
