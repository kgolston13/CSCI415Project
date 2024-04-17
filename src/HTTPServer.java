import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class HTTPServer {
    public static void main(String[] args) throws Exception {
        ServerSocket web = new ServerSocket(80);

        while (true) {
            Socket clientConnection = web.accept(); // blocking statement

            // Connection from client is set up
            Scanner In = new Scanner(clientConnection.getInputStream());
            DataOutputStream Out = new DataOutputStream(clientConnection.getOutputStream());

            // Handle HTTP request
            String requestMessageLine1 = In.nextLine();
            String requestMessageLine2 = In.nextLine();
            System.out.println("Client sent: " + requestMessageLine1 + "\n" + requestMessageLine2);

            String[] requestedSite = requestMessageLine2.split(" ");
            String fileName = requestedSite[1] + ".html";

            File file = new File("data" + fileName);

            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put(".jpg", "image/jpg");
            headers.put(".png", "image/png");
            headers.put(".html", "text/html");
            headers.put(".txt", "text/plain");

            // Send a response back
            String statusLine = "HTTP/1.1 200 OK\r\n";
            String contentType = "Content-Type: " + headers.get(fileName.substring(fileName.lastIndexOf('.'))) + "\r\n";
            String endheader = "\r\n";
            try {
                FileInputStream fileStream = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                Out.writeBytes(statusLine + contentType + endheader);
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    Out.write(buffer, 0, bytesRead);
                }
                fileStream.close();
            } catch (FileNotFoundException e) {
                String notFound = "HTTP/1.1 404 Not Found\r\n";
                Out.writeBytes(notFound + endheader + "<h1>404 Error</h1> <p>The file you were looking for was not found :(</p>");

            }
            // Close all connections
            In.close();
            Out.close();
            clientConnection.close();
        }
    }
}
