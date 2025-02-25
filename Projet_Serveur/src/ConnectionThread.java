import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
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
                            if(data.equalsIgnoreCase("WRITE")){
                                PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                                if(dataArray[1].equalsIgnoreCase(clientToken) && clientAdress.equalsIgnoreCase(client.getInetAddress().toString())){
                                    out.println("WRITE!");
                                    out.flush();
                                }
                                else{
                                    out.println("WRITE UNAUTHORIZED");
                                    out.flush();
                                }
                            }
                            else{
                                //File
                                if(data.equalsIgnoreCase("FILE")){
                                    System.out.println("Write File!");
                                }
                                else{
                                    //Read
                                    if(data.equalsIgnoreCase("READ")){
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
