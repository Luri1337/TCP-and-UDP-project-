import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class UDPClient {
    private DatagramSocket clientSocket;
    private InetAddress serverAddress;
    private int serverPort;
    private byte[] receiveData;
    private byte[] sendData;
    private final String quit = "QUIT";
    private final String put = "PUT";
    private final String delete = "DELETE";
    private final String get = "GET";
    private  final String keys = "KEYS";
    BufferedReader consoleInput;
    public UDPClient(String serverHost, int serverPort) {
        try {
            this.clientSocket = new DatagramSocket();
            this.serverAddress = InetAddress.getByName(serverHost);
            this.serverPort = serverPort;
            this.receiveData = new byte[1024];
            this.sendData = new byte[1024];
            consoleInput = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        UDPClient client = new UDPClient("localhost", 8081);
        client.startClient();
    }

    private void startClient(){
        System.out.println("\u001B[32m" + getCurrentTimeStamp() + " Connection Successful!" + "\u001B[0m");
        handleRequests(clientSocket);
    }

    private void handleRequests(DatagramSocket clientSocket) {
        try {
            while (true) {
                System.out.println("Please Input Command in either of the following forms:");
                System.out.println("    PUT <key> <value>");
                System.out.println("    GET <key>");
                System.out.println("    KEYS");
                System.out.println("    DELETE <key>");
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
                    case quit:
                        sendData(quit, clientSocket);
                        receiveDataPacket(clientSocket);
                        return;
                    case put:
                        if (parameters.length != 2) {
                            System.out.println("\u001B[31m" + getCurrentTimeStamp() + " Invalid input. Format: PUT <key> <value>"+ "\u001B[0m");
                            continue;
                        }
                        sendData(put + " " + parameters[0] + " " + parameters[1], clientSocket);
                        receiveDataPacket(clientSocket);
                        break;
                    case delete:
                        if (parameters.length != 1) {
                            System.out.println("\u001B[31m" + getCurrentTimeStamp() + " Invalid input. Format: DELETE <key>"+ "\u001B[0m");
                            continue;
                        }
                        sendData(delete + " " + parameters[0], clientSocket);
                        receiveDataPacket(clientSocket);
                        break;
                    case get:
                        if (parameters.length != 1) {
                            System.out.println("\u001B[31m" + getCurrentTimeStamp() + " Invalid input. Format: GET <key>"+ "\u001B[0m");
                            continue;
                        }
                        sendData(get + " " + parameters[0], clientSocket);
                        receiveDataPacket(clientSocket);
                        break;
                    case keys:
                        sendData(keys , clientSocket);
                       receiveDataPacket(clientSocket);
                        break;
                    default:
                        System.out.println("\u001B[31m" + getCurrentTimeStamp() + " Invalid action: " + action+ "\u001B[0m");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendData(String message, DatagramSocket clientSocket) throws IOException {
        sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        clientSocket.send(sendPacket);
    }
    private void receiveDataPacket(DatagramSocket clientSocket) throws IOException {
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println(receivedMessage);
    }
    private String getCurrentTimeStamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return "[" + dateFormat.format(new Date()) + "]";
    }
}
