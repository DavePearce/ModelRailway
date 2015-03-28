package modelrailway.util;

import java.util.*;

import modelrailway.core.*;
import modelrailway.core.Event.Listener;

/**
 * A very simplistic implementation of the controller interface. This manages
 * trains as they progress through specific routes. 
 * 
 * @author David J. Pearce
 *
 */
public class SimpleController implements Controller {
	/**
	 * The set of listeners for events generated by this controller.
	 */
	private ArrayList<Event.Listener> listeners = new ArrayList<Event.Listener>();
	
	/**
	 * The current trains being tracked on the network.
	 */
	private Train[] trains;
	
	/**
	 * The current routes each train is taking. A route is essentially a
	 * sequence of sections linked together. They can be null if the train is
	 * stopped and not currently following a route.
	 */
	private Route[] routes;
		
	public SimpleController(Train... trains) {
		this.routes = new Route[trains.length];
		this.trains = trains;
	}
	
	@Override
	public void register(Listener listener) {
		listeners.add(listener);
	}
	
	@Override
	public Train train(int trainID) {
		return trains[trainID];
	}

	@Override
	public boolean start(int trainID, Route route) {
		Train train = trains[trainID];
		// Now, check whether the train is on the starting section.
		if (route.firstSection() == train.currentSection()) {
			routes[trainID] = route;
			// In the simple controller, trains always move in the forwards
			// direction. This is necessary because the controller has no
			// knowledge of the network topology and cannot make any
			// distinctions about what directions make sense.
			send(new Event.DirectionChanged(trainID,true));
			// In the simple controller, trains always move at a fixed velocity.
			send(new Event.SpeedChanged(trainID,0.75f));
			return true;
		} else {
			routes[trainID] = null;
			stop(trainID);
			return false;
		}
	}

	@Override
	public void stop(int trainID) {
		send(new Event.SpeedChanged(trainID,0.0f));		
	}

	@Override
	public void notify(Event e) {
		// This function listens only to section changed events and makes sure
		// that the trains are progressing correctly along each section in their
		// route.
		if(e instanceof Event.SectionChanged) {
			Event.SectionChanged es = (Event.SectionChanged) e;
			// At this point, there are two things to do. Firstly, we need to
			// confirm that this section changed event was the expected event
			// for a route.  Second, we need to update the train with its
			// current predicted section location.
			int trainID = -1;

			if(es.getInto()) {
				// This indicates that a train has moved into a new detection
				// section. To figure out which train, we need to look at the
				// next expected section for each train to see whether it
				// matches any of them.
				for(int i=0;i!=trains.length;++i) {
					int expected = routes[i].nextSection(trains[i].currentSection());
					if(expected == es.getSection()) {
						// Matched
						trainID = i;
						break;
					}
				}
			} else {
				// This indicates that a train has moved out of a given
				// detection section. To figure out which train, we need simply
				// need to decide which train was in that section.
				for(int i=0;i!=trains.length;++i) {
					if(trains[i].currentSection() == es.getSection()) {
						// Matched
						trainID = i;
						break;
					}
				}
			}
			
			if(trainID == -1) {
				// this indicates a recognition failure. At this point, we just
				// stop all trains as a simplistic emergency procedure.
				for(int i=0;i!=trains.length;++i) {
					stop(i);
					routes[i] = null;
				}
			} else {
				// We managed to determine which train caused this event,
				// therefore we now update it's position.
				Train train = trains[trainID];
				Integer nextSection = routes[trainID].nextSection(train.currentSection());
				if(nextSection == null) {
					// The train has reached the end of its route.
					stop(trainID);
					routes[trainID] = null;
				} else {
					train.setSection(nextSection);
				}
			}
		}
	}
	
	/**
	 * A helper function for broadcasting events to all registered listeners.
	 * 
	 * @param e
	 */
	private void send(Event e) {
		for(Listener l : listeners) {
			l.notify(e);
		}
	}
}
