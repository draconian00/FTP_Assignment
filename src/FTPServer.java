import java.io.*;
import java.net.*;

public class FTPServer {

    public static void main(String[] args) throws IOException {

        try (
            final ServerSocket c_socket = new ServerSocket(2121);
            final ServerSocket d_socket = new ServerSocket(2020);
        ) {
            while (true) {
                System.out.println("Waiting for new Connection on 2121");
                threadedServer tes = new threadedServer(c_socket.accept(), d_socket.accept());
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("FTP Server Terminating");
    }
}

class threadedServer extends Thread {
    Socket c_client;
    Socket d_client;

    threadedServer(Socket c_soc, Socket d_soc) {
        try {
            c_client = c_soc;
            d_client = d_soc;
            System.out.println("FTP client Connected...");
            // Thread start;
            start();
        } catch (Exception e) {
            e.printStackTrace();
            try { c_client.close(); } catch (IOException ex) { }
        }
    }

    @Override
    public void run() {
        System.out.println("Connected to client using [" + Thread.currentThread() + "]");
        String curDir = System.getProperty("user.dir");
        System.out.println(curDir);

        try (
            BufferedReader br = new BufferedReader(new InputStreamReader(c_client.getInputStream()));
            PrintWriter c_out = new PrintWriter(c_client.getOutputStream(), true);
            DataInputStream d_in = new DataInputStream(d_client.getInputStream());
            DataOutputStream d_out = new DataOutputStream(d_client.getOutputStream());
        ) {
            String commandLine;
            String[] commandArray;
            while ((commandLine = br.readLine()) != null) {
                // Command Process block;
                System.out.println("Client request [" + Thread.currentThread() + "]: " + commandLine);

                commandArray = commandLine.split(" ");
                String command = commandArray[0];
                String arg = (commandArray.length > 1) ? commandArray[1] : "";
                String arg2 = (commandArray.length > 2) ? commandArray[2] : "";
                if ("cd".equalsIgnoreCase(command)) {
                    curDir = CD(c_out, arg, curDir);
                } else if ("list".equalsIgnoreCase(command) || "ls".equalsIgnoreCase(command)) {
                    LIST(c_out, arg, curDir);
                } else if ("get".equalsIgnoreCase(command)) {
                    GET(c_out, d_out, arg, curDir);
                } else if ("put".equalsIgnoreCase(command)) {
                    PUT(c_out, d_in, arg, arg2, curDir);
                } else {
                    c_out.println(command + ": command not found");
                }

            }
            System.out.println("Client [" + Thread.currentThread() + " connection terminated");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    String CD(PrintWriter c_out, String arg, String curDir) {
        try {

            String result;

            if (arg.compareTo("") == 0) {
                result = System.getProperty("user.dir");
            } else {
                File f;
                if (arg.charAt(0) == '/') {
                    f = new File(arg);
                } else {
                    f = new File(curDir + '/' + arg);
                }

                if (!f.exists() || !f.isDirectory()) {
                    throw new Exception("FAILED - Directory name is invalid");
                } else {
                    result = f.getCanonicalPath();
                }
            }

            c_out.println("OK - " + result);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            c_out.println("FAILED - Directory name is invalid");

            return curDir;
        }
    }

    void LIST(PrintWriter c_out, String arg, String curDir) {
        try {
            File dir;
            if (arg.compareTo("") == 0) {
                dir = new File(curDir);
            } else {
                if (arg.charAt(0) == '/') {
                    dir = new File(arg);
                } else {
                    dir = new File(curDir + '/' + arg);
                }
            }

            File[] files = dir.listFiles();
            String f_str;
            String response_str = "total " + files.length + "&#10;";

            for (int i=0; i<files.length; i++) {
                File f = files[i];
                String f_name = f.getName();

                if (f.isDirectory()) {
                    f_str = f_name + ", -";
                } else {
                    f_str = f_name + ", " + f.length();
                }

                if (i != files.length - 1) {
                    f_str += "&#10;";
                }

                response_str += f_str;
            }

            c_out.println(response_str);
        } catch (Exception e) {
            e.printStackTrace();
            c_out.println("FAILED - Directory name is invalid");
        }
    }

    void GET(PrintWriter c_out, DataOutputStream d_out, String arg, String curDir) {
        String filename;
        try {
            if (arg.compareTo("") == 0) {
                c_out.println("FAILED - Need file name argument (Server)");
                return;
            } else {
                if (arg.charAt(0) == '/') {
                    filename = arg;
                } else {
                    filename = curDir + '/' + arg;
                }
            }

            File f = new File(filename);

            if (!f.exists()) {
                c_out.println("FAILED - File does not exist (Server)");
                return;
            } else if (f.isDirectory()) {
                c_out.println("FAILED - " + filename + " is a directory (Server)");
                return;
            } else {
                c_out.println("READY " + f.getName());
                FileInputStream fin = new FileInputStream(f);
                int ch;
                do {
                    ch = fin.read();
                    d_out.writeUTF(String.valueOf(ch));
                } while (ch != -1);
                fin.close();
//                d_out.writeUTF("Received " + f.getName() + " / " + f.length() + "byte(s)");
                c_out.println("Received " + f.getName() + " / " + f.length() + "byte(s)");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void PUT(PrintWriter c_out, DataInputStream d_in, String arg, String arg2, String curDir) {
        String filename = curDir + "/" + arg;

        try {
            File f = new File(filename);

            if (f.exists()) {
                c_out.println("FAILED - File already exists (Server)");
                return;
            } else {
                c_out.println("READY " + arg);

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

                c_out.println("Sent " + arg + " / " + arg2 + "byte(s)");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}