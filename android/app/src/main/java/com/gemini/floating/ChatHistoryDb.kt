package com.gemini.floating

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ChatHistoryDb(context: Context) : SQLiteOpenHelper(context, "gemini_chat.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, text TEXT NOT NULL, created_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun add(role: String, text: String) { writableDatabase.execSQL("INSERT INTO messages(role,text,created_at) VALUES(?,?,?)", arrayOf(role, text, System.currentTimeMillis())) }
    fun latest(limit: Int = 24): List<Pair<String,String>> {
        val rows = readableDatabase.rawQuery("SELECT role,text FROM messages ORDER BY id DESC LIMIT ?", arrayOf(limit.toString()))
        val result = mutableListOf<Pair<String,String>>()
        rows.use { while (it.moveToNext()) result.add(it.getString(0) to it.getString(1)) }
        return result.asReversed()
    }
    fun clear() { writableDatabase.delete("messages", null, null) }
}
