package com.example.mcu_team25_voice_assistant;

// reference: https://stackoverflow.com/questions/2736389/how-to-pass-an-object-from-one-activity-to-another-on-android

import java.io.Serializable;

@SuppressWarnings("serial") //With this annotation we are going to hide compiler warnings
public class VoiceCommand implements Serializable {

    private String name;
    private String path;
    private float lastUsage;
    private int numUsages = 0;
    private float totalUsages = 0;

    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    public String getPath()
    {
        return path;
    }
    public void setPath(String path)
    {
        this.path = path;
    }

    public float getLastUsage()
    {
        return lastUsage;
    }
    public void setLastUsage(float lastUsage)
    {
        this.lastUsage = lastUsage;
    }

    public int getNumUsages()
    {
        return numUsages;
    }
    public float getTotalUsages() {return totalUsages; }
    public void addCurrentUsage(float usage)
    {
        setLastUsage(usage);
        numUsages++;
        totalUsages = totalUsages + usage;
    }
    public float getAverageUsages() {
        if (numUsages == 0) {
            return 0.0F;
        } else {
            return totalUsages / numUsages;
        }
    }

    public String printCommand() {
        return "Voice Command: " + this.name
                + "; Path: " + this.path
                + "; Last Usage: " + this.lastUsage
                + "; Number of total usages: " + this.numUsages
                + "; Total usages: " + this.totalUsages;
    }

    // constructor
    public VoiceCommand(String name, String path, float lastUsage, int numUsages, float totalUsages)
    {
        this.name = name;
        this.path = path;
        this.lastUsage = lastUsage;
        this.numUsages = numUsages;
        this.totalUsages = totalUsages;
    }
}
