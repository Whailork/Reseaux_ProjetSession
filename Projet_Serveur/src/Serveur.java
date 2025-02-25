import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Serveur {
    private ServerSocket server;
    public static File peersList;
    public static File filesList;
    public Serveur(String ipAdresse) throws Exception{
        if(ipAdresse != null && !ipAdresse.isEmpty()){

            this.server = new ServerSocket(0,1, InetAddress.getByName(ipAdresse));
        }
        else{
            this.server = new ServerSocket(0,1,InetAddress.getLocalHost());
        }
    }

    private void listen() throws Exception{
        new Thread(new ConnectionThread(server)).start();

    }

    public InetAddress getSocketAddress(){
        return this.server.getInetAddress();
    }

    public int getPort(){
        return this.server.getLocalPort();
    }

    public static void main(String[] args) throws Exception {
        Serveur app = new Serveur(InetAddress.getLocalHost().getHostAddress());
        if(args != null){
            if(args.length > 0){
                peersList =  new File(args[0]);
                filesList = new File(args[1]);
                System.out.println("fichiers detectes");

                int ch;
                FileReader fl = new FileReader(filesList);
                while ((ch=fl.read())!=-1)
                    System.out.print((char)ch);

                // close the file
                fl.close();
            }
        }


        System.out.println("\r\nRunning Server: " + app.getSocketAddress().getHostAddress() + " " + app.getPort());

        app.listen();



    }
}

