/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.server.notification;

import android.util.Log;
import edu.buffalo.cse.phonelab.json.StrictJSONObject;

import java.util.Comparator;

/**
 * Sorts notifications individually into attention-relelvant order.
 */
public class NotificationComparator
        implements Comparator<NotificationRecord> {
    public final static String STATUS = "status";

    private NotificationManagerService.MaybeNotificationDelegate maybeNotificationDelegate;

    public void setMaybeNotificationDelegate(NotificationManagerService.MaybeNotificationDelegate delegate) {
        maybeNotificationDelegate = delegate;
    }

    @Override
    public int compare(NotificationRecord left, NotificationRecord right) {
        StrictJSONObject log = new StrictJSONObject(NotificationManagerService.MAYBE_TAG)
                .put(StrictJSONObject.KEY_ACTION, "compare");
        if (maybeNotificationDelegate != null) {
            String leftPkg = left.sbn.getPackageName();
            String rightPkg = right.sbn.getPackageName();
            log.put("leftPkg", leftPkg);
            if (leftPkg != null && !leftPkg.equals(rightPkg)) {
                float lScore = maybeNotificationDelegate.getPkgScore(leftPkg);
                float rScore = maybeNotificationDelegate.getPkgScore(rightPkg);
                log.put("rightPkg", rightPkg);
                log.put("lScore", lScore);
                log.put("rScore", rScore);
                if (Math.abs(lScore - rScore) > 0.0128f) {
                    log.put(STATUS, "success");
                    log.log();
                    return -1 * Float.compare(lScore, rScore);
                } else {
                    log.put(STATUS, "equal");
                }
            } else {
                log.put(STATUS, "abort");
            }
        } else {
            log.put(STATUS, "cancel");
        }
        log.log();

        final int leftPackagePriority = left.getPackagePriority();
        final int rightPackagePriority = right.getPackagePriority();
        if (leftPackagePriority != rightPackagePriority) {
            // by priority, high to low
            return -1 * Integer.compare(leftPackagePriority, rightPackagePriority);
        }

        final int leftScore = left.sbn.getScore();
        final int rightScore = right.sbn.getScore();
        if (leftScore != rightScore) {
            // by priority, high to low
            return -1 * Integer.compare(leftScore, rightScore);
        }

        final float leftPeople = left.getContactAffinity();
        final float rightPeople = right.getContactAffinity();
        if (leftPeople != rightPeople) {
            // by contact proximity, close to far
            return -1 * Float.compare(leftPeople, rightPeople);
        }

        // then break ties by time, most recent first
        return -1 * Long.compare(left.getRankingTimeMs(), right.getRankingTimeMs());
    }
}
