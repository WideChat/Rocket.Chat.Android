const ENDPOINT = 'http://etheriumpublic-2079999181.us-east-1.elb.amazonaws.com:8545';
var Web3 = require('web3');
web3 = new Web3(new Web3.providers.HttpProvider(ENDPOINT));
const EthereumTx = require('ethereumjs-tx')

/**
 * Function get the Ether balance at the specified address
 */
function getBalance(address, callback) {
  if (!address.startsWith("0x")) {
    address = "0x" + address;
  }
  web3.eth.getBalance(address).then(function (bal) {
    callback(web3.utils.fromWei(bal, 'ether'));
  });
}

/**
 * Function to send tokens to a user's account
 *
 * The receiptCallback will be called with amount sent and the recipient's
 *  new balance once the transaction has completed.
 */
function sendTokens(recipient, amount, receiptCallback) {

  if (!recipient.startsWith("0x")) {
    recipient = "0x" + recipient;
  }

  const sender = "0xcf3c2e6e0edc6417e73dcf49eb5f03cd6990651b";
  const password = "password";
  const privateKey = "029BAA521A81F0BD7E075667D51CA930D28E605F196537CE6AA157A38D03786D";

  // Get nonce
  var noncePromise = web3.eth.getTransactionCount(sender);

  // Get gas price
  var gasPricePromise = web3.eth.getGasPrice();

  var allPromises = Promise.all([
  	noncePromise,
  	gasPricePromise
  ]);

  // Build and send new transaction
  var details;
  var buildTransaction = allPromises.then( function(results){

  	var nonce = results[0];
  	var gasPrice = results[1];

  	details = {
  	  from: sender,
  	  to: recipient,
  	  value: web3.utils.toHex( web3.utils.toWei(amount.toString(), 'ether') ),
  	  gasLimit: web3.utils.toHex(1000000),
  	  gasPrice: web3.utils.toHex(gasPrice),
  	  nonce: nonce
  	}

  	var transaction = new EthereumTx(details);

  	// Sign transaction
  	transaction.sign( Buffer.from(privateKey, 'hex') );

  	// Serialize transaction
  	var serializedTransaction = transaction.serialize();

  	// Send transaction
  	web3.eth.sendSignedTransaction('0x' + serializedTransaction.toString('hex'), function(err, hash) {
  	  if (!err) console.log(hash)
  	  else console.log(err);
  	}).on('receipt', function(receipt) {
      Promise.all([
        web3.eth.getTransaction(receipt.transactionHash),
        web3.eth.getBalance(receipt.to)
      ]).then(function(results) {
          var amountSent = web3.utils.fromWei(results[0].value, 'ether');
          var newBalance = web3.utils.fromWei(results[1], 'ether');
          receiptCallback(amountSent, newBalance);
        }
      );
    });

  });

}



module.exports = { getBalance , sendTokens};
