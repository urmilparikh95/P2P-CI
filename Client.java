import java.net.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class Client {
    // initialize socket and input output streams
    private Socket socket = null;
    private DataInputStream input_terminal = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private static int upload_port;
    private static String hostname;

    // constructor to put ip address and port
    public Client(String server_address, int server_port) {
        // establish a connection
        try {
            socket = new Socket(server_address, server_port);
            System.out.println("Connected to CI Server\n");

            // takes input from terminal
            input_terminal = new DataInputStream(System.in);
            // read Server messages
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

        // Display methods available to client
        System.out.println(
                "Hello!!\n" + "List of methods available:\n" + "1. ADD: add an RFC to the peer to peer network\n"
                        + "2. LOOKUP: find peers that have a specified RFC\n" + "3. LIST: list all RFCs available\n"
                        + "4. GET: download an RFC\n" + "Type 'exit or 'Ctrl+C to terminate connection\n");

        // keep reading until "Exit" is input
        while (true) {
            try {
                System.out.print("Enter Method: ");
                line = input_terminal.readLine();
                if (line.equalsIgnoreCase("exit")) {
                    break;
                } else if (line.equalsIgnoreCase("ADD")) {
                    System.out.print("Enter RFC number: ");
                    String rfc = input_terminal.readLine();
                    System.out.print("Enter RFC Title: ");
                    String title = input_terminal.readLine();
                    String request = generateRequest("add", rfc, title);
                    out.writeUTF(request);
                    System.out.println(in.readUTF());
                    //
                } else if (line.equalsIgnoreCase("LOOKUP")) {
                    System.out.print("Enter RFC number: ");
                    String rfc = input_terminal.readLine();
                    System.out.print("Enter RFC Title: ");
                    String title = input_terminal.readLine();
                    String request = generateRequest("lookup", rfc, title);
                    out.writeUTF(request);
                    System.out.println(in.readUTF());
                    //
                } else if (line.equalsIgnoreCase("LIST")) {
                    String request = generateRequest("list", "", "");
                    out.writeUTF(request);
                    System.out.println(in.readUTF());
                    //
                } else if (line.equalsIgnoreCase("GET")) {
                    System.out.print("Enter RFC number: ");
                    String rfc = input_terminal.readLine();
                    String request = generateRequest("get", rfc, "");
                    out.writeUTF(request);
                    //
                } else {
                    System.out.println("Not a valid method. Please Try Again");
                }
                System.out.println();
            } catch (IOException i) {
                System.out.println(i);
            }
        }

        // close the connection
        try {
            in.close();
            input_terminal.close();
            out.close();
            socket.close();
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public static void main(String args[]) {
        int server_port = 7734;
        String server_addr = "127.0.0.1";
        if (args.length >= 2) {
            server_addr = args[0];
            server_port = Integer.parseInt(args[1]);
        }
        Scanner sc = new Scanner(System.in);
        System.out.print("Please Enter Hostname: ");
        hostname = sc.next();
        ServerSocket upload_server;
        int flag = 0;
        while (flag == 0) {
            try {
                System.out.print("Please Enter Client Upload Port Number: ");
                upload_port = sc.nextInt();
                upload_server = new ServerSocket(upload_port);
                System.out.println("Upload Server started at Port " + upload_port);
                flag = 1;
                // assign new thread to upload server
                Thread handler = new UploadServer(upload_server);
                handler.start();
            } catch (IOException e) {
                System.out.println("Port in use");
            }
        }
        new Client(server_addr, server_port);
    }

    public String generateRequest(String method, String rfc, String title) {
        String request = "";
        switch (method) {
        case "add":
        case "lookup":
            request += method.toUpperCase() + " RFC " + rfc + " P2P-CI/1.0\n";
            request += "Host: " + hostname + "\n";
            request += "Port: " + upload_port + "\n";
            request += "Title: " + title;
            break;
        case "list":
            request += method.toUpperCase() + " ALL P2P-CI/1.0\n";
            request += "Host: " + hostname + "\n";
            request += "Port: " + upload_port;
            break;
        case "get":
            request += method.toUpperCase() + " RFC " + rfc + " P2P-CI/1.0\n";
            request += "Host: " + hostname + "\n";
            request += "OS: " + OSInfo();
            break;
        }
        return request;
    }
}

class UploadServer extends Thread {
    final ServerSocket upload_server;

    public UploadServer(ServerSocket upload_server) {
        this.upload_server = upload_server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = upload_server.accept();
                System.out.println("\nNew Request from Peer at ip " + socket.getRemoteSocketAddress().toString());

                // takes input from the client socket
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                String request, response;

                response = "P2P-CI/1.0 ";
                request = in.readUTF();

                LocalDateTime now = LocalDateTime.now();

                // Check for Bad Request
                if (!request.matches("GET RFC (\\d)* .*\\nHost: .*\\nOS: .*")) {
                    response += "400 Bad Request\nDate: " + formatToGMT(new Date()) + "\nOS: " + OSInfo();
                    out.writeUTF(response);
                    continue;
                }

                // parse request in arrays
                String[] lines = request.split("\n");
                String[] line0 = lines[0].split(" ");

                // Check if proper version
                if (!line0[line0.length - 1].equals("P2P-CI/1.0")) {
                    response += "505 P2P-CI Version Not Supported\nDate: " + formatToGMT(new Date()) + "\nOS: "
                            + OSInfo();
                    out.writeUTF(response);
                    continue;
                }

                in.close();
                out.close();

            } catch (Exception e) {
                // close connection and output error
                socket.close();
                e.printStackTrace();
            }
        }
    }

    public String formatToGMT(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
        return format.format(new Date(sdf.format(date))) + " GMT";
    }

    public String OSInfo() {
        return System.getProperty("os.name") + " " + System.getProperty("os.version");
    }
}