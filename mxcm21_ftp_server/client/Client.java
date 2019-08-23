package mxcm21_ftp_server.client;

import java.rmi.registry.*;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.concurrent.*;
import java.io.*;
import java.nio.channels.FileChannel;

import mxcm21_ftp_server.interfaces.*; 											// need access to interfaces

public class Client {

	private static FrontEndInterface frontEndStub;
	private static Scanner scan;
	private String directory;

    private Client() {

		try {
			connect(); 															// connect to front end
			scan = new Scanner(System.in); 										// initialsie scanner
			directory = "mxcm21_ftp_server/client/files/";						// directory path

			/* CLIENT'S PROMPT USER FOR SELECTION STATE */
			while(true) {														// loop till client wants to exit
				System.out.print("\n===== Selection =====\n" + 					// ask for input
					"1. CONN: Connect to Server\n" +
					"2. UPLD: Upload File\n" +
					"3. LIST: List Files\n" +
					"4. DWLD: Downlaod File\n" +
					"5. DELF: Delete File\n" +
					"6. QUIT: Close connection and exit\n" +
					"Choice (number or 4 letter abbreviation): ");

				String userInput = scan.nextLine().toUpperCase();				// get user choice

				/* CHECKS INPUT WAS CORRECT AND TURNS NUMBERS INTO 4 LETTER ABBREVIATIONS */
				switch(userInput) { 											// turn user choice into abbreviation
					case "1": userInput = "CONN"; break;
					case "CONN": break;
					case "2": userInput = "UPLD"; break;
					case "UPLD": break;
					case "3": userInput = "LIST"; break;
					case "LIST": break;
					case "4": userInput = "DWLD"; break;
					case "DWLD": break;
					case "5": userInput = "DELF"; break;
					case "DELF": break;
					case "6": userInput = "QUIT"; break;
					case "QUIT": break;
					default: userInput = "ERROR"; break;
				}

				if(userInput.equals("ERROR")) {								    // if user input wasn't correct
					System.out.println("\nIncorrect input, please choose again.");
					continue; 													// continue while loop
				}

				try {
					switch(userInput) {											// run associated function
						/* Runs function based on User Input */
						case "CONN": connect(); break;
						case "UPLD": upload(); break;
						case "LIST": list(); break;
						case "DWLD": download(); break;
						case "DELF": deleteFile(); break;
						case "QUIT": quit(); break;
						default: break;
					}
				} catch (RemoteException e) {									// catch error
					System.out.println("\n=== Frontend appears to be offline ===\n" +
							" Please restart it. Client will reconnect when it is up.");
					connect();													// try to connect to frontend again
				}

				if (userInput.equals("QUIT")) 									// break out of while loop if user wants to quit
					break;
			}
		} catch (Exception e) {
			System.err.println("Client exception!");
		} finally {
			System.out.println("\nThanks for using mxcm21's FTP server.\n" + 	// inform user session is closed
					"Session has been closed. Bye.");
		}
	}

    private void connect() {
    	try {
	    	// Get registry
	    	Registry registry = LocateRegistry.getRegistry("localhost", 11111);	// connect to registry

	    	/* Get stub for frontend */
	    	frontEndStub = (FrontEndInterface) registry.lookup("frontend");		// lookup front end
	    	System.out.println("\nConnected to mxcm21's FTP Frontend!\n"); 		// announce successfully connected
    	} catch (Exception e) { 												// catch excceptions
    		try {
	    		System.out.println("Error conencting ... check rmiregistry and frontend are running ... reconnecting ...");
	    		TimeUnit.SECONDS.sleep(2);										// pause inbetween checking
	    		connect(); 														// try to reconnect
    		} catch(InterruptedException ex) {System.out.println("Error reconnecting.");}
    	}
	}

