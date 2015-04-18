package modelrailway.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single section on the railway, which may be made up from
 * multiple individual components or track pieces. At the moment, these smaller
 * components are not modelled. However, the potential routes through the
 * section are modelled.
 * 
 * @author David J. Pearce
 *
 */
public class Section {

	/**
	 * Array of pairs of entry / exit routes.
	 */
	private final Config[] configurations;
	
	/**
	 * The "lock queue". That is the queue of trains wishing to lock (and,
	 * hence, enter) this section. The first train in the queue always has the
	 * lock.
	 * 
	 */
	private ArrayList<Integer> queue;

	public Section(Config... configurations) {
		this.configurations = configurations;
		this.queue = new ArrayList<Integer>(); 
	}

	/**
	 * Check whethera given entry / exit route is valid or not.
	 * 
	 * @param entry
	 * @return
	 */
	public boolean isValidConfiguration(int entry,int exit) {		
		return getTurnoutConfiguration(entry,exit) != null;
	}
	
	/**
	 * Get the turnout information associated with a given configuration. This
	 * is to enable the railway to configure the turnouts accordingly.
	 * 
	 * @param entry
	 * @param exit
	 * @return
	 */
	public Map<Integer, Boolean> getTurnoutConfiguration(int entry, int exit) {
		for (Config pair : configurations) {
			if (pair.entry == entry && pair.exit == exit) {
				return pair.turnouts;
			} else if (pair.entry == exit && pair.exit == entry) {
				return pair.turnouts;
			}
		}
		return null;
	}
	
	/**
	 * Attempt to lock the given route for the given train. If this succeeds,
	 * then this train has rights to enter the section. Otherwise, the train is
	 * queued for subsequent entry when the section is available again.
	 * 
	 * @param trainID
	 * @param entry
	 * @param exit
	 *            --- may be null if no exit planned
	 * @return
	 */
	public synchronized boolean lock(int trainID, int entry, Integer exit) {
		if(exit != null && !isValidConfiguration(entry,exit)) {
			return false;
		} 
		queue.add(trainID);
		return queue.size() == 1;
	}
	
	/**
	 * Unlock a given section which was previously locked by the train.
	 * 
	 * @param trainID
	 * @return
	 */
	public synchronized boolean unlock(int trainID) {
		if(queue.size() > 0 && queue.get(0) == trainID) {
			queue.remove(0);
			return true; 
		} else {
			return false;
		}
	}
	
	/**
	 * Determine the next train queued for entry to this section.
	 * @return
	 */
	public synchronized int nextQueued() {
		if(queue.size() == 0) {
			return -1;
		} else {
			return queue.get(0);
		}
	}
	
	/**
	 * A Section route is a specific route through a section. Most sections only
	 * have one route through them (i.e. they are straight pieces). However,
	 * sections which contain turnouts can have multiple routes through them.
	 * Each route needs to know which neighbouring sections it connects
	 * together, as well as how the turnouts need to be configured for it.
	 * 
	 * @author David J. Pearce
	 *
	 */
	public static class Config {
		public static final int CLOSED = 0;
		public static final int THROWN = 1;
		
		/**
		 * The section from which this route is entered when travelling this
		 * route in the forwards orientation.
		 */
		private int entry;
		
		/**
		 * The section to which follows on from this route when travelling in
		 * the forwards orientation.
		 */
		private int exit;
		
		/**
		 * The turnout configuration required for this route
		 */
		private Map<Integer,Boolean> turnouts;
		
		public Config(int entry, int exit, int[]... configuration) {
			this.entry = entry;
			this.exit = exit;
			this.turnouts = new HashMap<Integer,Boolean>();
			for(int[] pair : configuration) {
				this.turnouts.put(pair[0], pair[1] == THROWN);
			}
		}
	}
}
