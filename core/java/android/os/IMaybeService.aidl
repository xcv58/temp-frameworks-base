/**
 * Copyright (c) 2007, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;
import android.os.IMaybeListener;

interface IMaybeService
{

    String getCurrentTime();

    String getAppData(String pkgName);
    
    void requestMaybeUpdates(String pkgName, String url, in IMaybeListener listener);

	void removeMaybeUpdates(String pkgName, in IMaybeListener listener);

	

	int registerUrl(String pkgName, String url, String hash);

	int getMaybeAlternative(String pkgName, String label);

	void badMaybeAlternative(String pkgName, String label, int value);

	void scoreMaybeAlternative(String pkgName, String label, String jsonString);

	void logMaybeAlternative(String pkgName, String label, String jsonString);
}

