# Say GM and Earn Rewards
gm is a mobile app. 
Simply tap the "say gm" button to increment the global gm counter. When your taps align to create a special number on the gm counter (Example the 1,7,69,75th gm), you'll be rewarded with an NFT. It is live on Devnet, APK file can be downloaded from [here](https://drive.google.com/drive/folders/1_tdvZucRClpxs0dzVBDruOxqub2RaTPc?usp=sharing).

## App Demo
Click on the image below 👇🏻
[![Demo Video](/images/gm_demo_thumbnail.png)](https://www.youtube.com/watch?v=3tIPIRfJiek)

## Features
- **Special Number NFT Rewards** 🎁
    
    Users are awarded NFTs when their "gm" count coincides with predetermined special numbers.
    
- **Reward Showcase with Social Sharing** ✨
    
    A dedicated section allows users to view and appreciate all the NFT rewards they've earned. Winners can share their NFT achievements on social media platforms, celebrating their success.
    
- **Daily Limit** 🚫
    
    Each user can send "gm" only up to 3 times per day, fostering a sense of exclusivity and genuine interaction.
    
- **Reminder Notifications** 🔔
    
    To ensure engagement, users receive reminders if they haven't sent their "gm" yet.
  
## Screenshots

<table>
  <thead>
    <tr>
      <th><h3>Home</h3></th>
      <th><h3>NFT Dashboard</h3></th>
      <th><h3>NFT</h3></th>
    </tr>
  </thead>
  <tbody>
  <tr>
    <td><img src="/images/home_ss.png" alt="Image 1" width="400"></td>
    <td><img src="/images/dashboard_ss.png" alt="Image 2" width="400"></td>
    <td><img src="/images/nft_ss.png" alt="Image 3" width="400"></td>
  </tr>
    </tbody>
</table>

## Technical Details 

### Tools 🛠️
- Android Studio: Giraffe | 2022.3.1
- Emulator or Mobile Device (used to test the app) should have a wallet application (Solfare recommended) installed with wallet setup

### Program (gm-maker) 💻
- The Program creates a `Counter` account to store the global gm count and the latest timestamp.
- The Program creates a `User` account using the wallet address as seed. `User` account contains, wallet address, total attempts of gm today, latest gm count that user did and its timestamp.
- When `send_gm` instruction is called, it performs pre-checks to ensure the timestamp updates and rate limit of the user. It then updates the counter.

### App 📲
- The App is generated from [Mobile dApp Scaffold]() which leverages [Solana Mobile Adapter]() and [SolanaKT]() Libraries. 
- The App integrates the `gm-maker` program and invokes the instructions as required.
- After a special number (1,7,33,68,75,100) is encountered, the user can mint an NFT. This is possible because of [Underdog Protocol](https://underdogprotocol.com/). 
- Similarly, rate limits and more information are displayed,
- At a particular time, a notification is sent to "say gm."
- The App follows the MVVM Architecture.

## 👨‍💻 Contributing

- Any contributions you make to this project is **greatly appreciated**.

## License 🛡️
GM is licensed under the MIT License - see the [`LICENSE`](LICENSE.txt) file for more information.

## Bug / Feature Request

If you find a bug in the app, kindly open an issue [here](https://github.com/anamansari062/gm/issues/new?assignees=&labels=bug&template=bug_report.md&title=%5BBug%5D%3A+) to report it by
including a proper description of the bug and the expected result.

## Reachout 
[Anam Ansari | Linktree](https://linktr.ee/anamansari062)
