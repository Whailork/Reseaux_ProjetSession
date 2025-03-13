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

    public ArrayList<ServerLink> connectedServers;
    public HashMap<String,ServerLink> redirectConnections;
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
            if(args.length > 2){


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
                if(InetAddress.getByName(addressInfo[0]).isReachable(100)){
                    InetAddress address = InetAddress.getByName(addressInfo[0]);
                    try{
                        if(findConnectedServer(address,Integer.parseInt(addressInfo[1])) == null){
                            Socket socket = new Socket(address, Integer.parseInt(addressInfo[1]));
                            ServerLink serverLink = new ServerLink(socket);
                            connectedServers.add(serverLink);
                        }
                        else{
                            System.out.println("server already connected");
                        }

                    }
                    catch(Exception e){
                        System.out.println("cannot connect to server : " + peerAddress);
                    }

                }
                else {
                    System.out.println("cannot connect to server : " + peerAddress);
                }
            }
        }

    }

    public ServerLink findConnectedServer(InetAddress address, int port){
        for (ServerLink serverLink:connectedServers) {
            if(serverLink.linkSocket.getInetAddress() == address && serverLink.linkSocket.getPort() == port){
                return serverLink;
            }
        }
        return null;
    }

    public String findAvailableFiles(InetAddress instigatorAddress, int instigatorPort){
        try{
            LoadConnectedServers();
        }
        catch(Exception e){
            System.out.println("error while connecting to peer servers");
        }

        String availableFiles = "";
        //load local files
        for (String file:app.strFiles) {
            if(!(file.split(" ").length > 1)){
                availableFiles = availableFiles.concat(file + " ");
            }
        }
        //search for files on connected servers
        for (ServerLink serverLink:app.connectedServers) {
            try{
                if(instigatorAddress != serverLink.linkSocket.getInetAddress() && instigatorPort != serverLink.linkSocket.getPort()){
                   String response = serverLink.SendLSRequest(instigatorAddress,instigatorPort);
                    availableFiles = availableFiles.concat(response);
                }

            }
            catch(Exception e){
                System.out.println(e);
            }

        }
        return availableFiles;
    }
    public String FindFile(String fileName,InetAddress instigatorAddress, int instigatorPort){
        for (String file:app.strFiles) {
            String[] strFile = file.split(" ");
            if(fileName.equalsIgnoreCase(strFile[0])){
                if(strFile.length > 1){
                    return strFile[1];
                }
                if(instigatorAddress != server.getInetAddress() && instigatorPort != server.getLocalPort()){
                    return server.getInetAddress() + ":" + server.getLocalPort();
                }
                return "local";
            }
        }
        try{
            LoadConnectedServers();
        }
        catch(Exception e){
            System.out.println("error while connecting to peer servers");
        }
        String response = "";
        for (ServerLink serverLink:app.connectedServers){
            try{
                if(instigatorAddress != serverLink.linkSocket.getInetAddress() && instigatorPort != serverLink.linkSocket.getPort()){
                    response = serverLink.SendReadRequest(fileName,instigatorAddress,instigatorPort);
                    if(!response.equalsIgnoreCase("")){
                        if(instigatorAddress == server.getInetAddress() && instigatorPort == server.getLocalPort()){
                            String[] strResponse = response.split(" ");
                            redirectConnections.put(fileName + "/" + UUID.randomUUID().toString().replace("-","").substring(0,20),new ServerLink(new Socket(InetAddress.getByName(strResponse[1].substring(1)),Integer.parseInt(strResponse[2]))));
                        }
                    }
                }
            }
            catch(Exception e){
                System.out.println(e);
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
}

