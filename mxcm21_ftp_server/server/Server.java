package mxcm21_ftp_server.server;

import java.io.*;																		// for input and output
import java.net.*; 																		// for tcp socket connection
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;												// for utf-8 stream
import java.util.*;
import java.util.concurrent.*;

import mxcm21_ftp_server.interfaces.ServerInterface;									// needs interface
import java.rmi.registry.*; 															// for connecting and using rmi registry
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;


public class Server implements ServerInterface {
	/* Server file can be run multiple times, it will look at the rmiregistry
	 * and assign itself to a name and port not already in use. Since it takes
	 * some time to bind to the registry, you can hard code a number instead,
	 * allowing me to launch three at once */

	private static String directory; 													// want all files in sub folder
	private static Registry registry;
	private boolean working; 															// keeps track of if it is connected properly so reconenction messages can be displayed

	private Server() {
		working = true; 																// initially everything connected
	}

	public void runCheck(String name, Server obj) {
		/* Tries to rebind the server to the registry, if registry is down an error will be returned
		 * repeats every second, so if registry is taken down and brought back up it will
		 * automatically reconnect */
		ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleAtFixedRate(new Runnable() { 										// run every five seconds
		  @Override
		  public void run() {
		    	try {
		    		registry.rebind(name, obj); 							 			// check if registry is still there
		    		if (!working) {
		    			System.out.println("==== Successfully reconnected! ====");		// success message on reconnect
		    			working = true;													// reset
		    		}
		    	} catch (Exception e) {
		    		System.out.println("Registry down, trying to connect ...");
		    		working = false;
		    	}
		  }
		}, 0, 5, TimeUnit.SECONDS);
	}

	public List<String> list() {
		/* LISTS ALL FILES IN DIRECTORY */
		try {
			System.out.println("Getting Directory Listing");
			File folder = new File(directory); 											// path to file
			List<String> directories = new ArrayList<String>(); 						// directories list

			for(File f : folder.listFiles()) 											// loop through all files
				if(f.isFile())															// check it's a file
					directories.add(f.getName()); 										// append to listing

			return directories; 														// return list of files
		} catch (Exception e) {System.out.println("Error listing directories.");}
		return Arrays.asList("Error");
	}


	public boolean upload(File file, String filename) {
		/* UPLOADS FILE PASSED TO IT */
		try {
			System.out.println("Uploading: " + directory + filename);
			File newFile = new File(directory + filename);								// make a new file with same name

			FileChannel oldFileOut = new FileInputStream(file).getChannel();			// get output channel
			FileChannel newFileIn = new FileOutputStream(newFile).getChannel();			// get input channel
			newFileIn.transferFrom(oldFileOut, 0, oldFileOut.size());					// transfer contents

			oldFileOut.close();															// close streams
			newFileIn.close();
			return true; 																// return worked
		} catch (Exception e) {
			System.out.println("Error uploading file.");
			return false; 																// return failed
		}
	}

	public File download(String filename) {
		/* DOWNLOADS FILE */
		try {
			System.out.println("Downloading: " + directory + filename);
			File origFile = new File(directory + filename);								// get file
			return origFile; 															// return it
		} catch (Exception e) {
			System.out.println("Error downloading file.");
		}
		return null; 																	// return null if failed
	}

	public boolean delete(String filename) {
		/* DELETES FILE */
		try {
			System.out.println("Deleting: " + directory + filename);
			File file = new File(directory + filename);									// get file
			file.delete(); 																// delete file
			return true; 																// return success
		} catch (Exception e) {
			System.out.println("Error deleting file.");
		}
		return false;																	// return failure
	}

	public boolean exists(String name) {
		/* Returns if server has that file or not */
		File file = new File(directory + name); 										// get path to file
		return file.exists(); 															// reutrn if file exists there
	}

	public int numberOfFiles() {
		/* COUNTS ALL FILES STORED IN SERVER FOLDER */
		File folder = new File(directory); 												// path to folder
		return folder.listFiles().length; 												// get number of files stored there
	}

	public static void main(String[] args) {
		/* Attaches server to the RMI registry */

		try {
			int forcedNumber = -1; 															// set initial value
			if (args.length > 0) { 															// if argument provided
				if(args[0].length() > 1 || !Character.isDigit(args[0].charAt(0)))	 		// checks only 1 number is provided and it is a digit
					System.out.println("You should only provide a digit between 1 & 3!\n");
				else {
					forcedNumber = Integer.parseInt(args[0]); 								// if number specified
					if (forcedNumber > 3 || forcedNumber < 1) { 							// check it is 1,2 or 3
						System.out.println("Number provided must be between 1 & 3."); 		// can only start up 3 servers
						forcedNumber = -1;													// reset forced number if not 1,2 or 3
					}
				}
			}

			if (forcedNumber == -1) 															// if no number or incorrect number specified try to start up any server
				System.out.println("Checking if there are any offline servers to start up.");

			// Get registry
		    registry = LocateRegistry.getRegistry("localhost", 11111); 							// get registry

		    // Create server object
		    Server obj = new Server(); 															// make new object

		    // Create remote object stub from server object
		    ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(obj, 0); 	// export it

		    /* FOR GIVING EACH SERVER A UNIQUE NAME */
		    int next = forcedNumber; 															// initially is the forced number
			    if (forcedNumber == -1) {														// if we don't want to force, find next available
				    ArrayList<String> current = new ArrayList<String>(Arrays.asList(registry.list())); 	// get list from registry

				    /* if a number isn't forced take the next available number between 1 and 3 */
				    for(int i = 1; i <= 3; i++) 												// loop 1 -> 3
				    	if (current.indexOf("server"+i) == -1) { 								// if this isn't bound to rmi
				    		next = i; 															// set next available to it
				    		break; 																// and break from loop
				    	}
		    }

			if (next != -1) { 																	// if an available server was forced or found
				String name = "server" + next; 													// update name

				try {
					registry.lookup(name);														// check if already registered
					System.out.println("Server "+next+" is already running, run again without "
							+ "an argument to turn the next available server online.");
					System.exit(0); 															// close program
				} catch (NotBoundException e) {													// we want it not to be
				    // Bind the remote object's stub in the registry							// if it isn't already registered
				    registry.bind(name, stub); 													// bind it to the registry

				    // Write ready message to console
				    System.err.println("Server "+next+" ready!");

				    directory = "mxcm21_ftp_Server/server/server"+next+"Files/"; 				// assign file server folder

				    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() { 			// at shutdown
				        public void run() {
				        	try {
				        		System.out.println("Unbinding from RMI");
					            registry.unbind(name); 											// unbind from registry
				        	} catch (Exception e) {
				        		System.out.println("Error unbinding from RMI");
				        	}
				        }
				    }, "Shutdown-thread"));

				    obj.runCheck(name, obj); 													// checks if registry is still up and reconnects if it isn't
				}
			} else {
				System.out.println("All 3 servers are already running!"); 						// if available server not found print error message
				System.exit(0); 																// and close
			}
		} catch (Exception e) {																	// say registry didn't work
			System.err.println("Server can not connect to RMI registry, please ensure the registry"+
		    		" was started in the 'Frederick_Li' folder. \n... Trying to connect again ...");
		    main(args); 																		// and retry (so it works if rmi is brought up later)
		}
	}
}
