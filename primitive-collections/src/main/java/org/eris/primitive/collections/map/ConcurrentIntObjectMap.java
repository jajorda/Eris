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

import gnu.trove.impl.PrimeFinder;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Josh Jordan
 */
public class ConcurrentIntObjectMap<T> {

    private static final int MAX_SEGEMENT_LENGTH = Integer.valueOf(System.getProperty("org.primitive.max.segment.length", "200000"));
    private static final int MAX_NUM_SEGEMENTS = 64;
    private static final int STARTING_SEGEMENT_LENGTH = 16;
    private static final int DEFAULT_NUM_SEGMENTS = 16;
    private final Segment<T>[] segments;
    private final boolean lockfree;
    private final int initialcapacity;
    private static final float DEFAULT_LOAD_FACTOR = .70f;

    public ConcurrentIntObjectMap(int segcount) {
        this(segcount, STARTING_SEGEMENT_LENGTH, true);
    }

    public ConcurrentIntObjectMap(int segcount, int initialcapacity) {
        this(segcount, initialcapacity, true);
    }

    public ConcurrentIntObjectMap(int segcount, boolean lockfree) {
        this(segcount, STARTING_SEGEMENT_LENGTH, lockfree);
    }

    public ConcurrentIntObjectMap(int segcount, int initialcapacity, boolean lockfree) {
        segments = new Segment[segcount];
        this.lockfree = lockfree;
        this.initialcapacity = initialcapacity;
    }

    private Segment<T> findSegement(int hash) {
        int index = hash % segments.length;
        if (index >= 0) {
            Segment<T> segment = segments[index];
            if (segment == null) {
                segment = new Segment<T>(PrimeFinder.nextPrime(initialcapacity), DEFAULT_LOAD_FACTOR);
                segments[index] = segment;
            }
            return segment;
        }
        return null;
    }

    public T put(int key, T value) {
        int hash = key & 0x7fffffff;
        return findSegement(hash).put(hash, key, value);
    }

    public T get(int key) {
        int hash = key & 0x7fffffff;
        return findSegement(hash).get(hash, key);
    }

    public T remove(int key) {
        return findSegement(key).remove(key);
    }

    public int[] getKeys() {
        int[] keyset = new int[0];
        for (Segment segment : segments) {
        }
        return keyset;
    }

    public void printStats() {
        for (Segment segment : segments) {
            long avg = segment.total;
            if (segment.cnt > 0) {
                avg = segment.total / segment.cnt;
            }
            System.out.println(String.format("Segment size: %d\trehases: %d\ttotal: %d\t avg: %d\tversion: %d\toverage: %d", segment._size, segment.cnt, segment.total, avg, segment._version, segment.overage));
        }
    }

    ;
    protected class Segment<T> extends ReentrantLock {

        // take from trove4j TIntHash
        public static final byte FREE = 0;
        public static final byte FULL = 1;
        public static final byte REMOVED = 2;
        T[] _values;
        int[] _keys;
        byte[] _occupied;
        int _size;
        int maxsize;
        final float loadfactor;
        int _version;
        int cnt = 0;
        long total = 0;
        int overage = 0;

        Segment(int capacity, float loadfactor) {
            _size = 0;
            this.loadfactor = loadfactor;
            maxsize = PrimeFinder.nextPrime((int) (loadfactor * capacity));
            _values = (T[]) new Object[maxsize];
            _keys = new int[maxsize];
            _occupied = new byte[maxsize];
            _version = 1;
        }

        T put(int hash, int key, T value) {
            while (true) {
                int version = _version;
                int index = findIndexForUpdate(hash, key, _keys, _occupied);
                try {
                    lock();
                    if (version == _version) {
                        T rtn = _values[index];
                        _values[index] = value;
                        _keys[index] = key;
                        if (_occupied[index] == FREE) {
                            _size++;
                            _occupied[index] = FULL;
                        }
                        needsRehashing(_size);
                        return rtn;
                    }
                } finally {
                    unlock();
                }
            }
        }

        T remove(int key) {
            return null;
        }

        int hash(int key) {
            return key & 0x7fffffff;
        }

        T get(int key) {
            int hash = hash(key);
            return get(hash, key);
        }

        T get(int hash, int key) {
            while (true) {
                int version = _version;
                int index = hash % maxsize;
                int probe = 0;
                final int loopIndex = index;

                do {
                    if (_occupied[index] == FREE) {
                        if (version == _version) {
                            return null;
                        }
                        break;
                    }

                    if (_keys[index] == key) {
                        if (_occupied[index] == FULL) {
                            if (version == _version) {
                                return _values[index];
                            }
                        }
                        if (version == _version) {
                            return null;
                        }
                        break;
                    }
                    if (probe == 0) {
                        probe = 1 + (hash % (maxsize - 2));
                    }

                    index -= probe;
                    if (index < 0) {
                        index += maxsize;
                    }
                } while (index != loopIndex);

                return null;
            }
        }

        int findIndexForUpdate(int key, int[] keys, byte[] occupied) {
            int hash = hash(key);
            return findIndexForUpdate(hash, key, keys, occupied);
        }

        int findIndexForUpdate(int hash, int key, int[] keys, byte[] occupied) {

            int length = keys.length;
            int index = hash % length;
            int probe = 0;
            final int loopIndex = index;
            int loops = 0;

            do {
                loops++;
                if (occupied[index] == FREE) {
                    return index;
                }

                if (occupied[index] == FULL && keys[index] == key) {
                    return index;
                }
                if (probe == 0) {
                    probe = 1 + (hash % (length - 2));
                }

                index -= probe;
                if (index < 0) {
                    index += length;
                }
            } while (index != loopIndex);
            return -1;
        }

        void needsRehashing(int size) {
            long start = 0;
            int oldsize = maxsize;
            if (size == _size && size >= oldsize) {
                if (oldsize == maxsize) {
                    int newsize;
                    if (2 * maxsize > Integer.MAX_VALUE) {
                        newsize = Integer.MAX_VALUE;
                    } else {
                        int prime = PrimeFinder.nextPrime(maxsize << 1);
                        
                        newsize = prime;
                    }
                    T[] newvalues = (T[]) new Object[newsize];
                    int[] newkeys = new int[newsize];
                    byte[] newoccupied = new byte[newsize];
                    for (int i = maxsize; i-- > 0;) {
                        if (_occupied[i] == FULL) {
                            int index = findIndexForUpdate(_keys[i], newkeys, newoccupied);
                            newkeys[index] = _keys[i];
                            newvalues[index] = _values[i];
                            newoccupied[index] = FULL;
                        }
                    }

                    _version++;
                    maxsize = newsize;
                    _values = newvalues;
                    _keys = newkeys;
                    _occupied = newoccupied;
                }

            }
        }

        @Override
        public void lock() {
            super.lock();
        }

        @Override
        public void unlock() {
            super.unlock();
        }
    }
}
