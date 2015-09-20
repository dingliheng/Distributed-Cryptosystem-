package com.distsys.jun;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import static com.distsys.jun.Common.*;
/**
 * Created by Liheng on 2015/9/19.
 */
public class TicketProcessor {
    public String request;
    public String name;
    public int count;
    public TicketServer.RequestType type;
    public TicketProcessor(String clientrequest) {
        StringTokenizer strT1 = new StringTokenizer(clientrequest," ");
        int n = strT1.countTokens(); // the number of elements in clientrequest
        request = strT1.nextToken();
        name = strT1.nextToken();
        if(n>2){
            count = Integer.parseInt(strT1.nextToken());
        }
        if (request.equals("reserve")||request.equals("delete")){
            type = TicketServer.RequestType.WRITE;
        }
        if (request.equals("search")){
            type = TicketServer.RequestType.READ;
        }
    }
    public void execute(Seat seat) {
        if(request.equals("reserve")){
            seat.reserve(name,count);
        }
        if(request.equals("search")){
            seat.search(name);
        }
        if(request.equals("delete")){
            seat.delete(name);
        }
    }
    public static void main(String args[]){
        TicketProcessor a = new TicketProcessor("asdf sdf 5");
        System.out.println(a.count);
        System.out.println(a.name);
        System.out.println(a.request);
    }
}
