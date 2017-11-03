package org.collegeboard.dmf.essayscore.jsonconversion;

/**
 *  Represents file record /row/
 */
public class DelimitedFileRecord
{
    private Long lineNumber;
    private String recordData;

    /**
     *
     * @param lineNumber Row number in the file
     * @param recordData String representing complete row data
     */
    public DelimitedFileRecord(Long lineNumber, String recordData)
    {
        this.lineNumber = lineNumber;
        this.recordData = recordData;
    }


    public Long getLineNumber()
    {
        return lineNumber;
    }

    public void setLineNumber(Long lineNumber)
    {
        this.lineNumber = lineNumber;
    }

    public String getRecordData()
    {
        return recordData;
    }

    public void setRecordData(String recordData)
    {
        this.recordData = recordData;
    }

}
