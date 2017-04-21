package picard.analysis;

import org.openjdk.jmh.annotations.*;
import picard.analysis.CollectAlignmentSummaryMetrics;
import picard.analysis.CollectGcBiasMetrics;
import picard.analysis.CollectInsertSizeMetrics;

import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;



public class SimpleBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(1)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    public void measureCASM() {
        String[] args=new String[3];
        StringTokenizer stringTokenizer=new StringTokenizer("R=/home/henry/Documents/epam-practice/test/hs37d5.fa \n" +
                "I=/home/henry/Documents/epam-practice/test/NA12878.chrom11.ILLUMINA.bwa.CEU.exome.20121211.bam \n" +
                "O=/home/henry/Documents/epam-practice/test/out-alignment_summary_metrics.txt");
        for(int i=0;stringTokenizer.hasMoreTokens();i++){
            args[i]=stringTokenizer.nextToken();
        }
        try {
            new CollectAlignmentSummaryMetrics().instanceMain(args);
        }catch (Exception e){

        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(1)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    public void measureGC() {
        String[] args=new String[5];
        StringBuilder sb=new StringBuilder();
        StringTokenizer stringTokenizer=new StringTokenizer("I=C:\\Users\\ABRA\\tmp.picard\\NA12878.chrom11.ILLUMINA.bwa.CEU.exome.20121211.bam\n" +
                "O=C:\\Users\\ABRA\\tmp.picard\\out_CollectGcBiasMetrics.txt\n" +
                "CHART=C:\\Users\\ABRA\\tmp.picard\\gc_bias_metrics.pdf\n" +
                "S=C:\\Users\\ABRA\\tmp.picard\\summary_metrics.txt\n" +
                "R=C:\\Users\\ABRA\\tmp.picard\\hs37d5-002.fa");
        for(int i=0;stringTokenizer.hasMoreTokens();i++){
            args[i]=stringTokenizer.nextToken();
        }

        CollectGcBiasMetrics.main(args);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(1)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    public void measureCISM() {
        String[] args=new String[7];
        StringBuilder sb=new StringBuilder();
        StringTokenizer stringTokenizer=new StringTokenizer("I=C:\\Users\\ABRA\\tmp.picard\\NA12878.chrom11.ILLUMINA.bwa.CEU.exome.20121211.bam\n" +
                "HISTOGRAM_FILE=C:\\Users\\ABRA\\tmp.picard\\out_CollectInsertSizeMetrics_hist.txt\n" +
                "O=C:\\Users\\ABRA\\tmp.picard\\out_CollectInsertSizeMetrics.txt\n" +
                "R=C:\\Users\\ABRA\\tmp.picard\\hs37d5-002.fa\n" +
                "VALIDATION_STRINGENCY=LENIENT\n" +
                "ASSUME_SORTED=TRUE\n" +
                "STOP_AFTER=0");
        for(int i=0;stringTokenizer.hasMoreTokens();i++){
            args[i]=stringTokenizer.nextToken();
        }

        CollectInsertSizeMetrics.main(args);

    }


//    public static void main(String[] args) {
//        measureName();
//    }
}
