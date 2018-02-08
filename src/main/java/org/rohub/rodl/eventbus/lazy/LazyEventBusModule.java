package org.rohub.rodl.eventbus.lazy;


import org.rohub.rodl.eventbus.EventBusModule;
import org.rohub.rodl.eventbus.lazy.listeners.LazySerializationListener;
import org.rohub.rodl.eventbus.listeners.ModesListener;
import org.rohub.rodl.eventbus.listeners.PermissionsListener;
import org.rohub.rodl.eventbus.listeners.PreservationListener;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

/**
 * A module that has lazy listeners, i.e you have to call commit() in the end.
 * 
 * @author pejot
 * 
 */
public class LazyEventBusModule implements EventBusModule {

    /** EventBus instance. */
    private EventBus eventBus;

    private static AsyncEventBus asyncEventBus = null;
    

    /** Lazy serialization listener. */
    private LazySerializationListener serializationListener;


    /**
     * Constructor.
     */
    public LazyEventBusModule() {
        eventBus = new EventBus("main-event-bus");
        serializationListener = new LazySerializationListener(eventBus);
        new PreservationListener(eventBus);
        new ModesListener(eventBus);
        new PermissionsListener(eventBus);
        
    }


    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
    
    public EventBus getAsyncEventBus(){
    	return asyncEventBus;
    }


    @Override
    public void commit() {
        serializationListener.commit();
    }
}
