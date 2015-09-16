package com.distsys.jun;

import java.io.*;
import java.net.*;

/**
 * Created by jpan on 9/16/15.
 */
public class Client {
    public static void main(String args[]) {
        try {
            Socket skt = new Socket("localhost", 1234);
            BufferedReader in = new BufferedReader(new
                    InputStreamReader(skt.getInputStream()));
            System.out.print("Received string: '");

            while (!in.ready()) {}
            System.out.println(in.readLine()); // Read one line and output it

            System.out.print("'\n");
            in.close();
        }
        catch(Exception e) {
            System.out.print("Whoops! It didn't work!\n");
        }
    }
}
