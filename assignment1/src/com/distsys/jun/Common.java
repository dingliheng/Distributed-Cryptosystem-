package com.distsys.jun;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpan on 9/16/15.
 */
public class Common {
    public static List<Integer> ReadPortFile(File fin) throws IOException {
        // Construct BufferedReader from FileReader
        List<Integer> portArray = new ArrayList<Integer>();
        BufferedReader br = new BufferedReader(new FileReader(fin));
        String line;
        while ((line = br.readLine()) != null) {
            portArray.add(Integer.parseInt(line.trim()));
//            System.out.println(line);
        }
        br.close();
        return portArray;
    }
}
