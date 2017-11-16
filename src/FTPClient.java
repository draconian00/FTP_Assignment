import java.io.*;
import java.net.*;
import java.util.*;

public class FTPClient {

    public static void main(String[] args) throws IOException {

        try (
            Socket c_socket = new Socket("127.0.0.1", 2121);
            Socket d_socket = new Socket("127.0.0.1", 2020);
            Scanner scanner = new Scanner(System.in);
            PrintWriter out = new PrintWriter(c_socket.getOutputStream(), true);
            BufferedReader br = new BufferedReader(new InputStreamReader(c_socket.getInputStream()));
        ) {
            System.out.println("Connected to FTP Server on port number 2121");

            while (true) {
                System.out.print("command $ ");
                String command = scanner.nextLine();
                out.println(command);

                if ("quit".equalsIgnoreCase(command)) {
                    break;
                }

                String response = br.readLine().replaceAll("&#10;", "\n");
                System.out.println(response);
            }

            System.out.println("Connection terminated");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
