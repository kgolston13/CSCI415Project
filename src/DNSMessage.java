import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

public class DNSMessage {
	// Constants
	private final int RDLENGTH = 4;
	private final long TTL = 3600;
	private final byte Z = 0;
	private final byte RA = 0;
	private final short QTYPE = 1;
	private final short QCLASS = 1;
	private final byte QR = 1;

	// byte[] data types used to create response messages
	private byte[] data;
	private byte[] headerSection;
	private byte[] querySection;
	private byte[] answerSection;
	private byte[] responseMessage;

	// Request data fields
	private short questionCount;
	private short answerCount;
	private short nsCount;
	private short arCount;
	private int flags;
	private byte id;
	private byte opcode;
	private byte aa;
	private byte tc;
	private byte rd;

	// Other variables
	private boolean isTypeAReq;
	private boolean isValidHost;
	private String name;
	private String address;
	private short rCode;

	private static final HashMap<String, String> addressList = new HashMap<String, String>();

	static {
		addressList.put("key1", "ip");
		addressList.put("key2", "ip");
		addressList.put("key3", "ip");
	}

	// Constructor
	public DNSMessage(byte[] data) {
		this.data = data;
		isTypeAReq = this.isTypeARequest();
		this.populateRequestData();
		this.createResponse();
	} // End of Constructor(byte[])

	// Public Methods

	public byte[] getResponse() {
		return responseMessage;
	} // End of method getResponse

	public String getDomainName() {
		return name;
	} // End of method getDomainName

	public boolean isTypeA() {
		return isTypeAReq;
	} // End of method isTypeA

	public boolean isValidHostName() {
		return isValidHost;
	} // End of method isValidHostName

	// private Methods

	private void initializeHeaderSection() {
		// Byte array of size 12 for the header section where every byte is comrpised of
		// specific values in specific bytes in accordance to RFC1035
		headerSection = new byte[12];
		headerSection[0] = (byte) ((id >> 8) & 0xFF);
		headerSection[1] = (byte) (id & 0xFF);
		headerSection[2] = (byte) ((QR << 7) | (opcode << 3) | (aa << 2) | (tc << 1) | rd);
		headerSection[3] = (byte) ((RA << 7) | (Z << 4) | rCode);
		headerSection[4] = (byte) ((questionCount >> 8) & 0xFF);
		headerSection[5] = (byte) (questionCount & 0xFF);
		headerSection[6] = (byte) ((answerCount >> 8) & 0xFF);
		headerSection[7] = (byte) (answerCount & 0xFF);
		headerSection[8] = (byte) ((nsCount >> 8) & 0xFF);
		headerSection[9] = (byte) (nsCount & 0xFF);
		headerSection[10] = (byte) ((arCount >> 8) & 0xFF);
		headerSection[11] = (byte) (arCount & 0xFF);
	} // End of method initializeHeaderSection

	// Queries section, 4 bytes(Query type and class) plus requested domain size
	private void initializeQuerySection() {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		// Add the domain name bytes
		byte[] domainBytes = this.convertDomainToBytes(name);
		outputStream.write(domainBytes, 0, domainBytes.length);

		// Add QTYPE (2 bytes)
		outputStream.write((QTYPE >> 8) & 0xFF);
		outputStream.write(QTYPE & 0xFF);

		// Add QCLASS (2 bytes)
		outputStream.write((QCLASS >> 8) & 0xFF);
		outputStream.write(QCLASS & 0xFF);

		querySection = outputStream.toByteArray();
	} // End of method initializeQuerySection

	// Answer section is default 12 bytes plus the size of the data
	// we are sending back (4 since we're only accepting type A req using ipv4)
	private void initializeAnswerSection() {
		if (rCode != 0) { // If response is not 0, we do not add an answer section
			answerSection = new byte[0];
			return;
		} // End of if

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
		// Address field, (4 bytes for Ipv4 since we're only accepting type A requests)
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
	} // End of method getDomain

	private String getAddress(String domain) {
		if (addressList.containsKey(name)) {
			isValidHost = true;
			return addressList.get(name);
		}

		String addressFromDomain;
		try { // Ignore arpa requests
			addressFromDomain = InetAddress.getByName(domain).getHostAddress();
			if (domain.toLowerCase().endsWith("arpa") || domain.toLowerCase().endsWith("home")) {
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
		} // End of try/catch statement
	} // End of method getAddress

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
			} // End of try/catch
		} // End of for

		// Terminating zero length octet
		byteArrayOutputStream.write(0);
		return byteArrayOutputStream.toByteArray();
	} // End of method convertDomainToBytes

	private boolean isTypeARequest() {
		if (data.length < 12) { // If request is not long enough to contain enough data for a DNS request
			return false;
		} // End of if

		// Checking for 00 01 00 01 sequence to determine if request is type A or not
		int queryTypePosition = 12;
		while (queryTypePosition + 4 <= data.length) {
			if (data[queryTypePosition] == 0x00 && data[queryTypePosition + 1] == 0x01
					&& data[queryTypePosition + 2] == 0x00 && data[queryTypePosition + 3] == 0x01) {
				return true; // Type A request found
			} // End of inner if
			queryTypePosition++;
		} // End of while
		return false; // Not a type A request
	} // End of method isTypeARequest

	private void populateRequestData() {
		id = (byte) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
		flags = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
		opcode = (byte) ((flags >> 11) & 0xF);
		aa = (byte) ((flags >> 10) & 1);
		tc = (byte) ((flags >> 9) & 1);
		rd = (byte) ((flags >> 8) & 1);
		questionCount = (short) (((data[4] & 0xFF) << 8) | (data[5] & 0xFF));
		nsCount = (short) (((data[8] & 0xFF) << 8) | (data[9] & 0xFF));
		arCount = (short) (((data[10] & 0xFF) << 8) | (data[11] & 0xFF));
		name = this.getDomain(12);
		address = this.getAddress(name);

		if (isTypeAReq & isValidHost) { // Type A and address is not null
			rCode = 0; // No error condition
			answerCount = 1;
		} else if (!isTypeAReq) { // Not Type A
			rCode = 4; // Not Implemented - The Name server does not support the requested kind of
						// query.
			answerCount = 0;
		} else { // Type A but address is null
			rCode = 3; // Name Error - Meaningful only for responses from an authoritative name server,
						// this code
						// signifies that the domain name referenced in the query does not exist.
			answerCount = 0;
		} // End of if

		this.initializeHeaderSection();
		this.initializeQuerySection();
		this.initializeAnswerSection();
	} // End of method populateRequestData

	// Combine header, query, and answer sections into a single byte array
	private void createResponse() {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		// Add each section
		outputStream.write(headerSection, 0, headerSection.length);
		outputStream.write(querySection, 0, querySection.length);
		outputStream.write(answerSection, 0, answerSection.length);

		// Get the complete response message as a byte array
		responseMessage = outputStream.toByteArray();
	} // ENd of method createResponse
} // End of class DNSMessage