import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Date;
import java.text.SimpleDateFormat;

public class ClientHandlerThread extends Thread {

	//private static final String DEFAULT_PATH = "~/ftpDir/"; //enable this for linux systems
	private static final String DEFAULT_PATH = "/home/minaz/Server";/*default server path,
	as a security measure, clients will only see inside this path*/
	private final Socket socket; //incoming socket
	private String currentDir;
	private File dir; //file object of current directory

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
			socket.close();
		} catch (EOFException e) {
			System.out.println("Client Disconnected");
			//System.exit(0);
			//e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param dir file object of current directory
	 * @param diStream Data Input Stream of this thread socket
	 * @param doStream Data Output Stream of this thread socket
	 */
	private synchronized void processCommand(File dir, DataInputStream diStream,
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
				fileList = fileList + "\n" + str + "\t";
				fileList = listFileAttributes(fileList, str);
				
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
				doStream.writeUTF("Now in directory: [ +" + this.currentDir
						+ "]");
			} else {
				dir.mkdirs();
				doStream.writeUTF("Directory was not found! so it was created by system");
			}

			processCommand(dir, diStream, doStream);
			break;
		case "upload":
			doStream.writeUTF("Enter file name");///
			String fn = diStream.readUTF();
			if (fn.equals("File does not exist!")) {
				processCommand(dir, diStream, doStream);
				break;
			}
			FileOutputStream fos = new FileOutputStream(currentDir
					+ File.separatorChar + fn);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			byte[] b = new byte[1024];
			int c;
			InputStream is = socket.getInputStream();
			while ((c = is.read(b)) >= 0) {
				bos.write(b, 0, c);
				if (c < 1024) {
					bos.flush();
					break;
				}
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
			String fName = diStream.readUTF();
			File myFile = new File(currentDir + File.separatorChar + fName);

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
		case "mv":
			doStream.writeUTF("Enter file name to move:");
			String resp = diStream.readUTF();
			File temp = new File(currentDir + File.separatorChar + resp);
			if (temp.exists()) {
				doStream.writeUTF("Enter subdirectory to move to: (ex. Mina/subdir)");
				resp = diStream.readUTF();
				File tempDir = new File(currentDir + File.separatorChar + resp);
				if (tempDir.exists()) {
					temp.renameTo(new File(tempDir.getPath() + File.separatorChar + temp.getName()));
				} else {
					tempDir.mkdir();
					temp.renameTo(new File(tempDir.getPath() + File.separatorChar + temp.getName()));
				}
				doStream.writeUTF("Move operation successful!");
			} else {
				doStream.writeUTF("File does not exist.. Please try again!");
				processCommand(dir, diStream, doStream);
			}
			break;
		case "q":
			doStream.writeUTF("Goodbye :)");
			socket.close();
			System.exit(0);
			break;
		default: // redundant
			doStream.writeUTF("Invalid command.. please try again, or enter \"q\" to quit");
			processCommand(dir, diStream, doStream);
			break;
		}
	}

	/**
	 * @param fileList a string that contains the current files listed so far
	 * @param str temporary variable that contains current file name
	 * @return the first parameter, concatenated with its file object attributes
	 */
	private String listFileAttributes(String fileList, String str) {
		File currentFile = new File(currentDir + File.separatorChar + str);
		if (currentFile.isDirectory()) {
			fileList = fileList + " (Folder)";
		} else if (currentFile.isFile()) {
			fileList = fileList + " (File)";
		}
		if (currentFile.canRead()) {
			if (currentFile.canWrite()) {
				fileList = fileList + " Writable";
			} else {
				fileList = fileList + " Read-only";
			}
			
		} else {
			fileList = fileList + " Unreadable";
		}
		if (currentFile.isHidden()) {
			fileList = fileList + " Hidden";
		} else {
			fileList = fileList + " Visible";
		}
		String lastModified = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss").format(
			    new Date(currentFile.lastModified()) 
			);
		fileList = fileList + " Last Modified: " + lastModified;
		return fileList;
	}
}
