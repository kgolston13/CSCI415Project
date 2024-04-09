import java.net.*;

public class DNSServer {
    public static void main(String[] args) {
        try {
            // Open port 53 (DNS) and begin listening for new requests
            DatagramSocket server = new DatagramSocket(53);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                server.receive(request);

                Thread requestHandler = new Thread(() -> {
                    handleRequest(request, server);
                });

                requestHandler.start();
            } // End of while

        } catch (Exception exception) {
            exception.printStackTrace();
        } // End of try catch
    } // End of method main

    private static void handleRequest(DatagramPacket newRequest, DatagramSocket newServer) {
        try {
            DNSMessage message = new DNSMessage(newRequest.getData());
            byte[] response = message.getResponse();
            DatagramPacket responsePacket = new DatagramPacket(response, response.length, newRequest.getAddress(),
                    newRequest.getPort());
            newServer.send(responsePacket);
            System.out.println("Packet sent");
        } catch (Exception exception) {
            exception.printStackTrace();
        } // End of try catch
    } // End of method handleRequest

} // End of class DNSServer