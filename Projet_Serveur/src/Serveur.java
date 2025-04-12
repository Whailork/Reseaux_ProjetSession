import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Serveur {
    public static Serveur app;
    private ServerSocket server;
    public File peersList;
    public File filesList;
    public ArrayList<String> strFiles;
    public ArrayList<String> strPeers;
    boolean IsQuerying;
    public ArrayList<ServerLink> connectedServers;
    public HashMap<String,ServerLink> redirectConnections;
    public String FilesPath;
    //constructeurs du serveur avec et sans la précision du port
    public Serveur(String ipAdresse) throws Exception{
        if(ipAdresse != null && !ipAdresse.isEmpty()){

            this.server = new ServerSocket(2000,1, InetAddress.getByName(ipAdresse));
        }
        else{
            this.server = new ServerSocket(2000,1,InetAddress.getLocalHost());
        }
    }
    public Serveur(String ipAdresse, int port) throws Exception{
        if(ipAdresse != null && !ipAdresse.isEmpty()){

            this.server = new ServerSocket(port,1, InetAddress.getByName(ipAdresse));
        }
        else{
            this.server = new ServerSocket(port,1,InetAddress.getLocalHost());
        }
    }

    // pour lancer un nouveau thread de connexion client
    private void listen(Serveur app) throws Exception{
        new Thread(new ConnectionThread(app,server)).start();
    }

    public InetAddress getSocketAddress(){
        return this.server.getInetAddress();
    }

    public int getPort(){
        return this.server.getLocalPort();
    }

    public static void main(String[] args) throws Exception {

        //création du serveur
        if(args.length > 0){
            app = new Serveur(InetAddress.getLocalHost().getHostAddress(),Integer.parseInt(args[0]));
        }
        else{
            app = new Serveur(InetAddress.getLocalHost().getHostAddress());
        }

        //si les arguments sont valides
        if(args != null){
            if(args.length > 3){
                // le troisième argument est le chemin du fichier dans lequel le serveur va enregistrer les nouveaux fichiers
                app.FilesPath = args[3];
                // le premier argument est le chemin vers la peer list
                app.peersList =  new File(args[1]);
                // le deuxième argument est le chemin vers la file list
                app.filesList = new File(args[2]);
                //on initialise les structures de données qui sont nécéssaires à la communication avec les peers
                app.connectedServers = new ArrayList<>();
                app.redirectConnections = new HashMap<>();
                System.out.println("fichiers detectes");
                app.strPeers = new ArrayList<>();
                app.strFiles = new ArrayList<>();
                app.connectedServers = new ArrayList<>();
                //on load les peers du fichier peer pour les utiliser plus tard
                FileReader fl = new FileReader(app.peersList);
                BufferedReader bfr = new BufferedReader(fl);
                String line = "";
                while ((line = bfr.readLine()) != null){


                    app.strPeers.add(line);
                }

                //on load les files du fichier file pour les utiliser plus tard
                FileReader fl2 = new FileReader(app.filesList);
                BufferedReader bfr2 = new BufferedReader(fl2);
                String line2 = "";
                while ((line2 = bfr2.readLine()) != null){
                    //on ignore le premier fichier de la liste s'il est exactement pareil à une réponse de serveur pour que le client n'interprète pas la réponse d'un ls comme la réponse d'une autre requête.
                    if(app.strFiles.isEmpty()){
                        if(!(line2.equals("FILE") || line2.equals("REGISTER") || line2.equals("WRITE") || line2.equals("LS") || line2.equals("READ")  || line2.equals("REGISTERED"))){
                            app.strFiles.add(line2);
                        }
                    }
                    else{
                        app.strFiles.add(line2);
                    }

                }


                // on ferme les fichiers et les fileReaders
                fl.close();
                fl2.close();
                bfr.close();
                bfr2.close();

            }

        }


        System.out.println("\r\nRunning Server: " + app.getSocketAddress().getHostAddress() + " " + app.getPort());

        app.listen(app);
    }

    //fonction qui permet de charger et véfifier la connexion avec les peers
    public void LoadConnectedServers() throws IOException {
        for (String peerAddress: strPeers) {
            String[] addressInfo = peerAddress.split(" ");
            //si le format d'adresse est valide
            if(addressInfo.length > 1){
                InetAddress address = InetAddress.getByName(addressInfo[0]);
                try{
                    //s'il est possible de se connecter au serveur
                    ServerLink serverLink = findConnectedServer(address,Integer.parseInt(addressInfo[1]));
                    if(serverLink == null){
                        // si une connexion n'avait pas déjà été établie avec le serveur, on en ouvre une nouvelle
                        Socket socket = new Socket(address, Integer.parseInt(addressInfo[1]));
                        ServerLink newServerLink = new ServerLink(socket);
                        connectedServers.add(newServerLink);
                    }
                    else{
                        //on vérifie si une connexion précédemment établie est encore valide
                        if(!serverLink.linkSocket.isConnected()){
                            connectedServers.remove(serverLink);
                            System.out.println("connexion to server lost : " + peerAddress);
                        }
                        //sinon nous sommes déjà connectés au serveur
                        else{
                            System.out.println("server already connected");
                        }

                    }
                }
                //sinon il est impossible de se connecter à ce peer
                catch(Exception e){
                    System.out.println("cannot connect to server : " + peerAddress);
                }

            }
        }
    }

    // pour trouver un peer en particulier
    public ServerLink findConnectedServer(InetAddress address, int port){
        for (ServerLink serverLink:connectedServers) {

            if(serverLink.linkSocket.getInetAddress().equals(address)  && serverLink.linkSocket.getPort() == port){
                return serverLink;
            }
        }
        return null;
    }

    //pour trouver tous les fichiers disponibles depuis ce serveur (LS)
    public String findAvailableFiles(InetAddress instigatorAddress, int instigatorPort){
        String availableFiles = "";
        //on charge les fichiers locaux
        for (String file:app.strFiles) {
            //si le fichier est local, on l'ajoute directement au LS
            if(!(file.split(" ").length > 1)){
                availableFiles = availableFiles.concat(file + "|");
            }
            //On regarde ensuite l'état de connexion des serveurs
            else{
                try{
                    LoadConnectedServers();
                    String[] serverAddress = file.split(" ")[1].split(":");
                    if(serverAddress.length > 1){
                        ServerLink serverLink = findConnectedServer(InetAddress.getByName(serverAddress[0].replace("/","")),Integer.parseInt(serverAddress[1]));
                        //si le serveur est connecté, on ajoute le nom du fichier à la liste
                        if(serverLink != null){
                            availableFiles = availableFiles.concat(file.split(" ")[0] + "|");
                        }
                    }
                    else{
                        System.out.println("Nomenclature du fichier file non conforme");
                    }

                }
                catch (Exception e){
                    System.out.println(e.toString());
                }

            }
        }

        return availableFiles;
    }
    // pour trouver un fichier précis (quand le client a envoyé un read par exemple)
    public String FindFile(String fileName,InetAddress instigatorAddress, int instigatorPort){
        String response = "";
        for (String file:app.strFiles) {
            String[] strFile = file.split(" ");
            if(fileName.equalsIgnoreCase(strFile[0])){
                //si le fichier n'est pas local
                if(strFile.length > 1){
                    try{
                        // on vérifie l'état de connexion des serveurs
                        LoadConnectedServers();
                        String[] serverAddress = file.split(" ")[1].split(":");
                        if(serverAddress.length > 1){
                            //on regarde si le serveur existe et la connexion est établie
                            ServerLink serverLink = findConnectedServer(InetAddress.getByName(serverAddress[0].replace("/","")),Integer.parseInt(serverAddress[1]));
                            //si le serveur est connecté, le serveur établie une connexion spéciale avec le peer, pour permettre le read redirect
                            if(serverLink != null){
                                response = serverLink.SendReadRequest(fileName,instigatorAddress,instigatorPort);
                                if(response.equalsIgnoreCase("READ-REDIRECT")){
                                        String redirectToken = UUID.randomUUID().toString().replace("-","").substring(0,20);
                                        redirectConnections.put(fileName + "/" + redirectToken,new ServerLink(new Socket(serverLink.linkSocket.getInetAddress(),serverLink.linkSocket.getPort())));
                                        response = "READ-REDIRECT|" + serverLink.linkSocket.getInetAddress() + ":" + serverLink.linkSocket.getPort() + "|" + redirectToken;

                                }
                                return response;
                            }
                            //s'il n'est pas connecté, le fichier est non disponible
                            else{
                                return "no file available with name : " +fileName;
                            }
                        }

                    }
                    catch (Exception e){
                        System.out.println(e.toString());
                    }

                }
                if(!(instigatorAddress.equals(app.server.getInetAddress()) && instigatorPort == app.server.getLocalPort())){
                    return "READ-REDIRECT";
                }
                //si le fichier est local, la fonction retourne "local"
                return "local";
            }
        }
        return response;


    }

    //pour lire le contenu d'un fichier sur la machine du serveur
    public String getFileMessageLocal(String fileName) throws IOException {
        StringBuilder message = new StringBuilder();
        strFiles = app.strFiles;
        for (String file:strFiles) {
            if(fileName.equalsIgnoreCase(file)){
                File newFile = new File(FilesPath.replaceAll("\"","")+"\\"+fileName+".txt");
                BufferedReader bfr = new BufferedReader(new FileReader(newFile));
                String line;
                while ((line = bfr.readLine()) != null) {
                    message.append(line);
                }
            }
        }

        return message.toString();
    }


    // pour dire au peers de rajouter un fichier dans leur fichier file lors de l'ajout d'un nouveau fichier au serveur
    public void BroadCastNewFileToPeers(String fileName, String serverIp, String serverPort){
        try{
            LoadConnectedServers();
        }
        catch (Exception e){
            System.out.println(e);
        }

        for (ServerLink serverLink:connectedServers) {
            serverLink.AddNewFileToAvailableFiles(fileName,serverIp,serverPort);
        }
    }
    //pour rajouter un nom de fichier au fichier file du serveur
    public void AddNewFileToFileList(String fileName, String serverIp, String serverPort){
        try{
            FileWriter fileWriter = new FileWriter(filesList);
            String newFileContent = "";
            //on load les fichiers déjà existant
            for (String file:strFiles) {
                newFileContent = newFileContent.concat(file);
                newFileContent = newFileContent.concat("\n");
            }
            // on y rajoute le nom du nouveau fichier
            newFileContent = newFileContent.concat(fileName + " " + serverIp+":"+serverPort);
            fileWriter.write(newFileContent);
            fileWriter.close();
            strFiles.add(fileName + " " + serverIp+":"+serverPort);
        }
        catch(Exception e){
            System.out.println(e);
        }

    }
}

