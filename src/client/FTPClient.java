package client;

import common.FTPCommand;

import java.io.*;
import java.net.Socket;

public class FTPClient
{
    public static void main(final String[] args)
    {
        if((args.length == 0) || (args.length > 2))
        {
            System.out.println("Usage:  java FTPClient <FTP server host address> [FTP server port]");
            System.out.flush();
        }
        else
        {
            final String fTPServerAddress = args[0];
            int fTPServerControlPort = -1;

            if(args.length == 1)
            {
                fTPServerControlPort = 21;
            }
            else
            {
                try
                {
                    fTPServerControlPort = Integer.parseInt(args[1]);

                    if(fTPServerControlPort < 0)
                    {
                        System.out.println("Usage:  FTP server port can not be negative");
                        System.out.flush();
                    }
                }
                catch(final NumberFormatException e)
                {
                    System.out.println("Usage:  FTP server port specified is not an integer");
                    System.out.flush();
                }
            }

            if(fTPServerControlPort > 0)
            {
                Socket fTPServerControlSocket = null;
                BufferedReader controlSocketInputStream = null;
                PrintWriter controlSocketOutputStream = null;

                try
                {
                    System.out.println("Connecting to FTP server " + fTPServerAddress + ":" + fTPServerControlPort + "...");
                    System.out.flush();

                    fTPServerControlSocket = new Socket(fTPServerAddress, fTPServerControlPort);
                    controlSocketInputStream = new BufferedReader(new InputStreamReader(fTPServerControlSocket.getInputStream()));
                    controlSocketOutputStream = new PrintWriter(fTPServerControlSocket.getOutputStream(), true);

                    final char[] incomingData = new char[1024];
                    final int numBytesRead = controlSocketInputStream.read(incomingData, 0, incomingData.length);

                    if(numBytesRead > 0)
                    {
                        System.out.println("Welcome message:\n" + new String(incomingData, 0, numBytesRead));
                        System.out.flush();

                        processUserCommands(controlSocketInputStream, controlSocketOutputStream);
                    }
                    else
                    {
                        System.out.println("Connection to FTP server failed");
                        System.out.flush();
                    }

                    controlSocketInputStream.close();
                    controlSocketOutputStream.close();
                }
                catch(final IOException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    if(controlSocketInputStream != null)
                    {
                        try
                        {
                            controlSocketInputStream.close();
                        }
                        catch(final IOException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    if(controlSocketOutputStream != null)
                    {
                        controlSocketOutputStream.close();
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

    private static void processUserCommands(final BufferedReader controlSocketInputStream, final PrintWriter controlSocketOutputStream)
    {
       BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

       String userInput;
       boolean done = false;

       do
       {
           System.out.print("FTP command:  ");
           System.out.flush();

           userInput = null;

           try
           {
               userInput = stdIn.readLine();
           }
           catch (final IOException e)
           {
               e.printStackTrace();
           }

           if(userInput != null)
           {
               done = processUserCommand(userInput, controlSocketInputStream, controlSocketOutputStream);
           }
       }while(!done);
    }

    private static boolean processUserCommand(final String userInput, final BufferedReader controlSocketInputStream, final PrintWriter controlSocketOutputStream)
    {
        boolean done = false;
        FTPCommand fTPCommand = null;

        try
        {
            fTPCommand = FTPCommand.fromString(userInput.split(" ")[0]);
        }
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

            controlSocketOutputStream.println(userInput);

            try
            {
                System.out.println(controlSocketInputStream.readLine());
                System.out.flush();
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
        }

        return done;
    }
}