package com.aware.app.stop.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

public class Provider extends ContentProvider {

    /**
     * Authority of this content provider
     */
    public static String AUTHORITY = "com.aware.app.stop.database.provider.game";

    /**
     * ContentProvider database version. Increment every time you modify the database structure
     */
    public static final int DATABASE_VERSION = 1;

    /**
     * Database stored in external folder: /AWARE/plugin_myo.db
     */
    public static final String DATABASE_NAME = "stop_game.db";

    //Database table names
    public static final String DB_TBL_GAME = "table_game";
    public static final String DB_TBL_MEDICATION = "medication";

    //ContentProvider query indexes
    private static final int TABLE_GAME_DIR = 1;
    private static final int TABLE_GAME_ITEM = 2;
    private static final int TABLE_MEDICATION_DIR = 3;
    private static final int TABLE_MEDICATION_ITEM = 4;

    /**
     * Database tables:
     * - ball game data
     */
    public static final String[] DATABASE_TABLES = {
            DB_TBL_GAME, DB_TBL_MEDICATION
    };

    //These are columns that we need to sync data, don't change this!
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
        //String DATA = "data";
    }

    /**
     * Game table
     */
    public static final class Game_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_GAME);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.table_game";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.table_game";

        public static final String DATA = "data";
    }

    //Game table fields
    private static final String DB_TBL_GAME_FIELDS =
            Game_Data._ID + " integer primary key autoincrement," +
                    Game_Data.TIMESTAMP + " real default 0," +
                    Game_Data.DEVICE_ID + " text default ''," +
                    Game_Data.DATA + " text default ''";

    /**
     * Medication table
     */
    public static final class Medication_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_MEDICATION);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.medication";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.app.stop.database.provider.medication";
    }

    //Medication table fields
    private static final String DB_TBL_MEDICATION_FIELDS =
            Medication_Data._ID + " integer primary key autoincrement," +
                    Medication_Data.TIMESTAMP + " real default 0," +
                    Medication_Data.DEVICE_ID + " text default ''";


    /**
     * Share the fields with AWARE so we can replicate the table schema on the server
     */
    public static final String[] TABLES_FIELDS = {
            DB_TBL_GAME_FIELDS, DB_TBL_MEDICATION_FIELDS
    };

    //Helper variables for ContentProvider - DO NOT CHANGE
    private UriMatcher sUriMatcher;
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;
    private void initialiseDatabase() {
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }
    //--

    //For each table, create a hashmap needed for database queries
    private HashMap<String, String> tableGameHash;
    private HashMap<String, String> tableMedicationHash;

    /**
     * Returns the provider authority that is dynamic
     * @return
     */
    public static String getAuthority(Context context) {
        AUTHORITY = context.getPackageName() + ".database.provider.game";
        return AUTHORITY;
    }

    @Override
    public boolean onCreate() {
        //This is a hack to allow providers to be reusable in any application/plugin by making the authority dynamic using the package name of the parent app
        AUTHORITY = getContext().getPackageName() + ".database.provider.game";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //Game table indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], TABLE_GAME_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", TABLE_GAME_ITEM);

        //Game table indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], TABLE_MEDICATION_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", TABLE_MEDICATION_ITEM);

        //Game table HasMap
        tableGameHash = new HashMap<>();
        tableGameHash.put(Game_Data._ID, Game_Data._ID);
        tableGameHash.put(Game_Data.TIMESTAMP, Game_Data.TIMESTAMP);
        tableGameHash.put(Game_Data.DEVICE_ID, Game_Data.DEVICE_ID);
        tableGameHash.put(Game_Data.DATA, Game_Data.DATA);

        //Medication table HasMap
        tableMedicationHash = new HashMap<>();
        tableMedicationHash.put(Medication_Data._ID, Medication_Data._ID);
        tableMedicationHash.put(Medication_Data.TIMESTAMP, Medication_Data.TIMESTAMP);
        tableMedicationHash.put(Medication_Data.DEVICE_ID, Medication_Data.DEVICE_ID);

        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            case TABLE_GAME_DIR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;

            case TABLE_MEDICATION_DIR:
                count = database.delete(DATABASE_TABLES[1], selection, selectionArgs);
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        initialiseDatabase();

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();

        database.beginTransaction();

        switch (sUriMatcher.match(uri)) {

            case TABLE_GAME_DIR:
                long game_id = database.insert(DATABASE_TABLES[0], Game_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (game_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Game_Data.CONTENT_URI, game_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            case TABLE_MEDICATION_DIR:
                long medication_id = database.insert(DATABASE_TABLES[1], Medication_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (medication_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Game_Data.CONTENT_URI, medication_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {

            case TABLE_GAME_DIR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(tableGameHash); //the hashmap of the table
                break;

            case TABLE_MEDICATION_DIR:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(tableMedicationHash); //the hashmap of the table
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        //Don't change me
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            case TABLE_GAME_DIR:
                return Game_Data.CONTENT_TYPE;
            case TABLE_GAME_ITEM:
                return Game_Data.CONTENT_ITEM_TYPE;

            case TABLE_MEDICATION_DIR:
                return Medication_Data.CONTENT_TYPE;
            case TABLE_MEDICATION_ITEM:
                return Medication_Data.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            case TABLE_GAME_DIR:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;

            case TABLE_MEDICATION_DIR:
                count = database.update(DATABASE_TABLES[1], values, selection, selectionArgs);
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null, false);

        return count;
    }
}
