All my java files are in the package "mxcm21_ftp_server".
Make sure you have javac and java (on a University Windows machine you might have to first run Eclipse to have access to these)

First navigate on the command-line to the folder "Frederick_Li". The file compileAndRun.bat* will execute the following commands automatically:
	- rmiregistry 11111 (running on port 11111)
	- javac mxcm21_ftp_server\interfaces\*.java  (compiles all interfaces)
	- javac mxcm21_ftp_server\server\*.java	& java mxcm21_ftp_server.server.Server x (where x is 1,2, or 3 for the three different servers, if no x is supplied, or an 
	  incorrect x is, it will bring online the first available server, if all 3 servers are running it won't run anything)
	- javac mxcm21_ftp_server\frontend\*.java & java mxcm21_ftp_server.frontend.FrontEnd (the frontend connects the client to the servers)
	- javac mxcm21_ftp_server\client\*.java	& java mxcm21_ftp_server.client.Client (the client will prompt you with appropriate menus for carrying out all tasks).

If the rmiregistry, front end, or all servers go down appropriate messages will be displayed to the client, and efforts
made to reconnect everything, meaning as soon as the rmiregistry is back up the servers will automatically reconnect, 
or if the frontend is brought down and backup the client will keep trying to reconnect untill it is brought backup.

* (If you get an "Access Denied" error trying to execute the batch file - use the commands javac & java once on the command line to fix it)
* (If you get a file not found error navigate to the folder by clicking on the J drive directly. i.e. if the address in windows explorer is \\Hudson\ instead of J:\ it won't find the file)