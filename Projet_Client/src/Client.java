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
    private Client(InetAddress serverAddress, int serverPort) throws Exception{
        this.socket = new Socket(serverAddress, serverPort);
        this.scanner = new Scanner(System.in);
    }

    private void start() throws IOException {
        String input;
        BufferedReader bfr = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        while(true){
            input = scanner.nextLine();
            if(Token != null){
                input += " " +Token;
            }
            PrintWriter out = new PrintWriter(this.socket.getOutputStream(),true);
            out.println(input);
            out.flush();
            Token = bfr.readLine();
            System.out.println(Token);

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