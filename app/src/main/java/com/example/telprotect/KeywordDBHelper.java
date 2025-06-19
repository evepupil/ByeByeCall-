package com.example.telprotect;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class KeywordDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "keywords.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_KEYWORDS = "keywords";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_KEYWORD = "keyword";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_KEYWORDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_KEYWORD + " TEXT UNIQUE NOT NULL);";

    public KeywordDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEYWORDS);
        onCreate(db);
    }

    // 添加关键字
    public boolean addKeyword(String keyword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_KEYWORD, keyword);

        long result = db.insertWithOnConflict(
                TABLE_KEYWORDS,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        
        return result != -1;
    }

    // 删除关键字
    public boolean deleteKeyword(String keyword) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(
                TABLE_KEYWORDS,
                COLUMN_KEYWORD + " = ?",
                new String[]{keyword});
        db.close();
        
        return result > 0;
    }

    // 获取所有关键字
    public List<String> getAllKeywords() {
        List<String> keywordList = new ArrayList<>();
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_KEYWORDS,
                new String[]{COLUMN_KEYWORD},
                null,
                null,
                null,
                null,
                COLUMN_KEYWORD + " ASC");

        if (cursor.moveToFirst()) {
            do {
                keywordList.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        return keywordList;
    }
} 