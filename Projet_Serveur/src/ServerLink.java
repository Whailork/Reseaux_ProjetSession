import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class ServerLink {
    public Socket linkSocket;
    public String linkToken;

    // le serveur link est la classe qui sert à conserver les informations de connexion entre le serveur et un peer
    public ServerLink(Socket socket){
        linkSocket = socket;
        try{
            //on établit lors de la création, un connexion avec le peer en tant que client
            BufferedReader bfr = new BufferedReader(new InputStreamReader(linkSocket.getInputStream()));

            PrintWriter out = new PrintWriter(linkSocket.getOutputStream(),true);
            out.println("REGISTER|" + linkSocket.getInetAddress().toString());
            out.flush();

            String response = bfr.readLine();
            String[] splitResponse = response.split("\\|");
            linkToken = splitResponse[1];

        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    //pour recevoir un fragment lors du transfert pendant le read redirect
    public String ReceiveFragment(){
        String response = "";
        try{
            PrintWriter out = new PrintWriter(linkSocket.getOutputStream(),true);
            BufferedReader in = new BufferedReader(new InputStreamReader(linkSocket.getInputStream()));
            out.println("Fragment Received");
            out.flush();

            response = in.readLine();
        }
        catch(Exception e){
            System.out.println(e.toString());
        }
        return response;
    }
    //pour propager la requête de read au serveur peer lors d'un read redirect
    public String SendReadRequest(String fileName,InetAddress instigatorAddress, int instigatorPort){
        String response = "File not found";
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(linkSocket.getInputStream()));

            PrintWriter out = new PrintWriter(linkSocket.getOutputStream(),true);

            out.println("READ|" + linkToken + "|"+ fileName + "|" + instigatorAddress.toString() + ":" + instigatorPort);
            out.flush();
            response = in.readLine();

        }
        catch (Exception e){
            System.out.println(e);

        }
        return response;
    }
    //pour propager aux peers l'ajout d'un nouveau fichier sur le serveur
    public void AddNewFileToAvailableFiles(String fileName, String serverIp, String serverPort){
        try{
            PrintWriter out = new PrintWriter(linkSocket.getOutputStream(),true);

            out.println("AddAvailableFile|"+fileName+"|"+serverIp+"|"+serverPort);
            out.flush();
        }catch(Exception e){
            System.out.println(e);
        }


    }
}
