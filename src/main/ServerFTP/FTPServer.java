package ServerFTP;

import java.util.*;
import java.io.*;
import java.net.*;

/* The server program takes two command line parameters,
 * which are the port numbers where the server will wait on (one for normal commands and
 * another for the “terminate” command). The port for normal commands are henceforth
 * referred to as “nport” and the terminate port is referred to as “tport”. Once the myftpserver
 * program is invoked, it should create two threads. The first thread will create a socket on the
 * first port number and wait for incoming clients. The second thread will create a socket on the
 * second port and wait for incoming “terminate” commands.
 * When a client connects to the normal port, the server will spawn off a new thread to handle the
 * client commands. The operation of this thread is same as described in project 1. However,
 * when the serve gets a “get” or a “put” command, it will immediately send back a command ID
 * to the client. This is for the clients to use when they need to terminate a currently-running
 * command. Furthermore, the threads executing the “get” and “put” commands, will periodically
 * (after transferring 1000 bytes) check the status of the command to see if the client needs the
 * command to be terminated. If so, it will stop transferring data, delete any files that were
 * created and will be ready to execute more commands.
 * Note that there might be several normal threads executing concurrency. The server should
 * handle the consistency issues arising out of such concurrency. For example, if two put requests
 * arrive concurrently for the same file, one request should be completed before the other starts.
 * You should carefully design the server paying close attention to which commands need mutual
 * exclusion and which ones do not.
 * When a client connects to the “tport”, the server accepts the terminate command. The
 * command will identify (using the command-ID) which command needs to be terminated. It will
 * set the status of that command to “terminate” so that the thread executing that command will
 * notice it and gracefully terminate.
 */

class MyFTPServer{

    public static void main(String[] args) {
        try {
            Controller middle = new Controller();
            final Operator operator = new Operator(args[0], middle);
            final Terminator terminator = new Terminator(args[1], middle);
            operator.run();
            terminator.run();
        } catch(Exception error) {
            System.out.println(error);
        }//try-catch
    }//main

}//MyFTPServer

class Operator extends Thread{

    private int portNumber;
    private ServerSocket server = null;
    private Controller middle = null;

    public Operator(String port, Controller host){
        try {
            middle = host;
            portNumber = Integer.parseInt(port);
            server = new ServerSocket(portNumber);
            System.out.println("Operator started on port " + port);
        } catch (Exception e) {
            System.out.println(e);
        }
    }//constructor

    public void run(){
        try{
            while (true){
                if (middle.clientAvailable()) {
                    middle.addClient(server.accept());
                    System.out.println("New Client Connected");
                }//if
            }//while
        } catch (Exception e) {
            System.out.println(e);
        }//try-catch
    }//run
}//Operator

class Terminator extends Thread{

    private int tPortNumber;
    private Socket socket = null;
    private ServerSocket listener = null;
    private DataInputStream in = null;
    private Controller middle = null;
    private int input = -1;

    public Terminator(String tport, Controller host){
        try {
            middle = host;
            tPortNumber = Integer.parseInt(tport);
            listener = new ServerSocket(tPortNumber);
            System.out.println("Terminator started on port " + tport);
        } catch (Exception e) {
            System.out.println(e);
        }//try-catch
    }//constructor

    @Override
    public void run(){
        while(true){
            try {
                socket = listener.accept();
                in = new DataInputStream(socket.getInputStream());
                input = in.readInt();
                middle.terminateProcess(input);
            } catch (Exception e) {
                System.out.println(e);
            }//try-catch
        }//while
    }//run
}//Terminator

class Controller{

    private int activeClients = 0;
    private Server[] clients = new Server[5];
    private int[] termList = new int[10];
    private int iterator = 0;
    private int processID = 0;

    public Controller() {
        for (int i = 0; i < clients.length(); i++) {
            clients[i] = new Server(-1, this);
        }//for
    }//for

    void terminateProcess(int id){
        if (iterator >= 10) {
            iterator = 0;
        }//if
        termList[iterator] = id;
    }//terminateProcess

    int getPID(){
        processID++;
        return processID;
    }//getPID

    int[] getTerminates(){
        return termList;
    }//getTerminates

    Boolean clientAvailable(){
        for (int i = 0; i < clients.length(); i++) {
            if (clients[i].isNull()) {
                return true;
            } else if (!s.isAlive()) {
                return true;
            }//if-else
        }//for
        return false;
    }//getClients

