REM add javac to path
set "path=%path%;C:\Program Files (x86)\Java\jdk1.8.0_111\bin"		

REM compile all interfaces
javac mxcm21_ftp_server\interfaces\*.java 			
REM compile all servers	
javac mxcm21_ftp_server\server\*.java				
REM compile frontend	
javac mxcm21_ftp_server\frontend\*.java			
REM compile client		
javac mxcm21_ftp_server\client\*.java					

REM load rmi registry
start cmd /k rmiregistry 11111 &
REM launch server after sleeping for 2 seconds to allow rmi to load
TIMEOUT 2
start cmd /k java mxcm21_ftp_server.server.Server 1
start cmd /k java mxcm21_ftp_server.server.Server 2
start cmd /k java mxcm21_ftp_server.server.Server 3
REM launch frontend 
start cmd /k java mxcm21_ftp_server.frontend.FrontEnd		
REM launch client after sleeping for 5 seconds to give everything a chance to bind to rmi
TIMEOUT 5
start cmd /k java mxcm21_ftp_server.client.Client 				