    private void upload() throws RemoteException {
    	/* UPLOADS FILE TO SERVER */
    	File file = null;
		String fileName = "";

    	do {
			System.out.println("Filename that you would like to upload (including the extension): "); 	// ask user for input
			fileName = scan.nextLine();												// get user input

			if (fileName.indexOf("/") == -1  && fileName.indexOf("\\") == -1)
				fileName = directory + fileName; 									// add path if just a file name

			file = new File(fileName);												// get file

			if(!file.exists()) 														// check file exists
				System.out.println("That file doesn't exist, make sure the file is in the client 'files'" +
						" folder or provide a direct path.\nType QUIT to return to main menu.");
		} while (!file.exists() && !fileName.equals(directory + "QUIT")); 			// loop until it exists or user wants to quit

    	if (file.exists()) { 														// if it does exist
    		boolean reliable = getConfirm("Do you want high reliability for this file? (Yes/No)"); 	// get reliability flag
    		String[] fileparts = fileName.split("[\\\\/]"); 						// extract file name from directory path
			fileName = fileparts[fileparts.length - 1];
    		System.out.println(frontEndStub.upload(file, fileName, reliable));		// upload through frontend
    	}
	}

	private void list() throws RemoteException {
		System.out.print(frontEndStub.list());										// print list generated by frontend
	}

	private void download() throws RemoteException {
		System.out.println("Filename that you would like to download (including the extension): "); 	// ask user for input
		String fileName = scan.nextLine();																// get user input

		boolean exists = frontEndStub.exists(fileName); 												// check if file exists on server
		if (exists) {																					// if it does exist
			try {
				long startTime = System.currentTimeMillis(); 											// get start time
				File origFile = frontEndStub.download(fileName);										// download file
				File newFile = new File(directory + fileName);  										// open new file with same name

				if (origFile != null) {
					FileChannel oldFileOut = new FileInputStream(origFile).getChannel();				// get output channel
					FileChannel newFileIn = new FileOutputStream(newFile).getChannel();					// get input channel
					newFileIn.transferFrom(oldFileOut, 0, oldFileOut.size());							// transfer contents

					oldFileOut.close();																	// close streams
					newFileIn.close();

					long endTime = System.currentTimeMillis(); 											// get end time
					String time = Double.toString((endTime - startTime) / 1000.0); 						// turn to seconds

			    	System.out.println("Successfuly downloaded file: "+fileName +", " + 				// print exception
			    			newFile.length() + " bytes in "+time+" seconds.");
				} else {
					System.out.println("Error downloading file."); 										// print if error
				}
			} catch (IOException e) {
				System.out.println("Error downloading file, please try again.");
			}
		} else {
			System.out.println("\nFile "+fileName+" is not on the server. If you are expecting it to be "+
					"check the RMI Registry and frontend are both running.");							// if file not ons erver it might be because a server or the rmi has gone down
		}
	}

	private void deleteFile() throws RemoteException {
		System.out.println("Filename that you would like to delete (including the extension): "); 		// ask user for input
		String fileName = scan.nextLine();																// get user input

		boolean exists = frontEndStub.exists(fileName); 												// check if file exists
		if (exists) {
			try {
				boolean confirm = getConfirm("File ("+fileName+") exists on the Server. Are you " + 	// ask for confirmation
						"sure you want to delete it, this can not be undo. (Yes/No)");

				if (confirm)																			// if user does want to delete
					System.out.println(frontEndStub.delete(fileName));									// delete file
				else	
					System.out.println("Delete abondoned by the user!"); 								// tell user they stopped it

			} catch (IOException e) {
				System.out.println("Error deleting file, please try again."); 							// print if error downloading
			}
		} else {
			System.out.println("\nFile "+fileName+" is not on the server. If you are expecting it to be "+
					"check the RMI Registry and frontend are both running.");		// if file doesn't exist tell user
		}
	}

	private void quit() throws RemoteException {
		System.out.println("Closing client."); 										// inform client they are quitting
	}

	private boolean getConfirm(String msg) {
		/* Asks the user for yes/no answer */
		String confirm = "";

		while (!confirm.equals("Y") && !confirm.equals("N")) {						// continuously ask till correct input
			System.out.println(msg);

			confirm = scan.nextLine().toUpperCase(); 								// change any input to upper case
			if (confirm.equals("YES"))												// map yes to y
				confirm = "Y";
			else if (confirm.equals("NO"))											// and no to n
				confirm = "N";
			else if (!confirm.equals("Y") && !confirm.equals("N")) 					// if it's not correct input ask again
				System.out.println("Please enter Yes or No.");
		}

		if (confirm.equals("Y")) 													// return boolean result
			return true;

		return false;
	}

    public static void main(String[] args) {
		new Client(); 																// to start launch new client
    }
}
