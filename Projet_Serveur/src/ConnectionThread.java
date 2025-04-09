import com.sun.nio.sctp.PeerAddressChangeNotification;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;
import java.util.Objects;
import java.util.UUID;

public class ConnectionThread implements Runnable {
    ServerSocket server;
    Serveur serveurObject;
    String clientToken = "";
    String clientAdress;
    Socket client;
    public ConnectionThread(Serveur serveurObject,ServerSocket server) {
        this.serveurObject = serveurObject;
        this.server = server;
    }

    public void run() {
        try {
            //on attends une demande de connexion
            String data = null;
            client = server.accept();
            //lors de la connexion, on lance un nouveau thread pour permettre au prochain client de se connecter au serveur
            new Thread(new ConnectionThread(serveurObject,server)).start();
            clientAdress = "";
            boolean writeAuthorized = false;

            // Parametre decodage
            String nomFicher;
            int offset;
            int isLast;
            String contenuFichier;
            String contenuFragment;

            // on attends une requête de la part du client
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            while ((data = in.readLine()) != null) {
                //traitement des types  de messages
                //on split les données reçus
                String[] dataArray = data.split("\\|");
                //Register
                if(dataArray.length > 1){
                    if(dataArray[0].equalsIgnoreCase("REGISTER") && clientToken.isEmpty()){
                        //on génère un token qu'on stocke sur le thread et on enregistre l'adresse ip du client
                        clientToken = UUID.randomUUID().toString().replace("-","").substring(0,20);
                        clientAdress = dataArray[1];
                        System.out.println(clientAdress);
                        //on renvoie ensuite le token généré au client
                        PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                        out.println("REGISTERED|" + clientToken);
                        out.flush();
                        System.out.println(clientToken);

                    }
                    // si le client a envoyé un message register alors qu'il est déjà connecté
                    else if(dataArray[0].equalsIgnoreCase("REGISTER") && !clientToken.isEmpty()){
                        PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                        out.println("AlREADY REGISTERED|" + clientToken);
                        out.flush();
                        System.out.println(clientToken);
                    }
                    else{
                        //Ls
                        if(dataArray[0].equalsIgnoreCase("LS")){
                            PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                            //on vérifie le token de connexion
                            if(dataArray[1].equalsIgnoreCase(clientToken)){
                                String[] instigatorInfo = dataArray[2].split(":");
                                String response = "";
                                // on get tous les fichiers disponibles sur le serveur
                                response =  serveurObject.findAvailableFiles(InetAddress.getByName(instigatorInfo[0].substring(1)),Integer.parseInt(instigatorInfo[1]));
                                if (response.isEmpty()){
                                    response = "NO FILE FOUND ON THE SERVER";
                                }
                                //on envoie la réponse au client
                                System.out.println(response);
                                out.println(response);
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
                                boolean fileNameAvailable = true;
                                // on vérifie si le nom de fichier est déjà utilisé ailleurs
                                for (String file : serveurObject.strFiles){
                                    if (Objects.equals(file, dataArray[2])){
                                        fileNameAvailable = false;
                                        break;
                                    }
                                }
                                    // on vérifie le token de connexion et que le nom de fichier est disponible
                                    if(dataArray[1].equalsIgnoreCase(clientToken) && fileNameAvailable){
                                    writeAuthorized = true;
                                    out.println("WRITE BEGIN");
                                    out.flush();
                                }
                                else{
                                    writeAuthorized = false;
                                    out.println("WRITE UNAUTHORIZED OR FILE ALREADY EXISTS");
                                    out.flush();
                                }
                            }
                            //File
                            else if(dataArray[0].equalsIgnoreCase("FILE")){
                                PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                                boolean fileNameAvailable = true;
                                // on revérifie si le nom de fichier est disponible au cas ou l'utilisateur aurait envoyé un autre nom de fichier après le write begin
                                for (String file : serveurObject.strFiles){
                                    if (Objects.equals(file, dataArray[1])){
                                        fileNameAvailable = false;
                                        break;
                                    }
                                }
                                //si le client à envoyé un message de type write précédement et que l'écriture a été approuvée par le serveur
                                if(writeAuthorized && fileNameAvailable) {
                                    //on Décode le message
                                    nomFicher = dataArray[1];
                                    offset = Integer.parseInt(dataArray[2]);
                                    isLast = Integer.parseInt(dataArray[3]);
                                    contenuFragment = dataArray[4];
                                    // on décrypte le message en enlevant les doublons de caractères tampons et le padding à la fin du message
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
                                    contenuFichier = "";
                                    contenuFichier = contenuFichier.concat(contenuFragment);
                                    //si le fragment est le dernier
                                    if(isLast == 1){
                                        writeAuthorized = false;
                                        File newFile = new File(serveurObject.FilesPath.replaceAll("\"","")+"\\"+nomFicher+".txt");
                                        if(newFile.createNewFile()){
                                            //on ajoute le nom du fichier à la liste de fichier du serveur
                                            serveurObject.strFiles.add(nomFicher);
                                            FileWriter fileWriter1 = new FileWriter(newFile);
                                            fileWriter1.write(contenuFichier);
                                            fileWriter1.close();
                                            try{
                                                //et on écrit le nom dans le fichier file
                                                String fileContent = "";
                                                //load files list for future use
                                                FileReader fl2 = new FileReader(serveurObject.filesList);
                                                BufferedReader bfr2 = new BufferedReader(fl2);
                                                String line2 = "";
                                                while ((line2 = bfr2.readLine()) != null){
                                                    if(fileContent.equals("")){
                                                        fileContent = fileContent.concat(line2);
                                                    }
                                                    else{
                                                        fileContent = fileContent.concat("\n"+line2);
                                                    }

                                                }
                                                fl2.close();
                                                fileContent = fileContent.concat("\n" +nomFicher);

                                                FileWriter fileWriter2 = new FileWriter(serveurObject.filesList);
                                                fileWriter2.write(fileContent);
                                                fileWriter2.close();
                                            }
                                            catch(Exception e){
                                                System.out.println(e.toString());
                                            }
                                            // on avertit les peers qu'il y a un nouveau fichier disponible sur ce serveur
                                            serveurObject.BroadCastNewFileToPeers(nomFicher,server.getInetAddress().toString().replace("/",""),server.getLocalPort() + "");
                                            out.println("FILE SAVED");
                                            out.flush();
                                        }
                                    }
                                    //s'il reste encore d'autres fragments
                                    else{
                                        out.println("FRAGMENT RECEIVED");
                                        out.flush();
                                    }



                                }
                                else  {
                                    out.println("FILE UNAUTHORIZED OR FILE ALREADY EXISTS");
                                    writeAuthorized = false;
                                    out.flush();
                                }
                            }
                            else{
                                //Read
                                if(dataArray[0].equalsIgnoreCase("READ")){
                                    PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                                    //on vérifie le token du client
                                    if(dataArray[1].equalsIgnoreCase(clientToken)){

                                            String fileName = dataArray[2];
                                            String[] instigatorInfo = dataArray[3].split(":");
                                            String response = "";
                                            // on cherche la provenance du fichier la réponse sera read-redirect,local ou file unavailable
                                            response = serveurObject.FindFile(fileName,InetAddress.getByName(instigatorInfo[0].substring(1)),Integer.parseInt(instigatorInfo[1]));
                                            //si le fichier est local, on début la fragmentation pour procéder à l'envoi
                                            if (response.equalsIgnoreCase("local")){
                                                String messageToFragment = serveurObject.getFileMessageLocal(fileName);;
                                                messageToFragment = messageToFragment.replace("~","~~");
                                                if (messageToFragment.length() > 500) { // Si contenuMessage est plus grand que 500

                                                    //on calcule le nombre de fragments
                                                    float nmbFragment = (float) messageToFragment.length() / 500;
                                                    if (nmbFragment % 1 > 0) {
                                                        nmbFragment = nmbFragment - (nmbFragment % 1);
                                                        nmbFragment = nmbFragment + 1;
                                                    }
                                                    for (int i = 0; i < nmbFragment; i++) {

                                                        // Calculer l'offset et déterminer si c'est le dernier fragment
                                                        int offset2 = (i * 500);
                                                        boolean isLast2 = false;


                                                        // Découper le fichier en morceaux de 500 caractères
                                                        int start = i * 500;
                                                        int end = Math.min(start + 500, messageToFragment.length());
                                                        String fragment = messageToFragment.substring(start, end);

                                                        if (i == nmbFragment - 1) {
                                                            //rajouter des caractères tampons
                                                            isLast2 = true;
                                                            int nbPadding = 500-fragment.length();
                                                            for(int x = 0; x < nbPadding;x++){
                                                                fragment = fragment.concat("~");
                                                            }

                                                        }

                                                        //on envoie le fragment
                                                        String messageComplet = "FILE" + "|" + fileName + "|" + offset2 + "|" + (isLast2 ? 1 : 0) + "|" + fragment;
                                                        System.out.println(messageComplet);
                                                        out.println(messageComplet);
                                                        out.flush();
                                                        System.out.println(in.readLine());

                                                    }
                                                }
                                                //s'il n'y a qu'un seul fragment
                                                else{
                                                    int nbPadding = 500-messageToFragment.length();
                                                    for(int x = 0; x < nbPadding;x++){
                                                        messageToFragment = messageToFragment.concat("~");
                                                    }
                                                    String messageComplet = "FILE"+ "|" + fileName + "|" + 0 + "|" + 1 + "|" + messageToFragment;
                                                    System.out.println(messageComplet);
                                                    out.println(messageComplet);
                                                    out.flush();
                                                    System.out.println(in.readLine());
                                                }
                                            }
                                            else{
                                                System.out.println(response);
                                                out.println(response);
                                                out.flush();
                                            }

                                    }
                                    else{
                                        //si le read provenait d'un read redirect
                                        if(dataArray.length > 3){
                                            String fileName = dataArray[2];
                                            String[] instigatorInfo = dataArray[3].split(":");
                                            String response = "";
                                            if(serveurObject.redirectConnections.containsKey(fileName+"/"+dataArray[1])){
                                                //on transfert  le premier fragment dans la connexion spéciale du redirect
                                                response = serveurObject.redirectConnections.get(fileName+"/"+dataArray[1]).SendReadRequest(fileName,InetAddress.getByName(instigatorInfo[0].substring(1)),Integer.parseInt(instigatorInfo[1]));
                                                String[] responseSplit = response.split("\\|");
                                                String Message = "";
                                                boolean isFragmenting = false;
                                                // si la connexion de redirect répond par un fragment, on entre en mode fragmentation et se prépare à recevoir le reste des fragments
                                                if (responseSplit[0].equalsIgnoreCase("FILE")){
                                                    if(responseSplit[3].equals("0")){
                                                        isFragmenting = true;
                                                    }
                                                    else{
                                                        isFragmenting = false;
                                                    }
                                                    out.println(response);
                                                    out.flush();
                                                    //s'il y a plus qu'un fragment, on boucle jusqu'à avoir tout reçu
                                                    while (isFragmenting){
                                                        response = serveurObject.redirectConnections.get(fileName+"/"+dataArray[1]).ReceiveFragment();
                                                        responseSplit = response.split("\\|");
                                                        if (responseSplit[3].equals("0"))
                                                        {
                                                            out.println(response);
                                                            out.flush();
                                                            System.out.println(response);
                                                        }
                                                        else{
                                                            isFragmenting = false;
                                                            out.println(response);
                                                            out.flush();
                                                            System.out.println(response);
                                                        }
                                                    }
                                                }
                                                else{
                                                    System.out.println(response);
                                                    out.println(response);
                                                    out.flush();
                                                }

                                            }
                                            else{
                                                out.println("READ UNAUTHORIZED");
                                                out.flush();
                                            }
                                        }

                                        else{
                                            out.println("READ UNAUTHORIZED");
                                            out.flush();
                                        }
                                    }
                                }
                                else{
                                    //AddNewAvailableFile
                                    //pour rajouter un nouveau fichier au files du serveur
                                    if(dataArray[0].equalsIgnoreCase("AddAvailableFile")){
                                        if(dataArray.length >3){
                                            serveurObject.AddNewFileToFileList(dataArray[1],dataArray[2],dataArray[3]);
                                        }
                                        else{
                                            PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                                            out.println("NO COMMAND RECOGNIZED");
                                        }

                                    }
                                    else {
                                        PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                                        out.println("NO COMMAND RECOGNIZED");
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
