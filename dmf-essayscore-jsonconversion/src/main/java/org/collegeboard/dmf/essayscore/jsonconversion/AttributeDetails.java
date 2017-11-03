package org.collegeboard.dmf.essayscore.jsonconversion;

public class AttributeDetails
{
    private int position;
    private int type;
    private boolean trim;

    public AttributeDetails()
    {

    }

    public AttributeDetails(int position, int type, boolean trim)
    {
        this.position = position;
        this.type = type;
        this.trim = trim;
    }

    public int getPosition()
    {
        return position;
    }

    public void setPosition(int position)
    {
        this.position = position;
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public boolean isTrim()
    {
        return trim;
    }

    public void setTrim(boolean trim)
    {
        this.trim = trim;
    }

}
