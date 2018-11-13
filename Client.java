import java.net.*;
import java.io.*;
import java.util.*;

public class Client {
    // initialize socket and input output streams
    private Socket socket = null;
    private DataInputStream input = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;

    // private String name = null;
    // private int upload_port = null;

    // constructor to put ip address and port
    public Client(String server_address, int server_port, int upload_port) {
        // establish a connection
        try {
            socket = new Socket(server_address, server_port);
            System.out.println("Connected");

            // takes input from terminal
            input = new DataInputStream(System.in);
            in = new DataInputStream(socket.getInputStream());
            // sends output to the socket
            out = new DataOutputStream(socket.getOutputStream());
        } catch (UnknownHostException u) {
            System.out.println(u);
        } catch (IOException i) {
            System.out.println(i);
        }

        // string to read message from input
        String line = "";

        // keep reading until "Over" is input
        while (!line.equals("Over")) {
            try {
                System.out.println(in.readUTF());
                line = input.readLine();
                out.writeUTF(line);
            } catch (IOException i) {
                System.out.println(i);
            }
        }

        // close the connection
        try {
            input.close();
            out.close();
            socket.close();
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public static void main(String args[]) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Please Enter Client Upload Port Number: ");
        int cport = sc.nextInt();
        Client client = new Client("127.0.0.1", 7734, cport);
    }
}