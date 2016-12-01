package engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import universe.Universe;

public class ServerThread extends Thread {

    private Socket socket = null;
    private Universe universe = null;
    private boolean connected = false;

    public ServerThread(Socket socket, Universe universe) {
        this.socket = socket;
        this.universe = universe;
    }

    @Override
    public void run() {
        try (
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()));) {
            while (true) {
                if (!connected) {
                    System.out.println("new connection");
                    //send connected message
                    out.println("you are connected!");
                    out.flush();

                    //ask for client id
                    System.out.println("requesting clientId");
                    out.println("clientId?");
                    out.flush();
                    connected = true;
                } else {
                    String fromClient = in.readLine();

                    if (fromClient != null) {
                        if (fromClient.contains("clientId:")) {
                            System.out.println("received clientId: " + fromClient);
                        }
                    }
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
