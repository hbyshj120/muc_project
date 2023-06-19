package com.example.mcu_team25_voice_assistant;

import android.app.Activity;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.List;

public class AddNewVoiceCommand extends Activity {

    private XYPlot plot;

    private static final String TAG = "Add New Voice Command: ";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addnewvoicecommand);

        Log.d(TAG, "Add New Voice Command Opened");

        // initialize our XYPlot reference:
        plot = (XYPlot) findViewById(R.id.plot);


        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Python python = Python.getInstance();
        String NewAudioName = getFilesDir().getAbsolutePath() + "/new.wav";
        PyObject pyObject = python.getModule("hello");
        List<PyObject> obj = pyObject.callAttr("process", NewAudioName).asList();

        int samplingFrequency = obj.get(0).toJava(int.class);
        int numberOfSamples = obj.get(1).toJava(int.class);
        double[] array = obj.get(2).toJava(double[].class);

        Number numbers[] = new Number[numberOfSamples];
        Number values[]  = new Number[numberOfSamples];
        for (int i = 0; i < numberOfSamples; i++) {
            numbers[i] = i + 1;
            values[i] = array[i];
        }

        Number[] domainLabels = numbers;
        Number[] series1Numbers = values;



        Log.d(TAG, String.valueOf(samplingFrequency) + String.valueOf(array.length) + String.valueOf(array));


        // create a couple arrays of y-values to plot:
//        final Number[] domainLabels = {1, 2, 3, 6, 7, 8, 9, 10, 13, 14};
//        Number[] series1Numbers = {1, 4, 2, 8, 4, 16, 8, 32, 16, 64};
//        final Number[] domainLabels = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
//        Number[] series1Numbers = {0.3, 0.2, 0.4, 0.1, -0.1, 0.6, 0.3, 0.4, 0.2, -0.2, 0.5, 0.3};

        // turn the above arrays into XYSeries':
        // (Y_VALS_ONLY means use the element index as the x value)
        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series 1");

        // create formatters to use for drawing a series using LineAndPointRenderer
        // and configure them from xml:
        LineAndPointFormatter series1Format =
                new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);

        // just for fun, add some smoothing to the lines:
        // see: http://androidplot.com/smooth-curves-and-androidplot/
//        series1Format.setInterpolationParams(
//                new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);
        plot.setTitle("new");
        plot.setPadding(0, 0, 0, 0);
        plot.setPlotMargins(0, 0, 0, 0);
        plot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
//        plot.removeXMarkers();



//        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
//            @Override
//            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
//                int i = Math.round(((Number) obj).floatValue());
//                return toAppendTo.append(domainLabels[i]);
//            }
//            @Override
//            public Object parseObject(String source, ParsePosition pos) {
//                return null;
//            }
//        });

    }
}
