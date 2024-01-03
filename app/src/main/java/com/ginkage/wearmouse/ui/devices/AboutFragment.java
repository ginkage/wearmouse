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

package com.ginkage.wearmouse.ui.devices;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.ginkage.wearmouse.R;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_about, container, false);

        Context context = getContext();
        PackageInfo packageInfo;
        String versionInfo = "0.01";
        try {
            packageInfo =
                    context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            versionInfo = packageInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView message = root.findViewById(R.id.message);
        message.setText(versionInfo);

        Button license = root.findViewById(R.id.license);
        license.setOnClickListener(v -> createLicenseDialog(getContext()));

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().requestFocus();
    }

    private static void createLicenseDialog(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_open_source);
        TextView message = dialog.findViewById(R.id.license_text);
        message.setText(
                getTextFromInputStream(
                        context.getResources().openRawResource(R.raw.apache_license)));
        dialog.show();
        dialog.findViewById(R.id.root_view).requestFocus();
    }

    private static String getTextFromInputStream(InputStream stream) {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream textArray = new ByteArrayOutputStream();

        try {
            int bytes;
            while ((bytes = stream.read(buffer, 0, buffer.length)) != -1) {
                textArray.write(buffer, 0, bytes);
            }
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read license", e);
        }

        try {
            return textArray.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(
                    "Unsupported encoding UTF8. This should always be supported.", e);
        }
    }
}
