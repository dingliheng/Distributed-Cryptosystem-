package com.distributedsys; /**
 * Created by Liheng on 2015/11/14.
 */
import java.math.BigInteger;
import java.io.*;
import java.util.Arrays;

class RsaKeyDecryption {
    private BigInteger n, d;
    public static void main(String[] a) {
//        if (a.length<3) {
//            System.out.println("Usage:");
//            System.out.println("java com.distributedsys.RsaKeyDecryption key input output");
//            return;
//        }
//        String keyFile = a[0];
//        String input = a[1];
//        String output = a[2];
        String interput;
        RsaKeyDecryption decryptor3 = new RsaKeyDecryption(3);
        interput = decryptor3.initdecrypt("output.txt");  // "initdencrypt" method
        RsaKeyDecryption decryptor5 = new RsaKeyDecryption(5);
        interput = decryptor5.interdecrypt(interput);    // "interdencrypt" method
        System.out.println("intervalue: "+ interput);
        RsaKeyDecryption decryptor4 = new RsaKeyDecryption(4);
        interput = decryptor4.interdecrypt(interput);
        RsaKeyDecryption decryptor6 = new RsaKeyDecryption(6);
        interput = decryptor6.interdecrypt(interput);
        RsaKeyDecryption decryptor1 = new RsaKeyDecryption(1);
        interput = decryptor1.interdecrypt(interput);
        RsaKeyDecryption decryptor2 = new RsaKeyDecryption(2);
        interput =  decryptor2.interdecrypt(interput);
        RsaKeyDecryption decryptor = new RsaKeyDecryption(0);
//        decryptor.findecrypt(interput, "decrypt.txt");    // "findencrypt" method
        interput =  decryptor.interdecrypt(interput);
        decryptor.finalwritetofile(interput, "decrypt.txt");
        System.out.println("intervalue: " + interput);

        RsaKeyEncryption encryptor6 = new RsaKeyEncryption(0); //create an encryptor which uses publick key6
        interput = encryptor6.interencrypt(interput); //the first encryption uses the "initencrypt" method
        RsaKeyEncryption encryptor1 = new RsaKeyEncryption(5);
        interput =encryptor1.interencrypt(interput);  //the intermediate encryptions use the "interencrypt" method
        RsaKeyEncryption encryptor2 = new RsaKeyEncryption(6);
        interput =encryptor2.interencrypt(interput);
        RsaKeyEncryption encryptor3 = new RsaKeyEncryption(1);
        interput =encryptor3.interencrypt(interput);
        RsaKeyEncryption encryptor4 = new RsaKeyEncryption(2);
        interput =encryptor4.interencrypt(interput);
        RsaKeyEncryption encryptor5 = new RsaKeyEncryption(3);
        interput =encryptor5.interencrypt(interput);
        RsaKeyEncryption encryptor0 = new RsaKeyEncryption(4);
        interput =encryptor0.interencrypt(interput);

//        try {
//            BufferedReader br=new BufferedReader(new FileReader("initBiginter.txt"));
//            String line="";
//            StringBuffer  buffer = new StringBuffer();
//            while((line=br.readLine())!=null){
//                buffer.append(line+"\n");
//            }
//            String fileContent = buffer.toString();
//            interput = fileContent;
//        }catch (Exception ex) {
//            ex.printStackTrace();
//        }
        System.out.println("the result of verification :" + decryptor6.verifydecrypt("output.txt", interput));
    }

