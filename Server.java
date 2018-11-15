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

    // Constructor
    public ClientHandler(Socket socket, DataInputStream in, DataOutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        String request, response;
        String hostname = "", upload_port, rfc, title, method;
        while (true) {
            try {
                response = "P2P-CI/1.0 ";
                request = in.readUTF();

                // Check for Bad Request
                if(!request.matches("(ADD|LOOKUP) RFC (\\d)* .*\\nHost: .*\\nPort: (\\d)*\\nTitle: .*") || !request.matches("LIST ALL .*\\nHost: .*\\nPort: (\\d)*") ){
                    response += "400 Bad Request";
                    out.writeUTF(response);
                    continue;
                }

                // parse request in arrays
                String[] lines = request.split("\n");
                String[] line0 = lines[0].split(" ");

                // Check if proper version
                if (line0[line0.length-1].equals("P2P-CI/1.0")){
                    response+= "505 P2P-CI Version Not Supported";
                    out.writeUTF(response);
                    continue;
                }

                method = line0[0];
                hostname = lines[1].substring(6) + socket.getRemoteSocketAddress().toString();
                upload_port = lines[2].substring(6);
                addHost(hostname, upload_port);

                switch(method){
                    case "ADD":
                        rfc = line0[2];
                        title = lines[2].substring(7);
                        addRFC(rfc, title, hostname);
                        response += "200 OK\nRFC " + rfc + " " + title + " " + upload_port;
                        break;
                    case "LOOKUP":
                        rfc = line0[2];
                        title = lines[2].substring(7);
                        if(Server.rfcs.containsKey(Integer.parseInt(rfc))){
                            response += "200 OK\n";
                            for(Integer i : Server.rfcs.keySet()){
                                RFC temp = Server.rfcs.get(i);
                                for(String h : temp.hosts) {

                                }
                            }
                        } else {
                            response += "404 Not Found";
                        }
                        break;
                }
                out.writeUTF(response);
            } catch (IOException e) {
                // remove the client entries
                removeHost(hostname);
                System.out.println("Connection with " + hostname + " terminated");
                break;
            }
        }

        try {
            // closing resources
            this.socket.close();
            this.in.close();
            this.out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeHost(String hostname) {
        for (Peer peer : Server.peers) {
            if (peer.hostname == hostname) {
                Server.peers.remove(peer);
                break;
            }
        }
        for (Integer i : Server.rfcs.keySet()) {
            RFC temp = Server.rfcs.get(i);
            temp.removeHost(hostname);
            if (temp.hosts.size() == 0) {
                Server.rfcs.remove(i);
            }
        }
    }

    public void addHost(String hostname, String port) {
        Server.peers.add(new Peer(hostname,port));
    }

    public void addRFC(String rfc, String title, String hostname) {
        if(Server.rfcs.containsKey(Integer.parseInt(rfc))){
            Server.rfcs.addHost(hostname);
        } else {
            RFC temp = new RFC(rfc, title);
            temp.addHost(hostname);
            Server.rfcs.put(Integer.parseInt(rfc), temp);
        }
    }
}