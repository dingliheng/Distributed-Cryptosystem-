package com.distsys.jun;

import java.io.*;
import java.net.*;
import java.util.*;

import static com.distsys.jun.Common.*;
/**
 * Created by Liheng on 2015/9/19.
 */
public class TicketProcessor {
    public String request;
    public String name;
    public int count;
    public TicketServer.RequestType type;
    public MessageTOclient messageTOclient = new MessageTOclient();
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
    public MessageTOclient execute(Seat seat) {
        Map<Integer, String> num_nameBefore = new HashMap<Integer, String>(seat.num_name);
//        num_nameBefore = seat.num_name;
        if(request.equals("reserve")){
            messageTOclient.message = seat.reserve(name, count);
            if(num_nameBefore.equals(seat.num_name)){
                messageTOclient.change = false;
            }else{
                messageTOclient.change = true;
            }
        }
        if(request.equals("search")){
            messageTOclient.message = seat.search(name);
            messageTOclient.change = true;
        }
        if(request.equals("delete")){
            messageTOclient.message = seat.delete(name);
            if(num_nameBefore.equals(seat.num_name)){
                messageTOclient.change = false;
            }else{
                messageTOclient.change = true;
            }
        }
        return messageTOclient;
    }
    public static class MessageTOclient{
        String message;
        boolean change;

        public MessageTOclient() {
            this.message = "";
            this.change = false;
        }
    }
    public static void main(String args[]){

    }
}
