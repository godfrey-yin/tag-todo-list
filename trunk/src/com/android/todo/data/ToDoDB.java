//class made by Teo ( www.teodorfilimon.com ). More about the app in readme.txt

package com.android.todo.data;

import java.io.File;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.todo.AlarmReceiver;
import com.android.todo.BootReceiver;
import com.android.todo.R;
import com.android.todo.TagToDoList;
import com.android.todo.Utils;

/**
 * This class handles all the interactions with the database. The database
 * contains 2 tables. One of them is called "tags", the other one is called
 * "entries". An entry is what the user visually perceives as a task.
 */
public final class ToDoDB extends ADB {

  // key for the written note (if any)
  public static final String KEY_WRITTEN_NOTE = "writtennote";

  // keys useful for the subtasks feature
  public static final String KEY_DEPTH = "depth";
  public static final String KEY_SUPERTASK = "supertask";
  public static final String KEY_SUBTASKS = "subtasks";
  // some flags to see if there are certain types of notes
  public static final String KEY_NOTE_IS_WRITTEN = "iswrittennote";
  public static final String KEY_NOTE_IS_GRAPHICAL = "isgraphicalnote";
  public static final String KEY_NOTE_IS_AUDIO = "isaudionote";
  // if 1 it means the task is collapsed
  public static final String KEY_COLLAPSED = "iscollapsed";
  
  

  private static final String DB_TAG_TABLE = "tags";
  private DatabaseHelper mDbHelper;
  private static String PRIORITY_ORDER_TOKEN = "";
  private static String ALPHABET_ORDER_TOKEN = "";
  private static String DUEDATE_ORDER_TOKEN = "";
  private static boolean FLAG_UPDATED = false;

