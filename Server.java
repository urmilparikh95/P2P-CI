import java.net.*;
import java.io.*;
import java.util.*;

class Peer {

    public String hostname;
    public String port;

    public Peer(String hostname, String port) {
        this.hostname = hostname;
        this.port = port;
    }

    public String toString() {
        return hostname;
    }
}

class RFC {

    public String number;
    public String title;
    public List<Peer> peers;

    public RFC(String number, String title) {
        this.number = number;
        this.title = title;
        peers = new ArrayList<>();
    }

    public String toString() {
        return peers.toString();
    }

}

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
        hostname = socket.getRemoteSocketAddress().toString();
        String request, response;
        String upload_port, rfc, title, method;
        int init = 0;
        Peer host = null;
        while (true) {
            try {
                response = "P2P-CI/1.0 ";
                request = in.readUTF();

                // Check for Bad Request
                if (!request.matches("(ADD|LOOKUP) RFC (\\d)* .*\\nHost: .*\\nPort: (\\d)*\\nTitle: .*")
                        && !request.matches("LIST ALL .*\\nHost: .*\\nPort: (\\d)*")) {
                    response += "400 Bad Request";
                    out.writeUTF(response);
                    continue;
                }

                // parse request in arrays
                String[] lines = request.split("\n");
                String[] line0 = lines[0].split(" ");

                // Check if proper version
                if (!line0[line0.length - 1].equals("P2P-CI/1.0")) {
                    response += "505 P2P-CI Version Not Supported";
                    out.writeUTF(response);
                    continue;
                }

                method = line0[0];
                hostname = lines[1].substring(6) + socket.getRemoteSocketAddress().toString();
                upload_port = lines[2].substring(6);

                if (init == 0) {
                    host = addHost(hostname, upload_port);
                    init++;
                }

                switch (method) {
                case "ADD":
                    rfc = line0[2];
                    title = lines[3].substring(7);
                    addRFC(rfc, title, host);
                    response += "200 OK\nRFC " + rfc + " " + title + " " + hostname + " " + upload_port;
                    break;
                case "LOOKUP":
                    rfc = line0[2];
                    title = lines[3].substring(7);
                    if (Server.rfcs.containsKey(Integer.parseInt(rfc))) {
                        response += "200 OK\n";
                        RFC temp = Server.rfcs.get(Integer.parseInt(rfc));
                        for (Peer p : temp.peers) {
                            response += "RFC " + temp.number + " " + temp.title + " " + p.hostname + " " + p.port
                                    + "\n";
                        }
                    } else {
                        response += "404 Not Found";
                    }
                    break;
                case "LIST":
                    if (Server.rfcs.size() > 0) {
                        response += "200 OK\n";
                        for (Integer i : Server.rfcs.keySet()) {
                            RFC temp = Server.rfcs.get(i);
                            for (Peer p : temp.peers) {
                                response += "RFC " + temp.number + " " + temp.title + " " + p.hostname + " " + p.port
                                        + "\n";
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
                removeHost(host);
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

    public void removeHost(Peer host) {
        for (Integer i : Server.rfcs.keySet()) {
            RFC temp = Server.rfcs.get(i);
            temp.peers.remove(host);
            if (temp.peers.size() == 0) {
                Server.rfcs.remove(i);
            }
        }
        Server.peers.remove(host);
    }

    public Peer addHost(String hostname, String port) {
        Peer host = new Peer(hostname, port);
        Server.peers.add(host);
        return host;
    }

    public void addRFC(String rfc, String title, Peer host) {
        if (Server.rfcs.containsKey(Integer.parseInt(rfc))) {
            RFC temp = Server.rfcs.get(Integer.parseInt(rfc));
            temp.peers.add(host);
        } else {
            RFC temp = new RFC(rfc, title);
            temp.peers.add(host);
            Server.rfcs.put(Integer.parseInt(rfc), temp);
        }
    }
}