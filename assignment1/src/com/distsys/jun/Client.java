package com.distsys.jun;

import java.io.*;
import java.net.*;
import static com.distsys.jun.Common.*;

/**
 * Created by jpan on 9/17/15.
 */
//TODO encapsulate message to message closure
//TODO Create class to parse client command and perform seat operation


public class Client {
    public static void main(String args[]) {
        try {
            BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please import your request");
            String line; //input string from console
            line=sin.readLine();
            while(line!="over")    {  //if import 'over' break
                Socket skt = new Socket("localhost", 1234); //random port
                InputStream in = skt.getInputStream();
                OutputStream out = skt.getOutputStream();
//                PrintWriter os = new PrintWriter(skt.getOutputStream());
                MessageClosure clientmessage = new MessageClosure(line);
                socketObjSend(clientmessage, out);
//                os.println(line);
//                os.flush();
////                System.out.print("Received string:'");
//                while (!in.ready()) {}
//                System.out.println(in.readLine()); // Read one line and output it
                MessageClosure receivedmessage = (MessageClosure) socketObjReceive(in);

                if (receivedmessage.getMyType() != String.class){
                    System.out.println("Wrong message");
                }
                System.out.print(receivedmessage.toString());
                System.out.print("\n");
                System.out.println("Please import your request");
                line=sin.readLine();
                out.flush();
                in.close();
                out.close();
                skt.close();
            }
        } catch(Exception e) {
            System.out.print(e.getClass().getName()+"\n"+e.getMessage()+"\n");
            e.printStackTrace(System.out);
        }


    }

}
