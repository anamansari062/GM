use anchor_lang::prelude::*;

declare_id!("354LDN9wip3V2yNrmNkv2xsgvkAN2yQK6R4HvszbB431");

#[program]
pub mod gm_maker {
    use super::*;

    pub fn send_gm(ctx: Context<SendGm>) -> Result<()> {
        let counter: &mut Account<Counter> = &mut ctx.accounts.counter;

        let clock = Clock::get().unwrap();

        let account_slot = counter.timestamp / 86400; // Timestamp in the counter account
        let current_slot = clock.unix_timestamp / 86400; // Current Timestamp

        if account_slot!=current_slot {
            counter.gm = 0 // gm count is changed to 0 after 24 hours
        }

        // Updating gm count and setting the current timestamp
        counter.gm = counter.gm + 1; 
        counter.timestamp = clock.unix_timestamp;

        msg!("{} gm at {}", counter.gm, counter.timestamp);

        Ok(())
    }
}

#[account]
pub struct Counter {
    pub gm: u32,
    pub timestamp: i64 // Timestamp for the latest gm
}

impl Counter {
    pub const LEN: usize = 8 + 8 + 8; // discriminator + gm + timestamp
}

#[derive(Accounts)]
pub struct SendGm<'info> {
    #[account(
        init_if_needed, // Will be initialized only once
        payer = author, 
        space = Counter::LEN,
        seeds = [b"gm_counter"],
        bump
    )]
    pub counter: Account<'info, Counter>,

    #[account(mut)]
    pub author: Signer<'info>,

    pub system_program: Program<'info, System>,
}
