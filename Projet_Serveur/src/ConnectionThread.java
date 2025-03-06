import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.IllegalFormatCodePointException;
import java.util.UUID;

public class ConnectionThread implements Runnable {
    ServerSocket server;
    String clientToken;
    String clientAdress;
    Socket client;
    public ConnectionThread(ServerSocket server) {
        this.server = server;
    }

    public void run() {
        try {
            String data = null;
            client = server.accept();
            new Thread(new ConnectionThread(server)).start();
            clientAdress = "";
            boolean writeAuthorized = false;

            // Parametre decodage
            String nomFicher;
            int offset;
            int isLast;
            String contenuFichier;


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
                    else{
                        //Ls
                        if(dataArray[0].equalsIgnoreCase("LS")){
                            PrintWriter out = new PrintWriter(client.getOutputStream(),true);

                            if(dataArray[1].equalsIgnoreCase(clientToken) && clientAdress.equalsIgnoreCase(client.getInetAddress().toString())){
                                out.println("Liste des fichiers");
                                out.flush();
                            }
                            else{
                                out.println("LS UNAUTHORIZED");
                                out.flush();
                            }

                        }else{
                            //Write
                            if(dataArray[0].equalsIgnoreCase("WRITE")){
                                PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                                if(dataArray[1].equalsIgnoreCase(clientToken) /* && verifier si le file existe dans la liste du server*/){
                                    writeAuthorized = true;
                                    out.println("WRITE AUTHORIZED");
                                    out.flush();
                                }
                                else{
                                    writeAuthorized = false;
                                    out.println("WRITE UNAUTHORIZED OR FILE ALREADY EXISTS");
                                    out.flush();
                                }
                            }
                            else if(dataArray[0].equalsIgnoreCase("FILE")){
                                PrintWriter out = new PrintWriter(client.getOutputStream(),true);

                                if(writeAuthorized) {
                                    // Décoder le message
                                    nomFicher = dataArray[1];
                                    offset = Integer.parseInt(dataArray[2]);
                                    isLast = Integer.parseInt(dataArray[3]);
                                    contenuFichier = dataArray[4];


                                    out.println("FILE AUTHORIZED");
                                    out.flush();
                                }
                                else  {
                                    out.println("FILE UNAUTHORIZED");
                                    out.flush();
                                }
                            }

                            else{
                                //Read
                                if(dataArray[0].equalsIgnoreCase("READ")){
                                    PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                                    if(dataArray[1].equalsIgnoreCase(clientToken) && clientAdress.equalsIgnoreCase(client.getInetAddress().toString())){
                                        out.println("READ!");
                                        out.flush();
                                    }
                                    else{
                                        out.println("READ UNAUTHORIZED");
                                        out.flush();
                                    }

                                }

                            }
                        }
                    }
                }
                else{
                    PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                    out.println("NO CONNECTION TOKEN PROVIDED");
                    out.flush();
                }


                System.out.println("\r\nMessage from " + clientAdress + ": " + data);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