    // Reading in RSA private key
    RsaKeyDecryption(int keyNum) {
        try {
            BufferedReader in = new BufferedReader(new FileReader("RSAkeys.txt"));
            String line = in.readLine();
            while (line!=null) {
                if (line.indexOf("Modulus: ")>=0) {
                    n = new BigInteger(line.substring(9));
                }
                if (line.indexOf("Private key"+keyNum+": ")>=0) {
                    d = new BigInteger(line.substring(14));
                }
                line = in.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("--- Reading private key ---");
        System.out.println("Modulus: "+n);
        System.out.println("Key size: "+n.bitLength());
        System.out.println("Private key" + keyNum + ": " + d);
    }

    // Decrypting cipher text
    public String initdecrypt(String intput) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        System.out.println("Cleartext block size: "+clearTextSize);
        System.out.println("Ciphertext block size: "+cipherTextSize);
        try {
            FileInputStream fis = new FileInputStream(intput);
            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream("initBiginter.txt"),"utf-8");
            StringWriter outString = new StringWriter();
            byte[] clearTextBlock = new byte[clearTextSize];
            byte[] cipherTextBlock = new byte[cipherTextSize];
            long blocks = 0;
            int dataSize = 0;
            while (fis.read(cipherTextBlock)>0) {
                blocks++;
                BigInteger cipherText = new BigInteger(1,cipherTextBlock);
                fos.write(cipherText.toString() + "\n");
                BigInteger clearText = cipherText.modPow(d,n);
                System.out.println(clearText);
//                byte[] clearTextData = clearText.toByteArray();
//                putBytesBlock(clearTextBlock,clearTextData);
                dataSize = clearTextSize;
                if (fis.available()==0) {
                    dataSize = getDataSize(clearTextBlock);
                }
                if (dataSize>=0) {
                    outString.write(clearText.toString()+"\n");
                }
            }
            fis.close();
            outString.close();
            fos.close();
            System.out.println("Decryption block count: " + blocks);
            return outString.toString().substring(0, outString.toString().length()-1);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // Putting bytes data into a block
    public static void putBytesBlock(byte[] block, byte[] data) {
        int bSize = block.length;
        int dSize = data.length;
        int i = 0;
        while (i<dSize && i<bSize) {
            block[bSize-i-1] = data[dSize-i-1];
            i++;
        }
        while (i<bSize) {
            block[bSize-i-1] = (byte)0x00;
            i++;
        }
    }

    // Getting data size from a padded block
    public static int getDataSize(byte[] block) {
        int bSize = block.length;
        int padValue = block[bSize-1];
        return (bSize-padValue)%bSize;
    }

    public String interdecrypt(String string) {
        String output = "";
        try {
            String[] in = string.split("\n");
            int k=0;
            while (k<in.length) {
                BigInteger ciperText = new BigInteger(in[k]);
                BigInteger clearText = ciperText.modPow(d, n);
                output = output + clearText + "\n";
                k++;
//                OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(file),"utf-8");

            }
            return output.substring(0, output.toString().length() - 1);
            }catch(Exception ex){
                ex.printStackTrace();
                return null;
            }
        }

    public String finalwritetofile(String input, String outputfile) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
//        System.out.println("Cleartext block size: "+clearTextSize);
//        System.out.println("Ciphertext block size: "+cipherTextSize);
        String output = "";
        try {
            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(outputfile),"utf-8");
            StringWriter foss = new StringWriter();

            byte[] clearTextBlock = new byte[clearTextSize];
            long blocks = 0;
            int dataSize = 0;
            String[] in = input.split("\n");
            int k = 0;
            int padding_bytes=0;
            while (k<in.length) {
                blocks++;
                BigInteger cipherText = new BigInteger(in[k]);
//                BigInteger clearText = cipherText.modPow(d, n);
                BigInteger clearText = cipherText;
                byte[] clearTextData = clearText.toByteArray();
                padding_bytes = clearTextData[clearTextData.length-1];
//                putBytesBlock(clearTextBlock, clearTextData);
                String str = new String(clearTextData,"utf-8");
                output = output+str;
                k++;
            }
//            char s = output.charAt(output.length() - 1);
//            String match = "("+s+")*$";
//            output = output.replaceAll(match, "");
            output = output.substring(0,output.length()-padding_bytes);
            fos.write(output);
            foss.write(output);
            fos.close();
            foss.close();
//            System.out.println("Decryption block count: "+blocks);
            return foss.toString();
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public String findecrypt(String input) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize-1)/8,256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        System.out.println("Cleartext block size: "+clearTextSize);
        System.out.println("Ciphertext block size: "+cipherTextSize);
        String output = "";
        try {
            BufferedReader in = new BufferedReader(new StringReader(input));
//            OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(outputfile),"utf-8");
            StringWriter fos = new StringWriter();
            byte[] clearTextBlock = new byte[clearTextSize];
            long blocks = 0;
            int dataSize = 0;
            String line = in.readLine();
            while (line != null && line.length() != 0) {
                blocks++;
                BigInteger cipherText = new BigInteger(line);
                System.out.println(cipherText);
                BigInteger clearText = cipherText.modPow(d, n);
                System.out.println(clearText);
                byte[] clearTextData = clearText.toByteArray();
                putBytesBlock(clearTextBlock, clearTextData);
                String str = new String(clearTextData, "utf-8");
                output = output+str;
                line = in.readLine();
            }
            char s = output.charAt(output.length() - 1);
            String match = "("+s+")*$";
            output = output.replaceAll(match, "");
            fos.write(output);
            in.close();
            fos.close();
            System.out.println("Decryption block count: " + blocks);
            return fos.toString();
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public boolean verifydecrypt (String inputfile,String interput) {
        int keySize = n.bitLength();                       // In bits
        int clearTextSize = Math.min((keySize - 1) / 8, 256);   // In bytes
        int cipherTextSize = 1 + (keySize-1)/8;            // In bytes
        String ciphertxt = "";
        System.out.println("Cleartext block size: "+clearTextSize);
        System.out.println("Ciphertext block size: "+cipherTextSize);
        try {
            FileInputStream fis = new FileInputStream(inputfile);
            byte[] clearTextBlock = new byte[clearTextSize];
            byte[] cipherTextBlock = new byte[cipherTextSize];
            long blocks = 0;
            int dataSize = 0;
            while (fis.read(cipherTextBlock)>0) {
                blocks++;
                BigInteger cipherText = new BigInteger(1,cipherTextBlock);
                ciphertxt = ciphertxt + cipherText+"\n";
                dataSize = clearTextSize;
                if (fis.available()==0) {
                    dataSize = getDataSize(clearTextBlock);
                }
            }
            fis.close();
            System.out.println("The Biginter of original message: "+ciphertxt);
            if (ciphertxt.equals(interput)){
                return true;
            }
            else return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
