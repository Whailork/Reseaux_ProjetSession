import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private Scanner scanner;
    private String Token;
    private String Response;

    Socket RedirectSocket;
    private Client(InetAddress serverAddress, int serverPort) throws Exception{
        this.socket = new Socket(serverAddress, serverPort);
        this.scanner = new Scanner(System.in);
    }

    //to register Register 192.168.0.15
    private void start() throws IOException {
        String inputToSend;
        String input;
        int fragmentSize = 500;
        StringBuilder fullMessage = new StringBuilder();
        boolean isFragmenting = false;
        String[] tableauInput = new String[0];

        PrintWriter out = new PrintWriter(this.socket.getOutputStream(),true);
        BufferedReader bfr = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        while(true){
            if(!isFragmenting){
            // Conserve l'input utilisateur
            input = scanner.nextLine();

            // Split l'input dans un tableau de string
                tableauInput = input.split(" ");


            // Register (tableau taille == 1)
            // Ls (tableau taille == 1)
            // Write (tableau taille == 2)
            // File == continuation de write (tableau taille == plus grand que 2)
            // Read (tableau taille == 2)


            if(Token != null){

                String nom_fichier;
                // write
                if(tableauInput.length > 1){
                    inputToSend = tableauInput[0] + " " + Token + " " + tableauInput[1];

                }
                // LS
                else{
                    inputToSend = tableauInput[0] + " " + Token;
                }

                switch (tableauInput[0].toUpperCase()){
                    case "FILE":
                        StringBuilder contenuMessage = new StringBuilder();
                        if (tableauInput.length > 2) {
                            for (int i = 2; i < tableauInput.length; i++) {
                                contenuMessage.append(tableauInput[i]);
                                contenuMessage.append(" ");
                            }
                        }
                        if (contenuMessage.length() > fragmentSize) { // Si contenuMessage est plus grand que 500
                            float nmbFragment = (float) contenuMessage.length() / fragmentSize;
                            if (nmbFragment % 1 > 0) {
                                nmbFragment = nmbFragment - (nmbFragment % 1);
                                nmbFragment = nmbFragment + 1;
                            }
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

                                String messageComplet = tableauInput[0] + " " + tableauInput[1] + " " + offset + " " + (isLast ? 1 : 0) + " " + fragment;
                                System.out.println(messageComplet);
                                out.println(messageComplet);
                                out.flush();

                            }
                        }
                        else{
                            String messageComplet = tableauInput[0] + " " + tableauInput[1] + " " + 0 + " " + 1 + " " + contenuMessage;
                            System.out.println(messageComplet);
                            out.println(messageComplet);
                            out.flush();
                        }

                        break;
                    case "LS":
                        inputToSend = inputToSend.concat(" " + socket.getInetAddress().toString() + ":" + socket.getPort());
                        out.println(inputToSend);
                        out.flush();
                        break;
                    case "READ":
                        inputToSend = inputToSend.concat(" " + socket.getInetAddress().toString() + ":" + socket.getPort());
                        out.println(inputToSend);
                        out.flush();
                        break;
                    default:
                        out.println(inputToSend);
                        out.flush();
                        break;
                }
            }
            else{
                // Register
                inputToSend = input;
                inputToSend += " " + socket.getInetAddress().toString();
                out.println(inputToSend);
                out.flush();
            }
            }


            Response = bfr.readLine();
            String[] splitResponse = Response.split(" ");

            if(Token == null && splitResponse[0].equalsIgnoreCase("REGISTERED")){
                Token = splitResponse[1];
            }
            System.out.println(Response);
            if(splitResponse[0].equalsIgnoreCase("READ-REDIRECT")){
                String RedirectToken = splitResponse[2];
                System.out.println("Sending READ request to " + splitResponse[1] + " with token : " + splitResponse[2]);
                String[] adress = splitResponse[1].split(":");
                //RedirectSocket = new Socket(adress[0].replace("/",""),Integer.parseInt(adress[1]));

                String strRedirect = "READ " + RedirectToken + " " + tableauInput[1] + " " + splitResponse[1];

                out.println(strRedirect);
                out.flush();
                Response = bfr.readLine();
                System.out.println(Response);
                String[] responseSplit = Response.split(" ");
                if (responseSplit[0].equalsIgnoreCase("FILE")){
                    if(responseSplit[3].equals("0")){
                        isFragmenting = true;
                        for (int i = 4; i < responseSplit.length; i++) {
                            fullMessage.append(responseSplit[i]);
                            fullMessage.append(" ");
                        }
                    }
                }
            }
            if (splitResponse[0].equalsIgnoreCase("FILE")){
                for (int i = 4; i < splitResponse.length; i++) {
                    fullMessage.append(splitResponse[i]);
                    fullMessage.append(" ");
                }
                if (splitResponse[3].equals("0")) {
                    isFragmenting = true;

                    out.println("FRAGMENT RECEIVED");
                    out.flush();

                }
                else{
                    isFragmenting = false;
                    System.out.println(fullMessage);
                    fullMessage = new StringBuilder();
                    out.println("MESSAGE RECEIVED");
                    out.flush();
                    bfr.readLine();
                }

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