import java.awt.FlowLayout;
import java.awt.Font;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class DNSServer {
    public static void main(String[] args) {
        ServerGUI frame = new ServerGUI();
        try {
            // Open port 53 (DNS) and begin listening for new requests
            DatagramSocket server = new DatagramSocket(53);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                server.receive(request);

                Thread requestHandler = new Thread(() -> {
                    handleRequest(request, server, frame);
                });

                requestHandler.start();
            } // End of while

        } catch (Exception exception) {
            exception.printStackTrace();
        } // End of try catch
    } // End of method main

    private static void handleRequest(DatagramPacket newRequest, DatagramSocket newServer, ServerGUI newFrame) {
        try {
            newFrame.incrementRequests();
            DNSMessage message = new DNSMessage(newRequest.getData());
            byte[] response = message.getResponse();
            DatagramPacket responsePacket = new DatagramPacket(response, response.length, newRequest.getAddress(),
                    newRequest.getPort());
            newServer.send(responsePacket);
            System.out.println("Packet sent");
            newFrame.incrementResponses();
        } catch (Exception exception) {
            exception.printStackTrace();
        } // End of try catch
    } // End of method handleRequest

} // End of class DNSServer

class ServerGUI extends JFrame {
    private JLabel requestLabel;
    private JLabel responseLabel;
    private int reqNum;
    private int resNum;

    public void incrementRequests() {
        ++reqNum;
        requestLabel.setText("Requests: " + reqNum);
    }

    public void incrementResponses() {
        ++resNum;
        responseLabel.setText("Responses: " + resNum);
    }

    public ServerGUI() {
        Font defaultFont = new Font("Times New Roman", Font.BOLD, 20);
        reqNum = 0;
        resNum = 0;

        requestLabel = new JLabel("Requests: " + reqNum);
        responseLabel = new JLabel("Responses: " + resNum);

        requestLabel.setFont(defaultFont);
        responseLabel.setFont(defaultFont);

        this.setLayout(new FlowLayout());
        this.add(requestLabel);
        this.add(responseLabel);
        this.setTitle("DNS Server");
        this.setSize(400, 400);
        this.setLocationRelativeTo(null); // Center the frame
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    } // End of default constructor

} // End of class ServerGUI