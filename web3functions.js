const ENDPOINT = 'http://etheriumpublic-2079999181.us-east-1.elb.amazonaws.com:8545';
var Web3 = require('web3');
web3 = new Web3(new Web3.providers.HttpProvider(ENDPOINT));
const EthereumTx = require('ethereumjs-tx')
const keythereum = require('keythereum');
const datadir = './'; //TODO change to where the private key file being stored



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
 */
function sendTokens(recipient, amount) {

  if (!recipient.startsWith("0x")) {
    recipient = "0x" + recipient;
  }

  const sender = "0xcf3c2e6e0edc6417e73dcf49eb5f03cd6990651b";
  const password = "password";

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

	// Get private key
	var keyObject = keythereum.importFromFile(sender, datadir);
	var privateKey = keythereum.recover(password, keyObject);

	// Sign transaction
	transaction.sign( privateKey );

	// Serialize transaction
	var serializedTransaction = transaction.serialize();

	// Send transaction
	web3.eth.sendSignedTransaction('0x' + serializedTransaction.toString('hex'), function(err, hash) {
	  	if (!err) console.log(hash)
	  	else console.log(err); 
	}).on('receipt', console.log);

  });

}



module.exports = { getBalance , sendTokens};
