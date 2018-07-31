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

@DynamoDBTable(tableName = "wb-mobilehub-1933814412-Wallets")

public class WalletsDO {
    private String _userId;
    private Double _balance;
    private String _mnemonic;
    private String _password;
    private String _privateKey;
    private String _publicKey;
    private String _walletAddress;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }
    @DynamoDBAttribute(attributeName = "balance")
    public Double getBalance() {
        return _balance;
    }

    public void setBalance(final Double _balance) {
        this._balance = _balance;
    }
    @DynamoDBAttribute(attributeName = "mnemonic")
    public String getMnemonic() {
        return _mnemonic;
    }

    public void setMnemonic(final String _mnemonic) {
        this._mnemonic = _mnemonic;
    }
    @DynamoDBAttribute(attributeName = "password")
    public String getPassword() {
        return _password;
    }

    public void setPassword(final String _password) {
        this._password = _password;
    }
    @DynamoDBAttribute(attributeName = "privateKey")
    public String getPrivateKey() {
        return _privateKey;
    }

    public void setPrivateKey(final String _privateKey) {
        this._privateKey = _privateKey;
    }
    @DynamoDBAttribute(attributeName = "publicKey")
    public String getPublicKey() {
        return _publicKey;
    }

    public void setPublicKey(final String _publicKey) {
        this._publicKey = _publicKey;
    }
    @DynamoDBAttribute(attributeName = "walletAddress")
    public String getWalletAddress() {
        return _walletAddress;
    }

    public void setWalletAddress(final String _walletAddress) {
        this._walletAddress = _walletAddress;
    }

}
