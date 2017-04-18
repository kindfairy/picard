/*
 * The MIT License
 *
 * Copyright (c) 2015 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package picard.analysis;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFileWalker;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.ProgressLogger;
import htsjdk.samtools.util.SequenceUtil;
import javafx.util.Pair;
import picard.PicardException;
import picard.cmdline.CommandLineProgram;
import picard.cmdline.Option;
import picard.cmdline.StandardOptionDefinitions;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * Super class that is designed to provide some consistent structure between subclasses that
 * simply iterate once over a coordinate sorted BAM and collect information from the records
 * as the go in order to produce some kind of output.
 *
 * @author Tim Fennell
 */
public abstract class SinglePassSamProgram extends CommandLineProgram {

    private static final int MAX_SIZE = 1000;
    private static final int QUEUE_MAX_SIZE = 5;


    @Option(shortName = StandardOptionDefinitions.INPUT_SHORT_NAME, doc = "Input SAM or BAM file.")
    public File INPUT;

    @Option(shortName = "O", doc = "File to write the output to.")
    public File OUTPUT;

    @Option(doc = "If true (default), then the sort order in the header file will be ignored.",
            shortName = StandardOptionDefinitions.ASSUME_SORTED_SHORT_NAME)
    public boolean ASSUME_SORTED = true;

    @Option(doc = "Stop after processing N reads, mainly for debugging.")
    public long STOP_AFTER = 0;

    private static final Log log = Log.getInstance(SinglePassSamProgram.class);

    /**
     * Final implementation of doWork() that checks and loads the input and optionally reference
     * sequence files and the runs the sublcass through the setup() acceptRead() and finish() steps.
     */
    @Override
    protected final int doWork() {
        makeItSo(INPUT, REFERENCE_SEQUENCE, ASSUME_SORTED, STOP_AFTER, Arrays.asList(this));
        return 0;
    }

