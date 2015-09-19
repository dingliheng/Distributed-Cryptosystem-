package com.distsys.jun;
import java.util.*;
/**
 * Created by Liheng on 2015/9/18.
 */
public class Seat {
    public Map<Integer, String> num_name = new HashMap<Integer, String>();
    public int c;
    public int seats_reserved;
    public String message_to_client;
    public int name_seats_reserved;
    public ArrayList<Integer> seat_number = new ArrayList();

    public Seat(int total){
        for(int i=0;i<total;i++){
            c = total;
            num_name.put(i,"");
        }
    }


    public synchronized String reserve(String name,int count) {
        seats_reserved = 0;
        for (int i = 0; i < c; i++) {
            if (num_name.get(i) != "") {
                seats_reserved++;
            }
        }
        if (count > (c - seats_reserved)) {
            message_to_client = String.format("Failed:only %d seats left but %d seats are requested.", c - seats_reserved, count);
            return message_to_client;
        }
        name_seats_reserved = 0;
        seat_number.clear();
        for (int i = 0; i < c; i++) {
            if (num_name.get(i) == name) {
                name_seats_reserved++;
                seat_number.add(i);
            }
        }
        if (name_seats_reserved!=0) {
            message_to_client = String.format("Failed: "+name+" has booked the following seats:"+seat_number+".");
            return message_to_client;
        }
        for (int i = 0; i < c; i++) {
            if(name_seats_reserved<count) {
                if (num_name.get(i) == "") {
                    num_name.put(i,name);
                    name_seats_reserved++;
                    seat_number.add(i);
                }
            }else{
                break;
            }
        }
        message_to_client = String.format("The seats have been reversed for "+name+":"+seat_number+".");
        return message_to_client;
    }

    public synchronized String search(String name){
        name_seats_reserved = 0;
        seat_number.clear();
        for (int i = 0; i < c; i++) {
            if (num_name.get(i) == name) {
                name_seats_reserved++;
                seat_number.add(i);
            }
        }
        if(name_seats_reserved==0){
            message_to_client = "Failed: no reservation is made by "+name;
            return  message_to_client;
        }else{
            message_to_client = String.format(""+seat_number);
            return  message_to_client;
        }
    }

    public synchronized String delete(String name){
        name_seats_reserved = 0;
        seats_reserved = 0;
        for (int i = 0; i < c; i++) {
            if(num_name.get(i) != ""){
                seats_reserved++;
            }
            if (num_name.get(i) == name) {
                name_seats_reserved++;
                num_name.put(i,"");
            }
        }
        if(name_seats_reserved == 0){
            message_to_client = "Failed: no reservation is made by "+name;
            return message_to_client;
        }else {
            message_to_client = String.format("%d seats have been releasd. %d seats are now available",name_seats_reserved,c-seats_reserved);
            return  message_to_client;
        }
    }
    
    public static void main(String[] args) {
        Seat a = new Seat(15);
        System.out.println(a.num_name);
        System.out.println(a.reserve("bob", 20));
        System.out.println(a.num_name);
        System.out.println(a.reserve("bob", 5));
        System.out.println(a.num_name);
        System.out.println(a.reserve("bob", 5));
        System.out.println(a.num_name);
        System.out.println(a.search("bob"));
        System.out.println(a.num_name);
        System.out.println(a.search("Amy"));
        System.out.println(a.num_name);
        System.out.println(a.delete("bob"));
        System.out.println(a.num_name);
        System.out.println(a.delete("Amy"));
        System.out.println(a.num_name);
        System.out.println(a.reserve("candy", 1));
        System.out.println(a.reserve("bob", 3));
        System.out.println(a.delete("candy"));
        System.out.println(a.reserve("amy", 2));
        System.out.println(a.num_name);
    }

}
