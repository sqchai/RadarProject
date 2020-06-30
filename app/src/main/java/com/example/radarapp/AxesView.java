package com.example.radarapp;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Arrays;

public class AxesView extends View {
    private Resources resources = this.getResources();

    private String title;
    private String xLabel;
    private String yLabel;

    private final int LIST_DATABUF_SIZE = resources.getInteger(R.integer.LIST_DATABUF_SIZE);

    // Buffer pointer (point to the position to be written).
    private static int listDatabufPointer = 0;
    // Real values.
    private int[] listData1 = new int[LIST_DATABUF_SIZE];
    private int[] listData2 = new int[LIST_DATABUF_SIZE];

    private boolean isHave2DataSet;

    static {
        listDatabufPointer = 0;
    }

    public AxesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AxesView);
        this.title = ta.getString(R.styleable.AxesView_title);
        this.isHave2DataSet = ta.getBoolean(R.styleable.AxesView_isHave2DataSet, false);
        ta.recycle();
        this.xLabel = "Time (s)";
        this.yLabel = "Voltage (V)";
    }

    public void reset() {
        Arrays.fill(listData1, 0);
        if (isHave2DataSet) {
            Arrays.fill(listData2, 0);
        }
    }

    public static int getListDatabufPointer() {
        return listDatabufPointer;
    }

    public static void setListDatabufPointer(int listDatabufPointer) {
        AxesView.listDatabufPointer = listDatabufPointer;
    }

    public int[] getListData() {
        return getListData(true);
    }

    public int[] getListData(boolean which) { // true: 1, false: 2
        if (!which && !isHave2DataSet) {
            throw new IllegalArgumentException("No data list 2!");
        }
        if (which) {
            return listData1;
        } else {
            return listData2;
        }
    }

    private int[] minmaxResult = new int[2];

    public void getMinMaxNum(int[] data) {
        minmaxResult[0] = data[0]; // min
        minmaxResult[1] = data[0]; // max
        for (int i = 0; i < LIST_DATABUF_SIZE; i++) {
            if (data[i] < minmaxResult[0]) {
                minmaxResult[0] = data[i];
            }
            if (data[i] > minmaxResult[1]) {
                minmaxResult[1] = data[i];
            }
        }
    }

    //    private int[] waveXLabels = new int[]{0, 100, 200, 300, 400, 500}, waveYLabels = new int[3];
    private int[] waveXLabels = new int[]{0, 1, 2, 3, 4, 5};
    private double[] waveYLabels = new double[3];
    private int[] waveXLabelWidths = new int[waveXLabels.length];

    private Paint paint = new Paint();
    // The Path instance is used to draw the yLabel and the wave chart.
    private Path path = new Path();
    // This instance is used to measure the height of a String.
    private Rect measureText = new Rect();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(resources.getColor(R.color.FRAME_BGCOlOR));

        // Get the data range.
        getMinMaxNum(listData1);
        int dataMin = minmaxResult[0], dataMax = minmaxResult[1];
        if (isHave2DataSet) {
            getMinMaxNum(listData2);
            if (dataMin > minmaxResult[0]) {
                dataMin = minmaxResult[0];
            }
            if (dataMax < minmaxResult[1]) {
                dataMax = minmaxResult[1];
            }
        }

        // Get the labels.
        int waveYLabelsLength;
        if (dataMin == dataMax) {
            waveYLabels[1] = dataMin / 4096.0 * 3.3;
            waveYLabels[0] = waveYLabels[1] + 1;
            waveYLabels[2] = waveYLabels[1] - 1;
            waveYLabelsLength = 3;
        } else {
            if (dataMax - dataMin <= 50) {
                waveYLabels[0] = dataMax / 4096.0 * 3.3;
                waveYLabels[1] = dataMin / 4096.0 * 3.3;
                waveYLabelsLength = 2;
            } else {
                waveYLabels[0] = dataMax / 4096.0 * 3.3;
                waveYLabels[2] = dataMin / 4096.0 * 3.3;
                waveYLabels[1] = (waveYLabels[0] + waveYLabels[2]) / 2;
                waveYLabelsLength = 3;
            }
        }

        for (int i = 0; i < waveYLabelsLength; i++) {
            waveYLabels[i] = Math.round(waveYLabels[i] * 100) / 100.0;
        }

        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(40f);
        paint.setStrokeWidth(2f);

        int maxWidthOfYLabels = 0;

        for (int i = 0; i < waveXLabels.length; i++) {
            waveXLabelWidths[i] = (int) paint.measureText(waveXLabels[i] + "");
        }
        for (int i = 0; i < waveYLabelsLength; i++) {
            int tempWidth = (int) paint.measureText(waveYLabels[i] + "");
            maxWidthOfYLabels = maxWidthOfYLabels < tempWidth ? tempWidth : maxWidthOfYLabels;
        }

        // Get the String height.
        paint.getTextBounds(title, 0, 1, measureText);
        int stringHeight = measureText.height();

        // Calculate the wave area.
        final int STRING_PRINT_MARGIN = stringHeight / 2;
        int waveX1 = stringHeight + maxWidthOfYLabels + STRING_PRINT_MARGIN * 4;
        int waveY1 = stringHeight + STRING_PRINT_MARGIN * 2;
        int waveX2 = this.getWidth() - waveXLabelWidths[waveXLabelWidths.length - 1] / 2 - STRING_PRINT_MARGIN;
        int waveY2 = this.getHeight() - stringHeight * 2 - STRING_PRINT_MARGIN * 4;
        paint.setColor(resources.getColor(R.color.WAVE_BGCOLOR));
        canvas.drawRect(waveX1, waveY1, waveX2, waveY2, paint);

        paint.setColor(resources.getColor(R.color.LABELS_COLOR));

        // Draw the title and the axes labels.
        paint.setFakeBoldText(true);
        canvas.drawText(title, (waveX1 + waveX2 + paint.measureText(title)) / 2, stringHeight + STRING_PRINT_MARGIN, paint);
        paint.setFakeBoldText(false);
        canvas.drawText(xLabel, (waveX1 + waveX2 + paint.measureText(xLabel)) / 2, this.getHeight() - STRING_PRINT_MARGIN, paint);

        path.moveTo(STRING_PRINT_MARGIN + stringHeight / 2, (waveY1 + waveY2 - paint.measureText(yLabel)) / 2);
        path.lineTo(STRING_PRINT_MARGIN + stringHeight / 2, (waveY1 + waveY2 + paint.measureText(yLabel)) / 2);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        canvas.drawTextOnPath(yLabel, path, 0f, stringHeight / 2, paint);

        // Draw the coordinate axes.
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(waveX1, waveY1, waveX1, waveY2, paint);
        canvas.drawLine(waveX1, waveY2, waveX2, waveY2, paint);
        canvas.drawLine(waveX1, waveY1, waveX1 + STRING_PRINT_MARGIN, waveY1, paint);
        if (waveYLabelsLength == 3) {
            canvas.drawLine(waveX1, (waveY1 + waveY2) / 2, waveX1 + 4, (waveY1 + waveY2) / 2, paint);
        }
        for (int i = 1; i < waveXLabels.length; i++) {
            float tempX;
            canvas.drawLine((tempX = (float) i / (waveXLabels.length - 1) * (waveX2 - waveX1) + waveX1), waveY2 - 4, tempX, waveY2, paint);
        }

        // Draw the scale labels.
        paint.setStyle(Paint.Style.FILL);
        float labelX = waveX1 - STRING_PRINT_MARGIN;
        float labelY;
        if (waveYLabelsLength == 3) {
            canvas.drawText(waveYLabels[0] + "", labelX, waveY1 + stringHeight / 2, paint);
            canvas.drawText(waveYLabels[1] + "", labelX, (waveY1 + waveY2 + stringHeight) / 2, paint);
            canvas.drawText(waveYLabels[2] + "", labelX, waveY2 + stringHeight / 2, paint);
        } else {
            canvas.drawText(waveYLabels[0] + "", labelX, waveY1 + stringHeight / 2, paint);
            canvas.drawText(waveYLabels[1] + "", labelX, waveY2 + stringHeight / 2, paint);
        }
        labelY = waveY2 + stringHeight + STRING_PRINT_MARGIN;
        for (int i = 0; i < waveXLabels.length; i++) {
            labelX = waveX1 + (float) i / (waveXLabels.length - 1) * (waveX2 - waveX1) + waveXLabelWidths[i] / 2f;
            canvas.drawText(waveXLabels[i] + "", labelX, labelY, paint);
        }

        // Plot the wave.
        drawWave(canvas, listData1, waveX1, waveY1, waveX2, waveY2, resources.getColor(R.color.WAVE_COLOR1), dataMin, dataMax);
        if (isHave2DataSet) {
            drawWave(canvas, listData2, waveX1, waveY1, waveX2, waveY2, resources.getColor(R.color.WAVE_COLOR2), dataMin, dataMax);
        }
    }

    private void drawWave(Canvas canvas, int[] data, int waveX1, int waveY1, int waveX2, int waveY2, int color, int dataMin, int dataMax) {
        float height = waveX2 - waveX1;
        float xDelta = height / (data.length - 1);
        paint.setStrokeWidth(2f);
        paint.setColor(color);
        if (dataMin == dataMax) {
            int waveY = (waveY1 + waveY2) / 2;
            canvas.drawLine(waveX1, waveY, waveX2, waveY, paint);
        } else {
            float k = (float) (waveY2 - waveY1) / (dataMin - dataMax);
            float b = (float) (waveY1 * dataMin - waveY2 * dataMax) / (dataMin - dataMax);
            for (int i = 0; i < data.length - 1; i++) {
                canvas.drawLine(
                        xDelta * i + waveX1,
                        k * data[i] + b,
                        xDelta * (i + 1) + waveX1,
                        k * data[i + 1] + b,
                        paint);
            }
        }
    }

}