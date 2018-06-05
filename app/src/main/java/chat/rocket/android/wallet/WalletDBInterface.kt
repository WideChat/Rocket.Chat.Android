package chat.rocket.android.wallet

import chat.rocket.android.main.ui.WalletsDO
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import timber.log.Timber
import kotlin.concurrent.thread

class WalletDBInterface {
    private var dynamoDBMapper: DynamoDBMapper? = null

    constructor() {
        val client = AmazonDynamoDBClient(AWSMobileClient.getInstance().credentialsProvider)
        dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(client)
                .awsConfiguration(AWSMobileClient.getInstance().configuration)
                .build()
    }

    fun getBalance(userId: String) {
        thread(true) {
            var walletItem = dynamoDBMapper?.load(WalletsDO::class.java, "test1")
            runOnUiThread {
                Timber.d(walletItem?.balance.toString())
                Timber.d(walletItem?.userId)
            }
        }
    }

    fun sendTokens(senderId: String, recipientId: String, amount: Double) {
        thread(true) {
            // Check that sender has enough tokens to send
            var senderWallet = dynamoDBMapper?.load(WalletsDO::class.java, senderId)
            if (senderWallet !== null) {
                runOnUiThread {
                    Timber.d("Database does not contain user: $senderId")
                }
            }
            if (amount > senderWallet.balance) {
                runOnUiThread {
                    Timber.d("User $senderId does not have sufficient tokens.")
                }
            }

            // Get current balances of sender and recipient

            // Save updated balances to the DB

        }
    }
}