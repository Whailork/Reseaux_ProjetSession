import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
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
        try {
            String inputToSend;
            String input;
            //infos de fragment
            int fragmentSize = 500;
            StringBuilder fullMessage = new StringBuilder();
            boolean isFragmenting = false;

            //tableau qui va contenir les éléments de réponse lorsque séparés selon les |
            String[] tableauInput = new String[0];

            PrintWriter out = new PrintWriter(this.socket.getOutputStream(),true);
            BufferedReader bfr = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            while(true){
                // si le client n'est pas en mode fragmentation
                if(!isFragmenting){
                    // Conserve l'input utilisateur
                    input = scanner.nextLine();

                    // Split l'input dans un tableau de string
                    tableauInput = input.split("\\|");


                    // Register (tableau taille == 1)
                    // Ls (tableau taille == 1)
                    // Write (tableau taille == 2)
                    // File == continuation de write (tableau taille == plus grand que 2)
                    // Read (tableau taille == 2)

                    // on vérifie l'existance du token de connexion
                    if(Token != null){

                        //on rajoute le token après le type de message, mais avant le reste du contenu s'il sagit d'une requête ayant plus d'un argument
                        if(tableauInput.length > 1){
                            inputToSend = tableauInput[0] + "|" + Token + "|" + tableauInput[1];

                        }
                        //sinon on rajoute tout simplement le token à la fin
                        else{
                            inputToSend = tableauInput[0] + "|" + Token;
                        }
                        // on regarde quel type de message a été envoyé par l'utilisateur
                        switch (tableauInput[0].toUpperCase()){
                            case "FILE":
                                String contenuMessage;
                                contenuMessage = tableauInput[2];
                                //on ajoute le caractère tampon si on rencontre un caractère tampon dans le message pour signifier que ce n'est pas la fin du fragment
                                contenuMessage = contenuMessage.replace("~","~~");
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



                                        // Découper le fichier en morceaux de 500 caractères
                                        int start = i * fragmentSize;
                                        int end = Math.min(start + fragmentSize, contenuMessage.length());
                                        String fragment = contenuMessage.substring(start, end);

                                        if (i == nmbFragment - 1) {
                                            //rajouter des caractères tampons
                                            isLast = true;
                                            int nbPadding = 500-fragment.length();
                                            for(int x = 0; x < nbPadding;x++){
                                                fragment = fragment.concat("~");
                                            }

                                        }

                                        // Afficher le message de type FILE (simuler l'envoi)

                                        String messageComplet = tableauInput[0] + "|" + tableauInput[1] + "|" + offset + "|" + (isLast ? 1 : 0) + "|" + fragment;
                                        out.println(messageComplet);
                                        out.flush();

                                    }
                                }
                                else{
                                    //s'il n'y a qu'un fragment
                                    int nbPadding = 500-contenuMessage.length();
                                    for(int x = 0; x < nbPadding;x++){
                                        contenuMessage = contenuMessage.concat("~");
                                    }
                                    String messageComplet = tableauInput[0] + "|" + tableauInput[1] + "|" + 0 + "|" + 1 + "|" + contenuMessage;
                                    System.out.println(messageComplet);
                                    out.println(messageComplet);
                                    out.flush();
                                }

                                break;
                                //on ajoute l'adresse de provenance de la requête à la fin pour préciser au serveur de qui provient la demande originale
                            case "LS":
                                inputToSend = inputToSend.concat("|" + socket.getInetAddress().toString() + ":" + socket.getPort());
                                out.println(inputToSend);
                                out.flush();
                                break;
                            //on ajoute l'adresse de provenance de la requête à la fin pour préciser au serveur de qui provient la demande originale
                            case "READ":
                                inputToSend = inputToSend.concat("|" + socket.getInetAddress().toString() + ":" + socket.getPort());
                                out.println(inputToSend);
                                out.flush();
                                break;
                            default:
                                out.println(inputToSend);
                                out.flush();
                                break;
                        }
                    }
                    // si aucun token, on assume que c'est un message Register et on ajoute l'adresse du client au message
                    else{
                        // Register
                        inputToSend = input;
                        inputToSend += "|" + socket.getInetAddress().toString();
                        out.println(inputToSend);
                        out.flush();
                    }
                }

                // on se prépare pour recevoir une réponse de la part du serveur
                Response = bfr.readLine();
                String[] splitResponse = Response.split("\\|");

                // si la reponse est un Registered
                if(Token == null && splitResponse[0].equalsIgnoreCase("REGISTERED")){
                    Token = splitResponse[1];
                }
                System.out.println(Response);
                // si le serveur répond par un read redirect, le client envoie automatiquement la requête avec le nouveau token de connexion
                if(splitResponse[0].equalsIgnoreCase("READ-REDIRECT")){
                    String RedirectToken = splitResponse[2];
                    System.out.println("Sending READ request to " + splitResponse[1] + " with token : " + splitResponse[2]);
                    String[] adress = splitResponse[1].split(":");
                    String strRedirect = "READ|" + RedirectToken + "|" + tableauInput[1] + "|" + splitResponse[1];

                    out.println(strRedirect);
                    out.flush();

                    // le programme attend ensuite le premier fragment de réponse
                    Response = bfr.readLine();
                    System.out.println(Response);
                    String[] responseSplit = Response.split("\\|");

                    // on décrypte ensuite le message en enlevant les doublons de caractères tampons et le padding à la fin du message
                    String contenuFragment = responseSplit[4];
                    contenuFragment = contenuFragment.replace("~~","~");
                    int baseFragmentLength = contenuFragment.length()-1;
                    for (int i = baseFragmentLength; i > 0; i--){
                        if(contenuFragment.charAt(i) == '~'){
                            contenuFragment = contenuFragment.substring(0, contenuFragment.length() - 1);
                        }
                        else{
                            break;
                        }
                    }
                    //s'il reste encore au moins un fragment, on active le mode de fragmentation ce qui veut dire que le programme va passer par dessus l'envoi d'une nouvelle requête du client
                    if (responseSplit[0].equalsIgnoreCase("FILE")){
                        if(responseSplit[3].equals("0")){
                            isFragmenting = true;
                            fullMessage.append(contenuFragment);
                        }
                    }
                }
                //sinon si c'est un read normal
                if (splitResponse[0].equalsIgnoreCase("FILE")){
                    // on décrypte le message en enlevant les doublons de caractères tampons et le padding à la fin du message
                    String contenuFragment = splitResponse[4];
                    contenuFragment = contenuFragment.replace("~~","~");
                    int baseFragmentLength = contenuFragment.length()-1;
                    for (int i = baseFragmentLength; i > 0; i--){
                        if(contenuFragment.charAt(i) == '~'){
                            contenuFragment = contenuFragment.substring(0, contenuFragment.length() - 1);
                        }
                        else{
                            break;
                        }
                    }

                    //s'il reste encore au moins un fragment, on active le mode de fragmentation ce qui veut dire que le programme va passer par dessus l'envoi d'une nouvelle requête du client
                    fullMessage.append(contenuFragment);
                    if (splitResponse[3].equals("0")) {
                        isFragmenting = true;
                        // on renvoi des accusés de réception au serveur pour qu'il nous renvois ensuite le prochain fragment
                        out.println("FRAGMENT RECEIVED");
                        out.flush();

                    }
                    //sinon on termine la fragmentation pour permettre au client d'envoyer de nouvelles requêtes
                    else {
                        isFragmenting = false;
                        System.out.println(fullMessage);
                        fullMessage = new StringBuilder("");
                        // on envoie un accusé de réception pour dire au serveur de finaliser le processus de fragmentation
                        out.println("MESSAGE RECEIVED");
                        out.flush();
                        bfr.readLine();
                    }

                }

            }
        }
        // si la connexion échoue à un moment ou un autre, on permet au client d'ouvrir une connexion vers un nouveau serveur
        catch (SocketException e){
            System.out.println("CONNECTION LOST");
            AskForServerConection();
        }

    }
    public static void AskForServerConection(){
        //on demande une adresse de connexion au client et essaie de se connection jusqu'à ce que le client entre une adresse valide
        boolean ConexionFound = true;
        Scanner sc = new Scanner(System.in);
        Client client = null;
        do{
            try{

                System.out.println("entrez l'adresse ip du serveur suivit du port exemple : 192.168.0.15:3000");
                String serverInfo = sc.nextLine();
                String[] splitInfo = serverInfo.split(":");
                client = new Client(InetAddress.getByName(splitInfo[0]), Integer.parseInt(splitInfo[1]));
                ConexionFound = true;
            }
            catch(Exception e){
                ConexionFound = false;
                System.out.println("Connexion failed : ");
            }

        }while (!ConexionFound);


        System.out.println("\r\nConnected to Server: " + client.socket.getInetAddress());
        try{
            client.start();
        }
        catch (Exception e){
            System.out.println(e);
        }

    }

    public static void main(String[] args) throws Exception {
        AskForServerConection();
    }


}