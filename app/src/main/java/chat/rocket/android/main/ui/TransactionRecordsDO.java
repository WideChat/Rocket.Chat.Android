package chat.rocket.android.main.ui;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@DynamoDBTable(tableName = "wb-mobilehub-1933814412-TransactionRecords")

public class TransactionRecordsDO {
    private String _account;
    private List<String> _transactions;

    @DynamoDBHashKey(attributeName = "account")
    @DynamoDBAttribute(attributeName = "account")
    public String getAccount() {
        return _account;
    }

    public void setAccount(final String _account) {
        this._account = _account;
    }
    @DynamoDBAttribute(attributeName = "transactions")
    public List<String> getTransactions() {
        return _transactions;
    }

    public void setTransactions(final List<String> _transactions) {
        this._transactions = _transactions;
    }

}
