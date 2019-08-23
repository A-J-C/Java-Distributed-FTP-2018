package mxcm21_ftp_server.interfaces;

import java.rmi.Remote;                                                         // for remote and exception
import java.rmi.*;
import java.util.List;
import java.io.File;

public interface ServerInterface extends Remote {
    List<String> list() throws RemoteException;
    File download(String name) throws RemoteException;
	boolean upload(File file, String filename) throws RemoteException;
	boolean delete(String name) throws RemoteException;
	boolean exists(String name) throws RemoteException;
	int numberOfFiles() throws RemoteException;
}
