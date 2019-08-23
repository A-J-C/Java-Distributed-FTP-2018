package mxcm21_ftp_server.frontend;

import java.rmi.registry.*; 													// for connecting to registry
import java.rmi.*; 																// for using registry
import java.rmi.server.UnicastRemoteObject;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

import mxcm21_ftp_server.interfaces.*; 											// needs access to interface

public class FrontEnd implements FrontEndInterface {

	private static Registry registry;
	private static ServerInterface serverStub;
	private boolean flag;																// keeps track of registry being up or down
	private boolean working; 															// keeps track of if it is connected properly so reconenction messages can be displayed

    public FrontEnd() {
    	flag = false; 																	// initialsie flags
    	working = true;
    }

    public void runCheck(FrontEnd obj) {
		/* Tries to rebind the server to the registry, if registry is down an error will be returned
		 * repeats every second, so if registry is taken down and brought back up it will
		 * automatically reconnect */
		ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleAtFixedRate(new Runnable() {
		  @Override
		  public void run() {
		    	try {
		    		registry.rebind("frontend", obj); 							 			// check if registry is still there by rebinding
		    		if (!working) {
		    			System.out.println("==== Successfully reconnected! ====");			// success message on reconnect
		    			working = true;														// reset
		    		}
		    	} catch (Exception e) {
		    		System.out.println("Registry down, trying to connect, check you don't have"
		    				+ " multiple frontends running ...");
		    		working = false;
		    	}
		  }
		}, 0, 5, TimeUnit.SECONDS);
	}

    public String list() {
    	/* Gets a list of all files servers have and filters duplicates*/

    	String listing = "\n== Directory Listing for mxcm21's FTP Server: ==\n\n";		// output string
    	List<ServerInterface> servers = getServers();

    	if (servers != null) {
	    	try {
	    		List<String> files = new ArrayList<String>();

	    		for(ServerInterface serverStub : servers)								// for each server
	    			for(String file : serverStub.list()) 								// get its files
	    				if (!files.contains(file) && !file.equals("Error")) 	{		// if file not already listed
	    					files.add(file);											// add to list
	    					listing += "  - " + file + "\n"; 							// and add to output string
	    				}

	    		if (files.isEmpty())
	    			listing = "\n== No files found, try uploading a file, or ensuring all 3 servers are running ==\n";
	    	} catch(RemoteException e) {
	    		return "Error retriving lists, please try again.";
	    	}
	    	return listing;
    	}

    	if (flag)
    		return "\n=== RMI Registry is down, please start it again, the servers should reconnect themselves ===\n";

    	return "\n=== No servers appear to be online ===\n";
    }

    public String upload(File file, String filename, boolean reliable) {
    	/* takes a File and puts it on the server with the least files, or on all available
    	 * servers if reliable flag is set */
    	long startTime = System.currentTimeMillis(); 									// get start time
    	List<ServerInterface> servers = getServers();									// get all servers currently online

    	if (servers != null) { 															// if there are servers online
    		boolean worked = true; 														// set flag to true
	    	try {
	    		if (reliable) {
	    			for(ServerInterface serverStub : servers)							// for each server
	    				if(!serverStub.upload(file, filename)) 							// upload file
	    					worked = false;												// if it didn't work set to false

	    		} else {
	    			ServerInterface smallestServer = null;								// keeps track of server with least files
	    			int smallest = Integer.MAX_VALUE;

	    			for(ServerInterface sStub : servers) {
	    				int numberFiles = sStub.numberOfFiles(); 						// get numbfer of files for server
	    				if(numberFiles < smallest) { 									// if less than current
	    					smallest = numberFiles; 									// update values
	    					smallestServer = sStub;
	    				}
	    			}
	    			worked = smallestServer.upload(file, filename);						// upload file to smallest server
	    		}
	    	} catch(RemoteException e) {
	    		return "Error uploading file, please try again.";
	    	}

	    	long endTime = System.currentTimeMillis(); 									// get end time
			String time = Double.toString((endTime - startTime) / 1000.0); 				// turn to seconds

	    	if (worked)
	    		return "Successfuly uploaded file: "+filename +", "+file.length()+" bytes in "+time+" seconds."; // return success
	    	else
	    		return "Error uploading file, please try again";
    	}

    	if (flag)
    		return "\n=== RMI Registry is down, please start it again, the servers should reconnect themselves ===\n";

    	return "\n=== No servers appear to be online ===\n";
    }

