import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class ConnectionThread implements Runnable {
    ServerSocket server;
    String clientToken;

    public ConnectionThread(ServerSocket server) {
        this.server = server;
    }

    public void run() {
        try {
            String data = null;
            Socket client = server.accept();
            new Thread(new ConnectionThread(server)).start();
            String clientAdress = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            while ((data = in.readLine()) != null) {
                //traitement des types  de messages
                //Register
                String[] dataArray = data.split(" ");
                if(dataArray.length > 1){
                    if(dataArray[0].equalsIgnoreCase("REGISTER")){
                        clientToken = UUID.randomUUID().toString().replace("-","").substring(0,20);
                        clientAdress = dataArray[1];
                        System.out.println(clientAdress);
                        PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                        out.println("REGISTERED " + clientToken);
                        out.flush();
                        System.out.println(clientToken);

                    }
                    //Ls
                    if(dataArray[0].equalsIgnoreCase("LS")){
                        if(dataArray[1].equalsIgnoreCase(clientToken)){
                            System.out.println("liste des fichiers!");
                        }
                        else{
                            System.out.println("LS UNAUTHORIZED");
                        }

                    }
                    //Write
                    if(data.equalsIgnoreCase("WRITE")){
                        if(dataArray[1].equalsIgnoreCase(clientToken)){
                            System.out.println("Write!");
                        }
                        else{
                            System.out.println("WRITE UNAUTHORIZED");
                        }
                    }
                    //File
                    if(data.equalsIgnoreCase("FILE")){
                        System.out.println("Write File!");
                    }
                    //Read
                    if(data.equalsIgnoreCase("READ")){
                        System.out.println("read!");
                    }


                }


                System.out.println("\r\nMessage from " + clientAdress + ": " + data);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
