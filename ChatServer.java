import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static ConcurrentHashMap<String, ClientHandler> clientMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public static void sendPrivateMessage(String clientId, String message) {
        ClientHandler client = clientMap.get(clientId);
        if (client != null) {
            client.sendMessage(message);
        }
    }

    public static void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        clientMap.remove(clientHandler.getClientId());
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                clientId = in.readLine();
                clientMap.put(clientId, this);
                System.out.println("Client connected: " + clientId);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("@")) {
                        int spaceIndex = message.indexOf(' ');
                        if (spaceIndex != -1) {
                            String targetClientId = message.substring(1, spaceIndex);
                            String privateMessage = message.substring(spaceIndex + 1);
                            sendPrivateMessage(targetClientId, privateMessage);
                            System.out.println("Private message from " + clientId + " to " + targetClientId + ": " + privateMessage);
                        }
                    } else {
                        broadcastMessage(clientId + ": " + message);
                        System.out.println("Broadcast message from " + clientId + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                removeClient(this);
                System.out.println("Client disconnected: " + clientId);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public String getClientId() {
            return clientId;
        }
    }
}