    public static void makeItSo(final File input,
                                final File referenceSequence,
                                final boolean assumeSorted,
                                final long stopAfter,
                                final Collection<SinglePassSamProgram> programs) {


        // Setup the standard inputs
        IOUtil.assertFileIsReadable(input);
        final SamReader in = SamReaderFactory.makeDefault().referenceSequence(referenceSequence).open(input);


        // Optionally load up the reference sequence and double check sequence dictionaries
        final ReferenceSequenceFileWalker walker;
        if (referenceSequence == null) {
            walker = null;
        } else {
            IOUtil.assertFileIsReadable(referenceSequence);
            walker = new ReferenceSequenceFileWalker(referenceSequence);

            if (!in.getFileHeader().getSequenceDictionary().isEmpty()) {
                SequenceUtil.assertSequenceDictionariesEqual(in.getFileHeader().getSequenceDictionary(),
                        walker.getSequenceDictionary());
            }
        }

        // Check on the sort order of the BAM file
        {
            final SortOrder sort = in.getFileHeader().getSortOrder();
            if (sort != SortOrder.coordinate) {
                if (assumeSorted) {
                    log.warn("File reports sort order '" + sort + "', assuming it's coordinate sorted anyway.");
                } else {
                    throw new PicardException("File " + input.getAbsolutePath() + " should be coordinate sorted but " +
                            "the header says the sort order is " + sort + ". If you believe the file " +
                            "to be coordinate sorted you may pass ASSUME_SORTED=true");
                }
            }
        }


        // Call the abstract setup method!
        boolean anyUseNoRefReads = false;
        for (final SinglePassSamProgram program : programs) {
            program.setup(in.getFileHeader(), input);
            anyUseNoRefReads = anyUseNoRefReads || program.usesNoRefReads();
        }






















        final ProgressLogger progress = new ProgressLogger(log);

        //one particular thread for reading a ReferenceSequence from walker
        ExecutorService refReader = Executors.newSingleThreadExecutor();
        BlockingQueue<List<SAMRecord>> tasksForRefReader = new LinkedBlockingQueue<>(QUEUE_MAX_SIZE);

        //one particular thread for executing method program.acceptRead()
        ExecutorService acceptReadService = Executors.newSingleThreadExecutor();
        BlockingQueue<List<Pair<SAMRecord, ReferenceSequence>>> tasksForAcceptRead = new LinkedBlockingQueue<>(QUEUE_MAX_SIZE);

        final List<SAMRecord> POISON_PILL = Collections.emptyList();


        ExecutorService support1 = Executors.newSingleThreadExecutor();
        support1.submit(new Runnable() {
            @Override
            public void run() {
                final List<Pair<SAMRecord, ReferenceSequence>> POISON_PILL = Collections.emptyList();
                while (true) {
                    try {
                        final List<SAMRecord> records = tasksForRefReader.take();
                        if(records.isEmpty()) {
                            tasksForAcceptRead.add(POISON_PILL);
                            return;
                        }else {
                            refReader.submit(new Runnable() {
                                @Override
                                public void run() {
                                    List<Pair<SAMRecord, ReferenceSequence>> pairs = new ArrayList<>(MAX_SIZE);
                                    for (SAMRecord tmpRecord : records) {
                                        ReferenceSequence ref;
                                        if (walker == null || tmpRecord.getReferenceIndex() == SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX) {
                                            ref = null;
                                        } else {
                                            ref = walker.get(tmpRecord.getReferenceIndex());
                                        }
                                        pairs.add(new Pair<>(tmpRecord, ref));
                                    }
                                    try {
                                        tasksForAcceptRead.put(pairs);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).get();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                }
            }
        });


        ExecutorService support2 = Executors.newSingleThreadExecutor();
        support2.submit(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        final List<Pair<SAMRecord, ReferenceSequence>> pairs = tasksForAcceptRead.take();
                        if( pairs.isEmpty() ){
                            return;
                        }
                        acceptReadService.submit(new Runnable() {
                            @Override
                            public void run() {
                                for (Pair<SAMRecord, ReferenceSequence> pair : pairs) {
                                    for (final SinglePassSamProgram program : programs) {
                                        program.acceptRead(pair.getKey(), pair.getValue());
                                    }
                                }
                            }
                        }).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        });



        //chunk of SAMRecords
        List<SAMRecord> records = new ArrayList<>(MAX_SIZE);

        Iterator<SAMRecord> it = in.iterator();
        while (it.hasNext()) {


            final SAMRecord record = it.next();
            records.add(record);

            if (records.size() == MAX_SIZE) {

                final List<SAMRecord> tmpRecords = records;
                records = new ArrayList<>(MAX_SIZE);

                try {
                    tasksForRefReader.put(tmpRecords);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            progress.record(record);

            // See if we need to terminate early?
            if (stopAfter > 0 && progress.getCount() >= stopAfter) {
                break;
            }

            // And see if we're into the unmapped reads at the end
            if (!anyUseNoRefReads && record.getReferenceIndex() == SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX) {
                break;
            }
        }
        //need to process last chunk with size < MAX_SIZE
        {
            List<SAMRecord> tmpRecords = records;

            tasksForRefReader.add(tmpRecords);

        }

        tasksForRefReader.add(POISON_PILL);

        //it's necessary to wait for termination refReadService before we shutdown acceptReadService
        //because this service submits tasks to acceptReadService

        support1.shutdown();
        try {
            support1.awaitTermination(1, TimeUnit.DAYS);
        }catch (InterruptedException e){
            e.printStackTrace();
        }


        refReader.shutdown();
        try{
            refReader.awaitTermination(1, TimeUnit.DAYS);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        support2.shutdown();
        try {
            support2.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        acceptReadService.shutdown();
        try {
            acceptReadService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }






















        CloserUtil.close(in);


        for (final SinglePassSamProgram program : programs) {
            program.finish();
        }

    }

    /**
     * Can be overriden and set to false if the section of unmapped reads at the end of the file isn't needed.
     */
    protected boolean usesNoRefReads() {
        return true;
    }

    /**
     * Should be implemented by subclasses to do one-time initialization work.
     */
    protected abstract void setup(final SAMFileHeader header, final File samFile);

    /**
     * Should be implemented by subclasses to accept SAMRecords one at a time.
     * If the read has a reference sequence and a reference sequence file was supplied to the program
     * it will be passed as 'ref'. Otherwise 'ref' may be null.
     */
    protected abstract void acceptRead(final SAMRecord rec, final ReferenceSequence ref);

    /**
     * Should be implemented by subclasses to do one-time finalization work.
     */
    protected abstract void finish();

}
