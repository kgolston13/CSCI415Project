import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DNSMessage {
	private byte[] data;
	private byte[] headerSection;
	private byte[] querySection;
	private byte[] answerSection;
	private byte[] domainBytes;
	private byte[] responseMessage;
	
	private short questionCount;
	private short answerCount;
	private short nsCount;
	private short arCount;

	private final int RDLENGTH = 4;
	private final long TTL = 3600;
	private final byte Z = 0;
	private final short RCODE = 0;
	private final byte RA = 0;
	private final short QTYPE = 1;
	private final short QCLASS = 1;
	private final byte QR = 1;

	private byte id;
	private int flags;
	private byte opcode;
	private byte aa;
	private byte tc;
	private byte rd;

	private boolean isTypeAReq;
	private boolean isValidHost;
	private String name;
	private String address;

	// Constructor
	public DNSMessage(byte[] data) {
		this.data = data;
		if (this.isTypeARequest()) {
			isTypeAReq = true;
			this.populateRequestData();
			if(!isValidHost) {
				return;
			}
			this.createResponse();
			return;
		}
		isTypeAReq = false;
	}

	// Public Methods

	public byte[] getResponse() {
		return responseMessage;
	}

	public String getDomainName() {
		return name;
	}

	public boolean isTypeA() {
		return isTypeAReq;
	}

	public boolean isValidHostName() {
		return isValidHost;
	}

	// private Methods

	private void initializeHeaderSection() {
		headerSection = new byte[12];
		headerSection[0] = (byte) ((id >> 8) & 0xFF);
		headerSection[1] = (byte) (id & 0xFF);
		headerSection[2] = (byte) ((QR << 7) | (opcode << 3) | (aa << 2) | (tc << 1) | rd);
		headerSection[3] = (byte) ((RA << 7) | (Z << 4) | RCODE);
		headerSection[4] = (byte) ((questionCount >> 8) & 0xFF);
		headerSection[5] = (byte) (questionCount & 0xFF);
		headerSection[6] = (byte) ((answerCount >> 8) & 0xFF);
		headerSection[7] = (byte) (answerCount & 0xFF);
		headerSection[8] = (byte) ((nsCount >> 8) & 0xFF);
		headerSection[9] = (byte) (nsCount & 0xFF);
		headerSection[10] = (byte) ((arCount >> 8) & 0xFF);
		headerSection[11] = (byte) (arCount & 0xFF);
	}

	private void initializeQuerySection() {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		// Add the domain name bytes
		outputStream.write(domainBytes, 0, domainBytes.length);

		// Add QTYPE (2 bytes)
		outputStream.write((QTYPE >> 8) & 0xFF);
		outputStream.write(QTYPE & 0xFF);

		// Add QCLASS (2 bytes)
		outputStream.write((QCLASS >> 8) & 0xFF);
		outputStream.write(QCLASS & 0xFF);

		querySection = outputStream.toByteArray();
	}

	private void initializeAnswerSection() {
	    // Creating byte array for the answer section
	    int totalLength = 12 + RDLENGTH;
	    answerSection = new byte[totalLength];
	    int index = 0;
	    
	    // Name field (Pointer, 2 bytes)
	    answerSection[index++] = (byte) 0xc0;
	    answerSection[index++] = (byte) 0x0c;
	    // Type field (2 bytes)
	    answerSection[index++] = (byte) ((QTYPE >> 8) & 0xFF);
	    answerSection[index++] = (byte) (QTYPE & 0xFF);
	    // Class field (2 bytes)
	    answerSection[index++] = (byte) ((QCLASS >> 8) & 0xFF);
	    answerSection[index++] = (byte) (QCLASS & 0xFF);
	    // TTL field (4 bytes)
	    answerSection[index++] = (byte) ((TTL >> 24) & 0xFF);
	    answerSection[index++] = (byte) ((TTL >> 16) & 0xFF);
	    answerSection[index++] = (byte) ((TTL >> 8) & 0xFF);
	    answerSection[index++] = (byte) (TTL & 0xFF);
	    // Data length field (2 bytes)
	    answerSection[index++] = (byte) ((RDLENGTH >> 8) & 0xFF);
	    answerSection[index++] = (byte) (RDLENGTH & 0xFF);

	    // Converting IP address to byte array
	    String[] addressParts = address.split("\\."); // Period delimiter
	    // Address field, (4 bytes for Ipv4)
	    for (int i = 0; i < RDLENGTH; i++) {
	    	answerSection[index++] = (byte) (Integer.parseInt(addressParts[i]) & 0xFF); 
	    } // End of for

	} // End of method initializeAnswerSection

	// Method to get the domain from the DNS request
	private String getDomain(int offset) {
		StringBuilder domainBuilder = new StringBuilder();
		int length = data[offset++];
		while (length != 0) {
			// Pointer label so we recursively call to get each part of the domain
			if ((length & 0xC0) == 0xC0) {
				int pointer = ((length & 0x3F) << 8) | (data[offset++] & 0xFF);
				getDomain(pointer);
				return domainBuilder.toString();
			}
			// Regular label
			for (int i = 0; i < length; i++) {
				domainBuilder.append((char) data[offset++]);
			}
			domainBuilder.append('.');
			length = data[offset++];
		}
		// Remove the last dot
		domainBuilder.deleteCharAt(domainBuilder.length() - 1);
		return domainBuilder.toString();
	}

	private String getAddress(String domain) {
		String addressFromDomain;
		try {
			addressFromDomain = InetAddress.getByName(domain).getHostAddress();
			if(domain.toLowerCase().endsWith("arpa") || domain.toLowerCase().endsWith("home")) {
				isValidHost = false;
				return null;
			}
			System.out.println("Resolved address for domain " + domain + ": " + addressFromDomain);
			isValidHost = true;
			return addressFromDomain;
		} catch (UnknownHostException e) {
			isValidHost = false;
			System.err.println("Error resolving domain: " + domain);
			return null;
		}
	}

	private byte[] convertDomainToBytes(String domain) {
		String[] labels = domain.split("\\.");
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		for (String label : labels) {
			byte length = (byte) label.length();
			byteArrayOutputStream.write(length);
			try {
				byteArrayOutputStream.write(label.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Terminating zero length octet
		byteArrayOutputStream.write(0);
		return byteArrayOutputStream.toByteArray();
	}

	private boolean isTypeARequest() {
		if (data.length < 12) {
			return false;
		}
		// Checking for 00 01 00 01 sequence to determine if request is type A or not
		int queryTypePosition = 12;
		while (queryTypePosition + 4 <= data.length) {
			if (data[queryTypePosition] == 0x00 && data[queryTypePosition + 1] == 0x01
					&& data[queryTypePosition + 2] == 0x00 && data[queryTypePosition + 3] == 0x01) {
				return true; // Type A request found
			}
			queryTypePosition++;
		}
		return false; // Not a type A request
	}

	private void populateRequestData() {
		if (data.length < 12) {
			return;
		}
		id = (byte) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
		flags = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
		opcode = (byte) ((flags >> 11) & 0xF);
		aa = (byte) ((flags >> 10) & 1);
		tc = (byte) ((flags >> 9) & 1);
		rd = (byte) ((flags >> 8) & 1);
		questionCount = (short) (((data[4] & 0xFF) << 8) | (data[5] & 0xFF));
		answerCount = questionCount;
		nsCount = (short) (((data[8] & 0xFF) << 8) | (data[9] & 0xFF));
		arCount = (short) (((data[10] & 0xFF) << 8) | (data[11] & 0xFF));
		name = this.getDomain(12);
		address = this.getAddress(name);

		if (address == null) {
			System.out.println("returning because add is null");
			return;
		}
		
		domainBytes = this.convertDomainToBytes(name);
		
		this.initializeHeaderSection();
		this.initializeQuerySection();
		this.initializeAnswerSection();
	}

	private void createResponse() {
	    // Combine header, query, and answer sections into a single byte array
	    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	    // Add each section
	    outputStream.write(headerSection, 0, headerSection.length);
	    outputStream.write(querySection, 0, querySection.length);
	    outputStream.write(answerSection, 0, answerSection.length);

	    // Get the complete response message as a byte array
	    responseMessage = outputStream.toByteArray();
	}
}