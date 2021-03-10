package ClientFTP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

/*
 * The ftp client program will take two command line parameters the machine name
 * where the server resides and the port number. Once the client starts up, it will display a prompt
 * “mytftp>”. It should then accept and execute commands by relaying the commands to the
 * server and displaying the results and error messages where appropriate. The client should
 *terminate when the user enters the “quit” command.
 */

public class FTPClient {

		public static void main (String [] args) {
			String port = args[1];
			String tPort = args[2];
			int tPortNumber = Integer.parseInt(tPort);
			int portNumber = Integer.parseInt(port);
			try {
				InetAddress server = InetAddress.getByName(args[0]);
				//create Client object
				Client client = new Client(server, portNumber, tPortNumber);
				String command = "";
				Scanner scan = new Scanner(System.in);

				/*
				 * give a stream of inputs to the server, processes each input through the client
				 * using the methods associated with each command.
				*/
				while (true) {
					System.out.print("myftp> ");

					//parse the input into a command and argument
					command = scan.nextLine();
					String [] split = command.split(" ", 2);

					//redirect the argument to the appropriate method
					if (split[0].compareTo("get") == 0) {
						if (split.length() > 2 && split[2].compareTo("&") == 0)
							client.threadedGet(split[1]);
						else
							client.get(split[1]);
					}
					else if (split[0].compareTo("put") == 0) {
						if (split.length() > 2 && split[2].compareTo("&") == 0)
							client.threadedPut(split[1]);
						else
							client.put(split[1]);
					}
					else if (split[0].compareTo("delete") == 0) {
						client.delete(split[1]);
					}
					else if (split[0].compareTo("ls") == 0) {
						client.listFiles();
					}
					else if (split[0].compareTo("cd") == 0) {
						client.cd(split[1]);
					}
					else if (split[0].compareTo("mkdir") == 0) {
						client.makeDirectory(split[1]);
					}
					else if (split[0].compareTo("pwd") == 0) {
						client.pwd();
					}
					else if (split[0].compareTo("terminate") == 0){
						client.terminate(split[1]);
					}
					else if (split[0].compareTo("quit") == 0) {
						client.quit();
						break;
					}
				}//while
			}//try
			catch (Exception e) {
                System.out.println("Error in FTPClient.main()");
				System.out.println(e);
			}//catch
		}//main
}//FTPClient


class Client{

	Socket soc;
	Socket terminateSoc;
	DataInputStream input;
	DataOutputStream out;
	BufferedReader buf;
	DataOutputStream terminateOut;

	Client(InetAddress server, int port, int tport){
		try {
			//sets up the socket between the server and the client
			terminateSoc = new Socket(server, tport);
			soc = new Socket(server, port);
			System.out.println("Connected");
			//set up data input and output streams
			input = new DataInputStream(soc.getInputStream());
			out = new DataOutputStream(soc.getOutputStream());
			terminateOut = new DataOutputStream(terminateSoc.getOutputStream());
			buf = new BufferedReader(new InputStreamReader(System.in));
		}//try
		catch (Exception e) {
            System.out.println("Error in Client constructor");
			System.out.println(e);
		}//catch
	}//constructor




	void pwd() {
		try {
			out.writeUTF("pwd");
			String directory = input.readUTF();
			System.out.println("Current Directory: " + directory);
		}//try
		catch (Exception e) {
            System.out.println("Error in Client.pwd()");
            System.out.println(e);
        }
	}//pwd

	void cd(String directory){
		try{
			out.writeUTF("cd " + directory);
		}//try
		catch (Exception e){
            System.out.println("Error in Client.cd()");
			System.out.println(e);
		}//catch
	}//cd

	void quit() {
		try {
			out.writeUTF("quit");
			input.close();
			out.close();
			soc.close();
		}//try
		catch (Exception e) {
            System.out.println("Error in Client.quit()");
            System.out.println(e);
        }
	}//quit

	void makeDirectory(String directory) {
		System.out.println("Creating remote Directory");
		try {
		out.writeUTF("mkdir " + directory);
		}//try
		catch (Exception e) {
            System.out.println("Error in Client.makeDirectory()");
            System.out.println(e);
        }
	}//makeDirectory

	void delete(String file) {
		try {
			out.writeUTF("delete " + file);
			String message = input.readUTF();
			System.out.println(message);
		}//try
		catch (Exception e) {
            System.out.println("Error in Client.delete()");
            System.out.println(e);
        }
	}//delete

