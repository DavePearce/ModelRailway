package modelrailway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static modelrailway.core.Section.Config.*;
import modelrailway.core.Controller;
import modelrailway.core.Event;
import modelrailway.core.Route;
import modelrailway.core.Event.SpeedChanged;
import modelrailway.core.Section;
import modelrailway.core.Train;
import modelrailway.util.SimpleController;

/**
 * Provides a simple command-line interface for controlling the model railway.
 * The user enters commands which then take effects on the railway.
 * 
 * @author David J. Pearce
 *
 */
public class Main {
	private Event.Listener railway;
	private Controller controller;
	
	public Main(Event.Listener railway, Controller controller) {
		this.railway = railway;
		this.controller = controller;
	}
	
	// =========================================================================
	// Commands.  
	// =========================================================================	
	// Below here is the set of all commands recognised by the interface. If you
	// want to add a new command, then add a public static function for it and
	// an appropriate entry in the commands array.
	
	/**
	 * The list of commands recognised by the readEvaluatePrintLoop(). To add
	 * more functions, simply extend this list!
	 */
	private Command[] commands = {
		this.new Command("quit",getMethod("quit")),
		this.new Command("help",getMethod("printHelp")),
		this.new Command("verbose",getMethod("setVerbose",boolean.class)),
		this.new Command("start",getMethod("startLocomotive",int.class, float.class)),
		this.new Command("stop",getMethod("stopLocomotive",int.class)),
		this.new Command("route",getMethod("routeLocomotive",int.class,int[].class)),
		this.new Command("loop",getMethod("loopLocomotive",int.class,int[].class)),
		this.new Command("locate",getMethod("setLocation",int.class,int.class)),
		this.new Command("turnout",getMethod("setTurnout",int.class,boolean.class))
	};
	
	public void quit() {
		System.exit(0);
	}
	
	public void startLocomotive(int locomotive, float speed) {
		boolean direction = speed >= 0;
		speed = Math.abs(speed);
		System.out.println("SETTING SPEED: " + speed);
		railway.notify(new Event.DirectionChanged(locomotive, direction));
		railway.notify(new Event.SpeedChanged(locomotive, speed));		
	}
	
	public void stopLocomotive(int locomotive) {
		System.out.println("EMERGENCY STOP: " + locomotive);
		railway.notify(new Event.EmergencyStop(locomotive));
	}

	public void routeLocomotive(int locomotive, int[] route) {
		System.out.println("Starting train: " + locomotive + " on route: " + Arrays.toString(route));
		if(!controller.start(locomotive, new Route(false,route))) {
			System.out.println("Error starting route (perhaps train not in starting section?)");
		}
	}
	
	public void loopLocomotive(int locomotive, int[] route) {
		System.out.println("Starting train: " + locomotive + " on loop: " + Arrays.toString(route));
		if(!controller.start(locomotive, new Route(true,route))) {
			System.out.println("Error starting route (perhaps train not in starting section?)");
		}
	}
	
	public void setLocation(int locomotive, int section) {
		System.out.println("Setting location: " + locomotive + " to: " + section);
		controller.train(locomotive).setSection(section);
	}
	
	public void setTurnout(int turnout, boolean thrown) {
		System.out.println("Setting turnout: " + turnout + " to: " + thrown);
		controller.set(turnout, thrown);
	}

	public void setVerbose(boolean flag) {
		// railway.setVerbose(flag)
	}
	
	public void printHelp() {
		System.out.println("Model rail commands:");
		for(Command c : commands) {
			System.out.println("\t" + c.keyword);
		}
	}

	// =========================================================================
	// Read, Evaluate, Print loop
	// =========================================================================	
	// Below here is all the machinery for the REPL. You shouldn't need to touch
	// this.
	
	/**
	 * This function provides a simple interface to the model railway system. In
	 * essence, it waits for user input. Each command consists of a line of
	 * text, and has a specific form. The commands are dispatched to handlers
	 * which then interface with the railway. The interface remains in the loop
	 * continually waiting for user input.
	 */
	private void readEvaluatePrintLoop() {
		final BufferedReader input = new BufferedReader(new InputStreamReader(
				System.in));
		ArrayList<String> history = new ArrayList<String>();
		try {
			System.out.println("Welcome to the Model Railway!");
			
			while (true) {
				System.out.print("> ");
				// Read the input line
				String line = input.readLine();
				// Check whether this is a meta operation
				if(line.equals("\"")) {
					// Pull out command from history
					line = history.get(history.size()-1);
					System.out.print("> " + line);
					line = line + input.readLine();
				}
				// Record the current command in the history
				history.add(line);
				// Attempt to execute the input line
				boolean isOK = execute(line);
				if(!isOK) {
					// If we get here, then it means that the command was not
					// recognised. Therefore, print error!
					System.out.println("Error: command not recognised");
				}
			}
		} catch (IOException e) {
			System.err.println("I/O Error - " + e.getMessage());
		}
	}

