import java.io.*;

/**
 * Created by jpan on 11/9/15.
 */
public class Message {
    public static Object socketObjReceive(InputStream inputStream) throws IOException, ClassNotFoundException {
        Object obj;
        ObjectInputStream ois = new ObjectInputStream(inputStream);
        while ((obj = ois.readObject()) == null){}
        return obj;
    }

    public static void socketObjSend(Serializable obj, OutputStream outputStream) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(obj);
    }

    public static class MessageClosure<T extends Serializable> implements Serializable{
        private final T object;
        private final Class<T> type;

        public Class<T> getMyType() {
            return this.type;
        }


        MessageClosure(T object){
            this.object = object;
            this.type = (Class<T>) object.getClass();
        }

        public T getObject() {
            return object;
        }
    }

}
