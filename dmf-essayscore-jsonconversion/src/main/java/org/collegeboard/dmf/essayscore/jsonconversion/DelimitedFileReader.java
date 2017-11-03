package org.collegeboard.dmf.essayscore.jsonconversion;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;

/**
 * Custom File Reader created to read delimited flat file records.
 */
public final class DelimitedFileReader implements Closeable
{

    private final BufferedReader in;
    private final String atomicElementHeader;
    private String headerRecord;
    private String trailerRecord;
    private final String header;
    private final String trailer;
    private String dataRecord;
    private long atomicDataSegmentsNumber;
    private long lineNumber = 0;
    private String atomicHeaderRecordCache;
    private String recordDelimiter;


    /**
     * @param file
     * @param atomicElement
     * @param header
     * @param trailer
     * @throws IOException
     */
    public DelimitedFileReader(InputStream in, String atomicElement, String header, String trailer,
                                       String recordDelimiter) throws IOException
    {
        //        validateInputParams(file, atomicElement, header, trailer);



        this.in = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        this.atomicElementHeader = atomicElement;
        this.header = header;
        this.trailer = trailer;
        this.recordDelimiter = recordDelimiter;

        //--populate this.trailerRecord and this.headerRecord
        //        readHeaderAndTrailer(file, atomicElement, header, trailer);

    }

    /**
     * Based on provided init params this method will read and return next atomic portion of data from the file.
     * Each element in the <code>List<String><code> represents one file record /line/.
     *
     * @return
     * @throws java.io.IOException
     */
    public LinkedList<DelimitedFileRecord> readAtomicDataSegment() throws IOException
    {
        LinkedList<DelimitedFileRecord> atomicSection = new LinkedList<DelimitedFileRecord>();

        while ((dataRecord = in.readLine()) != null)
        {
            lineNumber++;
            //--strip record delimiter out
            dataRecord = StringUtils.substringBeforeLast(dataRecord.trim(), recordDelimiter);

            if (StringUtils.isNotEmpty(atomicHeaderRecordCache))
            {
                //--we have read at least one atomic section from file before
                atomicSection.add(new DelimitedFileRecord(lineNumber, atomicHeaderRecordCache));
                atomicHeaderRecordCache = null;
            }

            if (dataRecord.startsWith(header) || dataRecord.isEmpty())
            {
                //--we are only interested in data records at this point
                continue;

            } else if (dataRecord.startsWith(trailer))
            {
                //--with assumption that trailer is always the last record in file
                break;

            } else if (dataRecord.startsWith(atomicElementHeader))
            {
                //--atomic section header record read from file

                if (!atomicSection.isEmpty())
                {
                    //--current record starts new atomic section
                    atomicHeaderRecordCache = dataRecord;
                    break;
                } else
                {
                    //--current record is a first atomic section header record read from this file
                    atomicSection.add(new DelimitedFileRecord(lineNumber, dataRecord));
                }
            } else
            {
                atomicSection.add(new DelimitedFileRecord(lineNumber, dataRecord));
            }
        }

        if (atomicSection.isEmpty())
        {
            atomicSection = null;
        }

        return atomicSection;
    }

    /**
     * Moves the file pointer offset forward to beginning atomicSectionNumber
     * 
     * @throws java.io.IOException
     * @throws java.io.DMFServiceInputValidationException
     * */
    public void seek(long atomicSectionNumber) throws IOException
    {
        long currentAtomicSection = 0L;

        while ((dataRecord = in.readLine()) != null)
        {
            lineNumber++;


            if (dataRecord.startsWith(atomicElementHeader))
            {
                currentAtomicSection++;
                if (currentAtomicSection == atomicSectionNumber)
                {
                    //--strip record delimiter out
                    dataRecord = StringUtils.substringBeforeLast(dataRecord.trim(), recordDelimiter);
                    atomicHeaderRecordCache = dataRecord;
                    return;
                }
            }
        }
        //if control comes here means, File has reached EOF but still not able to find atomicSectionNumber
        throw new RuntimeException("Invalid AtomicSectionNumber");
    }


    /**
     * Closes open input stream
     */
    @Override
    public void close() throws IOException
    {

        if (in != null)
        {
            in.close();
        }

    }


    /**
     * Returns Header file record
     *
     * @return
     */
    public String getHeaderRecord()
    {
        return headerRecord;
    }

    /**
     * Returns Trailer file record
     *
     * @return
     */
    public String getTrailerRecord()
    {
        return trailerRecord;
    }


    /**
     * Returns number of atomic segments in file
     *
     * @return
     */
    public long getAtomicDataSegmentsNumber()
    {
        return atomicDataSegmentsNumber;
    }

    /**
     * @param file
     * @param atomicElement
     * @param header
     * @param trailer
     * @throws IOException
     */
    private void readHeaderAndTrailer(File file, String atomicElement, String header, String trailer) throws IOException
    {
        java.io.FileReader reader = new java.io.FileReader(file);
        BufferedReader br = new BufferedReader(reader);

        String lDataRecord;
        try
        {
            while ((lDataRecord = br.readLine()) != null)
            {
                if (lDataRecord.startsWith(header))
                {
                    this.headerRecord = StringUtils.stripEnd(lDataRecord.trim(), recordDelimiter);

                    if (StringUtils.isEmpty(this.headerRecord))
                    {
                        throw new RuntimeException("No header record in input file.");
                    }
                }
                if (lDataRecord.startsWith(trailer))
                {
                    this.trailerRecord = StringUtils.stripEnd(lDataRecord.trim(), recordDelimiter);
                }
                if (lDataRecord.startsWith(atomicElement))
                {
                    atomicDataSegmentsNumber++;
                }
            }

        } finally
        {
            br.close();
        }
    }

    private void validateInputParams(File file, String atomicElement, String header, String trailer)
    {

        if (file == null)
        {
            throw new RuntimeException("File cannot be null");
        }

        if (!file.exists())
        {
            throw new RuntimeException("File path: " + file.getAbsolutePath() + " does not " + "exist");
        }

        if (StringUtils.isEmpty(atomicElement))
        {
            throw new RuntimeException("atomicElementHeader is empty");
        }

        if (StringUtils.isEmpty(header))
        {
            throw new RuntimeException("header is empty");
        }

        if (StringUtils.isEmpty(trailer))
        {
            throw new RuntimeException("trailer is empty");
        }

    }

}
