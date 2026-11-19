package de.gymmane.scanner.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import de.gymmane.scanner.domain.Product

/** All access is serialized by ProductRepository and runs on Dispatchers.IO. */
class ProductCache(context: Context, databaseName: String = "products.db") : SQLiteOpenHelper(context, databaseName, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE products (id TEXT PRIMARY KEY, payload TEXT NOT NULL, accessed INTEGER NOT NULL, manual INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun find(id: String): Product? = readableDatabase.query("products", arrayOf("payload"),
        "id=?", arrayOf(id), null, null, null).use { cursor ->
        if (cursor.moveToFirst()) ProductJson.decode(cursor.getString(0)) else null
    }

    fun history(): List<Product> = readableDatabase.query("products", arrayOf("payload"), null,
        null, null, null, "accessed DESC").use { cursor ->
        buildList { while (cursor.moveToNext()) add(ProductJson.decode(cursor.getString(0))) }
    }

    fun save(product: Product) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict("products", null, ContentValues().apply {
                put("id", product.id); put("payload", ProductJson.encode(product))
                put("accessed", System.currentTimeMillis()); put("manual", if (product.manual) 1 else 0)
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.execSQL("DELETE FROM products WHERE manual=0 AND id NOT IN (SELECT id FROM products WHERE manual=0 ORDER BY accessed DESC LIMIT 100)")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun delete(id: String) { writableDatabase.delete("products", "id=?", arrayOf(id)) }
}
