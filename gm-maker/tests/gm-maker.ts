import * as anchor from "@coral-xyz/anchor";
import { Program } from "@coral-xyz/anchor";
import { GmMaker } from "../target/types/gm_maker";
import {encode} from "@coral-xyz/anchor/dist/cjs/utils/bytes/utf8";
import { PublicKey } from '@solana/web3.js';

describe("gm-maker", () => {
  const provider = anchor.AnchorProvider.env()
  anchor.setProvider(provider)

  const program = anchor.workspace.GmMaker as Program<GmMaker>;

  it("Is initialized!", async () => {
    // Program Derived Address for the counter account
    const [counterPDA, _] = anchor.web3.PublicKey.findProgramAddressSync(
          [
              encode("gm_counter"),
          ],
          new PublicKey("354LDN9wip3V2yNrmNkv2xsgvkAN2yQK6R4HvszbB431")
      );

    const tx = await program.methods
        .sendGm()
        .accounts({
            author: provider.wallet.publicKey,
            counter: counterPDA,
            systemProgram: anchor.web3.SystemProgram.programId
        })
        .rpc();

    // Fetching the counter account
    const counterAccount = await program.account.counter.fetch(counterPDA);

    console.log(counterAccount)
    console.log("Transaction successful: https://solana.fm/tx/" + tx + "?cluster=devnet-solana");
  });
});