	/**
	 * Attempt to execute a command-line
	 * @param line
	 * @param railway
	 * @return
	 */
	public boolean execute(String line) {
		for (Command c : commands) {
			Object[] args = c.match(line);
			if (args != null) {
				// Yes, this command was matched. Now, sanity check the
				// arguments.
				for (int i = 0; i != args.length; ++i) {
					if (args[i] == null) {
						// this indicates a problem converting this
						// argument, so report an error to the user.
						System.out.println("Command \"" + c.keyword
								+ "\": syntax error on argument " + (i+1));
						return false;
					}
				}
				try {
					// Ok, attemp to execute the command;
					c.method.invoke(this, args);
					return true;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					throw (RuntimeException) e.getCause();
				}
			}
		}
		return false;
	}
		
	/**
	 * This simply returns a reference to a given name. If the method doesn't
	 * exist, then it will throw a runtime exception.
	 * 
	 * @param name
	 * @param paramTypes
	 * @return
	 */
	public static Method getMethod(String name, Class... paramTypes) {
		try {
			return Main.class.getMethod(name, paramTypes);
		} catch (Exception e) {
			throw new RuntimeException("No such method: " + name, e);
		}
	}
	
	/**
	 * Represents a given interface command in the railway. Each command
	 * consists of an initial keyword, followed by zero or more parameters. The
	 * class provides simplistic checking of option types.
	 * 
	 * @author David J. Pearce
	 *
	 */
	private class Command {
		public final String keyword;
		public final Method method;

		public Command(String keyword, Method method) {
			this.keyword = keyword;
			this.method = method;
		}

		/**
		 * Check whether a given line of text matches the command or not. For
		 * this to be true, the number of arguments must match the expected
		 * number, and the given keyword must match as well. If so, an array of
		 * the converted arguments is returned; otherwise, null is returned.
		 * When we cannot convert a given argument because it has the wrong
		 * type, a null entry is recorded to help with error reporting,
		 * 
		 * @param line
		 * @return
		 */
		public Object[] match(String line) {
			Class[] parameters = method.getParameterTypes();
			String[] tokens = line.split(" ");
			if (tokens.length != parameters.length + 1) {
				return null;
			} else if (!tokens[0].equals(keyword)) {
				return null;
			} else {
				Object[] arguments = new Object[tokens.length-1];
				for(int i=1;i!=tokens.length;++i) {
					arguments[i-1] = convert(parameters[i-1],tokens[i]);
				}
				return arguments;
			}
		}
		
		/**
		 * Convert a string representation of this argument into an actual
		 * object form. If this fails for some reason, then null is returned.
		 * 
		 * @param token
		 * @return
		 */
		private Object convert(Class parameter, String token) {
			if(parameter == boolean.class) {
				if(token.equals("off")) {
					return false;
				} else if(token.equals("on")) {
					return true;
				} else {
					return null;
				}
			} else if(parameter == int.class) {
				try {
					return Integer.parseInt(token);
				} catch(NumberFormatException e) {
					return null;
				}
			} else if(parameter == float.class) {
				try {
					return Float.parseFloat(token);
				} catch(NumberFormatException e) {
					return null;
				}
			} else if (parameter == int[].class) {
				String[] numbers = token.split(",");
				ArrayList<Integer> list = new ArrayList<Integer>();
				for (int i = 0; i != numbers.length; ++i) {
					String number = numbers[i];					
					try {						
						int split = number.indexOf('-');
						int first,last;
						if(split == -1) {
							first = last = Integer.parseInt(numbers[i]); 
						} else{							
							first = Integer.parseInt(number.substring(0,split));
							last = Integer.parseInt(number.substring(split+1));
						}		
						if (first <= last) {
							for (int k = first; k <= last; ++k) {
								list.add(k);
							}
						} else {
							for (int k = first; k >= last; --k) {
								list.add(k);
							}
						}
					} catch (NumberFormatException e) {
						return null;
					}
				}
				
				// Hmmm, so ArrayList.toArray() doesn't work here. That's
				// annoying!
				int[] r = new int[list.size()];
				for(int i=0;i!=list.size();++i) {
					r[i] = list.get(i);
				}
				return r;
			} else if(parameter == String.class) {
				return token;
			} else {
				// In this case, the argument was not recognised.
				return null;
			}
		}
	}
	

