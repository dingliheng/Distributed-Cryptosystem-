package com.distsys.jun;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import static com.distsys.jun.Common.*;

/**
 * Created by jpan on 9/17/15.
 */
//TODO encapsulate message to message closure
//TODO Create class to parse client command and perform seat operation

public class Client {
    public static void main(String args[]) {
        try{
            BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please import your request");
            String line; //input string from console
            line=sin.readLine();
            String request;
            boolean connected; //when connected successfully, true; if nor, false
            File portFile = new File("port.txt");
            ArrayList<Integer> portList = ReadPortFile(portFile);
            int random_port = 0;
            while (true) {
                try {
                    while(line!="over") {  //if import 'over' break
                        StringTokenizer strT1 = new StringTokenizer(line, " ");
                        int count = strT1.countTokens(); // the number of elements in clientrequest
                        request = strT1.nextToken();
                        if ((!request.equals("reserve")) && (!request.equals("delete")) && (!request.equals("search"))) {
                            System.out.println("illegal import,Please import again");
                            line = sin.readLine();
                            continue;
                        }
                        int n = portList.size();
                        if (n==0){
                            portList = ReadPortFile(portFile);
                            n = portList.size();
                        }
                        random_port = new Random().nextInt(n);
                        System.out.println("try to connect to server: "+ random_port);
                        long startMili=System.currentTimeMillis();
                        while((System.currentTimeMillis()-startMili)<5000){
                            try{
                                Socket skt = new Socket("localhost", portList.get(random_port));
                                System.out.println("connect successfully");
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
                                out.flush();
                                in.close();
                                out.close();
                                skt.close();
                                System.out.println("Please import your request");
                                line=sin.readLine();
                                connected = true;
                                break;
                            }catch (Exception e){
                                connected = false;
//                                System.out.println("sorry,try to connect again");
                                continue;
                            }
                        }
//                    Socket skt = new Socket("localhost", 1234); //random port}
                    }
                } catch(Exception e) {
                    System.out.print(e.getClass().getName() + "\n" + e.getMessage() + "\n");
                    e.printStackTrace(System.out);
                    System.out.println("Woops, the server is not online");
                    portList.remove(random_port);
                }
            }
        }catch (Exception e){
            System.out.println("Woops, the clinet is not online");
        }


    }

}