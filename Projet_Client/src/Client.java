import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private Scanner scanner;
    private String Token;
    private String Response;
    private Client(InetAddress serverAddress, int serverPort) throws Exception{
        this.socket = new Socket(serverAddress, serverPort);
        this.scanner = new Scanner(System.in);
    }

    //to register Register 192.168.0.15
    private void start() throws IOException {
        String inputToSend;
        String input;
        int fragmentSize = 500;

        StringBuilder contenuMessage = new StringBuilder();
        BufferedReader bfr = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        while(true){

            input = scanner.nextLine();
            String[] tableauInput = input.split(" ");
            if(Token != null){

                String nom_fichier;
                if(tableauInput.length > 1){
                    inputToSend = tableauInput[0] + " " + Token + " " + tableauInput[1];
                }
                else{
                    inputToSend = tableauInput[0] + " " + Token;
                }

                if(tableauInput[0].equalsIgnoreCase("file")) {
                    if (tableauInput.length > 4) {
                        for (int i = 4; i < tableauInput.length; i++) {
                            contenuMessage.append(tableauInput[i]);
                        }
                    } else {
                        if (contenuMessage.length() > fragmentSize) { // Si contenuMessage est plus grand que 500
                            int nmbFragment = contenuMessage.length() / fragmentSize;
                            for (int i = 0; i < nmbFragment; i++) {

                                    // Calculer l'offset et déterminer si c'est le dernier fragment
                                    int offset = (i * fragmentSize);
                                    boolean isLast = false;

                                    if (i == nmbFragment - 1) {
                                        isLast = true;
                                    }

                                    // Découper le fichier en morceaux de 500 caractères
                                    int start = i * fragmentSize;
                                    int end = Math.min(start + fragmentSize, contenuMessage.length());
                                    String fragment = contenuMessage.substring(start, end);

                                    // Afficher le message de type FILE (simuler l'envoi)
                                    System.out.println("FILE|" + tableauInput[2] + "|" + offset + "|" + (isLast ? 1 : 0) + "|" + fragment);

                            }
                        }
                    }
                }
                else{
                    if(tableauInput[0].equalsIgnoreCase("ls") || tableauInput[0].equalsIgnoreCase("read")){
                        inputToSend = inputToSend.concat(" " + socket.getInetAddress().toString() + ":" + socket.getPort());
                    }
                }
            }
            else{ // Register
                inputToSend = input;
                inputToSend += " " + socket.getInetAddress().toString();
            }
            PrintWriter out = new PrintWriter(this.socket.getOutputStream(),true);
            out.println(inputToSend);
            out.flush();

            Response = bfr.readLine();
            if(Token == null){
                String[] splitResponse = Response.split(" ");
                Token = splitResponse[1];
            }
            System.out.println(Response);
            String[] splitResponse = Response.split(" ");
            if(splitResponse[0].equalsIgnoreCase("READ-REDIRECT")){
                String RedirectToken = splitResponse[2];
                System.out.println("Sending READ request to " + splitResponse[1] + " with token : " + splitResponse[2]);
                String strRedirect = "READ " + RedirectToken + " " + tableauInput[1] + " " + splitResponse[1];
                out.println(strRedirect);
                out.flush();
                Response = bfr.readLine();
                System.out.println(Response);
            }

        }
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client(
                InetAddress.getByName(args[0]),
                Integer.parseInt(args[1]));

        System.out.println("\r\nConnected to Server: " + client.socket.getInetAddress());
        client.start();
    }
}