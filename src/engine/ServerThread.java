package engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.AstralIO;
import universe.Universe;

public class ServerThread extends Thread {

    private Socket socket = null;
    private Universe universe = null;
    private boolean connected = false;

    public ServerThread(Socket socket, Universe universe) {
        this.socket = socket;
        this.universe = universe;
    }

    private String getValue(String pack) {
        if (pack != null) {
            return pack.split(":")[1];
        }

        return "";
    }

    private String serializeGame(Universe universe) throws Exception {
        try {
            //generate serializable universe
            AstralIO.Everything everything = new AstralIO().new Everything(universe);
            //serialize universe
            String o = AstralIO.compress(everything);
            return o;
        } catch (ConcurrentModificationException e) {
            return serializeGame(universe);
        }
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
                    initialConnection(out);
                    askForClientId(out);

                    connected = true;
                } else {
                    String fromClient = in.readLine();

                    if (fromClient != null) {
                        if (fromClient.contains("clientId:")) {
                            respondToClientId(fromClient, out);
                        }
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);

                    sendError(out);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendError(final PrintWriter out) {
        out.print("error!");
        out.flush();
    }

    /*
    * Responds to the client sending its ID by sending the current state
    * of the host's universe for client initialization.
     */
    private void respondToClientId(String fromClient, final PrintWriter out) {
        String id = getValue(fromClient);
        System.out.println("received clientId: " + id);

        //send the universe
        String u;
        try {
            u = serializeGame(universe);
            char[] arr = ("universe:" + u).toCharArray();

            bufferedWrite(out, arr);
        } catch (Exception ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);

            sendError(out);
        }
    }

    /*
    * Asks the client for its unique identifier.
    * todo: match this up with an account
     */
    private void askForClientId(final PrintWriter out) {
        //ask for client id
        System.out.println("requesting clientId");
        out.println("clientId?");
        out.flush();
    }

    private void initialConnection(final PrintWriter out) {
        System.out.println("new connection");
        //send connected message
        out.println("you are connected!");
        out.flush();
    }

    /*
    * Sends a large amount of data as a series of writes.
     */
    private void bufferedWrite(final PrintWriter out, char[] arr) {
        out.println("BUFFER_START:");
        String toSend = "";
        int c = 0;
        for (int a = 0; a < arr.length; a++) {
            toSend += arr[a];
            c++;

            if (c == 64 || a + 1 >= arr.length) {
                out.println(toSend);
                out.flush();
                toSend = "";
                c = 0;
            }
        }

        out.println(":BUFFER_STOP");
        out.flush();
    }
}
