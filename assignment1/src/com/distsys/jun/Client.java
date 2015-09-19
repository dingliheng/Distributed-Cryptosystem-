package com.distsys.jun;

        import java.io.*;
        import java.net.*;

/**
 * Created by jpan on 9/17/15.
 */
public class Client {
    public static void main(String args[]) {
        try {
            BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please import your request");
            String line = null; //input string from console
            line=sin.readLine();
            while(line!="over")    {  //if import 'over' break
                Socket skt = new Socket("localhost", 1234); //random port
                BufferedReader in = new BufferedReader(new
                        InputStreamReader(skt.getInputStream()));
                PrintWriter os = new PrintWriter(skt.getOutputStream());
                os.println(line);
                os.flush();
                System.out.print("Received string:'");
                while (!in.ready()) {}
                System.out.println(in.readLine()); // Read one line and output it
                System.out.print("\n");
                System.out.println("Please import your request");
                line=sin.readLine();
                in.close();
                os.close();
                skt.close();
            }
        } catch(Exception e) {
            System.out.print("Whoops! It didn't work!\n");
        }


    }

}
