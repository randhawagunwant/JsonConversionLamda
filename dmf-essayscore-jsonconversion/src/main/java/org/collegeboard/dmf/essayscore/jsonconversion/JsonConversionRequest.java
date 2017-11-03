package org.collegeboard.dmf.essayscore.jsonconversion;

public class JsonConversionRequest
{
    private long startAtomicSegmentIndex;

    private long endAtomicSegmentIndex;

    private String bucketName;

    private String fileName;

    private long totalAtomicSegmentCount;

    public long getStartAtomicSegmentIndex()
    {
        return startAtomicSegmentIndex;
    }

    public void setStartAtomicSegmentIndex(long startAtomicSegmentIndex)
    {
        this.startAtomicSegmentIndex = startAtomicSegmentIndex;
    }

    public long getEndAtomicSegmentIndex()
    {
        return endAtomicSegmentIndex;
    }

    public void setEndAtomicSegmentIndex(long endAtomicSegmentIndex)
    {
        this.endAtomicSegmentIndex = endAtomicSegmentIndex;
    }

    public String getBucketName()
    {
        return bucketName;
    }

    public void setBucketName(String bucketName)
    {
        this.bucketName = bucketName;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public long getTotalAtomicSegmentCount()
    {
        return totalAtomicSegmentCount;
    }

    public void setTotalAtomicSegmentCount(long totalAtomicSegmentCount)
    {
        this.totalAtomicSegmentCount = totalAtomicSegmentCount;
    }

}
