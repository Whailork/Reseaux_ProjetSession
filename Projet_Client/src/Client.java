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
    private Client(InetAddress serverAddress, int serverPort) throws Exception{
        this.socket = new Socket(serverAddress, serverPort);
        this.scanner = new Scanner(System.in);
    }

    //to register Register 192.168.0.15
    private void start() throws IOException {
        String input;
        BufferedReader bfr = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        while(true){

            input = scanner.nextLine();
            if(Token != null){
                input += " " +Token;
            }
            else{
                input+= " " + socket.getInetAddress().toString();
            }
            PrintWriter out = new PrintWriter(this.socket.getOutputStream(),true);
            out.println(input);
            out.flush();

            Response = bfr.readLine();
            if(Token == null){
                String[] splitResponse = Response.split(" ");
                Token = splitResponse[1];
            }
            else{

            }
            System.out.println(Response);

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