/*
 * Copyright 2018 Google LLC All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ginkage.wearmouse.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Helper class to gather some stats and store the calibration data. Right now it calculates a lot
 * more stats than actually needed. Some of them are used for logging the sensors quality (and
 * filing bugs), other may be required in the future, e.g. for bias.
 */
class CalibrationData {

    private static final String TAG = "CalibrationData";

    private static final String DATA_PREF = "com.ginkage.wearmouse.CALIBRATION";
    static final String KEY_MEDIAN = "median";
    static final String KEY_MEAN = "mean";
    static final String KEY_SIGMA = "sigma";
    static final String KEY_DELTA = "delta";
    static final String KEY_COMPLETE = "complete";

    // Student's distribution T values for 95% (two-sided) confidence interval.
    private static final double[] Tn = {
        12.71, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228,
        2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086,
        2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045, 2.042,
        2.021, 2.009, 2.000, 1.990, 1.984, 1.980, 1.960
    };

    // Number of samples (degrees of freedom) for the corresponding T values.
    private static final int[] Nn = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
        11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
        40, 50, 60, 80, 100, 120, 200
    };

    private final SharedPreferences sharedPref;

    private int count;
    private boolean complete;
    private final Vector sum = new Vector();
    private final Vector sumSq = new Vector();
    private final Vector mean = new Vector();
    private final Vector m2 = new Vector();
    private final Vector d = new Vector();
    private final Vector s2 = new Vector();
    private final Vector sigma = new Vector();
    private final Vector s = new Vector();
    private final Vector delta = new Vector();
    private final Vector low = new Vector();
    private final Vector high = new Vector();
    private final Vector temp = new Vector();
    private final Vector median = new Vector();
    private ArrayList<Float> xData = new ArrayList<>();
    private ArrayList<Float> yData = new ArrayList<>();
    private ArrayList<Float> zData = new ArrayList<>();

    /** @param context The Context to access shared preferences with. */
    CalibrationData(Context context) {
        this.sharedPref = context.getSharedPreferences(DATA_PREF, Context.MODE_PRIVATE);
        readData();
    }

    /**
     * Check if the sensors were calibrated before.
     *
     * @return {@code true} if calibration data is available, or {@code false} otherwise.
     */
    boolean isComplete() {
        return complete;
    }

    /** Prepare to collect new calibration data. */
    void reset() {
        complete = false;
        count = 0;
        sum.reset();
        sumSq.reset();
        mean.reset();
        median.reset();
        sigma.reset();
        delta.reset();
        xData = new ArrayList<>();
        yData = new ArrayList<>();
        zData = new ArrayList<>();
        storeData();
    }

    /**
     * Retrieve the median gyroscope readings.
     *
     * @return Three-axis median vector.
     */
    Vector getMedian() {
        return median;
    }

    /**
     * Retrieve the mean gyroscope readings.
     *
     * @return Three-axis mean vector.
     */
    Vector getMean() {
        return mean;
    }

    /**
     * Retrieve the standard deviation of gyroscope readings.
     *
     * @return Three-axis standard deviation vector.
     */
    Vector getSigma() {
        return sigma;
    }

    /**
     * Retrieve the confidence interval size of gyroscope readings.
     *
     * @return Three-axis confidence interval size vector.
     */
    Vector getDelta() {
        return delta;
    }

    /**
     * Add a new gyroscope reading to the stats.
     *
     * @param data gyroscope values vector.
     * @return {@code true} if we now have enough data for calibration, or {@code false} otherwise.
     */
    boolean add(float[] data) {
        if (complete) {
            return true;
        }

        xData.add(data[0]);
        yData.add(data[1]);
        zData.add(data[2]);

        sum.add(temp.set(data[0], data[1], data[2]));
        sumSq.add(temp.square());
        count++;

        if (count >= Nn[Nn.length - 1]) {
            calcDelta();
        }

        return complete;
    }

    // Calculates the confidence interval (mean +- delta) and some other related values, like
    // standard deviation, etc. See https://en.wikipedia.org/wiki/Student%27s_t-distribution
    void calcDelta() {
        int idx = Arrays.binarySearch(Nn, count);
        median.set(median(xData), median(yData), median(zData));

        mean.set(sum).divide(count);
        m2.set(mean).square();
        d.set(sumSq).divide(count).subtract(m2);
        s2.set(d).multiply(count).divide(count - 1);
        sigma.set(d).sqrt();
        s.set(s2).sqrt();
        delta.set(s).multiply(Tn[idx]).divide(Math.sqrt(count));
        low.set(mean).subtract(delta);
        high.set(mean).add(delta);

        Log.d(
                TAG,
                String.format(
                        "M[x] = { %f ... %f }  //  median = %f"
                                + "  //  avg = %f  //  delta = %f  //  sigma = %f\n"
                                + "M[y] = { %f ... %f }  //  median = %f"
                                + "  //  avg = %f  //  delta = %f  //  sigma = %f\n"
                                + "M[z] = { %f ... %f }  //  median = %f"
                                + "  //  avg = %f  //  delta = %f  //  sigma = %f",
                        low.x, high.x, median.x, mean.x, delta.x, sigma.x, low.y, high.y, median.y,
                        mean.y, delta.y, sigma.y, low.z, high.z, median.z, mean.z, delta.z,
                        sigma.z));

        if (idx == Nn.length - 1) {
            complete = true;
            storeData();
        }
    }

    void readData() {
        mean.set(sharedPref.getString(KEY_MEAN, "0,0,0"));
        median.set(sharedPref.getString(KEY_MEDIAN, "0,0,0"));
        sigma.set(sharedPref.getString(KEY_SIGMA, "0,0,0"));
        delta.set(sharedPref.getString(KEY_DELTA, "0,0,0"));
        complete = sharedPref.getBoolean(KEY_COMPLETE, false);
    }

    private static float median(ArrayList<Float> list) {
        Collections.sort(list);
        int count = list.size();
        int middle = count / 2;
        return (count % 2 == 1)
                ? list.get(middle)
                : (list.get(middle - 1) + list.get(middle)) / 2.0f;
    }

    private void storeData() {
        sharedPref
                .edit()
                .putString(KEY_MEAN, mean.toString())
                .putString(KEY_MEDIAN, median.toString())
                .putString(KEY_SIGMA, sigma.toString())
                .putString(KEY_DELTA, delta.toString())
                .putBoolean(KEY_COMPLETE, complete)
                .apply();
    }
}
