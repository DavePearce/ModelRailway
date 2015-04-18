package modelrailway.core;

import java.util.Iterator;

/**
 * A route represents a potential path through the rail network that a train
 * might take. A route is simply a sequence of sections which are to be
 * followed. Routes can be "loops", in which case the last section must follow
 * into the first section.
 * 
 * @author David J. Pearce
 * 
 */
public class Route {
	
	/**
	 * The list of section numbers making up this route. These should be verfied
	 * as being sequential sections on the track.
	 */
	private int[] sections;
	
	/**
	 * Determine whether this route is a continous loop or not.
	 */
	private boolean isLoop;
	
	/**
	 * Construct a give route.
	 * 
	 * @param isLoop
	 * @param sections
	 */
	public Route(boolean isLoop, int... sections) {
		this.isLoop = isLoop;
		this.sections = sections;
	}
	
	public Integer firstSection() {
		return sections[0];
	}
	
	public int lastSection() {
		if (isLoop) {
			return -1;
		} else {
			return sections[sections.length - 1];
		}
	}
	
	/**
	 * Determine the next section in this route after a given section. the
	 * <code>null</code> value is returned if this route is not a loop, and the
	 * given section is the last section. An exception is raised if the given
	 * section is not in this route.
	 * 
	 * @param section
	 * @return
	 */
	public Integer nextSection(int section) {
		// Go through each section looking for a match.
		for (int i = 0; i != sections.length; ++i) {
			if (sections[i] == section) {
				// Found matching section. Now, determine next section (if it
				// exists).
				i = (i + 1) % sections.length;
				// Check whether we reached the end of the route or not
				if (i == 0 && !isLoop) {
					// Yes, we did.
					return null;
				} else {
					// No, there is still more.
					return sections[i];
				}
			}
		}
		throw new IllegalArgumentException("Invalid section for route");
	}		
	
	/**
	 * Determine the previous section in this route before a given section. the
	 * <code>null</code> value is returned if this route is not a loop, and the
	 * given section is the first section. An exception is raised if the given
	 * section is not in this route.
	 * 
	 * @param section
	 * @return
	 */
	public Integer prevSection(int section) {
		// Go through each section looking for a match.
		for (int i = (sections.length-1); i >= 0; --i) {
			if (sections[i] == section) {
				// Found matching section. Now, determine previous section (if it
				// exists).
				if (i == 0 && !isLoop) {
					// We reached the first section of the route.
					return null;
				} else if(i > 0) {
					// No, there are still more sections before us
					return sections[i-1];
				} else {
					// We're on a loop!
					return sections[sections.length-1];
				}
			}
		}
		throw new IllegalArgumentException("Invalid section for route");
	}
}
