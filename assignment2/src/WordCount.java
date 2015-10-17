/**
 * Created by jpan on 10/15/15.
 */
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class WordCount {

    public static class TextArrayWritable extends ArrayWritable {
        public TextArrayWritable() {
            super(Text.class);
        }

        public TextArrayWritable(List<String> stringList) {
            super(Text.class);
            Text[] texts = new Text[stringList.size()];
            int i = 0;
            for (String string : stringList) {
                texts[i] = new Text(string);
                i++;
            }
            set(texts);
        }
    }


    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, TextArrayWritable>{

        private Text word = new Text();

        public void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {
            String delims = "[ ]+";
            String[] tokens = value.toString().replaceAll("[^a-zA-Z0-9]"," ").toLowerCase().split(delims);
            List<String> tokenList = new ArrayList<>(Arrays.asList(tokens));
            tokenList.removeAll(Collections.singleton(""));
            int i = 0;
            ArrayList<String> dupList = new ArrayList<>();
            for (String token: tokenList ){
                if (dupList.contains(token)){
                    continue;
                }
                dupList.add(token);
                word.set(token);
                List<String> newList = new ArrayList<>(tokenList);
                newList.remove(token);
                context.write(word, new TextArrayWritable(newList));
            }

        }

    }

    public static class IntSumReducer
            extends Reducer<Text,TextArrayWritable,Text,MapWritable> {

        public void reduce(Text key, Iterable<TextArrayWritable> values,
                           Context context
        ) throws IOException, InterruptedException {
            MapWritable vector = new MapWritable() ;
            IntWritable tmpInt;
            for (TextArrayWritable textArray: values){
                for (Writable val: textArray.get()){
                    tmpInt = (IntWritable) vector.get(val);
                    if(tmpInt == null) {
                        tmpInt = new IntWritable(0);
                        vector.put(new Text((Text)val), tmpInt);
                    }
                    tmpInt.set(tmpInt.get() + 1);
                }
            }
            context.write(key, vector);
        }
    }

    public static class MyTextOutputFormat extends FileOutputFormat<Text, MapWritable> {
        @Override
        public RecordWriter<Text, MapWritable> getRecordWriter(TaskAttemptContext arg0) throws IOException, InterruptedException {
            //get the current path
            Path path = FileOutputFormat.getOutputPath(arg0);
            //create the full path with the output directory plus our filename
            Path fullPath = new Path(path, "result.txt");
            //create the file in the file system
            FileSystem fs = path.getFileSystem(arg0.getConfiguration());
            FSDataOutputStream fileOut = fs.create(fullPath, arg0);

            return new TextAndMapWritableRecordWriter(fileOut);
        }
    }

    public static class TextAndMapWritableRecordWriter extends RecordWriter<Text, MapWritable> {
        private static final String utf8 = "UTF-8";
        private DataOutputStream out;

        public TextAndMapWritableRecordWriter(DataOutputStream out) {
            this.out = out;
        }

        @Override
        public void write(Text key, MapWritable value) throws IOException {
            out.write(key.toString().getBytes(utf8));
            out.write("\n".getBytes(utf8));

            Iterator<Writable> it = value.keySet().iterator();
            while (it.hasNext()) {
                out.write("<".getBytes(utf8));
                Writable k = it.next();
                IntWritable v = (IntWritable) value.get(k);

                out.write(((Text) k).getBytes());
                out.write(", ".getBytes(utf8));
                out.write(v.toString().getBytes(utf8));
                out.write(">\n".getBytes(utf8));
            }

            out.write("\n".getBytes(utf8));
        }

        @Override
        public void close(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
            this.out.close();
        }
    }



    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(WordCount.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(TextArrayWritable.class);
//        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(MapWritable.class);
        job.setOutputFormatClass( MyTextOutputFormat.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}