import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
    // initialize socket and input stream
    private Socket socket = null;
    private ServerSocket server = null;

    public static List<Peer> peers;
    public static Map<Integer, RFC> rfcs;

    // constructor with port
    public Server(int port) throws IOException {

        // starts server and waits for a connection
        server = new ServerSocket(port);
        System.out.println("Server started at port " + port);
        System.out.println("Waiting for a client ...");

        peers = new LinkedList<>();
        rfcs = new HashMap<>();

        while (true) {
            try {
                socket = server.accept();
                System.out.println("Client connected from ip " + socket.getRemoteSocketAddress().toString());

                // takes input from the client socket
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                System.out.println("Assigning new thread for this client");

                // create a new thread object
                Thread client_handler = new ClientHandler(socket, in, out);

                client_handler.start();

            } catch (Exception e) {
                // close connection and output error
                socket.close();
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) throws IOException {
        int port = 7734;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new Server(port);
    }
}

class ClientHandler extends Thread {
    final DataInputStream in;
    final DataOutputStream out;
    final Socket socket;
    private String hostname;

    // Constructor
    public ClientHandler(Socket socket, DataInputStream in, DataOutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        while (true) {
            try {
                out.writeUTF(
                        "Connected to Server..\n" + "Enter total number of RFCs you have (must be greater than 0)");

                hostname = socket.getRemoteSocketAddress().toString();
                int count = Integer.parseInt(in.readUTF());

                for (int i = 0; i < count; i++) {
                    out.writeUTF("Enter RFC number for RFC " + i);
                    int rfc_no = Integer.parseInt(in.readUTF());
                    out.writeUTF("Enter RFC title for RFC " + i);
                    String rfc_title = in.readUTF();
                    if (Server.rfcs.containsKey(rfc_no)) {
                        RFC temp = Server.rfcs.get(rfc_no);
                        temp.addHost(hostname);
                    } else {
                        RFC temp = new RFC(rfc_no, rfc_title);
                        temp.addHost(hostname);
                        Server.rfcs.put(rfc_no, temp);
                    }
                }

                out.writeUTF("Enter upload port");
                int u_port = Integer.parseInt(in.readUTF());
                Peer temp_peer = new Peer(hostname, u_port);
                Server.peers.add(temp_peer);

                System.out.println("Client - " + hostname + " has been setup as a peer in the network");

                String received;
                String toreturn;

                out.writeUTF("Hello!!\n" + "Type an RFC number to get the list of peers containing it\n"
                        + "Type 'list' to list the available RFCs\n" + "Type 'info' to list the commands available\n"
                        + "Type 'exit' to leave\n");

                // define flag to terminate server on exit
                int flag = 0;
                while (flag != 1) {
                    received = in.readUTF();
                    switch (received) {
                    case "list":
                        toreturn = "RFC numbers available are ";
                        for (int i : Server.rfcs.keySet()) {
                            toreturn += i + ", ";
                        }
                        out.writeUTF(toreturn.trim().substring(0, toreturn.length() - 1));
                        break;
                    case "info":
                        out.writeUTF("Type an RFC number to get the list of peers containing it\n"
                                + "Type 'list' to list the available RFCs\n"
                                + "Type 'info' to list the commands available\n" + "Type 'exit' to leave\n");
                        break;
                    case "exit":
                        out.writeUTF("Connection closed");
                        // remove the client entries
                        for(Peer peer : Server.peers){
                            if(peer.hostname == hostname){
                                Server.peers.remove(peer);
                                break;
                            }
                        }
                        for(Integer i : Server.rfcs.keySet()){
                            Server.rfcs.get(i).removeHost(hostname);
                        }
                        this.socket.close();
                        System.out.println("Connection with " + hostname + " closed");
                        flag = 1;
                        break;
                    default:
                        if(received.matches("^\\d+$")){
                            int rfc_no = Integer.parseInt(received);
                            if(Server.rfcs.keySet().contains(rfc_no)){
                                toreturn = "Peers containing RFC " + rfc_no +  " are ";
                                for (String host : Server.rfcs.get(rfc_no).hosts) {
                                    toreturn += host + ", ";
                                }
                                toreturn = toreturn.trim().substring(0, toreturn.length() - 1);
                            } else {
                                toreturn = "No Peer contains that RFC";
                            }
                            out.writeUTF(toreturn);
                        } else {
                            out.writeUTF("Command not Supported");
                        }
                    }
                }

            } catch (IOException e) {
                // e.printStackTrace();
                // remove the client entries
                for(Peer peer : Server.peers){
                    if(peer.hostname == hostname){
                        Server.peers.remove(peer);
                        break;
                    }
                }
                for(Integer i : Server.rfcs.keySet()){
                    Server.rfcs.get(i).removeHost(hostname);
                }
                this.socket.close();
                System.out.println("Connection with " + hostname + " closed");
                break;
            }
        }

        try {
            // closing resources
            this.in.close();
            this.out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}