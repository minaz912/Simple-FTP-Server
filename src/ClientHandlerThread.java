import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandlerThread extends Thread {
	
	private static final String DEFAULT_PATH = "/home/minaz/ftpDir/";
	private final Socket socket;
	private String currentDir;
	private File dir;

	// private static final String[] SUPPORTED_CMDS = {"help", "list", "cd",
	// "download", "upload"};

	public ClientHandlerThread(Socket incomingSocket) {
		socket = incomingSocket;
		currentDir = DEFAULT_PATH;
		dir = new File(this.currentDir);
		if (!dir.exists()) {
			dir.mkdir();
		}
	}

	public void run() {
		try {
			
			DataOutputStream doStream = new DataOutputStream(
					socket.getOutputStream());
			DataInputStream diStream = new DataInputStream(
					socket.getInputStream());

			doStream.writeUTF("Connected to FTP Server\n");
			doStream.writeUTF("\nPlease enter command, enter \"help\" to list all commands, or \"q\" to quit");

			processCommand(dir, diStream, doStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processCommand(File dir, DataInputStream diStream,
			DataOutputStream doStream) throws IOException {

		String response = diStream.readUTF();

		switch (response) {
		case "help":
			doStream.writeUTF("list\t lists all files and folders in the current directory\n"
					+ "cd [x]\t changes the current working directory to [x]\n"
					+ "upload\t initiates upload process to server, you will be prompted to select which file to upload\n"
					+ "download\t ititiates file download process, you will be prompted to select which file to download");
			processCommand(dir, diStream, doStream);
			break;
		case "list":
			String fileList = "";

			for (String str : dir.list()) {
				fileList = fileList.concat(", ").concat(str);
			}
			doStream.writeUTF("Now Listing all files:\n" + fileList);
			processCommand(dir, diStream, doStream);
			break;
		case "cd":
			doStream.writeUTF("Enter directory:");
			String cdDest = diStream.readUTF();
			this.currentDir = DEFAULT_PATH + File.separatorChar + cdDest;
			dir = new File(this.currentDir);
			if (dir.exists()) {
				doStream.writeUTF("Now in directory: [ +" + this.currentDir + "]");
			} else {
				dir.mkdirs();
				doStream.writeUTF("Directory was not found! so it was created by system");
			}
			
			// TODO: Choose design & Implement
			processCommand(dir, diStream, doStream);
			break;
		case "upload":
			FileOutputStream fos = new FileOutputStream(currentDir + File.separatorChar + "file");
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			byte[] b = new byte[1024];
			int c;
			InputStream is = socket.getInputStream();
			while((c = is.read(b)) >= 0){
				fos.write(b, 0, c);
			}
			fos.close();
			processCommand(dir, diStream, doStream);
			/*
			 * PrintWriter pw = new PrintWriter(DEFAULT_PATH, "uploadedFile");
			 * FileOutputStream fos=new FileOutputStream("uploadedFile");
			 * DataOutputStream dops=new DataOutputStream(fos); boolean done =
			 * true; BufferedReader br =new BufferedReader(new
			 * InputStreamReader(socket.getInputStream())); while(done) { String
			 * fc = br.readLine(); if(fc==null) { done=false; } else {
			 * dops.writeChars(fc); } }
			 */
			break;
		case "download":
			doStream.writeUTF("Please enter file name to download");
			String fn = diStream.readUTF();
			File myFile = new File(currentDir + File.separatorChar + fn);

			if (!myFile.exists()) {
				doStream.writeUTF("Invalid file name, please try again");
				processCommand(dir, diStream, doStream);
			} else {
				doStream.writeUTF("Download request received, initiating download...");
				int count;
				byte[] buffer = new byte[1024];

				OutputStream os = socket.getOutputStream();
				BufferedInputStream bis = new BufferedInputStream(
						new FileInputStream(myFile));
				while ((count = bis.read(buffer)) >= 0) {
					os.write(buffer, 0, count);
					// out.flush();
				}
				bis.close();
			}
			// System.out.println(diStream.readUTF());
			// doStream.writeUTF(input.next());
			
			//
			processCommand(dir, diStream, doStream);
			break;
		case "q":
			doStream.writeUTF("Goodbye :)");
			socket.close();
			System.exit(0);
			break;
		default: // redundant
			doStream.writeUTF("Invalid command... please try again, or enter \"q\" to quit");
			processCommand(dir, diStream, doStream);
			break;
		}
	}
}
