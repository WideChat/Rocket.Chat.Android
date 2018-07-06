package chat.rocket.android.wallet;


import android.content.Context;

import org.web3j.crypto.Bip39Wallet;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.File;
import java.io.FileFilter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import chat.rocket.android.util.OwnWalletUtils;
import timber.log.Timber;


public class BlockchainInterface {

    private Web3j web3;
    private Context context;

    public BlockchainInterface(Context c) {
        // Connect to the private ethereum network via the json rpc url
        HttpService httpService = new HttpService("http://etheriumPublic-2079999181.us-east-1.elb.amazonaws.com:8545");
        this.web3 = Web3jFactory.build(httpService);
        this.context = c;
    }

    /**
     * Find all wallet files in the app's internal storage
     * @return array of wallet addresses
     */
    public String[] findWallets() {
        List<String> addresses = new ArrayList<>();

        File[] fileList = getWalletFiles();
        for (File file: fileList) {
            addresses.add(getAddressFromFileName(file.getName()));
        }

        return addresses.toArray(new String[0]);
    }

    /**
     * Get balance in Wei of a wallet
     * @param address the address/public key of the wallet
     * @return BigDecimal amount of Wei in the wallet or
     *          -1 if the call failed
     */
    public BigDecimal getBalance(String address) {
        try {
            EthGetBalance ethGetBalance = this.web3
                    .ethGetBalance("0x"+address, DefaultBlockParameterName.LATEST)
                    .sendAsync()
                    .get();
            return Convert.fromWei(ethGetBalance.getBalance().toString(), Convert.Unit.ETHER);
        } catch(Exception e) {
            Timber.d("Exception Caught: ethGetBalance failed!");
            e.printStackTrace();
        }
        return BigDecimal.valueOf(-1);
    }

    /**
     * Create a new account on the blockchain and save private key file to internal storage
     * @param password user's password to make the account/wallet
     * @return the public key of the account/wallet
     */
    public String createWallet(String password) {
        String address = "";
        try {
            // Creates a new wallet and file, which is saved to the app's file directory
            String fileName = OwnWalletUtils.generateFullNewWalletFile(password, this.context.getFilesDir());
            // Send the public key back, to save on rocket.chat user's account
            address = getAddressFromFileName(fileName);
        } catch(Exception e) {
            Timber.d("Failed creating wallet!");
        } finally {
            Timber.d("Wallet Address: " + address);
            return address;
        }
    }

    /**
     * Create a new account on the blockchain and save private key file to internal storage
     * @param password user's password to make the account/wallet
     * @return the mnemonic for the account/wallet
     */
    public String createBip39Wallet(String password){
        String mnemonic = "";
        try {
            Bip39Wallet wallet = OwnWalletUtils.generateBip39Wallet(password, this.context.getFilesDir());
            mnemonic = wallet.getMnemonic();
        } catch (Exception e){
            Timber.d("Failed creating Bip39Wallet! Exception:" + e.getMessage());
            // printStackTrace method
            // prints line numbers + call stack
            e.printStackTrace();

            // Prints what exception has been thrown
            System.out.println(e);
        } finally {
            Timber.d("Blockchain Interface, mnemonic is: " + mnemonic);
            return mnemonic;
        }

    }

    private String getAddressFromFileName(String fileName) {
        // A file is in the format of "*--*--[public key].json"
        return fileName.split("--",3)[2].split("\\.",2)[0];
    }

    private File[] getWalletFiles() {
        // Wallet files exist in the app's file directory and start with 'UTC--'
        File[] fileList = context.getFilesDir().listFiles(new FileFilter(){
            @Override
            public boolean accept(File file) {
                return file.getName().startsWith("UTC--");
            }
        });

        return fileList;
    }
}
