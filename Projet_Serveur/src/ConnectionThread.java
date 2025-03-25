import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;
import java.util.UUID;

public class ConnectionThread implements Runnable {
    ServerSocket server;
    Serveur serveurObject;
    String clientToken;
    String clientAdress;
    Socket client;
    public ConnectionThread(Serveur serveurObject,ServerSocket server) {
        this.serveurObject = serveurObject;
        this.server = server;
    }

    public void run() {
        try {
            String data = null;
            client = server.accept();
            new Thread(new ConnectionThread(serveurObject,server)).start();
            clientAdress = "";
            boolean writeAuthorized = false;

            // Parametre decodage
            String nomFicher;
            int offset;
            int isLast;
            String contenuFichier;
            String contenuFragment;


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
                                String[] instigatorInfo = dataArray[2].split(":");
                                String response = "";
                                response =  serveurObject.findAvailableFiles(InetAddress.getByName(instigatorInfo[0].substring(1)),Integer.parseInt(instigatorInfo[1]));
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
                                if(dataArray[1].equalsIgnoreCase(clientToken) /* && verifier si le file existe dans la liste du server*/){
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
                            else if(dataArray[0].equalsIgnoreCase("FILE")){
                                PrintWriter out = new PrintWriter(client.getOutputStream(),true);

                                if(writeAuthorized) {
                                    // Décoder le message
                                    nomFicher = dataArray[1];
                                    offset = Integer.parseInt(dataArray[2]);
                                    isLast = Integer.parseInt(dataArray[3]);
                                    contenuFragment = dataArray[4];
                                    contenuFichier = "";
                                    contenuFichier = contenuFichier.concat(contenuFragment);
                                    if(isLast == 1){
                                        writeAuthorized = false;
                                        File newFile = new File(serveurObject.FilesPath.replaceAll("\"","")+"\\"+nomFicher+".txt");
                                        if(newFile.createNewFile()){
                                            serveurObject.strFiles.add(nomFicher);
                                            FileWriter fileWriter1 = new FileWriter(newFile);
                                            fileWriter1.write(contenuFichier);
                                            fileWriter1.close();
                                            try{
                                                String fileContent = "";
                                                //load files list for future use
                                                FileReader fl2 = new FileReader(serveurObject.filesList);
                                                BufferedReader bfr2 = new BufferedReader(fl2);
                                                String line2 = "";
                                                while ((line2 = bfr2.readLine()) != null){
                                                    fileContent = fileContent.concat("\n"+line2);
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
                                            out.println("FILE SAVED");
                                            out.flush();
                                        }
                                    }
                                    else{
                                        out.println("FRAGMENT RECEIVED");
                                        out.flush();
                                    }



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
                                        String fileName = dataArray[2];
                                        String[] instigatorInfo = dataArray[3].split(":");
                                        String response = "";
                                        response = serveurObject.FindFile(fileName,InetAddress.getByName(instigatorInfo[0].substring(1)),Integer.parseInt(instigatorInfo[1]));

                                        if (response == "local"){
                                            String messageToFragment = serveurObject.getFileMessageLocal(fileName);;

                                            if (messageToFragment.length() > 500) { // Si contenuMessage est plus grand que 500

                                                float nmbFragment = (float) messageToFragment.length() / 500;
                                                if (nmbFragment % 1 > 0) {
                                                    nmbFragment = nmbFragment - (nmbFragment % 1);
                                                    nmbFragment = nmbFragment + 1;
                                                }
                                                for (int i = 0; i < nmbFragment; i++) {

                                                    // Calculer l'offset et déterminer si c'est le dernier fragment
                                                    int offset2 = (i * 500);
                                                    boolean isLast2 = false;

                                                    if (i == nmbFragment - 1) {
                                                        isLast2 = true;
                                                    }

                                                    // Découper le fichier en morceaux de 500 caractères
                                                    int start = i * 500;
                                                    int end = Math.min(start + 500, messageToFragment.length());
                                                    String fragment = messageToFragment.substring(start, end);

                                                    // Afficher le message de type FILE (simuler l'envoi)

                                                    String messageComplet = "FILE" + " " + fileName + " " + offset2 + " " + (isLast2 ? 1 : 0) + " " + fragment;
                                                    System.out.println(messageComplet);
                                                    out.println(messageComplet);
                                                    out.flush();

                                                }
                                            }
                                            else{
                                                String messageComplet = "FILE"+ " " + fileName + " " + 0 + " " + 1 + " " + messageToFragment;
                                                System.out.println(messageComplet);
                                                out.println(messageComplet);
                                                out.flush();
                                            }
                                        }
                                        System.out.println(response);
                                        out.println(response);
                                        out.flush();
                                    }
                                    else{
                                        if(dataArray.length > 3){
                                            String fileName = dataArray[2];
                                            String[] instigatorInfo = dataArray[3].split(":");
                                            String response = "";
                                            if(serveurObject.redirectConnections.containsKey(fileName+"/"+dataArray[1])){
                                                response = serveurObject.redirectConnections.get(fileName+"/"+dataArray[1]).SendReadRequest(fileName,InetAddress.getByName(instigatorInfo[0].substring(1)),Integer.parseInt(instigatorInfo[1]));
                                                System.out.println(response);
                                                out.println(response);
                                                out.flush();
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
                                    PrintWriter out = new PrintWriter(client.getOutputStream(),true);
                                    out.println("NO COMMAND RECOGNIZED");
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
