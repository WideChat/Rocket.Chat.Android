# Wallet

Our goal is to integrate a cryptocurrency wallet into the Rocket.Chat Android app. We have added basic integration with Ethereum networks into the app, allowing the user to see their Wallet, send a Transaction, and Create a Wallet.

The wallet can be enabled/disabled and supports "managed"/"un-managed" modes. See the configuration section for more details on how these modes can be set. 

Note: This code has mainly been tested on a private Ethereum test network, and not on the main Ethereum network or more common test networks.

<div align="center">
  <img width="30%" src="https://preview.ibb.co/hiQiM8/Screenshot_1532719878.png">
  <img width="30%" src="https://preview.ibb.co/kueeTo/Screenshot_1532720121.png">
  <img width="30%" src="https://preview.ibb.co/bXJZuT/Screenshot_1532720468.png">
</div>

### Wallet Screen

The Wallet screen is where users can see their current wallet balance and recent transaction history (currently no backend implemented for trans. history). The user can also click a button to send tokens (Ether) to other users. A user can send to other Rocket.Chat users whom the user has an open direct message room with, or to non-Rocket.Chat users via their public address. Not implemented, but shown in the UI, is a way for users to scan a QR code to load the wallet address of the recipient of the transaction. 

Managed Wallet mode: 
A wallet will be auto-created for the user the first time they navigate to this screen.

Un-Managed Wallet mode:
If users do not have a wallet, they can navigate from this screen to the  "Create Wallet" screen to create one.

Navigate to the wallet screen from the nav-drawer.

### Transaction Screen

This screen is where the user begins and confirms a transaction.

Managed Wallet mode: 
The user will see their current wallet balance. They must enter an amount of tokens to send. If the recipient is a Rocket.Chat user, the user will also see the recipient's username and be able to enter a reason for the transaction. Signing transactions is hidden from the user.

Un-Managed Wallet mode:
The user will see their current wallet balance and the wallet address of the person they are sending to. They must enter an amount of tokens to send and then sign the transaction with their wallet password. If the recipient is a Rocket.Chat user, the user will also see the recipient's username and be able to enter a reason for the transaction.

The Transaction screen can be reached from the Wallet screen or from a direct message chat room.

### Create Wallet Screen

The Create Wallet screen is where a user can create a new wallet if they do not have one associated with their Rocket.Chat account or does not have the encrypted private key file stored on their device.

Users enter a wallet name (currently does nothing; may make sense if multiple wallets are supported in the future) and a password (8+ chars) for the new wallet. Confirming the creation will show the user with a mnemonic phrase to save, which could recover their account if the private key is lost (recovery is not implemented at the moment).

Navigating to this screen from the Wallet screen when the user does not have a wallet yet.

## Storing Private & Public Keys

Currently, the keys associated with a user's wallet are split, and the public key is stored in their Rocket.Chat profile, while the private key is stored on their own device.

When a user creates a wallet, the encrypted private key file is stored in the app's internal storage on the user's device, and the app will look for this file each time it attempts to load the Wallet screen or send a transaction.

The public key of the Ethereum account is stored alongside the user's Rocket.Chat profile in the __customFields__ of the user's info in the field __walletAddress__. To have this storage work on your server add something like the following in `Administration > Accounts > Registration > Custom Fields`:
```json
{
  "walletAddress": {
    "type": "text",
    "required": "false"
  }
}
```

Currently, the app makes REST API calls to the server to update/access customFields. These calls would ideally be made by the Kotlin SDK, but there is currently no support for __customFields__ in the SDK.

## Interaction with Ethereum Network

The wallet code uses the [web3j](https://web3j.io) API to interact with the Ethereum network. This works via an RPC connection to the network, which can be configured to different networks in the [BlockchainInterface](./app/src/main/java/chat/rocket/android/wallet/BlockchainInterface.java) file.

## Additions to Direct Message Rooms

Via the add attachment button in the message composer, a user has the option to send tokens to another user from the direct message room. Clicking the Send Tokens button will bring the user to the Transaction screen.

Whenever a user makes a transaction with another Rocket.Chat user (whether originally from the Wallet screen or from a chatroom), the user will be sent back from the Transaction screen to the direct message room with the recipient. A message will be auto-generated from the recipient detailing that the transaction was initiated.

__Note:__ This message sent in the chatroom is not a confirmation that the transaction was completed (mined) on the Ethereum blockchain, but rather that the transaction is pending. Currently, there is not implementation for notifying a user when the transaction is actually completed.

## Configuration (coming soon...)
Ability to add enable/disable wallet on your server.
Ability to set manage/unmanaged wallet mode on your server.
