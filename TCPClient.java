import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public final class TCPClient {
    private static final String QUIT = "QUIT";
    private static final String KEYS = "KEYS";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private static final String GET = "GET";
    private static final String STAT = "STATISTICS";
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private Socket socket;
    private BufferedReader consoleInput;

    public static void main(String[] args) {
        TCPClient client = new TCPClient();
        client.start();
    }

    public TCPClient() {
        consoleInput = new BufferedReader(new InputStreamReader(System.in));
    }
    private void start() {
        try {
            socket = new Socket("localhost", 8080);
            dataOut = new DataOutputStream(socket.getOutputStream());
            dataIn = new DataInputStream(socket.getInputStream());

            System.out.println("\u001B[32m" + getCurrentTimeStamp() + " Connection Successful!" + "\u001B[0m");

            handleRequests();

        } catch (IOException e){
            System.out.println("\u001B[31m" + getCurrentTimeStamp() + " Error. Connection Refused from Server. Make sure port number and IP are correct and the Server is running!" + "\u001B[0m");
        }
    }

    private void handleRequests() {
        try {
            while (true) {
                System.out.println("Please Input Command in either of the following forms:");
                System.out.println("    PUT <key> <value>");
                System.out.println("    GET <key>");
                System.out.println("    KEYS");
                System.out.println("    DELETE <key>");
                System.out.println("    STATISTICS");
                System.out.println("    QUIT");
                System.out.print("Enter Command: ");
                String input = consoleInput.readLine();
                if (input == null || input.trim().isEmpty()) {
                    continue;
                }
                String[] parts = input.trim().split("\\s+", 2);
                String action = parts[0];
                String[] parameters = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

                switch (action) {
                    case QUIT:
                        sendData(QUIT);
                        handleQuitRequest();
                        cleanUp();
                        return;
                    case PUT:
                        if (parameters.length != 2) {
                            System.out.println("\u001B[31m" + getCurrentTimeStamp() + " Invalid input. Format: PUT <key> <value>"+ "\u001B[0m");
                            continue;
                        }
                        handlePutRequest(parameters[0], parameters[1]);
                        break;
                    case KEYS:
                        handleKeysRequest();
                        String keysResponse = dataIn.readUTF();
                        System.out.println(keysResponse);
                        break;
                    case DELETE:
                        if (parameters.length != 1) {
                            System.out.println("\u001B[31m" + getCurrentTimeStamp() + " Invalid input. Format: DELETE <key>"+ "\u001B[0m");
                            continue;
                        }
                        handleDelRequest(parameters[0]);
                        break;
                    case GET:
                        if (parameters.length != 1) {
                            System.out.println("\u001B[31m" + getCurrentTimeStamp() + " Invalid input. Format: GET <key>"+ "\u001B[0m");
                            continue;
                        }
                        handleGetRequest(parameters[0]);
                        String getResponse = dataIn.readUTF();
                        System.out.println(getResponse);
                        break;
                    case STAT:
                        sendData("STAT");
                        handleStatRequest();
                        break;
                    default:
                        System.out.println("\u001B[31m" + getCurrentTimeStamp() + " Invalid action: " + action+ "\u001B[0m");
                }
            }
        } catch (IOException e) {
          e.printStackTrace();
        }
    }
    private void sendData(String message) {
        try {
            dataOut.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetRequest(String key) {
        sendData(GET + " " + key);
    }
    private void handleStatRequest() {
        try {
            String putResponse = dataIn.readUTF();
            System.out.println(putResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handlePutRequest(String key, String value) {
        sendData(PUT + " " + key + " " + value);
        try {
            String putResponse = dataIn.readUTF();
            System.out.println(putResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleDelRequest(String key) {
        sendData(DELETE + " " + key);
        try {
            String delResponse = dataIn.readUTF();
            System.out.println(delResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleKeysRequest() {
        sendData(KEYS);
    }
    private void handleQuitRequest() {
        try {
            String putResponse = dataIn.readUTF();
            System.out.println(putResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCurrentTimeStamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return "[" + dateFormat.format(new Date()) + "]";
    }
    private void cleanUp() {
        try {
            dataOut.close();
            dataIn.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
