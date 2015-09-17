package com.distsys.jun;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TicketServer {

    public static void main(String[] args) {
	// write your code here
        String data = "Toobie ornaught toobie";
        try {
            File portFile = new File("port.txt");
            List<Integer> portList = Common.ReadPortFile(portFile);
            for (int x : portList){
                System.out.print(""+x+"\n");
            }
            ServerSocket srvr = new ServerSocket(1234);
            Socket skt = srvr.accept();
            System.out.print("Server has connected!\n");
            PrintWriter out = new PrintWriter(skt.getOutputStream(), true);
            System.out.print("Sending string: '" + data + "'\n");
            out.print(data);
            out.close();
            skt.close();
            srvr.close();
        }
        catch(Exception e) {
            System.out.print(e.getClass().getName()+"\n"+e.getMessage());
        }
    }
}
