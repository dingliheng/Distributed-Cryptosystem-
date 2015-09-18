package com.distsys.jun;
import java.util.*;
/**
 * Created by Liheng on 2015/9/18.
 */
public class seat {
    public Map<Integer, String> num_name = new HashMap<Integer, String>();
    public static int c;
    public int seats_reserved;
    public String message_to_client;
    public int name_seats_reserved;
    public ArrayList<Integer> seat_number = new ArrayList();
    public seat(int total){
        for(int i=1;i<=total;i++){
            c = total;
            num_name.put(i,"");
        }
    }
    public  String reserve(String name,int count) {
        seats_reserved = 0;
        for (int i = 1; i <= c; i++) {
            if (num_name.get(i) != "") {
                seats_reserved++;
            }
        }
        if (count > (c - seats_reserved)) {
            message_to_client = String.format("Failed:only %d seats left but %d seats are requested", c - seats_reserved, count);
            return message_to_client;
        }
        name_seats_reserved = 0;
        seat_number.clear();
        for (int i = 1; i <= c; i++) {
            if (num_name.get(i) == name) {
                name_seats_reserved++;
                seat_number.add(i);
            }
        }
        if (name_seats_reserved!=0) {
            message_to_client = String.format("Failed: "+name+" has booked the following seats:"+seat_number+".");
            return message_to_client;
        }
        for (int i = 1; i <= c; i++) {
            if(name_seats_reserved<count) {
                if (num_name.get(i) == "") {
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
    public static void main(String[] args) {
        seat a = new seat(10);
        System.out.println(a.num_name);
        System.out.println(a.reserve("bob", 20));
        System.out.println(a.reserve("bob", 5));
        System.out.println(a.num_name);
        System.out.println(a.reserve("bob", 5));
    }
}
