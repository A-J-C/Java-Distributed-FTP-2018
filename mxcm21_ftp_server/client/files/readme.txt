I have two packages, in the first 'client', is my program Client.java. In the second is 'server', is my program Server.java.
Make sure your computer has access to javac. Files that you wish to transfer from the client should be in the "files" subfolder in the "client" folder.

First on the command-line navigate to the folder  "Sardar_Jaf".
Run and compile the server by typing "javac server\Server.java & java server.Server", it uses port 1234, so make sure nothing is currently using this port.
Next open a new command-line and navigate to "Sardar_Jaf" launch the client by typing "javac client\Client.java & java client.Client"

The above can be achieved in Windows my running my batch file "compileAndRun.bat"

Your client will then be prompted with a menu. 
Using either the number or four-letter abbreviation you can perform all the commands.
Appropriate prompts will then instruct you on what to do, and all errors are handled appropriately. 
If there are any catastrophic errros the Client will close and create a new connection, if the Server goes down, it will keep trying to reconnect untill the Server is back up.

My Server works for multiple Clients.