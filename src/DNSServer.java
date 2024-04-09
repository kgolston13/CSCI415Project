
import java.net.*;
import java.util.HashMap;

public class DNSServer {
    // Variable declarations
    private static HashMap<String, String> addressList;

    public static void main(String[] args) {
        addressList = new HashMap<String, String>();
        try {
            // Open port 53 (DNS) and begin listening for new requests
            DatagramSocket server = new DatagramSocket(53);
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                server.receive(request);
                DNSMessage message = new DNSMessage(request.getData());
                byte[] response = message.getResponse();
                DatagramPacket responsePacket = new DatagramPacket(response, response.length, request.getAddress(), request.getPort());
                server.send(responsePacket);
                System.out.println("Packet sent");
            } // End of while

        } catch (Exception exception) {
            exception.printStackTrace();
        } // End of try catch

    } // End of method main

} // End of class DNSServer