import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;

public class Serveur {
    public static Serveur app;
    private ServerSocket server;
    public File peersList;
    public File filesList;
    public ArrayList<String> strFiles;
    public ArrayList<String> strPeers;

    public ArrayList<Socket> connectedServers;
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
        new Thread(new ServerConnectionThread(app,server)).start();
        //new Thread(new ConnectionThread(server)).start();
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

                app.LoadConnectedServers();
            }

        }


        System.out.println("\r\nRunning Server: " + app.getSocketAddress().getHostAddress() + " " + app.getPort());

        app.listen(app);

        System.out.println(app.findAvailableFiles(app.getSocketAddress(),app.getPort()));

    }

    public void LoadConnectedServers() throws IOException {

        for (String peerAddress: strPeers) {
            String[] addressInfo = peerAddress.split(" ");
            if(addressInfo.length > 1){
                if(InetAddress.getByName(addressInfo[0]).isReachable(1000)){
                    InetAddress address = InetAddress.getByName(addressInfo[0]);
                    try{
                        Socket socket = new Socket(address, Integer.parseInt(addressInfo[1]));
                        connectedServers.add(socket);
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

    public ArrayList<String> findAvailableFiles(InetAddress instigatorAddress, int instigatorPort){
        ArrayList<String> availableFiles = new ArrayList<>();
        //load local files
        for (String file:app.strFiles) {
            if(!(file.split(" ").length > 1)){
                availableFiles.add(file);
            }
        }
        //search for files on connected servers
        for (Socket socket:app.connectedServers) {
            try{
                if(instigatorAddress != socket.getInetAddress() && instigatorPort != socket.getPort()){
                    BufferedReader bfr = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
                    out.println("LS " + server.getInetAddress().toString() + ":" + server.getLocalPort());
                    out.flush();

                    String Response = bfr.readLine();
                    for (String fileName: Response.split(" ")) {
                        if(!availableFiles.contains(fileName)){
                            availableFiles.add(fileName);
                        }
                    }
                }

            }
            catch(Exception e){
                System.out.println(e);
            }

        }
        return availableFiles;
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

