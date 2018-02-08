package org.rohub.rodl.storage;


import org.rohub.rodl.dl.DigitalLibrary;

/**
 * A factory producing digital library (storage) instances.
 * 
 * @author piotrekhol
 * 
 */
public interface DigitalLibraryFactory {

    /**
     * Return a new or existing digital library instance, in particular dLibra or filesystem DL.
     * 
     * @return a digital library
     */
    DigitalLibrary getDigitalLibrary();

}
