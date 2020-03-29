import java.io.IOException;
import java.util.StringTokenizer;
import java.util.*;
import java.text.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AverageTripDistance {
    private static final Log LOG = LogFactory.getLog(AveragePassengerCount.class);

    public static class TokenizerMapper extends Mapper<Object, Text, Text, DoubleWritable> {
        private DoubleWritable one = new DoubleWritable(1.0);
        private Text word = new Text();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] tokens = value.toString().split(",");
            if (!Character.isDigit(tokens[4].charAt(0))) // to skip the header
                return;
            one.set(Integer.valueOf(tokens[4]));
            word.set("avg_trip_distance");
            context.write(word, one);

            try {
                DateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                DateFormat outputDateFormat = new SimpleDateFormat("EEEE");
                Date pickupDate = inputDateFormat.parse(tokens[1]);

                word.set(outputDateFormat.format(pickupDate).toString());
                context.write(word, one);
            } catch (ParseException excpt) {
                excpt.printStackTrace();
            }
        }
    }

    public static class IntSumReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
        private DoubleWritable result = new DoubleWritable();

        public void reduce(Text key, Iterable<DoubleWritable> values, Context context)
                throws IOException, InterruptedException {
            Double sum = 0.0;
            Double ctr = 0.0;
            for (DoubleWritable val : values) {
                sum += val.get();
                ctr++;
            }
            result.set(sum / ctr);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        conf.addResource(new Path("/opt/hadoop-3.2.1/etc/hadoop/core-site.xml"));
        conf.addResource(new Path("/opt/hadoop-3.2.1/etc/hadoop/hdfs-site.xml"));
        Job job = Job.getInstance(conf, "passenger count");
        job.setJarByClass(AverageTripDistance.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);
        // FileSystem fs = FileSystem.get(conf);
        // FSDataInputStream inputStream = fs.open(new Path(args[0]));
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

}

// scp AveragePassengerCount.java anuraagdj@137.116.113.184:/home/anuraagdj/code
// docker cp AveragePassengerCount.java namenode:/home/code
// hadoop com.sun.tools.javac.Main AveragePassengerCount.java
// jar cf 41.jar AveragePassengerCount*.class
// hadoop jar 41.jar AveragePassengerCount ./dataset ./output4.1
