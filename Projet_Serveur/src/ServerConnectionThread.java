import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class ServerConnectionThread implements Runnable{

    Serveur serverObject;
    ServerSocket thisServer;
    String clientToken;
    String clientAdress;
    Socket clientServer;
    public ServerConnectionThread(Serveur serverObject,ServerSocket server) {
        this.serverObject = serverObject;
        this.thisServer = server;
    }
    public void run() {
        try{
            /*String data = null;
            clientServer = thisServer.accept();
            new Thread(new ServerConnectionThread(serverObject,thisServer)).start();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientServer.getInputStream()));
            while ((data = in.readLine()) != null) {
                String[] dataArray = data.split(" ");

                if(dataArray[0].equalsIgnoreCase("LS")){
                    System.out.println("LS requested from other server");
                    // get list of all available files
                    String[] instigatorInfo = dataArray[1].split(":");
                    ArrayList<String> response = new ArrayList<>();
                    response =  serverObject.findAvailableFiles(InetAddress.getByName(instigatorInfo[0].substring(1)),Integer.parseInt(instigatorInfo[1]));
                    System.out.println(response);
                    String responseString = "";
                    for (String str:response) {
                        responseString = responseString.concat(str + " ");
                    }
                    PrintWriter out = new PrintWriter(clientServer.getOutputStream(),true);
                    out.println(responseString);
                    out.flush();
                    System.out.println(responseString);
                }

                System.out.println("\r\nMessage from " + clientServer.getInetAddress().toString() + ": " + data);
            }*/
        }
        catch(Exception e){
            System.out.println(e);
        }

    }
}


