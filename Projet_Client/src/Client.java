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

    private Client(InetAddress serverAddress, int serverPort) throws Exception {
        this.socket = new Socket(serverAddress, serverPort);
        this.scanner = new Scanner(System.in);
    }

    //to register Register 192.168.0.15
    private void start() throws IOException {
        String inputToSend;
        String input;
        int fragmentSize = 500;

        PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
        StringBuilder contenuMessage = new StringBuilder();
        BufferedReader bfr = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        while (true) {

            // Conserve l'input utilisateur
            input = scanner.nextLine();

            // Split l'input dans un tableau de string
            String[] tableauInput = input.split(" ");


            // Register (tableau taille == 1)
            // Ls (tableau taille == 1)
            // Write (tableau taille == 2)
            // File == continuation de write (tableau taille == plus grand que 2)
            // Read (tableau taille == 2)


            if (Token != null) {

                String nom_fichier;

                // Write
                if (tableauInput.length > 1) {
                    inputToSend = tableauInput[0] + " " + Token + " " + tableauInput[1];
                }
                // Ls
                else {
                    inputToSend = tableauInput[0] + " " + Token;
                }
                /*
                if(tableauInput[0].equalsIgnoreCase("ls")){
                    inputToSend = inputToSend.concat(" " + socket.getInetAddress().toString() + ":" + socket.getPort());
                }
                else if (tableauInput[0].equalsIgnoreCase("read")){
                    inputToSend = inputToSend.concat(tableauInput[0] + " " + socket.getInetAddress().toString() + ":" + socket.getPort() + " " + tableauInput[2]);
                }
                */
                if (tableauInput[0].equalsIgnoreCase("file")) {
                    if (tableauInput.length > 2) {
                        for (int i = 2; i < tableauInput.length; i++) {
                            contenuMessage.append(tableauInput[i]);
                        }
                    }
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

                            String messageComplet = tableauInput[0] + tableauInput[2] + " " + offset + " " + (isLast ? 1 : 0) + " " + fragment;
                            System.out.println(messageComplet);
                            out.println(messageComplet);
                            out.flush();
                        }
                    }

                }
            } else {
                // Register
                inputToSend = input;
                inputToSend += " " + socket.getInetAddress().toString();
            }

            out.println(inputToSend);
            out.flush();

            Response = bfr.readLine();
            if (Token == null) {
                String[] splitResponse = Response.split(" ");
                Token = splitResponse[1];
            }
            System.out.println(Response);
            String[] splitResponse = Response.split(" ");
            if (splitResponse[0].equalsIgnoreCase("READ-REDIRECT")) {
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