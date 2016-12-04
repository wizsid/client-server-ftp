package client;

import common.FTPCommand;

import java.io.*;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * Represent the FTP client
 */
public class FTPClient
{
    private static File localDirectory = null;
    private static String fTPServerAddress = null;

    private static Socket fTPServerControlSocket = null;
    private static BufferedReader controlSocketIn = null;
    private static PrintWriter controlSocketOut = null;

    private static Socket fTPServerDataSocket = null;

    public static void main(final String[] args)
    {
        //Display a usage message if the number of arguments is not correct
        if((args.length < 2) || (args.length > 3))
        {
            System.out.println("Usage:  java FTPClient <Local Directory> <FTP server host address> [FTP server port]");
            System.out.flush();
        }
        else
        {
            localDirectory = new File(args[0]);

            //Create the local directory if it does not exist
            if(!localDirectory.exists())
            {
                localDirectory.mkdirs();
            }

            fTPServerAddress = args[1];
            int fTPServerControlPort = -1;

            //If user did not specify control port, assume it is the default of 21
            if(args.length == 2)
            {
                fTPServerControlPort = 21;
            }
            else
            {
                try
                {
                    fTPServerControlPort = Integer.parseInt(args[2]);

                    //Display an error message if the control port is negative integer
                    if(fTPServerControlPort < 0)
                    {
                        System.out.println("FTP server port can not be negative");
                        System.out.flush();
                    }
                }
                //Display an error message if the control port is not an integer
                catch(final NumberFormatException e)
                {
                    System.out.println("FTP server port specified is not an integer");
                    System.out.flush();
                }
            }

            if(fTPServerControlPort > 0)
            {
                try
                {
                    System.out.println("Connecting to FTP server " + fTPServerAddress + ":" + fTPServerControlPort + "...");
                    System.out.flush();

                    fTPServerControlSocket = new Socket(fTPServerAddress, fTPServerControlPort);
                    controlSocketIn = new BufferedReader(new InputStreamReader(fTPServerControlSocket.getInputStream()));
                    controlSocketOut = new PrintWriter(fTPServerControlSocket.getOutputStream(), true);

                    final char[] incomingData = new char[2048];
                    //Read the welcome message from the control socket
                    final int numBytesRead = controlSocketIn.read(incomingData, 0, incomingData.length);

                    if(numBytesRead > 0)
                    {
                        System.out.println("Welcome message:\n" + new String(incomingData, 0, numBytesRead));
                        System.out.flush();

                        processUserCommands();
                    }
                    else
                    {
                        System.out.println("Connection to FTP server failed");
                        System.out.flush();
                    }
                }
                catch(final IOException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    if(controlSocketIn != null)
                    {
                        try
                        {
                            controlSocketIn.close();
                        }
                        catch(final IOException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    if(controlSocketOut != null)
                    {
                        controlSocketOut.close();
                    }

                    if(fTPServerControlSocket != null)
                    {
                        try
                        {
                            fTPServerControlSocket.close();
                        }
                        catch(final IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static void processUserCommands()
    {
       final BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

       String userInput;
       boolean done = false;

       do
       {
           System.out.print("FTP command:  ");
           System.out.flush();

           userInput = null;

           try
           {
               //Read user's input from keyboard
               userInput = stdIn.readLine();
           }
           catch (final IOException e)
           {
               e.printStackTrace();
           }

           if(userInput != null)
           {
               done = processUserCommand(userInput);
           }
       }while(!done);
    }

    private static boolean processUserCommand(final String userInput)
    {
        boolean done = false;
        FTPCommand fTPCommand = null;

        //Cast user's input into an FTPCommand enum value
        try
        {
            fTPCommand = FTPCommand.fromUserInput(userInput);
        }
        //Display an error message if user's command is not supported
        catch(final IllegalArgumentException e)
        {
            System.out.println("Illegal or unsupported FTP command.");
            System.out.flush();
        }

        if(fTPCommand != null)
        {
            if(fTPCommand.equals(FTPCommand.QUIT))
            {
                done = true;
            }

            //send the command to FTP server
            controlSocketOut.println(userInput);

            try
            {
                //read FTP server response
                String fTPServerResponse = controlSocketIn.readLine();

                System.out.println(fTPServerResponse);
                System.out.flush();

                switch(fTPCommand)
                {
                    case EPSV:
                        if(fTPServerResponse.startsWith("229"))
                        {
                            //Parse out data port
                            final int fTPServerDataPort = Integer.parseInt(fTPServerResponse.replace("|", "")
                                                                                            .split(Pattern.quote("("))[1]
                                                                                            .split(Pattern.quote(")"))[0]);
                            //Connect data socket to data port sent by the server
                            fTPServerDataSocket = new Socket(fTPServerAddress, fTPServerDataPort);
                        }

                        break;
                    case MLSD:
                    case NLST:
                    case LIST:
                        if(fTPServerResponse.startsWith("150"))
                        {
                            //read the next response line for MLSD, NLST or LIST command (directory listing)
                            fTPServerResponse = controlSocketIn.readLine();

                            System.out.println(fTPServerResponse);
                            System.out.flush();

                            if(fTPServerResponse.startsWith("226"))
                            {
                                final BufferedReader dataSocketInputStream = new BufferedReader(new InputStreamReader(fTPServerDataSocket.getInputStream()));

                                final char[] incomingData = new char[2048];
                                //Read directory listing into incomingData
                                final int numBytesRead = dataSocketInputStream.read(incomingData, 0, incomingData.length);

                                if(numBytesRead > 0)
                                {
                                    //Display directory listing
                                    System.out.println(new String(incomingData, 0, numBytesRead));
                                    System.out.flush();
                                }

                                dataSocketInputStream.close();
                            }
                        }

                        closeFTPServerDataSocket();

                        break;
                    case RETR:
                        if(fTPServerResponse.startsWith("150"))
                        {
                            //read the next response line for RETR command (download file)
                            fTPServerResponse = controlSocketIn.readLine();

                            System.out.println(fTPServerResponse);
                            System.out.flush();

                            if(fTPServerResponse.startsWith("226"))
                            {
                                final InputStream dataIn = fTPServerDataSocket.getInputStream();
                                final OutputStream fileOut = new FileOutputStream(new File(localDirectory, userInput.split(fTPCommand.name())[1]));

                                final byte[] incomingData = new byte[2048];
                                int bytesRead;

                                //Read file data from the data socket
                                while((bytesRead = dataIn.read(incomingData)) > 0)
                                {
                                    //Write file data to the local file
                                    fileOut.write(incomingData, 0, bytesRead);
                                }

                                fileOut.close();
                                dataIn.close();
                            }
                        }

                        closeFTPServerDataSocket();

                        break;
                    case STOR:
                        if(fTPServerResponse.startsWith("150"))
                        {
                            final OutputStream dataOut = fTPServerDataSocket.getOutputStream();
                            final InputStream fileIn = new FileInputStream(new File(localDirectory, userInput.split(fTPCommand.name())[1].trim()));

                            final byte[] outgoingData = new byte[2048];
                            int bytesRead;

                            //Read data from the local file
                            while((bytesRead = fileIn.read(outgoingData)) > 0)
                            {
                                //Write data to the data socket
                                dataOut.write(outgoingData, 0, bytesRead);
                            }

                            fileIn.close();
                            dataOut.close();

                            //read and print the response for STOR command (upload file)
                            System.out.println(controlSocketIn.readLine());
                            System.out.flush();
                        }

                        closeFTPServerDataSocket();

                        break;
                }
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
        }

        return done;
    }

    private static void closeFTPServerDataSocket()
    {
        if(fTPServerDataSocket != null)
        {
            try
            {
                fTPServerDataSocket.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}