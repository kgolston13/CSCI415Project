
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
                
                // If message is not a type A request we ignore the request and continue
                if (!message.isTypeA()) {
                    System.out.println("Request denied: Not Type-A Request");
                    continue;
                } else if(!message.isValidHostName()) {
                	System.out.println("Request denied: Not A Valid Domain Name");
                	continue;
                }

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