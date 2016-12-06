package server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Created by Sid on 12/4/16.
 */
public class FTPServer {

    static int port;
    ServerSocket sock;

    String setSharedFolder() {
        String pwd = System.getProperty("user.dir");
        String path = pwd+"/shared";
        File folder = new File(path);
        if (!folder.exists()) {
            if (folder.mkdir()) {
                System.out.println("Successfully created shared folder");
            } else {
                System.out.println("Failed to create directory!");
            }
        }
        return path;
    }

    public static void main(String[] args) {

        FTPServer obj = new FTPServer();

        obj.port = Integer.parseInt(args[0]);
        String rootDir = obj.setSharedFolder();
        try {
            obj.sock = new ServerSocket(port);
            System.out.println("Server Address : " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Server started on port " + port);

            while (true) {
                System.out.println("Waiting for Connection...");
                Socket soc = obj.sock.accept();
                System.out.println("Incoming connection from " + soc.getRemoteSocketAddress());
                FTPSession fobj = new FTPSession(soc, rootDir);
                Thread t = new Thread(fobj);
                t.start();
            }
        }
        catch (Exception e) {
            System.err.println("Socket already in use");
            e.printStackTrace();
        }
    }
}

class FTPSession implements Runnable {

    // Path information
//    private String root = "/Users/Sid/Workspace/CourseCode/651-Networking/FTP/shared";
    private String root;
    private String currDir;
    private String fileSep = System.getProperty("file.separator");

    // Command Connection
    private Socket cSock;
    private PrintWriter out;
    private BufferedReader in;

    // Data Connection
    private Socket dataSock;
    private OutputStream dataOut;
    private InputStream dataIn;


    FTPSession(Socket soc, String pwd) {
        this.cSock = soc;
        this.root = pwd;
        this.currDir = root;
//        this.currDir = root;
        System.out.println("This is root = " + root);
        System.out.println("This is currDir = " + currDir);
    }

    void deleteFile(String fileName) throws IOException {

        String path = currDir + fileSep + fileName;
        System.out.println(path);
        File file = new File(path);
        if (file.exists()) {
            if(file.delete()) {
                out.println("250 File deleted successfully");
            }
        }
        else {
            out.println("550 File not found");
        }
    }

    void deleteDir(String folder) {
        String path = currDir + fileSep + folder;
        File file = new File(path);
        if( file.isDirectory() ) {
            if(file.list().length>0){
                out.println("550 Directory not empty.");
            }
            else {
                if(file.delete()) {
                    out.println("250 Folder deleted successfully.");
                }
            }
        }
        else {
            System.out.println("Error");
        }
    }

    void receiveFile(String fileName) throws IOException {
        String filePath = currDir+ fileSep + fileName;
        System.out.println("Reading: "+ filePath);
        dataOut = new FileOutputStream(filePath);
        byte[] bytes = new byte[16*1024];
        int count;
        while ((count = dataIn.read(bytes)) > 0) {
            dataOut.write(bytes, 0, count);
        }
        dataOut.close();
        dataIn.close();
    }

    void sendFile(String fileName) throws IOException {

        String filePath = currDir + fileSep + fileName;
        File file = new File(filePath);
        if (file.isDirectory()) {
            currDir = filePath;

        }
        else {
            System.out.println(file.getAbsolutePath());
            RandomAccessFile reader = new RandomAccessFile(file, "rw");
            byte[] fileBytes = new byte[1024];
//        System.out.println(file.getAbsolutePath());
            while (reader.getFilePointer() < reader.length()) {
                dataOut.write(fileBytes, 0, reader.read(fileBytes));
            }
        }



        dataOut.flush();
        dataOut.close();
    }

    void sendFileList() throws IOException {
        String cmd = "ls -l";
        String listing = "";
        try
        {
            Process p = Runtime.getRuntime().exec(cmd, null, new File(currDir));
            //Process p=Runtime.getRuntime().exec("ls -l");

            p.waitFor();
            BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line=reader.readLine();
            while(line!=null)
            {
                listing = listing + line + "\r\n";
                line=reader.readLine();
            }

        }
        catch(IOException e1) {}
        catch(InterruptedException e2) {}
        dataOut.write(listing.getBytes());
        dataOut.flush();
        dataOut.close();
    }

    void cmdProcessor(String cmd, PrintWriter out) throws IOException {

        String[] args = cmd.split(" ");

        switch (args[0]) {

            case "PWD":
                out.println("257 "+'"'+ currDir +'"'+" is current directory.");
                break;

            case "CWD":
                    System.out.println(currDir);
                    if (root.equals(args[1])) {
                        System.out.println("testtest");
                        currDir = args[1];
                        out.println("257 " + '"' + currDir + '"' + " is current directory.");
                    } else {
                        System.out.println("test");
                        String cur = currDir;
                        System.out.println("this is cur " + cur);
                        System.out.println("this is args " + args[1]);
                        currDir = cur + fileSep + args[1];
                        out.println("257 " + '"' + currDir + '"' + " is current directory.");
                    }
//                out.println("257 "+'"' + root + fileSep +currDir +'"'+" is current directory.");

//                    out.println("550 No such directory.");
                break;

            case "TYPE":
                if (args[1].equals("I")) {
                    out.println("200 Type set to I.");
                }
                else {
                    out.println("200 Type set to A.");
                }
                break;

            case "EPSV":
                ServerSocket s = new ServerSocket(0);
                out.println("229 Entering Extended Passive Mode (|||"+ s.getLocalPort() +"|)");
                dataSock = s.accept();
                break;

            case "LIST":
                out.println("150 Opening data connection.\n");
                dataOut = dataSock.getOutputStream();
                sendFileList();
                out.println("226 Transfer complete.\n");
                break;

            case "STOR":
                dataIn = dataSock.getInputStream();
                out.println("150 File status ok.");
                receiveFile(args[1]);
                out.println("226 transfer of " + args[1] + " complete");
                break;

            case "RETR":
                dataOut = dataSock.getOutputStream();
                out.println("150 File status ok.");
                sendFile(args[1]);
                out.println("226 transfer of " + args[1] + " complete");
                break;

            case "DELE":
                deleteFile(args[1]);
                break;

            case "RMD":
                deleteDir(args[1]);
                break;

            case "MKD":
                break;

            default:
                out.println("502 Command not implemented");
                break;
        }
    }

    public void run() {
        //BufferedReader in = null;
        //PrintWriter out = null;
        this.in = null;
        this.out = null;
        try {
            this.in = new BufferedReader(new InputStreamReader(cSock.getInputStream()));
            this.out = new PrintWriter(cSock.getOutputStream(),true);
            String clientCmd = null;
            out.flush();
            out.println("220 Localhost Connected\r\n");
            out.println("331 Anonym no password needed\r\n");
            out.println("211-Features:");
            out.println("MDTM");
            out.println("REST STREAM");
            out.println("SIZE");
            out.println("MLST type*;size*;modify*;");
            out.println("MLSD");
            out.println("UTF8");
            out.println("CLNT");
            out.println("MFMT");
            out.println("211 End");

            boolean connected = true;

            while (connected) {
                clientCmd = in.readLine();

                if (clientCmd != null) {
                    System.out.println("Command from client: "+ clientCmd);
                    if (clientCmd.equals("QUIT")) {
                        connected = false;
                    }
                    else{
                        cmdProcessor(clientCmd, out);
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            currDir = "";
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                if (cSock != null)
                    cSock.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
