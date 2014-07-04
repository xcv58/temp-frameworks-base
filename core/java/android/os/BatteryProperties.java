/* Copyright 2013, The Android Open Source Project
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

import android.os.Parcel;
import android.os.Parcelable;

import edu.buffalo.cse.phonelab.json.StrictJSONObject;
import edu.buffalo.cse.phonelab.json.JSONable;

/**
 * {@hide}
 */
public class BatteryProperties implements Parcelable, JSONable {
    public boolean chargerAcOnline;
    public boolean chargerUsbOnline;
    public boolean chargerWirelessOnline;
    public int batteryStatus;
    public int batteryHealth;
    public boolean batteryPresent;
    public int batteryLevel;
    public int batteryVoltage;
    public int batteryCurrentNow;
    public int batteryChargeCounter;
    public int batteryTemperature;
    public String batteryTechnology;

    /*
     * Parcel read/write code must be kept in sync with
     * frameworks/native/services/batteryservice/BatteryProperties.cpp
     */

    private BatteryProperties(Parcel p) {
        chargerAcOnline = p.readInt() == 1 ? true : false;
        chargerUsbOnline = p.readInt() == 1 ? true : false;
        chargerWirelessOnline = p.readInt() == 1 ? true : false;
        batteryStatus = p.readInt();
        batteryHealth = p.readInt();
        batteryPresent = p.readInt() == 1 ? true : false;
        batteryLevel = p.readInt();
        batteryVoltage = p.readInt();
        batteryCurrentNow = p.readInt();
        batteryChargeCounter = p.readInt();
        batteryTemperature = p.readInt();
        batteryTechnology = p.readString();
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(chargerAcOnline ? 1 : 0);
        p.writeInt(chargerUsbOnline ? 1 : 0);
        p.writeInt(chargerWirelessOnline ? 1 : 0);
        p.writeInt(batteryStatus);
        p.writeInt(batteryHealth);
        p.writeInt(batteryPresent ? 1 : 0);
        p.writeInt(batteryLevel);
        p.writeInt(batteryVoltage);
        p.writeInt(batteryCurrentNow);
        p.writeInt(batteryChargeCounter);
        p.writeInt(batteryTemperature);
        p.writeString(batteryTechnology);
    }

    private String getPlugTypeString() {
        if (chargerAcOnline) {
            return "AC";
        }
        else if (chargerUsbOnline) {
            return "USB";
        }
        else if (chargerWirelessOnline) {
            return "Wireless";
        }
        else {
            return "None";
        }
    }

    private String getBatteryStatusString() {
        switch (batteryStatus) {
            case BatteryManager.BATTERY_STATUS_CHARGING :
                return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING :
                return "Discharging";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING :
                return "NotCharging";
            case BatteryManager.BATTERY_STATUS_FULL :
                return "Full";
            case BatteryManager.BATTERY_STATUS_UNKNOWN :
            default :
                return "Unknown";
        }
    }

    private String getBatteryHealthString() {
        switch (batteryHealth) {
            case BatteryManager.BATTERY_HEALTH_GOOD :
                return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT :
                return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD :
                return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE :
                return "OverVoltage";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE :
                return "UnspecifiedFailure";
            case BatteryManager.BATTERY_HEALTH_COLD :
                return "Cold";
            case BatteryManager.BATTERY_HEALTH_UNKNOWN :
            default :
                return "Unknown";
        }
    }

    /** @hide */
    public StrictJSONObject toJSONObject() {
        return (new StrictJSONObject())
            .put("PlugType", getPlugTypeString())
            .put("Status", getBatteryStatusString())
            .put("Health", getBatteryHealthString())
            .put("Present", batteryPresent)
            .put("Level", batteryLevel)
            .put("Voltage", batteryVoltage)
            .put("CurrentNow", batteryCurrentNow)
            .put("ChargeCounter", batteryChargeCounter)
            .put("Temperature", batteryTemperature)
            .put("Technology", batteryTechnology);
    }

    public static final Parcelable.Creator<BatteryProperties> CREATOR
        = new Parcelable.Creator<BatteryProperties>() {
        public BatteryProperties createFromParcel(Parcel p) {
            return new BatteryProperties(p);
        }

        public BatteryProperties[] newArray(int size) {
            return new BatteryProperties[size];
        }
    };

    public int describeContents() {
        return 0;
    }
}