    void addClient(Socket socket){
        for (int i = 0; i < clients.length(); i++) {
            if (clients[i].isNull()) {
                clients[i] = new Server(socket, this);
                break;
            } else if (!clients[i].isAlive()) {
                clients[i] = new Server(socket, this);
                break;
            }//if-else
        }//for
    }//addClient

}//controller

class Server extends Thread{

    private Boolean dummy = false;
    private Controller master = null;
    private Socket socket = null;
    private ServerSocket server = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private String currentDirectory = null;
    private String relativeDirectory = null;

    public Server(Socket port, Controller host){
        try{
            master = host;
            socket = port;

            if (socket == -1) {
                dummy = true;
            } else {
                //store starting directory to memory
                currentDirectory = System.getProperty("user.dir");
                relativeDirectory = "";

                //set up data input and output streams and initialize IO storage variable
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                run();
            }//if-else
        }catch (IOException e) {
            System.out.println(e);
        }//try-catch
    }//constructor

    public void run(){
        try {
            //process all inputs from the client
            String input = "";
            while(!input.equals("quit")){
                input = in.readUTF();
                System.out.println("Request recieved...");
                System.out.println(input);
                process(input);
            }//while

            //terminate socket on disconnect
            out.writeUTF("Confirm");
            System.out.println("Closing connection...");
            socket.close();
            in.close();
        } catch(IOException i) {
            System.out.println(i);
        }//try-catch
    }//run

    public Boolean isNull(){
        return dummy;
    }//isNull

    /*
    * Takes the input commands given in the input stream and redirects the argument to the method
    * associated with the given command.
    */
    private void process(String input){
        //parse the input into a command and argument
        int firstSpace = input.indexOf(" ");
        String command = "";
        String argument = "";
        if (firstSpace == -1) {
            command = input;
        } else {
            command = input.substring(0, firstSpace);
            argument = input.substring(firstSpace+1, input.length());
        }//if-else

        //redirect the argument to the appropriate command method
        if (command.equals("get")) {
            System.out.println("Retrieving file...");
            get(argument);
        } else if (command.equals("put")) {
            System.out.println("Preparing to recieve file...");
            put(argument);
        } else if (command.equals("delete")) {
            System.out.println("Preparing to delete file...");
            delete(argument);
        } else if (command.equals("ls")) {
            System.out.println("Preparing list of available files...");
            list();
        } else if (command.equals("cd")) {
            System.out.println("Changing directory...");
            changeDirectory(argument);
        } else if (command.equals("mkdir")) {
            System.out.println("Creating directory...");
            mkdir(argument);
        } else if (command.equals("pwd")) {
            System.out.println("Printing PWD...");
            pwd();
        }//if-else

    }//process

    /*
    * get (get <remote_filename>) -- Copy file with the name <remote_filename> from server directory
    * to client directory.
    */
    private void get(String filename){
        if (!relativeDirectory.equals("")) {
            filename = relativeDirectory + "/" + filename;
        }//if
        try {
            //opens the file
            File target = new File(filename);

            //check that the file exists
            if (target.exists() != true) {
                System.out.println("Error: file not found.");
                out.writeUTF("File not Found");

            //sends file if it exists
            } else {
                System.out.println("Sending file...");
                Boolean terminate = false;
                int id = master.getPID();
                out.writeInt(id);
                FileInputStream sendFile = new FileInputStream(target);
                out.writeLong(target.length());
                byte [] buf = new byte[4*1024];
                int bytes = 0;
                while((bytes = sendFile.read(buf)) != -1 && !terminate) {
                    out.write(buf,0,bytes);
                    out.flush();
                    for (int i : master.getTerminates()) {
                        if (id == i) {
                            terminate = true;
                        }//if
                    }//for
                }//while
                sendFile.close();
                System.out.println("File sent sucessfully...");
            }//if-else

        //if there is an error during sending, will print the error to the server comand line
        } catch(Exception e) {
            System.out.println(e);
        }//try-catch
    }//get

