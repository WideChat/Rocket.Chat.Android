package chat.rocket.android.wallet

import chat.rocket.android.R.id.amount
import chat.rocket.android.main.ui.WalletsDO
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import timber.log.Timber
import kotlin.concurrent.thread
import java.util.UUID

class WalletDBInterface {
    private var dynamoDBMapper: DynamoDBMapper? = null

    fun createWallet(userId: String?, callback: () -> Unit){
        val walletItem = WalletsDO()
        walletItem.userId = userId
        walletItem.balance = 200.0

        thread(start = true) {
            dynamoDBMapper?.save(walletItem)
            runOnUiThread(callback)
        }
    }

    fun deleteWallet(userId: String?, callback: () -> Unit) {
        thread (true) {
            val walletItem = WalletsDO()
            walletItem.userId = userId
            dynamoDBMapper?.delete(walletItem)
            runOnUiThread(callback)
        }
    }

    fun getBalance(userId: String?, callback: (Double) -> Unit) {
        thread (true) {
            val walletItem = dynamoDBMapper?.load(WalletsDO::class.java, userId)
            runOnUiThread {
                callback(walletItem?.balance ?: -1.0)
            }
        }
    }

    fun sendTokens(senderId: String, recipientId: String, amount: Double, callback: (Double) -> Unit) {
        thread(true) {

            // Return if trying to send 0 tokens
            if (amount <= 0){
                runOnUiThread {
                    Timber.d("ERROR: User must enter a positive number of tokens to send")
                }
                return@thread
            }

            // Get sender and recipient wallets from the DB
            val senderWallet = dynamoDBMapper?.load(WalletsDO::class.java, senderId)
            val recipientWallet = dynamoDBMapper?.load(WalletsDO::class.java, recipientId)

            if (senderWallet === null || recipientWallet === null) {
                runOnUiThread {
                    Timber.d("ERROR: Database does not contain user(s)")
                }
                return@thread
            }

            // Check that sender has enough tokens to send
            if (amount > senderWallet.balance) {
                runOnUiThread {
                    Timber.d("ERROR: User $senderId does not have sufficient tokens.")
                }
                return@thread
            }

            // Save updated balances to the DB
            senderWallet.balance -= amount
            recipientWallet.balance += amount
            dynamoDBMapper?.save(senderWallet)
            dynamoDBMapper?.save(recipientWallet)

            // Send updated balance to UI
            runOnUiThread {
                callback(senderWallet.balance)
            }
        }
    }

    init {
        val client = AmazonDynamoDBClient(AWSMobileClient.getInstance().credentialsProvider)
        dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(client)
                .awsConfiguration(AWSMobileClient.getInstance().configuration)
                .build()
    }
}