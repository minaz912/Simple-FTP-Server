import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionHandler {

	private static final int PORT_NO = 1500;

	public static void main(String[] args) {
		ServerSocket sSocket;

		try {
			if (args.length > 0) {
				sSocket = new ServerSocket(Integer.parseInt(args[0]));
			} else {
				sSocket = new ServerSocket(PORT_NO);
			}
			while (true) {
				Socket socket = sSocket.accept();
				Thread clientThr = new ClientHandlerThread(socket);
				clientThr.start();
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * doStream.writeUTF("Please enter file name to download: ");
		 * 
		 * DataInputStream diStream = new
		 * DataInputStream(socket.getInputStream()); String fileName =
		 * DataInputStream.readUTF(diStream);
		 * 
		 * for (String file : dir.list()) { if (file.equals(fileName)) { File
		 * fileToSend = new File(DEFAULT_PATH + fileName); byte[] bytesToSend =
		 * new byte[(int) fileToSend.length()]; doStream.write(bytesToSend, 0,
		 * bytesToSend.length); } }
		 */

		// diStream.close();
		/*
		 * doStream.close(); socket.close(); sSocket.close();
		 */

	}
}
