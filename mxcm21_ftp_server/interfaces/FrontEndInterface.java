package mxcm21_ftp_server.interfaces;

import java.rmi.*; 																// for Remote and Exception
import java.io.File;

public interface FrontEndInterface extends Remote {
	String list() throws RemoteException;
	File download(String name) throws RemoteException;
	String upload(File file, String filename, boolean reliable) throws RemoteException;
	String delete(String name) throws RemoteException;
	boolean exists(String name) throws RemoteException;
}
