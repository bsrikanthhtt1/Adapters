package com.istream;

import java.util.Timer;

import java.util.TimerTask;


public class Test {public String CurrentDate() {

    java.util.Date dt = new java.util.Date();
    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
    String currentTime = sdf.format(dt);
    return currentTime;

}

public static void main(String[] args) {
    class SayHello extends TimerTask {

    	  Test thisObj = new Test();
        public void run() {
            String todaysdate = thisObj.CurrentDate();
            System.out.println(todaysdate);
        }
    }
    Timer timer = new Timer();
    timer.schedule(new SayHello(), 0, 5000); 
}}
