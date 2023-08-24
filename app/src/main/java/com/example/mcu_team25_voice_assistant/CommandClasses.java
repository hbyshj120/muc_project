package com.example.mcu_team25_voice_assistant;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class CommandClasses {
//    public static String[] SoundClasses = new String[]{
//            "1min",
//            "10times",
//            "30s",
//            "plank",
//            "pullup",
//            "running",
//            "squat",
//            "yoga"
//    };
public CommandClasses() {
        addSoundTypes();
        addSoundClasses();
        addNumberMaps();
    }

    Dictionary<String, Integer> SoundTypes= new Hashtable<>();
    Dictionary<Integer, String> SoundClasses= new Hashtable<>();
    Dictionary<String, Integer> NumberMaps= new Hashtable<>();
    void addSoundTypes() {
        SoundTypes.put("minute",0);
        SoundTypes.put("eight",1);
        SoundTypes.put("pause",2);
        SoundTypes.put("running",3);
        SoundTypes.put("yoga",4);
        SoundTypes.put("squat",5);
        SoundTypes.put("nine",6);
        SoundTypes.put("times",7);
        SoundTypes.put("second",8);
        SoundTypes.put("stop",9);
        SoundTypes.put("walking",10);
        SoundTypes.put("pushup",11);
        SoundTypes.put("three",12);
        SoundTypes.put("one",13);
        SoundTypes.put("pullup",14);
        SoundTypes.put("zero",15);
        SoundTypes.put("seven",16);
        SoundTypes.put("two",17);
        SoundTypes.put("plank",18);
        SoundTypes.put("six",19);
        SoundTypes.put("continue",20);
        SoundTypes.put("cycling",21);
        SoundTypes.put("five",22);
        SoundTypes.put("four",23);
    }

    void addSoundClasses() {
        Enumeration enu = SoundTypes.keys();
        while (enu.hasMoreElements()) {
            String soundtype = (String) enu.nextElement();
            int classnumber = SoundTypes.get(soundtype);
            SoundClasses.put(classnumber, soundtype);
        }
    }

    void addNumberMaps() {
        NumberMaps.put("zero",0);
        NumberMaps.put("one",1);
        NumberMaps.put("two",2);
        NumberMaps.put("three",3);
        NumberMaps.put("four",4);
        NumberMaps.put("five",5);
        NumberMaps.put("six",6);
        NumberMaps.put("seven",7);
        NumberMaps.put("eight",8);
        NumberMaps.put("nine",9);
    }

    public String getSoundClass(int classnumber) {
        return SoundClasses.get(classnumber);
    }
    public boolean isExercise(int classnumber) {
        Set<String> exercises = new HashSet<String>();
        exercises.add("running");
        exercises.add("yoga");
        exercises.add("squat");
        exercises.add("walking");
        exercises.add("pushup");
        exercises.add("pullup");
        exercises.add("plank");
        exercises.add("cycling");

        String soundtype = SoundClasses.get(classnumber);
        return exercises.contains(soundtype);
    }

    public boolean isDigit(int classnumber) {
        Set<String> digits = new HashSet<String>();
        digits.add("zero");
        digits.add("one");
        digits.add("two");
        digits.add("three");
        digits.add("four");
        digits.add("five");
        digits.add("six");
        digits.add("seven");
        digits.add("eight");
        digits.add("nine");

        String soundtype = SoundClasses.get(classnumber);
        return digits.contains(soundtype);
    }

    public int getDigit(int classnumber) {
        String soundclass = getSoundClass(classnumber);
        return NumberMaps.get(soundclass);
    }
    public boolean isUnit(int classnumber) {
        Set<String> units = new HashSet<String>();
        units.add("second");
        units.add("minute");
        units.add("times");

        String soundtype = SoundClasses.get(classnumber);
        return units.contains(soundtype);
    }

    public int convertUnit(int classnumber) {
        if (classnumber == SoundTypes.get("minute")) {
            return 60;
        } else if (classnumber == SoundTypes.get("second")) {
            return 1;
        } else {
            return 1;
        }
    }

    public boolean isPause(int classnumber) {
        Set<String> interruptions = new HashSet<String>();
        interruptions.add("pause");
        String soundtype = SoundClasses.get(classnumber);
        return interruptions.contains(soundtype);
    }

    public boolean isContinue(int classnumber) {
        Set<String> interruptions = new HashSet<String>();
        interruptions.add("continue");

        String soundtype = SoundClasses.get(classnumber);
        return interruptions.contains(soundtype);
    }

    public boolean isStop(int classnumber) {
        Set<String> interruptions = new HashSet<String>();
        interruptions.add("stop");

        String soundtype = SoundClasses.get(classnumber);
        return interruptions.contains(soundtype);
    }

}