  private class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context) {
      super(context, DB_NAME, null, DATABASE_VERSION);
      mCtx = context;
      res = context.getResources();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      Resources r = res;

      // creating the tag and entry tables and inserting a default tag
      db.execSQL("CREATE TABLE " + DB_TAG_TABLE + " (" + KEY_ROWID
          + " integer primary key autoincrement, " + KEY_NAME
          + " text not null);");
      db.execSQL("INSERT INTO " + DB_TAG_TABLE + " (" + KEY_NAME
          + ") VALUES ('" + r.getString(R.string.default_content_tag1) + "')");
      db.execSQL("INSERT INTO " + DB_TAG_TABLE + " (" + KEY_NAME
          + ") VALUES ('" + r.getString(R.string.default_content_tag2) + "')");

      db.execSQL("CREATE TABLE " + DB_ENTRY_TABLE + " (" + KEY_ROWID
          + " integer primary key autoincrement, " + KEY_NAME
          + " text not null, " + KEY_STATUS + " integer, " + KEY_PARENT
          + " text not null);");
      db.execSQL("INSERT INTO " + DB_ENTRY_TABLE + " (" + KEY_NAME + ", "
          + KEY_STATUS + ", " + KEY_PARENT + ") VALUES ('"
          + r.getString(R.string.default_content_entry4) + "',0,'"
          + r.getString(R.string.default_content_tag2) + "')");
      db.execSQL("INSERT INTO " + DB_ENTRY_TABLE + " (" + KEY_NAME + ", "
          + KEY_STATUS + ", " + KEY_PARENT + ") VALUES ('"
          + r.getString(R.string.default_content_entry3) + "',0,'"
          + r.getString(R.string.default_content_tag1) + "')");
      db.execSQL("INSERT INTO " + DB_ENTRY_TABLE + " (" + KEY_NAME + ", "
          + KEY_STATUS + ", " + KEY_PARENT + ") VALUES ('"
          + r.getString(R.string.default_content_entry2) + "',0,'"
          + r.getString(R.string.default_content_tag1) + "')");
      db.execSQL("INSERT INTO " + DB_ENTRY_TABLE + " (" + KEY_NAME + ", "
          + KEY_STATUS + ", " + KEY_PARENT + ") VALUES ('"
          + r.getString(R.string.default_content_entry1) + "',0,'"
          + r.getString(R.string.default_content_tag1) + "')");

      // performing subsequent upgrades to this model
      onUpgrade(db, 73, 74);
      onUpgrade(db, 74, 75);
      onUpgrade(db, 75, 76);
      onUpgrade(db, 76, 78);
      onUpgrade(db, 78, 79);
      onUpgrade(db, 81, 82);
      onUpgrade(db, 85, 86);
      onUpgrade(db, 91, 92);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      // upgrade to db v74 (corresponding to app v1.2.0) or bigger;
      // 4 columns need to be added for entry dates (and other possible
      // future extra options).
      if (oldVersion < 74 && newVersion >= 74) {
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD "
              + KEY_EXTRA_OPTIONS + " INTEGER");
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_DUE_YEAR
              + " INTEGER");
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_DUE_MONTH
              + " INTEGER");
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_DUE_DATE
              + " INTEGER");
        } catch (Exception e) {
          // if we are here, it means there has been a downgrade and
          // then an upgrade, we don't need to delete the columns, but
          // we need to prevent an actual exception
        }
      }

      // upgrade to db v75 (corresponding to app v1.3.0) or bigger;
      // a column needs to be added for written notes
      if (oldVersion < 75 && newVersion >= 75) {
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD "
              + KEY_WRITTEN_NOTE + " TEXT");
        } catch (Exception e) {
        }
      }

      // upgrade to db v76 (corresponding to app v1.5.0) or bigger;
      // one column needs to be added for task priority
      if (oldVersion < 76 && newVersion >= 76) {
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_PRIORITY
              + " INTEGER DEFAULT 50");
        } catch (Exception e) {
        }
      }

      // upgrade to db v78 (corresponding to app v1.6.2) or bigger;
      // 2 columns need to be added for entry times
      if (oldVersion < 78 && newVersion >= 78) {
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_DUE_HOUR
              + " INTEGER");
        } catch (Exception e) {
        }

        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_DUE_MINUTE
              + " INTEGER");
        } catch (Exception e) {
        }
      }

      // upgrade to db v79 (corresponding to app v1.6.5) or bigger;
      // 1 column needs to be added for the due day of the week
      if (oldVersion < 79 && newVersion >= 79) {
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD "
              + KEY_DUE_DAY_OF_WEEK + " INTEGER DEFAULT -1");
        } catch (Exception e) {
        }
      }

      // upgrade to db v82 (corresponding to app v1.7.0) or bigger;
      // 1 column needs to be added for the depth of the task (part of the
      // subtask feature)
      // 1 column needs to be added for the parent of the eventual subtask
      // (not
      // the tag, but the supertask)
      // 1 column needs to be added for the number of subtasks of a
      // certain task
      // (just primary level)
      if (oldVersion < 82 && newVersion >= 82) {
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_DEPTH
              + " INTEGER DEFAULT 0");
        } catch (Exception e) {
        }
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_SUPERTASK
              + " TEXT");
        } catch (Exception e) {
        }
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_SUBTASKS
              + " INTEGER DEFAULT 0");
        } catch (Exception e) {
        }
      }

      // upgrade to db v86 (corresponding to app v1.8.0) or bigger;
      // 3 columns have been added, as boolean flags (but still int), to see
      // whether there are certain types of notes
      if (oldVersion < 86 && newVersion >= 86) {
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD "
              + KEY_NOTE_IS_WRITTEN + " INTEGER DEFAULT 0");
        } catch (Exception e) {
        }
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD "
              + KEY_NOTE_IS_GRAPHICAL + " INTEGER DEFAULT 0");
        } catch (Exception e) {
        }
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD "
              + KEY_NOTE_IS_AUDIO + " INTEGER DEFAULT 0");
        } catch (Exception e) {
        }
        final Cursor c = db.query(DB_ENTRY_TABLE, new String[] { KEY_NAME,
            KEY_WRITTEN_NOTE }, null, null, null, null, null);
        if (c.getCount() > 0) {
          c.moveToFirst();
          do {
            final String taskName = c.getString(0);

            // checking for a written note
            final String writtenNote = c.getString(1);
            if (!"".equals(writtenNote) && writtenNote != null) {
              final ContentValues args = new ContentValues();
              args.put(KEY_NOTE_IS_WRITTEN, 1);
              db.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + taskName
                  + "'", null);
            }

            // checking for a graphical note
            boolean found = true;
            try {
              mCtx.openFileInput(Utils.getImageName(taskName));
            } catch (Exception e) {
              found = false;
            }
            if (found) {
              final ContentValues args = new ContentValues();
              args.put(KEY_NOTE_IS_GRAPHICAL, 1);
              db.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + taskName
                  + "'", null);
            }

            // checking for an audio note
            found = true;
            try {
              mCtx.openFileInput(Utils.getAudioName(taskName));
            } catch (Exception e) {
              found = false;
            }
            if (found) {
              final ContentValues args = new ContentValues();
              args.put(KEY_NOTE_IS_AUDIO, 1);
              db.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + taskName
                  + "'", null);
            }
          } while (c.moveToNext());
        }
        c.close();
      }

      // upgrade to db v92 (corresponding to app v1.9.0) or bigger;
      // fixing a legacy bug, some people might not have the KEY_SUBTASKS column
      if (oldVersion < 92 && newVersion >= 92) {
        try {
          db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_COLLAPSED
              + " INTEGER DEFAULT 0");
        } catch (Exception e) {
        }
        
        Cursor c = null;
        try {
          c = db.query(DB_ENTRY_TABLE, new String[] { KEY_SUBTASKS }, null,
              null, null, null, null);
        } catch (Exception e) {
          try {
            db.execSQL("ALTER TABLE " + DB_ENTRY_TABLE + " ADD " + KEY_SUBTASKS
                + " INTEGER DEFAULT 0");
          } catch (Exception e2) {
          }
        } finally {
          if (c != null) {
            c.close();
          }
        }
      }

      // must be last:
      FLAG_UPDATED = true;
    }
  }

  /**
   * Constructor - takes the context to allow the database to be opened/created
   * 
   * @param ctx
   *          the Context within which to work
   */
  public ToDoDB(Context ctx) {
    mCtx = ctx;
  }

  /**
   * Open the database. If it cannot be opened, try to create a new instance of
   * the database. If it cannot be created, throw an exception to signal the
   * failure
   * 
   * @return this (self reference, allowing this to be chained in an
   *         initialization call)
   * @throws SQLException
   *           if the database could be neither opened or created
   */
  public ToDoDB open() throws SQLException {
    mDbHelper = new DatabaseHelper(mCtx);
    mDb = mDbHelper.getWritableDatabase();
    if (FLAG_UPDATED) {
      BootReceiver.setOldAlarms(mCtx, this);
    }
    return this;
  }

  public void close() {
    mDbHelper.close();
  }

  /**
   * Create a new tag using the provided text. If a tag with the given name
   * exists it will not be created.
   * 
   * @param title
   *          the name of the ToDo List tag, as it will appear visually
   * @return true if insertion went ok, false if such a tag already exists
   */
  public boolean createTag(final String tagName) {
    final Cursor c = mDb.query(DB_TAG_TABLE, new String[] { KEY_NAME },
        KEY_NAME + " = '" + tagName + "'", null, null, null, null);
    if (c.getCount() > 0) {
      c.close();
      return false;
    }

    // inserting the actual tag
    ContentValues args = new ContentValues();
    args.put(KEY_NAME, tagName);
    mDb.insert(DB_TAG_TABLE, null, args);
    c.close();
    return true;
  }

  /**
   * Creates a database backup on the SD card
   */
  public static final void createBackup() {
    try {
      // checking if the Tag-ToDo folder exists on the sdcard
      final File f = new File("/sdcard/Tag-ToDo_data/");
      if (f.exists() == false) {
        try {
          f.mkdirs();
        } catch (Exception e) {

        }
      }

      Utils.copy(new File("/data/data/com.android.todo/databases"), new File(
          "/sdcard/Tag-ToDo_data/database_backup"));
    } catch (Exception e) {
      Utils.showDialog(R.string.notification, R.string.backup_fail, mCtx);
    }
  }

  /**
   * Create a new entry with the provided name in the provided tag
   * 
   * @param tagName
   *          the parent tag
   * @param task
   *          the newly created task
   * @return null if such an entry is inexistent in the to-do list or a String
   *         representing the tag where such an entry was found
   */
  public String createEntry(final String tagName, final String task) {
    // checking for duplicate entries
    final Cursor c = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_NAME,
        KEY_PARENT }, KEY_NAME + " = '" + task + "'", null, null, null, null);
    if (c.getCount() > 0) {
      c.moveToFirst();
      String s = c.getString(c.getColumnIndexOrThrow(KEY_PARENT));
      c.close();
      return s;
    }

    // inserting the actual entry
    ContentValues args = new ContentValues();
    args.put(KEY_NAME, task);
    args.put(KEY_STATUS, 0);
    args.put(KEY_PARENT, tagName);
    mDb.insert(DB_ENTRY_TABLE, null, args);
    c.close();
    return null;
  }

  /**
   * Deletes the attached alarm, if any
   * 
   * @param entryName
   */
  public void deleteAlarm(String entryName) {
    if (isDueTimeSet(entryName)) {
      ((AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE))
          .cancel(PendingIntent.getBroadcast(mCtx, entryName.hashCode(),
              Utils.getAlarmIntent(new Intent(mCtx, AlarmReceiver.class),
                  entryName), 0));
    }
  }

  /**
   * Delete the tag with the given name and all the tasks in it
   * 
   * @param name
   *          name of the tag to be deleted
   */
  public void deleteTag(final String name) {
    mDb.delete(DB_TAG_TABLE, KEY_NAME + "='" + name + "'", null);
    final Cursor entries = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_NAME,
        KEY_PARENT }, KEY_PARENT + " = '" + name + "'", null, null, null, null);
    if (entries.getCount() > 0) {
      entries.moveToFirst();
      do {
        final int entryName = entries.getColumnIndexOrThrow(KEY_NAME);
        deleteEntry(entries.getString(entryName));
      } while (entries.moveToNext());
    }
    entries.close();
  }

  /**
   * Return a Cursor over the list of all tags in the database
   * 
   * @return Cursor over all tags
   */
  public Cursor getAllTags() {
    return mDb.query(DB_TAG_TABLE, new String[] { KEY_ROWID, KEY_NAME }, null,
        null, null, null, ALPHABET_ORDER_TOKEN != "" ? ALPHABET_ORDER_TOKEN
            .substring(2) : "");
  }

  /**
   * Return a Cursor over the list of tasks in a certain tag
   * 
   * @param tag
   *          for which to get all the tasks. If null, all the tasks from all
   *          the tags will be returned.
   * @param depth
   *          of tasks (depth 0 is a task, >0 is a subtask). If -1, all tasks,
   *          no matter the depth, will be returned.
   * @param superTask
   *          filters to subtasks of a certain supertask. If null, this filter
   *          won't exist.
   * @return Cursor over all tasks in a tag
   */
  public final Cursor getTasks(final String tag, final int depth,
      final String superTask) {

    return mDb
        .query(
            DB_ENTRY_TABLE,
            new String[] { KEY_ROWID, KEY_NAME, KEY_STATUS, KEY_PARENT,
                KEY_SUBTASKS },
            ((tag != null ? KEY_PARENT + " = '" + tag + "' " : "1=1 ")
                + (depth != -1 ? "AND " + KEY_DEPTH + " = " + depth + " " : "") + (superTask != null ? "AND "
                + KEY_SUPERTASK + " = '" + superTask + "' "
                : ""))
                + "ORDER BY "
                + KEY_STATUS
                + " DESC"
                + PRIORITY_ORDER_TOKEN
                + DUEDATE_ORDER_TOKEN + ALPHABET_ORDER_TOKEN, null, null, null,
            null);
  }

  /**
   * Returns the value of the given key on the given task
   * 
   * @param task
   * @param key
   * @return
   */
  public final int getFlag(final String task, final String key) {
    final Cursor tasks = mDb.query(DB_ENTRY_TABLE,
        new String[] { KEY_NAME, key }, KEY_NAME + "='" + task + "'", null,
        null, null, null);
    tasks.moveToFirst();
    final int value = tasks.getInt(1);
    tasks.close();
    return value;
  }

  /**
   * Return the priority of the given task
   * 
   * @param entryName
   * @return priority
   */
  public int getPriority(final String entryName) {
    final Cursor entry = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_ROWID,
        KEY_NAME, KEY_PRIORITY }, KEY_NAME + " = '" + entryName + "'", null,
        null, null, null);
    // for now, assuming we have a task named like this :)
    entry.moveToFirst();
    try {
      int p = entry.getInt(entry.getColumnIndexOrThrow(KEY_PRIORITY));
      entry.close();
      return p;
    } catch (Exception e) {
      entry.close();
      return 50;
    }
  }

  /**
   * Return a Cursor over the list of unchecked entries with due dates which
   * expire 'today' or earlier
   * 
   * @return Cursor
   */
  public Cursor getDueEntries() {
    GregorianCalendar gc = new GregorianCalendar();
    final int year = gc.get(GregorianCalendar.YEAR);
    final int month = gc.get(GregorianCalendar.MONTH);
    return mDb.query(DB_ENTRY_TABLE, new String[] { KEY_ROWID, KEY_NAME,
        KEY_STATUS }, KEY_EXTRA_OPTIONS + " = 1 AND " + KEY_STATUS
        + " = 0 AND (" + KEY_DUE_YEAR + " < " + year + " OR (" + KEY_DUE_YEAR
        + " = " + year + " AND (" + KEY_DUE_MONTH + " < " + month + " OR ("
        + KEY_DUE_MONTH + " = " + month + " AND " + KEY_DUE_DATE + " <= "
        + gc.get(GregorianCalendar.DAY_OF_MONTH) + "))))", null, null, null,
        null);
  }

  /**
   * Return a Cursor over the list of unchecked entries with due dates, from the
   * past or the future
   * 
   * @return Cursor
   */
  public Cursor getAllDueEntries() {
    return mDb.query(DB_ENTRY_TABLE, new String[] { KEY_ROWID, KEY_NAME,
        KEY_STATUS, KEY_DUE_YEAR, KEY_DUE_MONTH, KEY_DUE_DATE },
        KEY_EXTRA_OPTIONS + " = 1 AND " + KEY_STATUS + " = 0", null, null,
        null, null);
  }

  /**
   * Returns the written note for the given entry.
   * 
   * @param entryName
   * @return written note
   */
  public String getWrittenNote(String entryName) {
    final Cursor entry = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_ROWID,
        KEY_NAME, KEY_WRITTEN_NOTE }, KEY_NAME + " = '" + entryName + "'",
        null, null, null, null);
    // for now, assuming we have a task named like this :)
    entry.moveToFirst();
    String note = entry
        .getString(entry.getColumnIndexOrThrow(KEY_WRITTEN_NOTE));
    entry.close();
    return note;
  }

  /**
   * Return the count of unchecked entries in a tag
   * 
   * @param tag
   *          for which to fetch all the to-do list entries
   * @return number of unchecked entries (tasks left to do)
   */
  public int countUncheckedEntries(String tag) {
    final Cursor c = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_ROWID,
        KEY_NAME, KEY_STATUS, KEY_PARENT }, KEY_PARENT + " = '" + tag + "'",
        null, null, null, null);
    final int value = c.getColumnIndexOrThrow(KEY_STATUS);
    int unchecked = 0;
    if (c.getCount() > 0) {
      c.moveToFirst();
      do {
        if (c.getInt(value) == 0) {
          unchecked += 1;
        }
      } while (c.moveToNext());
    }
    c.close();
    return unchecked;
  }

  /**
   * Return the count of unchecked entries in ALL tags
   * 
   * @return number of unchecked entries (tasks left to do)
   */
  public final int countUncheckedEntries() {
    final Cursor c = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_ROWID,
        KEY_NAME, KEY_STATUS, KEY_PARENT }, KEY_STATUS + " = 0", null, null,
        null, null);
    final int count = c.getCount();
    c.close();
    return count;
  }

  /**
   * Sets the given flag on the given task with the given value.
   * 
   * @param task
   * @param key
   * @param value
   */
  public final void setFlag(final String task, final String key, final int value) {
    final ContentValues args = new ContentValues();
    args.put(key, value);
    mDb.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + task + "'", null);
  }

  /**
   * Update the tag using the details provided.
   * 
   * @param tagName
   *          name of the tag to update
   * @param newName
   * @return true if the tag was successfully updated, false otherwise
   */
  public boolean updateTag(final String tagName, final String newName) {
    final ContentValues args1 = new ContentValues();
    args1.put(KEY_PARENT, newName);
    mDb
        .update(DB_ENTRY_TABLE, args1, KEY_PARENT + " = '" + tagName + "'",
            null);
    final ContentValues args2 = new ContentValues();
    args2.put(KEY_NAME, newName);
    return mDb.update(DB_TAG_TABLE, args2, KEY_NAME + " = '" + tagName + "'",
        null) > 0;
  }

  /**
   * Wrapper to the second definition of the updateEntry method
   * 
   * @param entryName
   * @param checked
   * @return
   */
  public void updateEntry(String entryName, boolean checked) {
    updateEntry(entryName, checked, null); // actual task
    updateEntry(entryName, checked, Boolean.TRUE); // subtasks
    updateEntry(entryName, checked, Boolean.FALSE); // supertasks too

    if (checked) {
      // removing entries if their number surpasses a certain limit;
      // the removed entries can only be checked ones (finished)
      Cursor checkedC = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_NAME,
          KEY_STATUS }, KEY_STATUS + " = 1", null, null, null, null);
      SharedPreferences settings = mCtx.getSharedPreferences(
          TagToDoList.PREFS_NAME, 50);
      int limit = settings.getInt("listSizeLimit", 50);
      if (checkedC.getCount() >= limit) {
        int counter = checkedC.getCount();
        int name = checkedC.getColumnIndexOrThrow(KEY_NAME);
        checkedC.moveToFirst();
        do {
          deleteEntry(checkedC.getString(name));
          checkedC.moveToNext();
          counter -= 1;
        } while (counter > limit);
      } else if (checkedC.getCount() == limit - 1
          && !(settings.getBoolean("checkedTasksLimitAware", false))) {
        Utils.showDialog(R.string.notification,
            R.string.notification_checked_tasks_limit, ToDoDB.mCtx);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("checkedTasksLimitAware", true);
        editor.commit();
      }
      checkedC.close();

      // also need to remove attached alarms, if any
      deleteAlarm(entryName);
    } else {
      if (isDueTimeSet(entryName)) {
        // if it is unchecked and has an alarm, it needs to be remade
        PendingIntent pi = PendingIntent.getBroadcast(mCtx, entryName
            .hashCode(), Utils.getAlarmIntent(new Intent(mCtx,
            AlarmReceiver.class), entryName), 0);
        AlarmManager alarmManager = (AlarmManager) mCtx
            .getSystemService(Context.ALARM_SERVICE);
        if (isDueDateSet(entryName)) {// single occurence
          alarmManager.set(AlarmManager.RTC_WAKEUP, Utils.getTimeMillis(
              getDueTime(entryName), getDueDate(entryName),
              getDueDayOfWeek(entryName)), pi);
        } else {// daily or weekly
          int dayOfWeek = getDueDayOfWeek(entryName);
          alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Utils
              .getTimeMillis(getDueTime(entryName), -1, dayOfWeek),
              86400000L * (dayOfWeek > -1 ? 7 : 1), pi);
        }
      }
    }
  }

  /**
   * Updates the specified entry with the specified checked status. Can be
   * applied for subtasks and supertasks as well.
   * 
   * @param entryName
   *          the name of the entry to be modified
   * @param checked
   *          the new checked/unchecked status of that task
   * @param goDown
   *          applies the same status to subtasks, if true. If false, it goes
   *          up, to supertasks. If null, only refers to the present task.
   * @return true if successfully updated
   */
  private final void updateEntry(String entryName, boolean checked,
      Boolean goDown) {
    if (goDown == null) {
      ContentValues args = new ContentValues();
      args.put(KEY_STATUS, checked ? 1 : 0);
      mDb.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + entryName + "'",
          null);
    } else if (goDown.equals(Boolean.TRUE)) {
      // applying the same to subtasks
      Cursor c = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_NAME,
          KEY_SUPERTASK }, KEY_SUPERTASK + "='" + entryName + "'", null, null,
          null, null);
      if (c.getCount() > 0) {
        final int name = c.getColumnIndex(KEY_NAME);
        c.moveToFirst();
        do {
          updateEntry(c.getString(name), checked, null);
          updateEntry(c.getString(name), checked, Boolean.TRUE);
        } while (c.moveToNext());
      }
      c.close();
    } else {
      // must do the same with supertasks
      Cursor c = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_NAME,
          KEY_SUPERTASK }, KEY_NAME + "='" + entryName + "'", null, null, null,
          null);
      if (c.getCount() > 0) {
        c.moveToFirst();
        if (!checked) {
          updateEntry(c.getString(c.getColumnIndex(KEY_SUPERTASK)), false, null);
          updateEntry(c.getString(c.getColumnIndex(KEY_SUPERTASK)), false,
              Boolean.FALSE);
        } else {
          // must only check the supertask if all its subtasks are
          // checked
          Cursor subTasksOfSuperTaskC = mDb.query(DB_ENTRY_TABLE, new String[] {
              KEY_NAME, KEY_SUPERTASK, KEY_STATUS }, KEY_SUPERTASK + "='"
              + c.getString(c.getColumnIndex(KEY_SUPERTASK)) + "'", null, null,
              null, null);
          if (subTasksOfSuperTaskC.getCount() > 0) {
            final int status = subTasksOfSuperTaskC.getColumnIndex(KEY_STATUS);
            boolean allChecked = true;
            subTasksOfSuperTaskC.moveToFirst();
            do {
              if (subTasksOfSuperTaskC.getInt(status) == 0) {
                allChecked = false;
              }
            } while (subTasksOfSuperTaskC.moveToNext());
            if (allChecked) {
              updateEntry(c.getString(c.getColumnIndex(KEY_SUPERTASK)), true,
                  null);
              updateEntry(c.getString(c.getColumnIndex(KEY_SUPERTASK)), true,
                  Boolean.FALSE);
            }
          }
          subTasksOfSuperTaskC.close();
        }
      }
      c.close();
    }
  }

  /**
   * Updates the specified entry with the specified new name.
   * 
   * @param entryName
   *          The name of the entry to be modified
   * @param newName
   *          The new name
   * @return true if successfully updated
   */
  public void updateEntry(String entryName, String newName) {
    ContentValues args = new ContentValues();
    args.put(KEY_NAME, newName);
    mDb.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + entryName + "'", null);
    final Cursor subtasks = getTasks(null, 1, entryName);
    if (subtasks.getCount() > 0) {
      final int name = subtasks.getColumnIndex(KEY_NAME);
      subtasks.moveToFirst();
      do {
        try {
          setSuperTask(subtasks.getString(name), newName);
        } catch (Exception e) {
          // an exception won't be thrown here
        }
      } while (subtasks.moveToNext());
    }
    subtasks.close();
  }

  /**
   * Updates (Moves) the specified entry with a new parent (tag). Also takes
   * care of moving the subtasks.
   * 
   * @param task
   * @param newParent
   *          the new parent name
   * @param depth
   *          the intended depth of the task
   * @return true if successfully updated
   */
  public void updateEntryParent(final String task, final String newParent,
      final int depth) {
    final Cursor subtasks = getTasks(null, -1, task);
    if (subtasks.getCount() > 0) {
      final int name = subtasks.getColumnIndex(KEY_NAME);
      subtasks.moveToFirst();
      do {
        updateEntryParent(subtasks.getString(name), newParent, depth + 1);
      } while (subtasks.moveToNext());
    }
    subtasks.close();
    final ContentValues args = new ContentValues();
    args.put(KEY_PARENT, newParent);
    args.put(KEY_DEPTH, depth);
    mDb.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + task + "'", null);
  }

  /**
   * Updates the specified entry with the specified due date
   * 
   * @param entryName
   * @param year
   * @param month
   * @param date
   * @return true if successfully updated
   */
  public boolean updateEntry(String entryName, int year, int month, int date) {
    ContentValues args = new ContentValues();
    args.put(KEY_DUE_YEAR, year);
    args.put(KEY_DUE_MONTH, month);
    args.put(KEY_DUE_DATE, date);
    // args.put(KEY_EXTRA_OPTIONS, 1);
    return mDb.update(DB_ENTRY_TABLE, args,
        KEY_NAME + " = '" + entryName + "'", null) > 0;
  }

  /**
   * Updates the specified entry with the specified due time
   * 
   * @param entryName
   * @param year
   * @param month
   * @param date
   * @return true if successfully updated
   */
  public boolean updateEntry(String entryName, int hour, int minute) {
    ContentValues args = new ContentValues();
    args.put(KEY_DUE_HOUR, hour);
    args.put(KEY_DUE_MINUTE, minute);
    return mDb.update(DB_ENTRY_TABLE, args,
        KEY_NAME + " = '" + entryName + "'", null) > 0;
  }

  /**
   * Attaches a day of the week to a periodic task
   * 
   * @param entryName
   * @param dayOfWeek
   */
  public void updateEntry(String entryName, int dayOfWeek) {
    ContentValues args = new ContentValues();
    args.put(KEY_DUE_DAY_OF_WEEK, dayOfWeek);
    mDb.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + entryName + "'", null);
  }

  /**
   * Sets the necessity of extra options (due date) for an entry. If set to
   * false, the actual date isn't deleted, so it's reusable.
   * 
   * @param entryName
   * @param b
   * @return
   */
  public boolean setDueDate(String entryName, boolean b) {
    final Cursor c = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_NAME,
        KEY_EXTRA_OPTIONS }, KEY_NAME + " = '" + entryName + "'", null, null,
        null, null);
    c.moveToFirst();
    ContentValues args = new ContentValues();
    // the due date is given by the last bit of KEY_EXTRA_OPTIONS
    args.put(KEY_EXTRA_OPTIONS, b ? (c.getInt(c
        .getColumnIndex(KEY_EXTRA_OPTIONS)) | 1) : (c.getInt(c
        .getColumnIndex(KEY_EXTRA_OPTIONS)) & 2));
    c.close();
    return mDb.update(DB_ENTRY_TABLE, args,
        KEY_NAME + " = '" + entryName + "'", null) > 0;
  }

  /**
   * Sets the necessity of extra options (due time) for an entry. If set to
   * false, the actual time isn't deleted, so it's reusable.
   * 
   * @param entryName
   * @param b
   * @return
   */
  public boolean setDueTime(String entryName, boolean b) {
    Cursor c = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_NAME,
        KEY_EXTRA_OPTIONS }, KEY_NAME + " = '" + entryName + "'", null, null,
        null, null);
    c.moveToFirst();
    ContentValues args = new ContentValues();
    // the due date is given by the last bit of KEY_EXTRA_OPTIONS
    args.put(KEY_EXTRA_OPTIONS, b ? (c.getInt(c
        .getColumnIndex(KEY_EXTRA_OPTIONS)) | 2) : (c.getInt(c
        .getColumnIndex(KEY_EXTRA_OPTIONS)) & 5));
    c.close();
    return mDb.update(DB_ENTRY_TABLE, args,
        KEY_NAME + " = '" + entryName + "'", null) > 0;
  }

  /**
   * Sets the priority for an entry
   * 
   * @param entryName
   * @param priority
   * @return
   */
  public boolean setPriority(String entryName, int priority) {
    ContentValues args = new ContentValues();
    args.put(KEY_PRIORITY, priority);
    return mDb.update(DB_ENTRY_TABLE, args,
        KEY_NAME + " = '" + entryName + "'", null) > 0;
  }

  /**
   * Sets a written (text) note for the given entry
   * 
   * @param task
   * @param note
   * @return
   */
  public final void setWrittenNote(String task, String note) {
    final ContentValues args = new ContentValues();
    args.put(KEY_WRITTEN_NOTE, note);
    mDb.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + task + "'", null);
    setFlag(task, KEY_NOTE_IS_WRITTEN, !"".equals(note) && note != null ? 1 : 0);
  }

  /**
   * Sets whether the functions of this class which return Cursors with entries
   * should also order them by priority
   * 
   * @param order
   * @param ascending
   *          (if false it will be descending)
   */
  public final static void setPriorityOrder(boolean order, boolean ascending) {
    PRIORITY_ORDER_TOKEN = order ? ", " + KEY_PRIORITY
        + (ascending ? " DESC" : "") : "";
  }

  /**
   * Sets whether the functions of this class which return Cursors with entries
   * should also order them alphabetically
   * 
   * @param order
   * @param ascending
   *          (if false it will be descending)
   */
  public final static void setAlphabeticalOrder(boolean order, boolean ascending) {
    ALPHABET_ORDER_TOKEN = order ? ", " + KEY_NAME + (ascending ? " DESC" : "")
        : "";
  }

  /**
   * Sets whether the functions of this class which return Cursors with entries
   * should also order them by due date
   * 
   * @param order
   * @param ascending
   *          (if false it will be descending)
   */
  public final static void setDueDateOrder(boolean order, boolean ascending) {
    String direction = ascending ? " DESC" : "";
    DUEDATE_ORDER_TOKEN = order ? ", " + KEY_DUE_YEAR + direction + ", "
        + KEY_DUE_MONTH + direction + ", " + KEY_DUE_DATE + direction : "";
  }

  /**
   * The given task will be subordinate to the new supertask. The supertask will
   * also be made false. ASSUMPTION: this function is called when the task isn't
   * checked
   * 
   * @param task
   * @param superTask
   * @throws Exception
   *           if a task eventually is moved 'under itself'
   */
  public void setSuperTask(final String task, final String superTask)
      throws Exception {
    // checking subtasks first, we can't move a subtask
    if (task.equals(superTask)) {
      throw new Exception();
    }
    String curTask = superTask;
    Cursor c;
    while ((c = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_NAME,
        KEY_SUPERTASK }, KEY_NAME + " = '" + curTask + "'", null, null, null,
        null)).getCount() > 0) {
      c.moveToFirst();
      if (task.equals(curTask = c.getString(1))) {
        throw new Exception();
      }
    }

    c = mDb.query(DB_ENTRY_TABLE, new String[] { KEY_NAME, KEY_DEPTH,
        KEY_SUBTASKS, KEY_PARENT }, KEY_NAME + " = '" + superTask + "'", null,
        null, null, null);
    c.moveToFirst();
    ContentValues args = new ContentValues();
    args.put(KEY_SUPERTASK, superTask);
    args.put(KEY_DEPTH, c.getInt(1) + 1);
    updateEntryParent(task, c.getString(3), c.getInt(1) + 1);
    mDb.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + task + "'", null);
    args = new ContentValues();
    args.put(KEY_SUBTASKS, c.getInt(2) + 1);
    mDb.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '" + superTask + "'", null);
    c.close();
    updateEntry(superTask, false, null);
    updateEntry(superTask, false, Boolean.FALSE);
  }

  /**
   * Pushes the specified entry at the bottom of the (checked/unchecked) entry
   * list
   * 
   * @param task
   *          the name of the entry to be pushed down
   */
  public final void pushEntryDown(final String task) {
    Cursor c = mDb.query(true, DB_ENTRY_TABLE, new String[] { KEY_NAME,
        KEY_PARENT, KEY_STATUS }, KEY_NAME + " = '" + task + "'", null, null,
        null, null, null);
    final int name = c.getColumnIndexOrThrow(KEY_NAME);
    final int parent = c.getColumnIndexOrThrow(KEY_PARENT);
    final int status = c.getColumnIndexOrThrow(KEY_STATUS);
    c.moveToFirst();
    final String newParent = c.getString(parent);
    final boolean newStatus = c.getInt(status) == 1 ? true : false;
    Cursor all = mDb.query(true, DB_ENTRY_TABLE, new String[] { KEY_NAME,
        KEY_PARENT, KEY_STATUS }, KEY_PARENT + " = '" + newParent + "'", null,
        null, null, null, null);
    all.moveToFirst();
    mDb.delete(DB_ENTRY_TABLE, KEY_PARENT + "='" + newParent + "'", null);
    String aux;
    createEntry(newParent, task);
    updateEntry(task, newStatus);
    all.moveToFirst();
    do {
      aux = all.getString(name);
      if (!aux.equals(task)) {
        createEntry(newParent, aux);
        updateEntry(aux, all.getInt(status) == 1 ? true : false);
      }
    } while (all.moveToNext());
    c.close();
    all.close();
  }

  /**
   * Deletes all the entries in the given tag.
   * 
   * @param name
   *          name of the tag to clear of tasks
   */
  public void deleteEntries(String tag) {
    Cursor c = getTasks(tag, -1, null);
    if (c.getCount() > 0) {
      int name = c.getColumnIndexOrThrow(KEY_NAME);
      c.moveToFirst();
      do {
        deleteEntry(c.getString(name));
      } while (c.moveToNext());
    }
    c.close();
  }

  /**
   * @param parent
   *          tag
   * @return entries which are not checked from a certain tag
   */
  public Cursor getUncheckedEntries(String tag) {
    return mDb.query(DB_ENTRY_TABLE, new String[] { KEY_NAME, KEY_STATUS,
        KEY_PARENT }, (tag != null ? KEY_PARENT + " = '" + tag + "' AND " : "")
        + KEY_STATUS + " = 0", null, null, null, null);
  }

  /**
   * Deletes the entry with the given name. Also deletes the associated
   * graphical or audio notes and alarms if there are any.
   * 
   * @param name
   *          name of the entry to delete
   */
  public void deleteEntry(String name) {
    // decrementing the subtask count of the supertask
    Cursor subC = mDb.query(true, DB_ENTRY_TABLE, new String[] { KEY_NAME,
        KEY_SUPERTASK }, KEY_NAME + " = '" + name + "'", null, null, null,
        null, null);
    subC.moveToFirst();
    Cursor supC = mDb.query(true, DB_ENTRY_TABLE, new String[] { KEY_NAME,
        KEY_SUBTASKS }, KEY_NAME + " = '"
        + subC.getString(subC.getColumnIndex(KEY_SUPERTASK)) + "'", null, null,
        null, null, null);
    if (supC.getCount() > 0) {
      supC.moveToFirst();
      ContentValues args = new ContentValues();
      args
          .put(KEY_SUBTASKS, supC.getInt(supC.getColumnIndex(KEY_SUBTASKS)) - 1);
      mDb.update(DB_ENTRY_TABLE, args, KEY_NAME + " = '"
          + subC.getString(subC.getColumnIndex(KEY_SUPERTASK)) + "'", null);
    }
    supC.close();
    subC.close();

    mDb.delete(DB_ENTRY_TABLE, KEY_NAME + "='" + name + "'", null);
    mCtx.deleteFile(Utils.getImageName(name));
    new File(Utils.getAudioName(name)).delete();
    deleteAlarm(name);
  }
}
