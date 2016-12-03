import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Sid on 11/27/16.
 */
public class fServer {

    public static int port = 5521;
    public static final int MAX_CONCURRENT_CONN=100;
    public static final int DEFAULT_CONCURRENT_CONN=10;

    public static void main(String[] args) throws IOException {

        ServerSocket sock = new ServerSocket(port, DEFAULT_CONCURRENT_CONN);

        System.out.println("Server Hostname : " + InetAddress.getLocalHost().getHostName());
        System.out.println("Server started on port " + port);

        while (true) {
            try {
                System.out.println("Waiting for Connection...");
                Socket soc = sock.accept();
                System.out.println("Incoming connection from " + soc.getRemoteSocketAddress());
                handler obj = new handler(soc);
                Thread t = new Thread(obj);
                t.start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}

class handler implements Runnable {

    private Socket soc;
    DataInputStream din;
    DataOutputStream dos;

    handler(Socket sock) {
        this.soc = sock;
        try {
            din = new DataInputStream(soc.getInputStream());
            dos = new DataOutputStream(soc.getOutputStream());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void cmdProcessor(String cmd, PrintWriter out) {

        String[] args = cmd.split(" ");
        switch (args[0]) {

            case "CREATE":
                break;

            case "PWD":
                out.println("Success");
                break;

            default:
                break;
        }
    }

    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            out = new PrintWriter(soc.getOutputStream(),true);
            String clientCmd = null;

            out.println("ok\r\n");
            out.flush();
            out.println("220 localhost connected\r\n");
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
            out.println("230 OK. Current directory is /");

            boolean connected = true;

            while (connected) {
                clientCmd = in.readLine();
                System.out.println("Command from client: "+ clientCmd);
                if (clientCmd != null) {
                    if (clientCmd.equals("END")) {
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
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                if (soc != null)
                    soc.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}