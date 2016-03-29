import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;
import java.util.Random;

/*
  TTFTP UTILITIES - SUMMARY - 
    Ttftp_utils.
  long          extract_checksum         (array)
  long          extract_long             (array, offset)
  void          store_long               (array, offset, value)
  long          getCRC32                 (array)
  byte[]        pull_packet              (array, length)
  int           fromUByte                (byte)
  byte[]        appendHeader             (array, OPcode)
  void          output                   (verbose, message)
  byte[]        requestGETTER            (password, filename, windowsize)
  void          sendPacket               (socket, array, address, port)             ^exception: 
  boolean       validPacket              (byte)
  byte          trimHeader               (array)
*/

class TtftpServerWorker extends Thread {
    String pw;
    int port;
    boolean send_yes;
    int buffersize;
    DatagramPacket request;

    public void run() {
	byte[] data = new byte[request.getLength()];
	System.arraycopy(request.getData(), 0, data, 0, data.length);
	if(Ttftp_utils.validPacket(data)) {
	    System.out.println("valid");
	}

    }

    public TtftpServerWorker(String pw, boolean send_yes, int buffersize, 
			     DatagramPacket request) {
	this.pw = pw;
	this.port = port;
	this.send_yes = send_yes;
	this.buffersize = buffersize;
	this.request = request;
    }
}

class PushTtftp {
    private static String pw = "";
    private static int port = 20001;
    private static boolean send_yes = true;
    private static int buffersize = 1472;

    private static void start_server() {
	try {
	    DatagramSocket ds = new DatagramSocket(port);
	    System.out.println("Server started -" +
			       "\n     Listening on port: " + port + 
			       "\n     Using buffersize:  " + buffersize + 
			       "\n     Using password:   '" +pw + "'" +
			       "\n     Will send YES:     " + send_yes
			       );
	    for(;;) {
		byte[] buffer = new byte[buffersize];
		DatagramPacket p = new DatagramPacket(buffer, buffersize);

		ds.receive(p);
		TtftpServerWorker sw = new TtftpServerWorker(pw, send_yes, buffersize, p);
		sw.run();
	    }

	}
	catch (Exception e) {
	    System.out.println("***** Ex: " + e);
	}
    }

    public static void main(String[] args) {
	//usage: -p port, -pw pass, -y, -b buffsize
	if(args.length != 0) { //we can run this thing with no arguments, so this can be skipped
	    if(args[0].equals("-h")) {//print help info
		System.out.println(help);
		return;
	    }

	    if(args.length > 7) {
		System.out.println(usage);
		return;
	    }
	    else {
		for(int i = 0; i < args.length - 1; i++) { //make sure we can't mess up and overflow
		    if(args[i].equals("-p"))
			port = Integer.parseInt(args[++i]);
		    else if(args[i].equals("-pw"))
			pw = args[++i];
		    else if(args[i].equals("-b"))
			buffersize = Integer.parseInt(args[++i]);
		    else if(args[i].equals("-y"))
			send_yes = false;
		}
		if(args[args.length - 1].equals("-y") && args.length % 2 == 1) 
		    send_yes = false;
	    }
	}

	start_server();
    }

    private static String usage = "-p [port] -b [buffersize] -pw [password] -y\n     -h: help";
    private static String help  = 
	  "-p  [port]:       The port to listen on      - default 20,001" +
	"\n-b  [buffersize]: Define maximum packet size - default 1472 bytes" +
	"\n-pw [password]:   Set password to listen for - default ''" +
	"\n-y                Disable transmission of YES header";
	                          
}

/*
  TTFTP UTILITIES - SUMMARY - 
    Ttftp_utils.
  long          extract_checksum         (array)
  long          extract_long             (array, offset)
  void          store_long               (array, offset, value)
  long          getCRC32                 (array)
  byte[]        pull_packet              (array, length)
  int           fromUByte                (byte)
  byte[]        appendHeader             (array, OPcode)
  void          output                   (verbose, message)
  byte[]        requestGETTER            (password, filename, windowsize)
  void          sendPacket               (socket, array, address, port)             ^exception: 
  boolean       validPacket              (byte)
  byte          trimHeader               (array)
*/
class Ttftp_utils {
    public static final byte     GETTER  = 1;
    public static final byte     YES     = 2;
    public static final byte     NO      = 3;
    public static final byte     GIFT    = 4;
    public static final byte     ABORT   = 5;
    public static final byte     ACK     = 6;

