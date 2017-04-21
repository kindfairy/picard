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
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(1)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    public void measureGC() {
        String[] args=new String[5];
        StringBuilder sb=new StringBuilder();
        StringTokenizer stringTokenizer=new StringTokenizer("CHART=/home/henry/Documents/epam-practice/test/gc_bias_metrics.pdf \n" +
                "S=/home/henry/Documents/epam-practice/test/out-summary_metrics.txt \n" +
                "I=/home/henry/Documents/epam-practice/test/NA12878.chrom11.ILLUMINA.bwa.CEU.exome.20121211.bam \n" +
                "O=/home/henry/Documents/epam-practice/test/out-gc_bias_metrics.txt \n" +
                "R=/home/henry/Documents/epam-practice/test/hs37d5.fa");
        for(int i=0;stringTokenizer.hasMoreTokens();i++){
            args[i]=stringTokenizer.nextToken();
        }

        new CollectGcBiasMetrics().instanceMain(args);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(1)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    public void measureCISM() {
        String[] args=new String[3];
        StringBuilder sb=new StringBuilder();
        StringTokenizer stringTokenizer=new StringTokenizer("H=/home/henry/Documents/epam-practice/test/insert_size_histogram.pdf \n" +
                "I=/home/henry/Documents/epam-practice/test/NA12878.chrom11.ILLUMINA.bwa.CEU.exome.20121211.bam \n" +
                "O=/home/henry/Documents/epam-practice/test/out-insert_size_metrics.txt ");
        for(int i=0;stringTokenizer.hasMoreTokens();i++){
            args[i]=stringTokenizer.nextToken();
        }

        new CollectInsertSizeMetrics().instanceMain(args);

    }


//    public static void main(String[] args) {
//        measureName();
//    }
}
