import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public final class TCPServer {
   private ServerSocket serverSocket;
   private static final String QUIT = "QUIT";
   private static final String KEYS = "KEYS";
   private static final String PUT = "PUT";
   private static final String DELETE = "DELETE";
   private static final String GET = "GET";
   private static final String STAT = "STAT";
   private List<String> commandList;
   public TCPServer() {
      commandList = new ArrayList<>();
   }

   public static void main(String[] args) {
      TCPServer server = new TCPServer();
      server.startServer();
   }

   private void startServer() {
      try {
         serverSocket = new ServerSocket(8080);
         System.out.println("\u001B[34m" + getCurrentTimeStamp() + " Server started. Listening on port " + serverSocket.getLocalPort());
         while (true) {
            Socket clientSocket = serverSocket.accept();
            Thread clientThread = new Thread(() -> handleClientRequest(clientSocket));
            clientThread.start();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   private void handleClientRequest(Socket clientSocket){
      try (DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
           DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream())) {

         HashMap<String, String> clientData = new HashMap<>();
         commandList = new ArrayList<>();
         System.out.println("\u001B[32m" + getCurrentTimeStamp() + "Client["+clientSocket.getRemoteSocketAddress() + "]" + " Client Connection Successful!"+ "\u001B[0m");

         while (true) {
            String request = dataIn.readUTF();
            String[] parts = request.split(" ");
            String command = parts[0];
            String key = parts.length > 1 ? parts[1] : null;
            String value = parts.length > 2 ? parts[2] : null;

            commandList.add(command);

            switch (command) {
               case PUT:
                  handlePutRequest(clientSocket,dataOut, clientData, key, value);
                  break;
               case DELETE:
                  handleDelRequest(clientSocket,dataOut, clientData, key);
                  break;
               case GET:
                  handleGetRequest(clientSocket,dataOut, clientData, key);
                  break;
               case KEYS:
                  handleKeysRequest(clientSocket,dataOut, clientData);
                  break;
               case QUIT:
                  handleQuitRequest(clientSocket, dataOut);
                  return;
               case STAT:
                  handleStatRequest(dataOut, clientSocket);
               default:
                  break;
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   private synchronized void handlePutRequest(Socket clientSocket, DataOutputStream dataOut, HashMap<String, String> clientData, String key, String value) throws IOException {
      if (!key.matches("[a-zA-Z0-9]+")) {
         System.out.println("\u001B[31m" + getCurrentTimeStamp() + "Client[" + clientSocket.getRemoteSocketAddress() + "]" + " PUT request failed. Key: " + key + " contains invalid characters." + "\u001B[0m");
         dataOut.writeUTF("\u001B[31m" + getCurrentTimeStamp() + " Error: Key contains invalid characters. Only letters and digits are allowed." + "\u001B[0m");
      } else if (clientData.containsKey(key)) {
         System.out.println("\u001B[31m" + getCurrentTimeStamp() + "Client[" + clientSocket.getRemoteSocketAddress() + "]" + " PUT request failed. Key: " + key + " already exists." + "\u001B[0m");
         dataOut.writeUTF("\u001B[31m" + getCurrentTimeStamp() + " PUT request failed. Key: " + key + " already exists." + "\u001B[0m");
      } else if (key.length() > 10 || value.length() > 10) {
         System.out.println("\u001B[31m" + getCurrentTimeStamp() + "Client[" + clientSocket.getRemoteSocketAddress() + "]" + "Key or Value length exceeds the limit of 10 characters" + "\u001B[0m");
         dataOut.writeUTF("\u001B[31m" + getCurrentTimeStamp() + "Error. Key and Value can not be long (max. 10 characters)" + "\u001B[0m");
      } else {
         clientData.put(key, value);
         System.out.println("\u001B[32m" + getCurrentTimeStamp() + "Client[" + clientSocket.getRemoteSocketAddress() + "]" + " Key-Value Pair saved on the server. Key: " + key + ", Value: " + value + "\u001B[0m");
         dataOut.writeUTF("\u001B[32m" + getCurrentTimeStamp() + " Success: Key-Value Pair saved on the server. Key: " + key + ", Value: " + value + "\u001B[0m");
      }
   }
   private synchronized void handleGetRequest(Socket clientSocket ,DataOutputStream dataOut, HashMap<String, String> clientData, String key) throws IOException {
      String value = clientData.getOrDefault(key, "Key not found");
      if (value.equals("Key not found")){
         System.out.println("\u001B[31m" + getCurrentTimeStamp() + "Client["+ clientSocket.getRemoteSocketAddress() + "]"+ " Error. Key Not Found On Server"+ "\u001B[0m");
         dataOut.writeUTF("\u001B[31m" + getCurrentTimeStamp() + " Key not found"+ "\u001B[0m");
      }else {
         System.out.println("\u001B[32m" +getCurrentTimeStamp() + "Client["+ clientSocket.getRemoteSocketAddress() + "]"+ " Success. Key found in the Server. Key: " + key + ", Value: " + value+ "\u001B[0m");
         dataOut.writeUTF( "\u001B[32m" +getCurrentTimeStamp() + " Success: Key found in the store. Key: " + key + ", Value: " + value+ "\u001B[0m");
      }
   }
   private synchronized void handleDelRequest(Socket clientSocket ,DataOutputStream dataOut, HashMap<String, String> clientData, String key) throws IOException {
      if (clientData.containsKey(key)) {
         clientData.remove(key);
         System.out.println("\u001B[32m" +getCurrentTimeStamp() + "Client["+ clientSocket.getRemoteSocketAddress() + "]"+ " Success: Key " + key + " removed"+ "\u001B[0m");
         dataOut.writeUTF("\u001B[32m" +getCurrentTimeStamp() + " Success: Key " + key + " removed from the store"+ "\u001B[0m");
      } else {
         System.out.println("\u001B[31m" + getCurrentTimeStamp() + "Client["+ clientSocket.getRemoteSocketAddress() + "]"+ " Error. Key not found"+ "\u001B[0m");
         dataOut.writeUTF("\u001B[31m" + getCurrentTimeStamp() + " Key not found in the store."+ "\u001B[0m");
      }
   }
   private synchronized void handleKeysRequest(Socket clientSocket ,DataOutputStream dataOut, HashMap<String, String> clientData) throws IOException {
      StringBuilder keys = new StringBuilder();
      for (String key : clientData.keySet()) {
         keys.append(key).append(":");
      }
      if (clientData.keySet().size() > 1) {
         keys.setLength(keys.length() - 1);
         System.out.println("\u001B[32m" +getCurrentTimeStamp() + "Client["+ clientSocket.getRemoteSocketAddress() + "]"+ " Success! Keys: " + keys.toString()+ "\u001B[0m");
         dataOut.writeUTF( "\u001B[32m" +getCurrentTimeStamp() + " Success! Keys: " + keys.toString()+ "\u001B[0m");
      }
      else if(clientData.keySet().size() == 1) {
         keys.setLength(keys.length()-1);
         System.out.println("\u001B[32m" +getCurrentTimeStamp() + "Client["+ clientSocket.getRemoteSocketAddress() + "]"+ " Success! Keys: " + keys.toString()+ "\u001B[0m");
         dataOut.writeUTF("\u001B[32m" +getCurrentTimeStamp() + " Success! Keys:" + keys.toString()+ "\u001B[0m");
      }else {
         System.out.println( "\u001B[31m" + getCurrentTimeStamp() + "Client["+ clientSocket.getRemoteSocketAddress() + "]"+ " Error. There are no keys on Server"+ "\u001B[0m");
         dataOut.writeUTF("\u001B[31m" + getCurrentTimeStamp() + " There are no keys in the store."+ "\u001B[0m");
      }
   }
   private synchronized void handleStatRequest(DataOutputStream dataOut, Socket clientSocket) throws IOException {
      dataOut.writeUTF("\u001B[32m" + getCommandStatistics() + "\u001B[0m");
      System.out.println("\u001B[32m" +getCurrentTimeStamp() + "Client["+ clientSocket.getRemoteSocketAddress() + "]"+ " Statistics Handled" + "\u001B[0m");
   }
   private synchronized void handleQuitRequest(Socket clientSocket, DataOutputStream dataOut) throws IOException {
      dataOut.writeUTF("You have disconnected from the server!");
      System.out.println("\u001B[34m" + getCurrentTimeStamp() + "Client["+ clientSocket.getRemoteSocketAddress() + "]" +" Client disconnected from the server!" + "\u001B[0m");
   }
   private String getCurrentTimeStamp() {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      return "[" + dateFormat.format(new Date()) + "]";
   }
   public int getCommandCount(String command) {
      if(commandList.isEmpty()){
         return 0;
      }else{
      int count = 0;
      for (String cmd : commandList) {
         if (cmd.equals(command)) {
            count++;
         }
      }
      return count;
      }
   }
   public String getCommandStatistics() {
      StringBuilder statistics = new StringBuilder();
      statistics.append("Command statistics:\n");
      statistics.append("PUT: ").append(getCommandCount(PUT)).append("\n");
      statistics.append("GET: ").append(getCommandCount(GET)).append("\n");
      statistics.append("DELETE: ").append(getCommandCount(DELETE)).append("\n");
      statistics.append("KEYS: ").append(getCommandCount(KEYS)).append("\n");
      statistics.append("QUIT: ").append(getCommandCount(QUIT)).append("\n");
      return statistics.toString();
   }
}
