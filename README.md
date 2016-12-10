FTP Client & Server Implementation
-------------------------------------------------------
Authors : Rolandas Burbulis, Siddharth Subramanian
Course  : 651- Foundations of Computer Netwroks


FTP Server :
    This file contains the implementation of the FTP server which can connect
    multiple clients but can handle only service one client at a time. The
    server can run only UNIX based or LINUX systems due to the implementation
     of the LIST command. You can use this server with an open source and free
     FileZilla client.

    filename    : FTPServer.java
    arguments   : port number greater than 1024
    usage       : java FTPServer <port number>
                    Eg: java FTP Server 5521

    Commands the server can handle :
        PWD  : sends present working directory
        CWD  : changes the working directory to the argument specified
        EPSV : enters passive mode for IPv6
        TYPE : sets the type of trasnfer
        LIST : gives the listing of the present working directory
        STOR : receives a file from the client
        RETR : sends a file to the client
        DELE : deletes a file on the server
        RMD  : removes a directory on the server
        MKD  : makes a new directory on the server


FTP Client :
    Usage:  java FTPClient <local directory> <FTP server adderss> [FTP server
     port]

     Supported commands:
     CWD - change working directory to the argument specified
     DELE - delete specified file
     ESPV - enter passive mode
     LIST - list the contents of the current working directory
     MDTM - returns the last modified time for the specified file
     MKD - creates specified directory
     MLSD - list the contents of the current working directory
     NLST - name list of the contents of the current working directory
     NOOP - no operation
     PASS - send password of the user
     PWD - gets present working directory
     QUIT - disconnects from the FTP server
     RETR - downloads specified file into the local directory
     RMD - deletes specified directory
     RNFR - rename from
     RNTO - rename to
     SIZE - returns the size of the specified file or directory
     STOR - uploads specified file
     TYPE - sets the mode of the file transfer (i.e. I = binary, A = ASCII)
     USER - send username
