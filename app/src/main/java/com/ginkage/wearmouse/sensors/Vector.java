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

import java.util.Locale;

/** Simple 3D vector class with a string representation and some math operations. */
final class Vector {
    public double x;
    public double y;
    public double z;

    Vector() {
        reset();
    }

    Vector set(String s) {
        String[] values = s.split(",");
        set(
                Double.parseDouble(values[0]),
                Double.parseDouble(values[1]),
                Double.parseDouble(values[2]));
        return this;
    }

    void reset() {
        set(0, 0, 0);
    }

    Vector set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    Vector set(Vector v) {
        return set(v.x, v.y, v.z);
    }

    Vector add(Vector v) {
        x += v.x;
        y += v.y;
        z += v.z;
        return this;
    }

    Vector subtract(Vector v) {
        x -= v.x;
        y -= v.y;
        z -= v.z;
        return this;
    }

    Vector square() {
        x *= x;
        y *= y;
        z *= z;
        return this;
    }

    Vector sqrt() {
        x = Math.sqrt(x);
        y = Math.sqrt(y);
        z = Math.sqrt(z);
        return this;
    }

    Vector multiply(double a) {
        x *= a;
        y *= a;
        z *= a;
        return this;
    }

    Vector divide(double a) {
        x /= a;
        y /= a;
        z /= a;
        return this;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%.08f,%.08f,%.08f", x, y, z);
    }
}
