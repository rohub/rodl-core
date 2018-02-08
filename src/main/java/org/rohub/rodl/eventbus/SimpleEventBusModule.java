package org.rohub.rodl.eventbus;


import org.rohub.rodl.eventbus.listeners.ModesListener;
import org.rohub.rodl.eventbus.listeners.PreservationListener;
import org.rohub.rodl.eventbus.listeners.SimpleSerializationListener;

import com.google.common.eventbus.EventBus;

/**
 * Configure dependency injection.
 * 
 * @author pejot
 * 
 */
public class SimpleEventBusModule implements EventBusModule {

    /** EventBus instance. */
    private EventBus eventBus;
    
    /**
     * Constructor.
     */
    public SimpleEventBusModule() {
        eventBus = new EventBus("main-event-bus");
        new PreservationListener(eventBus);
        new SimpleSerializationListener(eventBus);
        new ModesListener(eventBus);

    }


    @Override
    public EventBus getEventBus() {
        return eventBus;
    }


    @Override
    public void commit() {
    }


	@Override
	public EventBus getAsyncEventBus() {
		return null;
	}
}
