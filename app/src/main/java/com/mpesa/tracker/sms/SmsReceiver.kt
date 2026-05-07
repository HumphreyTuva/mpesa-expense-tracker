package com.mpesa.tracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.mpesa.tracker.data.db.AppDatabase
import com.mpesa.tracker.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for incoming SMS messages, detects M-Pesa transactions,
 * and saves them to the local Room database automatically.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Reconstruct full message body (handles multi-part SMS)
        val sender = messages[0].originatingAddress ?: ""
        val body = messages.joinToString("") { it.messageBody }
        val timestamp = messages[0].timestampMillis

        // NEW: More lenient gatekeeper. If it's from an MPESA sender,
        // we let the parser try its luck rather than skipping too early.
        if (!MpesaParser.isMpesaMessage(sender, body)) {
            return
        }

        Log.d(TAG, "M-Pesa SMS detected. Attempting to parse...")

        // The parser now ignores "Fuliza" text and focus on the ID and Amount
        val transaction = MpesaParser.parse(sender, body, timestamp) ?: run {
            Log.w(TAG, "Parser returned null for potential M-Pesa message. Body: $body")
            return
        }

        // Save to database on IO thread to keep the UI/System thread responsive
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val dao = db.transactionDao()
                val mappingDao = db.categoryMappingDao()

                // 1. Prevent Duplicates (Crucial for Fuliza where messages might repeat info)
                val existing = dao.findByTransactionId(transaction.transactionId)
                if (existing != null) {
                    Log.d(TAG, "Transaction ${transaction.transactionId} already exists. Skipping.")
                    return@launch
                }

                // 2. Apply Categorization
                val txToSave = transaction.recipient?.let { recipient ->
                    val customCategory = mappingDao.getCategoryForText(recipient)
                    if (customCategory != null) {
                        transaction.copy(category = customCategory)
                    } else {
                        transaction
                    }
                } ?: transaction

                // 3. Persist to Room
                dao.insert(txToSave)
                Log.d(TAG, "Saved: ${txToSave.transactionId} - Ksh ${txToSave.amount}")

                // 4. Trigger Notification
                // Switch back to Main/UI thread if your NotificationHelper requires it,
                // though usually notification manager is thread-safe.
                NotificationHelper.showTransactionNotification(context, txToSave)

            } catch (e: Exception) {
                Log.e(TAG, "Error in database operation: ${e.message}")
            }
        }
    }
}