    /*
    * put (put <local_filename>) -- Copy file with the name <local_filename> from client directory to
    * server directory.
    */
    private void put(String filename){
        if (!relativeDirectory.equals("")) {
            filename = relativeDirectory + "/" + filename;
        }//if
        try {
            //check if the file name already exists
            File target = new File(filename);
            if (target.exists()) {
                System.out.println("Error: File already exists.");
                out.writeUTF("File Already Exists");

            //otherwise, receive the file contents and write to the file
            } else {
                System.out.println("Recieving file...");
                int id = master.getPID();
                out.writeInt(id);
                FileOutputStream recieveFile = new FileOutputStream(target);
                Boolean terminate = false;
                long size = in.readLong();
                byte [] buf = new byte[4*1024];
                int bytes = 0;
                while (size > 0 && (bytes = in.read(buf,0,(int)Math.min(buf.length,size))) != -1 && !terminate) {
                    recieveFile.write(buf,0,bytes);
                    size -= bytes;
                    for (int i : master.getTerminates()) {
                        if (id == i) {
                            terminate = true;
                        }//if
                    }//for
                }//while
                recieveFile.close();
                if (terminate) {
                    delete(filename);
                } else {
                    System.out.println("File recieved sucessfully...");
                }//if-else
            }//if-else

        //catch any errors and print to server command line
        } catch(Exception e) {
            System.out.println(e);
        }//try-catch
    }//put

    /*
    * delete (delete <remote_filename>)  Delete the file with the name <remote_filename> from the
    * server directory.
    */
    private void delete(String filename){
        if (!relativeDirectory.equals("")) {
            filename = relativeDirectory + "/" + filename;
        }//if
        File target = new File(filename);
        try {
            if (target.delete()) {
                System.out.println("Deleted the file sucessfully...");
                out.writeUTF("Deleted the file " + filename + " sucessfully.");
            } else {
                System.out.println("File deletion unsuccessful...");
                out.writeUTF("File deletion unsuccessful...");
            }//if-else
        //catch any errors and print the error to the server command line
        } catch(Exception e) {
            System.out.println(e);
        }//try-catch
    }//delete

    /*
    * ls (ls) -- List the files and subdirectories in the server directory
    */
    private void list(){
        try {
            File pwd = null;
            if (relativeDirectory.equals("")) {
                pwd = new File(currentDirectory);
            } else {
                pwd = new File(relativeDirectory);
            }//if-else
            String files [] = pwd.list();
            out.writeInt(files.length);
            out.writeUTF(currentDirectory);
            for (String file : files) {
                out.writeUTF(file);
            }//for
            System.out.println("List returned sucessfully...");
        } catch(Exception e) {
            System.out.println(e);
        }//try-catch
    }//list

    /*
    * cd (cd <remote_direcotry_name> or cd ..)  Change to the <remote_direcotry_name > on the server
    * or change to the parent directory of the current directory.
    */
    private void changeDirectory(String directory){
        //parse the current and given directories
        String [] parsed = directory.split("/");
        String [] pwd = currentDirectory.split("/");

        //create variables to store the changes to the current directory
        int delta = 0;
        String finalDirectory = "";

        //handle the case where the given directory is ..
        if (directory.equals("..")) {
            currentDirectory = "";
            for (int i = 0; i < pwd.length-1; i++) {
                currentDirectory = currentDirectory + "/" + pwd[i];
            }//for
            relativeDirectory = relativeDirectory + "../";

        //if the given directory contains more than one folder
        } else if (parsed.length != 1) {
            for (String s : parsed) {
                if (parsed[0].equals("..")) {
                    delta++;
                } else {
                    finalDirectory = finalDirectory + "/" + s;
                }//if-else
            }//for
            currentDirectory = pwd[0];
            for (int i = 1; i < pwd.length-delta; i++) {
                currentDirectory = currentDirectory + "/" + pwd[i];
            }//for
            currentDirectory = currentDirectory + finalDirectory;
            relativeDirectory = relativeDirectory + directory + "/";

        //handle the final case of a single folder given
        } else {
            currentDirectory = currentDirectory + "/" + directory;
            relativeDirectory = relativeDirectory + directory + "/";
        }//if-else
    }//cd

    /*
    * mkdir (mkdir <remote_directory_name>)  Create directory named <remote_direcotry_name> as the
    * sub-directory of the current working directory on the remote machine.
    */
    private void mkdir(String directory){
        File target = new File(currentDirectory + "/" + directory);
        if (target.mkdir()) {
            System.out.println("Directory creation sucessful...");
        } else {
            System.out.println("Failed to make new directory...");
        }//if-else
    }//mkdir

    /*
    * pwd (pwd)  Print the current working directory on the server.
    */
    private void pwd(){
        try {
            out.writeUTF(currentDirectory);
            System.out.println(currentDirectory);
        } catch(Exception e) {
            System.out.println(e);
        }//try-catch
    }//pwd
}//Server
