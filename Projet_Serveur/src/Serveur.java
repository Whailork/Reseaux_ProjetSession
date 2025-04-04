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

    private void listen(Serveur app) throws Exception{
        //new Thread(new ServerConnectionThread(app,server)).start();
        new Thread(new ConnectionThread(app,server)).start();
    }

    public InetAddress getSocketAddress(){
        return this.server.getInetAddress();
    }

    public int getPort(){
        return this.server.getLocalPort();
    }

    public static void main(String[] args) throws Exception {

        if(args.length > 0){
            app = new Serveur(InetAddress.getLocalHost().getHostAddress(),Integer.parseInt(args[0]));
        }
        else{
            app = new Serveur(InetAddress.getLocalHost().getHostAddress());
        }

        if(args != null){
            if(args.length > 3){

                app.FilesPath = args[3];
                app.peersList =  new File(args[1]);
                app.filesList = new File(args[2]);
                app.connectedServers = new ArrayList<>();
                app.redirectConnections = new HashMap<>();
                System.out.println("fichiers detectes");
                app.strPeers = new ArrayList<>();
                app.strFiles = new ArrayList<>();
                app.connectedServers = new ArrayList<>();
                //load peers list for future use
                FileReader fl = new FileReader(app.peersList);
                BufferedReader bfr = new BufferedReader(fl);
                String line = "";
                while ((line = bfr.readLine()) != null){
                    app.strPeers.add(line);
                }

                //load files list for future use
                FileReader fl2 = new FileReader(app.filesList);
                BufferedReader bfr2 = new BufferedReader(fl2);
                String line2 = "";
                while ((line2 = bfr2.readLine()) != null){
                    app.strFiles.add(line2);
                }


                // close the file
                fl.close();
                fl2.close();
                bfr.close();
                bfr2.close();

            }

        }


        System.out.println("\r\nRunning Server: " + app.getSocketAddress().getHostAddress() + " " + app.getPort());

        app.listen(app);

        //System.out.println(app.findAvailableFiles(app.server.getInetAddress(),app.server.getLocalPort());

    }

    public void LoadConnectedServers() throws IOException {

        for (String peerAddress: strPeers) {
            String[] addressInfo = peerAddress.split(" ");
            if(addressInfo.length > 1){

                InetAddress address = InetAddress.getByName(addressInfo[0]);
                try{
                    ServerLink serverLink = findConnectedServer(address,Integer.parseInt(addressInfo[1]));
                    if(serverLink == null){

                        Socket socket = new Socket(address, Integer.parseInt(addressInfo[1]));
                        ServerLink newServerLink = new ServerLink(socket);
                        connectedServers.add(newServerLink);
                    }
                    else{
                        if(!serverLink.linkSocket.isConnected()){
                            connectedServers.remove(serverLink);
                            System.out.println("connexion to server lost : " + peerAddress);
                        }
                        else{
                            System.out.println("server already connected");
                        }

                    }
                }
                catch(Exception e){
                    System.out.println("cannot connect to server : " + peerAddress);
                }

            }
        }
    }

    public ServerLink findConnectedServer(InetAddress address, int port){
        for (ServerLink serverLink:connectedServers) {

            if(serverLink.linkSocket.getInetAddress().equals(address)  && serverLink.linkSocket.getPort() == port){
                return serverLink;
            }
        }
        return null;
    }

    public String findAvailableFiles(InetAddress instigatorAddress, int instigatorPort){
        String availableFiles = "";
        //load local files
        for (String file:app.strFiles) {
            //if local file
            if(!(file.split(" ").length > 1)){
                availableFiles = availableFiles.concat(file + "|");
            }
            //check servers connection
            else{
                try{
                    LoadConnectedServers();
                    String[] serverAddress = file.split(" ")[1].split(":");
                    if(serverAddress.length > 1){
                        ServerLink serverLink = findConnectedServer(InetAddress.getByName(serverAddress[0].replace("/","")),Integer.parseInt(serverAddress[1]));
                        if(serverLink != null){
                            availableFiles = availableFiles.concat(file.split(" ")[0] + "|");
                        }
                    }
                    else{
                        //nomenclature pas conforme
                        //TODO: avertir le user que le fichier files est invalide
                    }

                }
                catch (Exception e){
                    System.out.println(e.toString());
                }

            }
        }

        return availableFiles;
    }
    public String FindFile(String fileName,InetAddress instigatorAddress, int instigatorPort){
        String response = "";
        for (String file:app.strFiles) {
            String[] strFile = file.split(" ");
            if(fileName.equalsIgnoreCase(strFile[0])){
                //if it is not local
                if(strFile.length > 1){
                    try{
                        LoadConnectedServers();
                        String[] serverAddress = file.split(" ")[1].split(":");
                        if(serverAddress.length > 1){
                            ServerLink serverLink = findConnectedServer(InetAddress.getByName(serverAddress[0].replace("/","")),Integer.parseInt(serverAddress[1]));
                            if(serverLink != null){
                                response = serverLink.SendReadRequest(fileName,instigatorAddress,instigatorPort);
                                if(response.equalsIgnoreCase("READ-REDIRECT")){
                                        String redirectToken = UUID.randomUUID().toString().replace("-","").substring(0,20);
                                        redirectConnections.put(fileName + "/" + redirectToken,new ServerLink(new Socket(serverLink.linkSocket.getInetAddress(),serverLink.linkSocket.getPort())));
                                        response = "READ-REDIRECT|" + serverLink.linkSocket.getInetAddress() + ":" + serverLink.linkSocket.getPort() + "|" + redirectToken;

                                }
                                return response;
                            }
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
                return "local";
            }
        }
        return response;


    }
    public ArrayList<String> getLocalFiles(Serveur server){
        ArrayList<String> localFiles = new ArrayList<>();
        for (String file:server.strFiles) {
            if(!(file.split(" ").length > 1)){
                localFiles.add(file);
            }
        }
        return localFiles;
    }

    public String getFileMessageLocal(String fileName) throws IOException {
        StringBuilder message = new StringBuilder();
        strFiles = app.strFiles;
        for (String file:strFiles) {
            if(fileName.equalsIgnoreCase(file)){
                File newFile = new File(FilesPath.replaceAll("\"","")+"\\"+fileName+".txt");
                BufferedReader bfr = new BufferedReader(new FileReader(newFile));
                String line;
                while ((line = bfr.readLine()) != null) {
                    message.append(line).append("\n");
                }
            }
        }

        return message.toString();
    }

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
    public void AddNewFileToFileList(String fileName, String serverIp, String serverPort){
        try{
            FileWriter fileWriter = new FileWriter(filesList);
            String newFileContent = "";
            for (String file:strFiles) {
                newFileContent = newFileContent.concat(file);
                newFileContent = newFileContent.concat("\n");
            }
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

