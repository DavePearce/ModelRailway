package modelrailway.core;

import java.util.ArrayList;

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
	private final int[][] routes;
	
	/**
	 * The "lock queue". That is the queue of trains wishing to lock (and,
	 * hence, enter) this section. The first train in the queue always has the
	 * lock.
	 * 
	 */
	private ArrayList<Integer> queue;

	public Section(int[]... routes) {
		this.routes = routes;
		this.queue = new ArrayList<Integer>(); 
	}

	/**
	 * Check whethera given entry / exit route is valid or not.
	 * 
	 * @param entry
	 * @return
	 */
	public boolean isValidRoute(int entry,int exit) {
		
		for(int[] pair : routes) {
			if(pair[0] == entry && pair[1] == exit) {
				return true;
			} else if(pair[0] == exit && pair[1] == entry) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Attempt to lock the given route for the given train. If this succeeds,
	 * then this train has rights to enter the section. Otherwise, the train is
	 * queued for subsequent entry when the section is available again.
	 * 
	 * @param trainID
	 * @param entry
	 * @param exit
	 * @return
	 */
	public synchronized boolean lockRoute(int trainID, int entry, int exit) {
		if(!isValidRoute(entry,exit)) {
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
	public synchronized boolean unlockRoute(int trainID) {
		if(queue.size() > 1 && queue.get(0) == trainID) {
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
}
