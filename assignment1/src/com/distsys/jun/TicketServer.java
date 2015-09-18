package com.distsys.jun;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TicketServer {

    private static int serverIdx;
    private static LamportClock serverClock;
    public static void main(String[] args) {
	// write your code here
        Map<Integer, String> seatMap = new HashMap<Integer, String>();
            try {
                File portFile = new File("port.txt");
                ArrayList<Integer> portList = Common.ReadPortFile(portFile);
//            for (int x : portList){
//                System.out.print(""+x+"\n");
//            }
                if (args.length < 1) {
                    System.out.print("Need at least 1 argument");
                    System.exit(-1);
                }
                TicketServer.serverIdx = Integer.parseInt(args[0]);
                System.out.println(portList.get(serverIdx));
                ServerSocket srvr = new ServerSocket(portList.get(serverIdx));

                serverClock = new LamportClock();
            /*MessageCreator message = new MessageCreator(serverClock);
            Runnable serverSender = new ServerSender(serverIdx,portList);
            new Thread(serverSender).start();

            while (true) {
                Socket clientSocket = srvr.accept();
                System.out.print("Server has connected!\n");
                OutputStream outputStream = clientSocket.getOutputStream();
                InputStream inputStream = clientSocket.getInputStream();

                Common.socketObjSend(message, outputStream);
                MessageCreator reconst = (MessageCreator) Common.socketObjReceive(inputStream);
                System.out.print("Server "+serverIdx+": "+reconst.toString());
                Thread.sleep(2000);
//                outputStream.close();
//                inputStream.close();
//                srvr.close();

                serverSender = new ServerSender(serverIdx,portList);
                new Thread(serverSender).start();
            }*/


            Socket clientSocket = srvr.accept();
            System.out.print("Server has connected!\n\n");
            OutputStream outputStream = clientSocket.getOutputStream();
            BufferedReader in=new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(outputStream, true);
            String lineRead;
            while (true) {
                while (!in.ready()) {}
                lineRead = in.readLine();
                System.out.println("Client:" + lineRead); // Read one line and output it
                System.out.print("Sending string: '" + lineRead + "'\n\n");
                //in.close();
                //out.close();
                //clientSocket.close();
                out.print(lineRead+"'\n");
                out.flush();
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

    public static class MessageCreator implements Serializable{
        LamportClock timestamp;
        MessageCreator(LamportClock lamportClock){
            this.timestamp = lamportClock;
        }

        public String toString(){
            String outstr = "Clock is "+timestamp.getClockValue()+"\n";
            return outstr;
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
                        MessageCreator message = new MessageCreator(serverClock);
                        Common.socketObjSend(message, out);
                        serverClock.clockInc();
                        MessageCreator reconst = (MessageCreator) Common.socketObjReceive(in);
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
