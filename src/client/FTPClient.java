package client;

import java.io.*;
import java.net.Socket;

public class FTPClient
{
    public static void main(final String[] args)
    {
        if(args.length == 0)
        {
            System.err.println("Usage:  java FTPClient <FTP server host address> [FTP server port]");
        }
        else
        {
            final String fTPServerAddress = args[0];
            int fTPServerControlPort = 21;

            if(args.length == 2)
            {
                try
                {
                    fTPServerControlPort = Integer.parseInt(args[1]);
                }
                catch(final NumberFormatException e)
                {
                    System.err.println("Usage:  FTP server port specified is not numeric");
                }
            }

            Socket fTPServerControlSocket = null;
            BufferedReader controlSocketInputStream = null;
            PrintWriter controlSocketOutputStream = null;

            try
            {
                System.out.println("Connecting to FTP server " + fTPServerAddress + ":" + fTPServerControlPort + "...");

                fTPServerControlSocket = new Socket(fTPServerAddress, fTPServerControlPort);
                controlSocketInputStream = new BufferedReader(new InputStreamReader(fTPServerControlSocket.getInputStream()));
                controlSocketOutputStream = new PrintWriter(fTPServerControlSocket.getOutputStream(), true);

                final char[] incomingData = new char[1024];
                final int numBytesRead = controlSocketInputStream.read(incomingData, 0, incomingData.length);

                if(numBytesRead > 0)
                {
                    System.out.println("Welcome message:\n" + new String(incomingData, 0, numBytesRead));

                    processUserCommands(controlSocketInputStream, controlSocketOutputStream);
                }
                else
                {
                    System.err.println("Connection to FTP server failed");
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
                    catch (final IOException e)
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
                    catch (final IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void processUserCommands(final BufferedReader controlSocketInputStream, final PrintWriter controlSocketOutputStream)
    {
       BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

       String command = null;

       do
       {
           System.out.print("FTP command:  ");

           try
           {
               command = stdIn.readLine();
           }
           catch (final IOException e)
           {
               e.printStackTrace();
           }

           processUserCommand(command, controlSocketInputStream, controlSocketOutputStream);
       }while((command != null) && !command.equalsIgnoreCase("QUIT") && !command.equalsIgnoreCase("BYE"));
    }

    private static void processUserCommand(final String command, final BufferedReader controlSocketInputStream, final PrintWriter controlSocketOutputStream)
    {
        controlSocketOutputStream.println(command);

        try
        {
            System.out.println(controlSocketInputStream.readLine());
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }
    }
}