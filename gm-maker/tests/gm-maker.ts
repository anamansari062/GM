import * as anchor from "@coral-xyz/anchor";
import { Program } from "@coral-xyz/anchor";
import { GmMaker } from "../target/types/gm_maker";
import {encode} from "@coral-xyz/anchor/dist/cjs/utils/bytes/utf8";
import { PublicKey } from '@solana/web3.js';

describe("gm-maker", () => {
  const provider = anchor.AnchorProvider.env()
  anchor.setProvider(provider)

  const program = anchor.workspace.GmMaker as Program<GmMaker>;
  const author = provider.wallet.publicKey;

  it("create pda", async () => {

      // Convert the author PublicKey instance to a Uint8Array
    const authorBuffer = new PublicKey("FGRXXizynNLs4TouMkfVPnN6Y9A7HZk8Jxu1cwrkbiSD").toBuffer();

    // Create a Uint8Array from the author buffer
    const encodedAuthor = new Uint8Array(authorBuffer);

      // Program Derived Address for the counter account
    const [userPDA1, userBump1] = anchor.web3.PublicKey.findProgramAddressSync(
          [
              encode("user"),
              encodedAuthor
          ],
          program.programId
      );

    const userAccount = await program.account.user.fetch(userPDA1);

    console.log(userAccount)

  })

//   it("Initializing user account and saying gm!", async () => {
//     // Convert the author PublicKey instance to a Uint8Array
//     const authorBuffer = author.toBuffer();

//     // Create a Uint8Array from the author buffer
//     const encodedAuthor = new Uint8Array(authorBuffer);

//     // Program Derived Address for the counter account
//     const [userPDA, userBump] = anchor.web3.PublicKey.findProgramAddressSync(
//           [
//               encode("user"),
//               encodedAuthor
//           ],
//           program.programId
//       );


//     // Sending the transaction
//     const txUser = await program.methods
//         .createUser()
//         .accounts({
//             author: author,
//             user: userPDA,
//             systemProgram: anchor.web3.SystemProgram.programId
//         })
//         .rpc();

//     // Fetching user account
//     var userAccount = await program.account.user.fetch(userPDA);

//     console.log("User creation successful: https://solana.fm/tx/" + txUser + "?cluster=devnet-solana");
//     console.log(userAccount)

//     // Program Derived Address for the counter account
//     const [counterPDA, counterBump] = anchor.web3.PublicKey.findProgramAddressSync(
//           [
//               encode("gm_counter"),
//           ],
//           program.programId
//       );

//     // Sending the transaction
//     const txCounter = await program.methods
//         .sendGm()
//         .accounts({
//             author: provider.wallet.publicKey,
//             counter: counterPDA,
//             user: userPDA,
//             systemProgram: anchor.web3.SystemProgram.programId
//         })
//         .rpc();

//     // Fetching the user and counter account
//     userAccount = await program.account.user.fetch(userPDA);
//     const counterAccount = await program.account.counter.fetch(counterPDA);

//     console.log(userAccount)
//     console.log(counterAccount)
//     console.log("Gm successful: https://solana.fm/tx/" + txCounter + "?cluster=devnet-solana");
//   });
});