	// =========================================================================
	// Main entry point
	// =========================================================================
	public static void main(String args[]) throws Exception {
		String port = args[0];
		boolean simulation = false;
		Event.Listener railway;
		
		// Construct the railway controller which will oversea the movement and
		// tracking of trains on the network.
		Controller controller = createRailwayController();
		
		// Now, construct the railway network connection, which may be the
		// physical hardware or just a simulation.
		if (simulation) {
			railway = null;
		} else {
			railway = createRailwayConnection(port,controller);
		}
		
		controller.register(railway);

		// Finally, create and enter the read-eval-print loop.
		new Main(railway,controller).readEvaluatePrintLoop();
	}
	
	/**
	 * <p>
	 * Create the railway controller which will manage the railway,
	 * independently of whether or not it's the physical railway or a
	 * simulation.
	 * </p>
	 * <p>
	 * NOTE: at the moment, this uses a hardcoded representation of the railway
	 * layout. However, it doesn't have to be like this!
	 * </p>
	 * 
	 * @return
	 */
	private static Controller createRailwayController() {

		Train[] trains = {
				new Train(0,true), // default config for train 0 
				new Train(0,true), // default config for train 1
				new Train(0,true)  // default config for train 2
		};
		Section[] sections = { null, // no section 0

				// Section 1
				new Section(new Section.Config(8, 2)),

				// Section 2
				new Section(new Section.Config(1, 3)),

				// Section 3
				new Section(
						new Section.Config(2, 4, new int[] { 6, CLOSED }),
						new Section.Config(10, 4, new int[] { 6, THROWN })),

				// Section 4
				new Section(
						new Section.Config(3, 5, new int[] { 7, CLOSED }),
						new Section.Config(3, 19, new int[] { 7, THROWN })),

				// Section 5
				new Section(new Section.Config(4, 6)),

				// Section 6
				new Section(new Section.Config(5, 7)),

				// Section 7
				new Section(new Section.Config(6, 8)),

				// Section 8
				new Section(
						new Section.Config(7, 1, new int[] { 2, CLOSED }),
						new Section.Config(7, 9, new int[] { 2, THROWN })),

				// Section 9
				new Section(
						new Section.Config(16, 10, new int[] { 3, CLOSED }),
						new Section.Config(8, 10, new int[] { 3, THROWN })),

				// Section 10
				new Section(
						new Section.Config(9, 11, new int[] { 4, CLOSED }),
						new Section.Config(9, 3, new int[] { 4, THROWN })),

				// Section 11
				new Section(
						new Section.Config(10, 12, new int[] { 5, CLOSED }),
						new Section.Config(17, 12, new int[] { 5, THROWN })),

				// Section 12
				new Section(
						new Section.Config(11, 13, new int[] { 8, CLOSED }),
						new Section.Config(11, 21, new int[] { 8, THROWN })),

				// Section 13
				new Section(new Section.Config(12, 14)),

				// Section 14
				new Section(new Section.Config(13, 15)),

				// Section 15
				new Section(new Section.Config(14, 16)),

				// Section 16
				new Section(
						new Section.Config(15, 9, new int[] { 1, CLOSED }),
						new Section.Config(15, 18, new int[] { 1, THROWN })),

				// Section 17
				new Section(new Section.Config(16, 18)),

				// Section 18
				new Section(new Section.Config(17, 11)),
		// new Section(new Section.Route()), // 19
		// new Section(new Section.Route()), // 20
		// new Section(new Section.Route()), // 21
		// new Section(new Section.Route()), // 22
		// new Section(new Section.Route()), // 23

		};
		return new SimpleController(trains,sections);
	}
	
	/**
	 * Construct our connection to the physical railway. This can fail in a
	 * variety of ways, for example if the USB cable is not plugged in!
	 * 
	 * @param port
	 * @return
	 * @throws Exception
	 */
	private static Event.Listener createRailwayConnection(String port,
			Controller controller) throws Exception {
		// Construct the model railway assuming the interface (i.e. USB
		// Cable) is on a given port. Likewise, we initialise it with three
		// locomotives whose addresses are 1,2 + 3. If more locomotives are
		// to be used, this needs to be updated accordingly.
		final ModelRailway railway = new ModelRailway(port,
				new int[] { 1, 2, 3 });

		// Add shutdown hook to make sure resources are released when
		// quiting the application, even if the application is quit in a
		// non-standard fashion.
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Disconnecting from railway...");
				railway.destroy();
			}
		}) {
		});

		railway.register(controller);
		
		return railway;
	}
}
