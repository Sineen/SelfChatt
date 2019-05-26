package com.example.ex5_v1;

public class Message {
    private String Id;
    private String TimeStamp;
    private String Text;
    private String Device;

    public Message(String Id, String time, String text, String device)
    {
        Text = text;
        TimeStamp = time;
        this.Id = Id;
        Device = device;
    }
    @Override
    public String toString()
    {
        return this.TimeStamp + ":" + Text;
    }

    public String getId() {
        return Id;
    }

    public String getTimeStamp() {
        return TimeStamp;
    }

    public String getText() {
           return Text;
    }

    public String getDevice() {
        return Device;
    }


}
