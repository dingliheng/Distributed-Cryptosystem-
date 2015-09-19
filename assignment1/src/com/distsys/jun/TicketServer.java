package com.distsys.jun;

import javax.naming.directory.SearchControls;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import static com.distsys.jun.Common.*;

public class TicketServer {

    private static int serverIdx;
    private static LamportClock serverClock;
    private static Seat seat;
    public static void main(String[] args) {
	// write your code here
        serverClock = new LamportClock();
        seat = new Seat(ticketNumber);
        try {
            File portFile = new File("port.txt");
            ArrayList<Integer> portList = ReadPortFile(portFile);
//            for (int x : portList){
//                System.out.print(""+x+"\n");
//            }
            if (args.length < 1) {
                System.out.print("Need at least 1 argument");
                System.exit(-1);
            }
            TicketServer.serverIdx = Integer.parseInt(args[0]);
            ServerSocket srvr = new ServerSocket(portList.get(serverIdx));
            System.out.println("Server "+serverIdx+" Port "+ portList.get(serverIdx) +" socket opened");

//            RequestCSMessage requestCSMessage = new RequestCSMessage(serverIdx, RequestType.READ);
//            MessageClosure message = new MessageClosure(serverClock, requestCSMessage);
//            Runnable serverSender = new ServerSender(serverIdx,portList);
//            new Thread(serverSender).start();
//
//            while (true) {
//                Socket clientSocket = srvr.accept();
////                System.out.print("Server has connected!\n");
//                OutputStream outputStream = clientSocket.getOutputStream();
//                InputStream inputStream = clientSocket.getInputStream();
//
//                socketObjSend(message, outputStream);
//                MessageClosure reconst = (MessageClosure) socketObjReceive(inputStream);
//                System.out.print("Server "+serverIdx+": "+reconst.toString());
//                Thread.sleep(2000);
////                outputStream.close();
////                inputStream.close();
////                srvr.close();
//
//                serverSender = new ServerSender(serverIdx,portList);
//                new Thread(serverSender).start();
//            }
            while (true) {
                Socket clientSocket = srvr.accept();
                System.out.print("\nServer has connected!\n");
                OutputStream outputStream = clientSocket.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while (!in.ready()) {}
                String lineRead = in.readLine();
                System.out.println(lineRead); // Read one line and output it
                PrintWriter out = new PrintWriter(outputStream, true);
                System.out.print("Sending string: '" + lineRead + "'\n");
                out.print(lineRead + "'\n");
                out.flush();
                in.close();
                out.close();
                clientSocket.close();
            }
                //Runnable requestHandler = new RequestHandler(clientSocket);
                //new Thread(requestHandler).start();
                //srvr.close();
        }
        catch(Exception e) {
            System.out.print(e.getClass().getName()+"\n"+e.getMessage()+"\n");
            e.printStackTrace(System.out);
        }
    }

    public static class LamportClock implements Serializable{
        private int clockValue;

        LamportClock(){
            clockValue = 0;
        }
        public void clockInc(){
            clockValue += 1;
        }
        public void clockInc(LamportClock lamportClock){
            this.clockValue = Math.max(this.clockValue, lamportClock.clockValue)+1;
        }

        public int getClockValue() {
            return clockValue;
        }
    }

    private enum RequestType{
        READ,
        WRITE
    }

    public static class RequestCSMessage implements Serializable{
        private int serverId;
        private RequestType type;

        public RequestCSMessage(int serverId, RequestType type) {
            this.serverId = serverId;
            this.type = type;
        }

        @Override
        public String toString() {
            return "RequestCSMessage("+serverId+","+type.name()+")\n";
        }
    }

    public static class Ackmessage implements Serializable{
        private int serverId;
        public Ackmessage(int serverId){
            this.serverId = serverId;
        }

        @Override
        public String toString() {
            return "Ack from server "+serverId+"\n";
        }
    }


    private static class ServerSender implements  Runnable{

        private int serverIdx;
        private ArrayList<Integer> portList;

        public ServerSender(int serverIdx,ArrayList<Integer> portList){
            this.serverIdx = serverIdx;
            this.portList = portList;
        }
        @Override
        public void run() {
            try {
                for (int port : portList) {
                    if (port!=portList.get(serverIdx)) {
                        Socket skt = new Socket("localhost", port); //random port
                        InputStream in = skt.getInputStream();
                        OutputStream out = skt.getOutputStream();
                        RequestCSMessage requestCSMessage = new RequestCSMessage(serverIdx, RequestType.WRITE);
                        MessageClosure message = new MessageClosure(serverClock, requestCSMessage);
                        socketObjSend(message, out);
                        serverClock.clockInc();
                        MessageClosure reconst = (MessageClosure) socketObjReceive(in);
                        System.out.print("server "+portList.indexOf(port)+": "+reconst.toString());
                        in.close();
                        out.close();
                        skt.close();
                    }
                }
            }
            catch(Exception e) {
                System.out.print(e.getClass().getName()+": "+e.getMessage()+"\n");
//                e.printStackTrace(System.out);
            }
        }
    }

    private static class RequestHandler implements Runnable{
        private final Socket clientSocket;
        private final String data = "Toobie ornaught toobie";
        PrintWriter out;
        public RequestHandler(Socket socket){
            clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                OutputStream outputStream = clientSocket.getOutputStream();
                BufferedReader in = new BufferedReader(new
                        InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(outputStream, true);
                while (!in.ready()) {}
                System.out.println(in.readLine()); // Read one line and output it
                System.out.print("Sending string: '" + data + "'\n");
                out.print(data);
                out.close();
                clientSocket.close();
            }
            catch(Exception e) {
                System.out.print(e.getClass().getName()+"\n"+e.getMessage()+"\n");
            }

        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }
    }




}
