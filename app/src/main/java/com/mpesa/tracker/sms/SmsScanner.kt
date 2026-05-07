package com.mpesa.tracker.sms

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mpesa.tracker.MpesaTrackerApp
import com.mpesa.tracker.data.db.AppDatabase

/**
 * Scans the device's SMS inbox for historical M-Pesa messages
 * and imports them into the local database.
 * Optimized to handle zero-balance/Fuliza scenarios and avoid duplicates.
 */
object SmsScanner {

    private const val TAG = "SmsScanner"
    private const val MAX_MESSAGES = 5000 // High limit to ensure we get all history

    suspend fun scanInbox(context: Context): Int {
        val db = AppDatabase.getInstance(context)
        val dao = db.transactionDao()
        var imported = 0

        // Use repository to benefit from smart category memory
        val repository = (context.applicationContext as MpesaTrackerApp).repository

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")

        // Include SAFARICOM and MPESA variants
        val selection = "address LIKE '%MPESA%' OR address LIKE '%M-PESA%' OR address LIKE '%SAFARICOM%'"
        val sortOrder = "date DESC LIMIT $MAX_MESSAGES"

        try {
            context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val addrIdx = cursor.getColumnIndexOrThrow("address")
                val dateIdx = cursor.getColumnIndexOrThrow("date")

                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addrIdx) ?: continue
                    val body   = cursor.getString(bodyIdx) ?: continue
                    val date   = cursor.getLong(dateIdx)

                    val tx = MpesaParser.parse(sender, body, date) ?: continue

                    // Avoid duplicates
                    if (dao.findByTransactionId(tx.transactionId) != null) {
                        continue
                    }

                    // Insert via repository so it applies learned categories
                    val rowId = repository.insertTransaction(tx)
                    if (rowId > 0) {
                        imported++
                        Log.d(TAG, "Imported History: ${tx.transactionId} | Ksh ${tx.amount}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning inbox: ${e.message}", e)
        }

        Log.d(TAG, "Inbox scan complete. Imported $imported transactions.")
        return imported
    }
}
