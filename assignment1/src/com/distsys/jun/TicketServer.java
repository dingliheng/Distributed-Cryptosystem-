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
            if (args.length < 1){
                System.out.print("Need at least 1 argument");
                System.exit(-1);
            }
            TicketServer.serverIdx = Integer.parseInt(args[0]);
            System.out.print(portList.get(serverIdx));
            ServerSocket srvr = new ServerSocket(portList.get(serverIdx));

            serverClock = new LamportClock();
            MessageCreator message = new MessageCreator(serverClock);



            Socket clientSocket = srvr.accept();
            System.out.print("Server has connected!\n");
            OutputStream outputStream = clientSocket.getOutputStream();
            InputStream inputStream = clientSocket.getInputStream();

            Common.socketObjSend(message, outputStream);
            MessageCreator reconst = (MessageCreator) Common.socketObjReceive(inputStream);
            System.out.print(reconst.toString());

            outputStream.close();
            inputStream.close();
            srvr.close();

//            while (true) {
//                Socket clientSocket = srvr.accept();
//                System.out.print("Server has connected!\n");
//                OutputStream outputStream = clientSocket.getOutputStream();
//                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//                while (!in.ready()) {
//                }
//                String lineRead = in.readLine();
//                System.out.println(in.readLine()); // Read one line and output it
//                PrintWriter out = new PrintWriter(outputStream, true);
//                System.out.print("Sending string: '" + lineRead + "'\n");
//                out.print(lineRead);
//                in.close();
//                out.close();
//                clientSocket.close();
//            }
//
//                Runnable requestHandler = new RequestHandler(clientSocket);
//                new Thread(requestHandler).start();
            srvr.close();
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
