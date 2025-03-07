import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class ServerLink {
    public Socket linkSocket;
    public String linkToken;


    public ServerLink(Socket socket){
        linkSocket = socket;
        try{
            BufferedReader bfr = new BufferedReader(new InputStreamReader(linkSocket.getInputStream()));

            PrintWriter out = new PrintWriter(linkSocket.getOutputStream(),true);
            out.println("REGISTER " + linkSocket.getInetAddress().toString());
            out.flush();

            String response = bfr.readLine();
            String[] splitResponse = response.split(" ");
            linkToken = splitResponse[1];

        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public String SendLSRequest(InetAddress instigatorAddress, int instigatorPort)
    {
        String response = "";
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(linkSocket.getInputStream()));

            PrintWriter out = new PrintWriter(linkSocket.getOutputStream(),true);
            out.println("LS " + linkToken + " " + instigatorAddress.toString() + ":" + instigatorPort);
            out.flush();

            response = in.readLine();

        }
        catch (Exception e){
            System.out.println(e);

        }
        return response;
    }
}
