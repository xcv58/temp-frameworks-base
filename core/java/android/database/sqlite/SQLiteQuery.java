/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.database.sqlite;

import java.util.Arrays;

import android.database.CursorWindow;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.util.Log;

import edu.buffalo.cse.phonelab.json.StrictJSONObject;

/**
 * Represents a query that reads the resulting rows into a {@link SQLiteQuery}.
 * This class is used by {@link SQLiteCursor} and isn't useful itself.
 * <p>
 * This class is not thread-safe.
 * </p>
 */
public final class SQLiteQuery extends SQLiteProgram {
    private static final String TAG = "SQLiteQuery";

    private final CancellationSignal mCancellationSignal;

    SQLiteQuery(SQLiteDatabase db, String query, CancellationSignal cancellationSignal) {
        super(db, query, null, cancellationSignal);

        mCancellationSignal = cancellationSignal;
    }

    /**
     * Reads rows into a buffer.
     *
     * @param window The window to fill into
     * @param startPos The start position for filling the window.
     * @param requiredPos The position of a row that MUST be in the window.
     * If it won't fit, then the query should discard part of what it filled.
     * @param countAllRows True to count all rows that the query would
     * return regardless of whether they fit in the window.
     * @return Number of rows that were enumerated.  Might not be all rows
     * unless countAllRows is true.
     *
     * @throws SQLiteException if an error occurs.
     * @throws OperationCanceledException if the operation was canceled.
     */
    int fillWindow(CursorWindow window, int startPos, int requiredPos, boolean countAllRows) {
        acquireReference();
        int numRows = 0;
        try {
            window.acquireReference();
            try {
                long startTime = System.nanoTime();
                numRows = getSession().executeForCursorWindow(getSql(), getBindArgs(),
                        window, startPos, requiredPos, countAllRows, getConnectionFlags(),
                        mCancellationSignal);
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);

                /**
                 * PhoneLab
                 *
                 * {
                 * "Category": "SQLite",
                 * "SubCategory": "Instumentation",
                 * "Tag": "SQLite-Instrumentation-PhoneLab",
                 * "Action": "SELECT",
                 * "Description": "Logging SELECT queries."
                 * }
                 */
                (new StrictJSONObject("SQLite-Query-PhoneLab"))
                  .put(StrictJSONObject.KEY_ACTION, "SELECT")
                  .put("Arguments", Arrays.toString(getBindArgs()))
                  .put("Results", getSql())
                  .put("Time", duration)
                  .put("Rows returned", numRows)
                  .log();

            } catch (NullPointerException e) {
                System.out.println("Encountered NullPointerException: "+e.getMessage());
            }catch (SQLiteDatabaseCorruptException ex) {
                onCorruption();
                throw ex;
            } catch (SQLiteException ex) {
                Log.e(TAG, "exception: " + ex.getMessage() + "; query: " + getSql());
                throw ex;
            } finally {
                window.releaseReference();
            }
        } finally {
            releaseReference();
        }
        return numRows;
    }

    @Override
    public String toString() {
        return "SQLiteQuery: " + getSql();
    }
}
