import java.io.*;
import java.net.*;
import java.util.*;

public class FTPClient {

    public static void main(String[] args) throws Exception {

        try (
            Socket c_socket = new Socket("127.0.0.1", 2121);
            Socket d_socket = new Socket("127.0.0.1", 2020);
        ) {
            launchClient client = new launchClient(c_socket, d_socket);
            client.launch();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

class launchClient {
    Socket c_socket;
    Socket d_socket;

    Scanner scanner;

    PrintWriter c_out;
    BufferedReader c_in;

    DataOutputStream d_out;
    DataInputStream d_in;

    launchClient(Socket c_soc, Socket d_soc) {
        try {
            c_socket = c_soc;
            d_socket = d_soc;

            scanner = new Scanner(System.in);

            c_out = new PrintWriter(c_socket.getOutputStream(), true);
            c_in = new BufferedReader(new InputStreamReader(c_socket.getInputStream()));

            d_out = new DataOutputStream(d_socket.getOutputStream());
            d_in = new DataInputStream(d_socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void launch() throws Exception {
        System.out.println("Connected to FTP Server on port number 2121");

        while (true) {
            System.out.print("command $ ");
            String commandLine = scanner.nextLine();

            String[] commandArray = commandLine.split(" ");
            String command = commandArray[0];

            if ("quit".equalsIgnoreCase(command)) {
                break;
            } else if ("cd".equalsIgnoreCase(command) || "list".equalsIgnoreCase(command) || "ls".equalsIgnoreCase(command)) {
                c_out.println(commandLine);
                String response = c_in.readLine().replaceAll("&#10;", "\n");
                System.out.println(response);
            } else if ("get".equalsIgnoreCase(command)) {
                ReceiveFile(c_in, c_out, d_in, commandLine);
            } else if ("put".equalsIgnoreCase(command)) {
                SendFile(c_in, c_out, d_out, commandLine);
            } else {
                // command not found
                System.out.println(command + ": command not found");
            }
        }
        System.out.println("Connection terminated");
    }

    void ReceiveFile(BufferedReader br, PrintWriter out, DataInputStream d_in, String commandLine) {
        try {
            c_out.println(commandLine);

            String response = br.readLine();
            String[] resArray = response.split(" ");
            String status = resArray[0];

            if ("failed".equalsIgnoreCase(status)) {
                System.out.println(response);
                return;
            } else {
                String filename = resArray[1];
                File f = new File(filename);

                if (f.exists()) {
                    System.out.println("FAILED - File already exist (Client)");
                    out.flush();
                } else {

                    FileOutputStream f_out = new FileOutputStream(f);
                    int ch;
                    String temp;
                    do {
                        temp = d_in.readUTF();
                        ch = Integer.parseInt(temp);
                        if (ch != -1) {
                            f_out.write(ch);
                        }
                    } while (ch != -1);
                    f_out.close();
                    System.out.println(br.readLine());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void SendFile(BufferedReader br, PrintWriter out, DataOutputStream d_out, String commandLine) {
        try {
            String[] commandArray = commandLine.split(" ");
            String command = commandArray[0];
            String argument = (commandArray.length > 1) ? commandArray[1] : "";

            if (argument.compareTo("") == 0) {
                System.out.println("FAILED - File does not exist (Client)");
                return;
            } else {
                String filename = argument;
                File f = new File(filename);

                if (!f.exists()) {
                    System.out.println("FAILED - File does not exist (Client)");
                    return;
                } else {
                    out.println(command + " " + f.getName() + " " + f.length());

                    String response = br.readLine();
                    String[] resArray = response.split(" ");
                    String status = resArray[0];

                    if ("failed".equalsIgnoreCase(status)) {
                        System.out.println(response);
                    } else {
                        FileInputStream f_in = new FileInputStream(f);
                        int ch;

                        do {
                            ch = f_in.read();
                            d_out.writeUTF(String.valueOf(ch));
                        } while (ch != -1);
                        f_in.close();

                        System.out.println(br.readLine());
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