	void listFiles() {
		String path = "";
		try {
			out.writeUTF("ls");
			int count = 0;
			count = input.readInt();
			path = input.readUTF();
			System.out.println(count);
			File [] fileAry = new File[count];
			for (int i=0; i<count;i++) {
				String name = input.readUTF();
				fileAry[i] = new File(path + "/" + name);
				System.out.println(fileAry[i]);
			}//for
		}//try
		catch (Exception e) {
            System.out.println("Error in Client.listFiles()");
            System.out.println(e);
        }
	}//listFiles

	void get (String file) {
		try {
			//writes command to server
			out.writeUTF("get " + file);

			//gets server message
			String message = input.readUTF();

			//receives of the file is not found
			if (message.compareTo("File not Found") == 0) {
				System.out.println("File Not Found");
				return;
			}
			//reads file if it exists
			else if (message.compareTo("Ready") == 0) {
				System.out.println("Recieving File ...");
				File f = new File(file);
				FileOutputStream fileOutput = new FileOutputStream(f);
				long size = input.readLong();
				byte [] buf = new byte[4*1024];
				int bytes = 0;
				while (size > 0 && (bytes = input.read(buf,0,(int)Math.min(buf.length,size))) != -1) {
					fileOutput.write(buf,0,bytes);
					size -= bytes;
				}
				fileOutput.close();
				System.out.println(input.readUTF());
			}//if-else
		}//try
		catch(Exception e) {
            System.out.println("Error in Client.get()");
            System.out.println(e);
        }
	}

	void threadedGet(String file) {
		final Thread inThread = new Thread() {
			@Override
			public void run() {
				try {
					//writes command to server
					out.writeUTF("get " + file);

					//gets server message
					String message = input.readUTF();

					//receives of the file is not found
					if (message.compareTo("File not Found") == 0) {
						System.out.println("File Not Found");
						return;
					}
					//reads file if it exists
					else if (message.compareTo("Ready") == 0) {
						System.out.println("Recieving File ...");
						File f = new File(file);
						FileOutputStream fileOutput = new FileOutputStream(f);
						long size = input.readLong();
						byte [] buf = new byte[4*1024];
						int bytes = 0;
						while (size > 0 && (bytes = input.read(buf,0,(int)Math.min(buf.length,size))) != -1) {
							fileOutput.write(buf,0,bytes);
							size -= bytes;
						}
						fileOutput.close();
						System.out.println(input.readUTF());
						System.out.println(input.readUTF());
					}//if-else
				}//try
				catch(Exception e) {
                    System.out.println("Error in Client.threadedGet().inThread");
                    System.out.println(e);
                }
			}
		};
		inThread.start();
	}//get

	void put(String file) {
		File f = new File(file);
		try {
			//gives command to server
			out.writeUTF("put " + file);

			//gets server message
			String serverMessage = input.readUTF();

			//checks if file already exists
			if (serverMessage.compareTo("File Already Exists") == 0) {
				System.out.println(serverMessage);
			}
			//sends file to the server
			else if (serverMessage.compareTo("Ready") == 0) {
				System.out.println("Sending File ...");

				FileInputStream fileInput = new FileInputStream(f);
				out.writeLong(f.length());
				byte [] buf = new byte[4*1024];
				int bytes = 0;
				while ((bytes = fileInput.read(buf))!= -1) {
					out.write(buf,0,bytes);
					out.flush();
				}
				fileInput.close();
				//System.out.println(input.readUTF());
			}
		}//try
		catch (Exception e) {
            System.out.println("Error in Client.put()");
            System.out.println(e);
        }
	}

	void threadedPut(String file) {
		final Thread outThread = new Thread() {
			@Override
			public void run() {
				File f = new File(file);
				try {
					//gives command to server
					out.writeUTF("put " + file);

					//gets server message
					String serverMessage = input.readUTF();

					//checks if file already exists
					if (serverMessage.compareTo("File Already Exists") == 0) {
						System.out.println(serverMessage);
					}
					//sends file to the server
					else if (serverMessage.compareTo("Ready") == 0) {
						System.out.println("Sending File ...");

						FileInputStream fileInput = new FileInputStream(f);
						out.writeLong(f.length());
						byte [] buf = new byte[4*1024];
						int bytes = 0;
						while ((bytes = fileInput.read(buf))!= -1) {
							out.write(buf,0,bytes);
							out.flush();
						}
						fileInput.close();
						System.out.println(input.readUTF());
					}
				}//try
				catch (Exception e) {
                    System.out.println("Error in Client.threadedPut().outThread");
                    System.out.println(e);
                }
			}
		};
		outThread.start();
	}//put

	void terminate(String commandID) {
		try {
            int id = Integer.parseInt(commandID);
			terminateOut.writeInt(id);
		}catch (Exception e) {
            System.out.println("Error in Client.terminate()");
            System.out.println(e);
        }
	}
}//Client