    public File download(String filename) {
    	/* Loops through all online servers untill it finds one that has the file
    	 * it then downloads the file and passes it on */
    	File file = null;

    	try {
    		List<ServerInterface> serverStubs = getServers();							// get all online servers

    		if(serverStubs != null) {
		    	for(ServerInterface serverStub : serverStubs)							// for each server
					if(serverStub.exists(filename)) { 									// checks if file exists
						file = serverStub.download(filename);							// if it does get file
						break; 															// and break out of loop
					}
    		}
    	} catch (RemoteException e) {
    		System.out.println("Error downloading files");
    	}

    	return file; 																	// return the file
    }

    public String delete(String file) {
    	/* Loops through all online servers and deletes the file if it has it */
    	String msg = "Error deleting file.";
    	try {
    		List<ServerInterface> serverStubs = getServers(); 							// get servers
    		boolean worked = true;

    		if(serverStubs != null)
		    	for(ServerInterface serverStub : serverStubs)							// for each server
					if(serverStub.exists(file)) 										// checks if file exists
						if(!serverStub.delete(file))									// if it does delete file
							worked = false; 											// if it failed set worked to false

    		if(worked)
    			msg = "Successfully deleted file."; 									// set success message
    		else
    			msg = "Error deleting file.";
    	} catch (RemoteException e) {
    		msg = "Error deleting file.";
    	}

    	return msg; 																	// return message
    }

    public boolean exists(String name) {
    	/* Checks all online servers for if they have a file */
    	boolean exists = false;

    	try {
    		List<ServerInterface> serverStubs = getServers(); 							// get all online servers

    		if(serverStubs != null)
				for(ServerInterface serverStub : serverStubs)							// for each server
					if(serverStub.exists(name)) { 										// checks if file exists
						exists = true;													// if it does set to tre
						break; 															// and break out of loop
					}

    	} catch(RemoteException e) {
    		System.out.println("Error checking for files existance.");
    	}

    	return exists; 																	// return boolean for if file exists or not
    }

    private List<ServerInterface> getServers() {
    	/* returns a list of all servers registered to rmi */
    	List<ServerInterface> serverStubs = new ArrayList<ServerInterface>();
    	flag = false; 																	// flag shows if RMI is up or down

    	try {
    		ArrayList<String> servers = new ArrayList<String>(Arrays.asList(registry.list())); 		// get registry list
		    System.out.println("\nCurrently online servers: ");
		    for(String server : servers)
		    	if (server.indexOf("server") == 0) {									// check it is an expected server
			    	serverStubs.add((ServerInterface) registry.lookup(server)); 		// lookup server and add to array
			    	System.out.println(server);
		    	}

    	} catch(Exception e) {
    		System.out.println("Registry is not working properly, and might be down.");
    		flag = true;
    	}

    	if (serverStubs.size() == 0)
    		return null;																// return null if no servers
    	else
    		return serverStubs; 														// else return list of servers
    }

    public static void main(String args[]) {
		try {

			// Get registry
		    registry = LocateRegistry.getRegistry("localhost", 11111); 					// find registry on port 11111

		    // Create server object
			FrontEnd obj = new FrontEnd(); 												// make new front end object

		    // Create remote object stub from server object
			FrontEndInterface stub = (FrontEndInterface) UnicastRemoteObject.exportObject(obj, 0); // export it

			try {
				registry.lookup("frontend");											// check if already registered
				System.out.println("Frontend is already running, multiple instances "
						+ "aren't allowed.");
				System.exit(0); 														// close program if already registered
			} catch (NotBoundException e) {												// we want it not to be
				  // Bind the remote object's stub in the registry
			    registry.bind("frontend", stub); 										// if not registered bind it to registry

			    // Write ready message to console
			    System.err.println("Frontend ready"); 									// announce front end is ready

			    obj.runCheck(obj); 														// start checking the rmi is still up

			    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() { 		// when it is shutdown
			        public void run() {
			        	try {
			        		System.out.println("Unbinding from RMI");
				            registry.unbind("frontend"); 								// unbind from registry
			        	} catch (Exception e) {
			        		System.out.println("Error unbinding from RMI");
			        	}
			        }
			    }, "Shutdown-thread"));
			}

		} catch (Exception e) {
		    System.err.println("Frontend can not connect to RMI registry, please ensure the registry"+
		    		" was started in the 'Frederick_Li' folder. \n... Trying to connect again ...");
		    main(args); 																// if error loading, try again
		}
    }
}
