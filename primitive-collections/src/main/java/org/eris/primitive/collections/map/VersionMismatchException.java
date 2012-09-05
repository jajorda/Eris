/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eris.primitive.collections.map;

/**
 *
 * @author Josh Jordan
 */
public class VersionMismatchException extends Exception {

    /**
     * Creates a new instance of
     * <code>VersionMismatchException</code> without detail message.
     */
    public VersionMismatchException() {
    }

    /**
     * Constructs an instance of
     * <code>VersionMismatchException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public VersionMismatchException(String msg) {
        super(msg);
    }
}
