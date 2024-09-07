import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UDPServer {
    private DatagramSocket serverSocket;
    private int port;
    private byte[] receiveData;
    private byte[] sendData;
    private final String quit = "QUIT";
    private final String put = "PUT";
    private final String delete = "DELETE";
    private final String get = "GET";
    private final String keys = "KEYS";
    private ExecutorService executor;
    private HashMap<String, String> clientData;

    public UDPServer(int port) {
        this.port = port;
        receiveData = new byte[1024];
        sendData = new byte[1024];
        executor = Executors.newFixedThreadPool(10);
        clientData = new HashMap<>();
    }

    public static void main(String[] args) {
        UDPServer server = new UDPServer(8081);
        server.startServer();
    }

    private void startServer() {
        try {
            serverSocket = new DatagramSocket(port);
            System.out.println("\u001B[34m" + getCurrentTimeStamp() + " Server started. Listening on port " + port);
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                executor.execute(() -> handleClientRequest(receivePacket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (executor != null) {
                executor.shutdown();
            }
        }
    }
    private void handleClientRequest(DatagramPacket receivePacket) {
        InetAddress clientAddress = receivePacket.getAddress();
        int clientPort = receivePacket.getPort();

        String request = new String(receivePacket.getData(), 0, receivePacket.getLength());
        String[] parts = request.split(" ");
        String command = parts[0];
        String key = parts.length > 1 ? parts[1] : null;
        String value = parts.length > 2 ? parts[2] : null;

        switch (command) {
            case put:
                handlePutRequest(key, value, clientAddress, clientPort);
                break;
            case delete:
                handleDelRequest(key, clientAddress, clientPort);
                break;
            case get:
                handleGetRequest(key, clientAddress, clientPort);
                break;
            case keys:
                handleKeysRequest(clientAddress, clientPort);
                break;
            case quit:
                handleQuitRequest(clientAddress, clientPort);
                return;
            default:
                break;
        }
    }
    private void handlePutRequest(String key, String value, InetAddress clientAddress, int clientPort) {
        if (!key.matches("[a-zA-Z0-9]+")) {
            System.out.println("\u001B[31m" +  getCurrentTimeStamp() + getClientAddress(clientAddress, clientPort) +  " PUT request failed. Key: " + key + " contains invalid characters." + "\u001B[0m");
            sendResponse("\u001B[31m" + getCurrentTimeStamp() + " Error: Key contains invalid characters. Only letters and digits are allowed." + "\u001B[0m", clientAddress, clientPort);
        } else if (clientData.containsKey(key)) {
            System.out.println("\u001B[31m" + getCurrentTimeStamp() + getClientAddress(clientAddress, clientPort) +  " PUT request failed. Key: " + key + " already exists." + "\u001B[0m");
            sendResponse("\u001B[31m" + getCurrentTimeStamp() + " PUT request failed. Key: " + key + " already exists." + "\u001B[0m", clientAddress, clientPort);
        } else if (key.length() > 10 || value.length() > 10) {
            sendResponse("\u001B[31m" + getCurrentTimeStamp() + "Error. Key and Value can not be long (max. 10 characters)" + "\u001B[0m", clientAddress, clientPort);
            System.out.println("\u001B[31m" + getCurrentTimeStamp() + getClientAddress(clientAddress, clientPort) +" Key or Value length exceeds the limit of 10 characters" + "\u001B[0m");
        } else {
            clientData.put(key, value);
            System.out.println("\u001B[32m" + getCurrentTimeStamp() + getClientAddress(clientAddress, clientPort) +" Key-Value Pair saved on the server. Key: " + key + ", Value: " + value + "\u001B[0m");
            sendResponse("\u001B[32m" + getCurrentTimeStamp() + " Success: Key-Value Pair saved on the server. Key: " + key + ", Value: " + value + "\u001B[0m", clientAddress, clientPort);
        }
    }
    private void handleGetRequest(String key, InetAddress clientAddress, int clientPort) {
        String value = clientData.getOrDefault(key, "Key not found");
        if (value.equals("Key not found")) {
            String response = value.equals("Key not found") ?"\u001B[31m" + "Error. Key "+ key +" not found" + "\u001B[0m" : "\u001B[32m" +"Success. Key found in the Server. Key: " + key + ", Value: " + value+ "\u001B[0m";
            System.out.println("\u001B[31m" + getCurrentTimeStamp() + getClientAddress(clientAddress, clientPort) +  " Error. Key " + key +" Not Found On Server"+ "\u001B[0m");
            sendResponse(response, clientAddress, clientPort);
        }else{
        String response = getCurrentTimeStamp()+ getClientAddress(clientAddress, clientPort)+"\u001B[32m" + " Success. Key found in the Server. Key: " + key + ", Value: " + value+ "\u001B[0m";
        System.out.println("\u001B[32m" + getCurrentTimeStamp() + " Success. Key found in the Server. Key: " + key + ", Value: " + value+ "\u001B[0m");
        sendResponse(response, clientAddress, clientPort);
        }
    }
    private void handleDelRequest(String key, InetAddress clientAddress, int clientPort) {
        if (clientData.containsKey(key)) {
            clientData.remove(key);
            sendResponse("\u001B[32m" + "Success: Key " + key + " removed from the store"+ "\u001B[0m", clientAddress, clientPort);
            System.out.println("\u001B[32m" + getCurrentTimeStamp() + getClientAddress(clientAddress, clientPort)+ " Success: Key " + key + " removed" + "\u001B[0m");
        } else {
            sendResponse("\u001B[31m" + "Key not found in the store" + "\u001B[0m", clientAddress, clientPort);
            System.out.println("\u001B[31m" + getCurrentTimeStamp() +  getClientAddress(clientAddress, clientPort)+" Error. Key not found" + "\u001B[0m");
        }
    }
    private void handleKeysRequest(InetAddress clientAddress, int clientPort) {
        StringBuilder keys = new StringBuilder();
        for (String key : clientData.keySet()) {
            keys.append(key).append(":");
        }
        String response;
        if (keys.length() > 0) {
            keys.setLength(keys.length() - 1);
            response = "\u001B[32m" + "Success! Keys: " + keys.toString() + "\u001B[0m";
            System.out.println("\u001B[32m" + getCurrentTimeStamp() + getClientAddress(clientAddress, clientPort) +  " Success! Keys: " + keys.toString()+ "\u001B[0m");
            sendResponse(response, clientAddress, clientPort);
        } else {
            response = "\u001B[31m" + "There are no keys in the store." + "\u001B[0m";
            System.out.println( "\u001B[31m" + getCurrentTimeStamp()+ getClientAddress(clientAddress, clientPort) + "There are no keys in the store." + "\u001B[0m");
            sendResponse(response, clientAddress, clientPort);
        }
    }
    private void handleQuitRequest(InetAddress clientAddress, int clientPort) {
        sendResponse("You have disconnected from the server!", clientAddress, clientPort);
        System.out.println("\u001B[34m" + getCurrentTimeStamp() + getClientAddress(clientAddress, clientPort) +" Client disconnected from the server!" + "\u001B[0m");
    }
    private void sendResponse(String message, InetAddress clientAddress, int clientPort) {
        try {
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String getCurrentTimeStamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return "[" + dateFormat.format(new Date()) + "]";
    }
    private String getClientAddress(InetAddress clientAddress, int clientPort){
        return "Client" +"[" + clientAddress + ":" + clientPort +"]";
    }
}
