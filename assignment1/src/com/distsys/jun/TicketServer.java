package com.distsys.jun;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TicketServer {

    public static void main(String[] args) {
	// write your code here

        try {
            File portFile = new File("port.txt");
            List<Integer> portList = Common.ReadPortFile(portFile);
            for (int x : portList){
                System.out.print(""+x+"\n");
            }
            ServerSocket srvr = new ServerSocket(1234);
            while(true) {
                Socket clientSocket = srvr.accept();
                System.out.print("Server has connected!\n");
                Runnable requestHandler = new RequestHandler(clientSocket);
                new Thread(requestHandler).start();
//                out.close();
//                skt.close();

            }
//            srvr.close();
        }
        catch(Exception e) {
            System.out.print(e.getClass().getName()+"\n"+e.getMessage()+"\n");
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
                out = new PrintWriter(outputStream, true);
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
