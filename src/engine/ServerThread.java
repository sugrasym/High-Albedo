package engine;
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerThread extends Thread {
    private Socket socket = null;
 
    public ServerThread(Socket socket) {
        this.socket = socket;
    }
     
    @Override
    public void run() {
        try (
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream()));
        ) {
            out.write("you are connected!");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}