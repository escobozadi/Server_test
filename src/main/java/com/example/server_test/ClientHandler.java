package com.example.server_test;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<ClientHandler>();
    public static ArrayList<String> clientNames = new ArrayList<String>();

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            this.clientUsername = dataInputStream.readUTF();
            clientHandlers.add(this);
            clientNames.add(clientUsername);

            // Send list of connected clients
            sendMessage("SENDING CLIENTS");
            sendMessage(convertStringArrayToString(clientNames, ","));

            System.out.println("Client "+ clientUsername + " setup");
        } catch (IOException e) {
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                // messageFromClient = bufferedReader.readLine();
                messageFromClient = dataInputStream.readUTF();

                // RECEIVING FILE
                if (messageFromClient.equals("SENDING FILE")) {
                    int fileNameLength = dataInputStream.readInt();
                    if (fileNameLength > 0) {
                        byte[] fileNameBytes = new byte[fileNameLength];
                        dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                        // String fileName= new String(fileNameBytes);

                        int fileContentLength = dataInputStream.readInt();
                        if (fileContentLength > 0) {
                            byte[] fileContentBytes = new byte[fileContentLength];

                            dataInputStream.readFully(fileContentBytes, 0, fileContentLength);
                            sendFile(fileNameBytes, fileContentBytes);

                        }
                    }

                } else if (messageFromClient.split("-")[1].equals("left")) {
                    System.out.println(messageFromClient);

                    clientNames.remove(messageFromClient.split("-")[0]);
                    // Send updated list of connected clients
                    sendMessage("SENDING CLIENTS");
                    sendMessage(convertStringArrayToString(clientNames, ","));

                } else {
                    System.out.println("Message received from " + clientUsername + ": " + messageFromClient);
                    broadcastMessage(messageFromClient);
                }

            }   catch (IOException e) {
                closeEverything(socket, dataInputStream, dataOutputStream);
                break;
            }
        }

    }

    public void sendFile(byte[] fileName, byte[] fileContent) {
        for (ClientHandler clientHandler: clientHandlers) {
            try {
                clientHandler.dataOutputStream.writeUTF("SENDING FILE");
                clientHandler.dataOutputStream.flush();

                dataOutputStream.writeInt(fileName.length);
                dataOutputStream.write(fileName);

                dataOutputStream.writeInt(fileContent.length);
                dataOutputStream.write(fileContent);

            } catch (IOException e) {
                closeEverything(socket, dataInputStream, dataOutputStream);
            }
        }
    }

    public void broadcastMessage(String message) {
        for (ClientHandler clientHandler: clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.dataOutputStream.writeUTF(message);
                    clientHandler.dataOutputStream.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, dataInputStream, dataOutputStream);
            }
        }
    }

    public void sendMessage(String message) {
        for (ClientHandler clientHandler: clientHandlers) {
            try {
                clientHandler.dataOutputStream.writeUTF(message);
                clientHandler.dataOutputStream.flush();

            } catch (IOException e) {
                closeEverything(socket, dataInputStream, dataOutputStream);
            }
        }
    }

    public void removeClient() {
        clientNames.remove(this.clientUsername);
        clientHandlers.remove(this);

        // Update client list
        sendMessage("SENDING CLIENTS");
        sendMessage(convertStringArrayToString(clientNames, ","));

        broadcastMessage("SERVER: " + clientUsername + " has left the chat!");
    }

    private static String convertStringArrayToString(ArrayList<String> strArr, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (String str : strArr)
            sb.append(str).append(delimiter);
        return sb.substring(0, sb.length() - 1);
    }

    public void closeEverything(Socket socket, DataInputStream bufferedReader, DataOutputStream bufferedWriter) {
        removeClient();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