    public static long extract_long(byte[] array, int off) {
	long a = array[off+0] & 0xff;
	long b = array[off+1] & 0xff;
	long c = array[off+2] & 0xff;
	long d = array[off+3] & 0xff;
	long e = array[off+4] & 0xff;
	long f = array[off+5] & 0xff;
	long g = array[off+6] & 0xff;
	long h = array[off+7] & 0xff;
	return (a<<56 | b<<48 | c<<40 | d<<32 | e<<24 | f<<16 | g<<8 | h);
    }
    
    public static long extract_checksum(byte[] array) { //gets checksum, stores zero in place
	long a = array[1] & 0xff;
	long b = array[2] & 0xff;
	long c = array[3] & 0xff;
	long d = array[4] & 0xff;
	long e = array[5] & 0xff;
	long f = array[6] & 0xff;
	long g = array[7] & 0xff;
	long h = array[8] & 0xff;

	store_long(array, 1, 0);
	return (a<<56 | b<<48 | c<<40 | d<<32 | e<<24 | f<<16 | g<<8 | h);
    }

    public static void store_long(byte[] array, int off, long val) {
	array[off + 0] = (byte)((val & 0xff00000000000000L) >> 56);
	array[off + 1] = (byte)((val & 0x00ff000000000000L) >> 48);
	array[off + 2] = (byte)((val & 0x0000ff0000000000L) >> 40);
	array[off + 3] = (byte)((val & 0x000000ff00000000L) >> 32);
	array[off + 4] = (byte)((val & 0x00000000ff000000L) >> 24);
	array[off + 5] = (byte)((val & 0x0000000000ff0000L) >> 16);
	array[off + 6] = (byte)((val & 0x000000000000ff00L) >>  8);
	array[off + 7] = (byte)((val & 0x00000000000000ffL));
	return;
    }

    public static long getCRC32(byte[] b) {
	CRC32 crc = new CRC32();
	crc.update(b);
	return crc.getValue();
    }

    //trims tail of byte array, giving array of length len
    public static byte[] pull_packet(byte[] arr, int len) {
	byte[] b_out = new byte[len];
	System.arraycopy(arr, 0, b_out, 0, len);
	return b_out;
    }

    //give unsigned equivalent of signed byte  -1 -> 255, -128 -> 128
    public static int fromUByte(byte b) {
	return (int)b & 0xFF;
    }

    //appends TTFTP a header to the start of a packet
    public static byte[] appendHeader(byte[] b, byte opcode) {
	byte[] header = new byte[9 + b.length];
	header[0] = opcode;
	//we calculate the checksum as if it were zero, so add in the packet
	if(b.length != 0) //will be needed for abort packet, which has no body
	    System.arraycopy(b, 0, header, 9, b.length);
	long crc = getCRC32(header);
	store_long(header, 1, crc);
	
	return header;
    }

    //chooses whether or not to output based on boolean value
    public static void output(boolean verbose, String output) {
	if(verbose)
	    System.out.println(output);
    }

    //requests a getter packet
    public static byte[] requestGETTER(String password, String filename, int windowsize) {
	//   2 bytes    string   1 byte    string   1 byte
	// +---------+----------+-------+----------+-------+
	// | winsize | password |   0   | filename |   0   |
	// +---------+----------+-------+----------+-------+	
	//get the byte arrays we need to store things
	byte[] pw = password.getBytes();
	byte[] fn = filename.getBytes();
	byte[] packet = new byte[4 + fn.length + pw.length];
	
	//add in window size
	packet[0] = (byte) ((windowsize & 0xff00) >> 8);
	packet[1] = (byte) ((windowsize & 0x00ff) >> 0);

	//copy parameters into packet
	System.arraycopy(pw, 0, packet, 2, pw.length);
	System.arraycopy(fn, 0, packet, 3 + pw.length, fn.length);

	return packet;
    }

    public static void sendPacket 
	(DatagramSocket s, byte[] packet, InetAddress addr, int port) throws Exception {
	DatagramPacket parcel = new DatagramPacket(packet, packet.length, addr, port);
	s.send(parcel);
    }

    public static boolean validPacket(byte[] b) {
	long datasum  = extract_checksum(b);
	long checksum = getCRC32(b);
	return datasum == checksum;
    }

    public static byte[] trimHeader(byte[] b) {
	byte[] output = new byte[b.length - 9]; //size of header
	if(output.length == 0)
	    return output; //just to be safe!
	
	System.arraycopy(b, 9, output, 0, output.length);
	return output;
    }
}
