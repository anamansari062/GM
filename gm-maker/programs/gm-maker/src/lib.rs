use anchor_lang::prelude::*;

declare_id!("CG6a1QDsyN6A4wSiGRrhLsqMT4gQY6oPbgvMcdfgh2r");

#[program]
pub mod gm_maker {
    use super::*;

    pub fn send_gm(ctx: Context<SendGm>) -> Result<()> {
        let counter: &mut Account<Counter> = &mut ctx.accounts.counter;
        let user: &mut Account<User> = &mut ctx.accounts.user; 

        let clock = Clock::get().unwrap();

        let user_slot = user.timestamp / 86400; // Timestamp in the user account
        let counter_slot = counter.timestamp / 86400; // Timestamp in the counter account
        let current_slot = clock.unix_timestamp / 86400; // Current Timestamp

        // Check if the user has already said gm
        require!(!(user_slot== current_slot && user.current_count==3), GmError::AlreadyGmed);

        if counter_slot!=current_slot {
            counter.gm = 0 // gm count is changed to 0 after 24 hours
        }

        // Updating total gm count for the day and setting the current timestamp of "counter"
        counter.gm = counter.gm + 1; 
        counter.timestamp = clock.unix_timestamp;

        // Updating gm count for the day and setting the current timestamp of "user"
        user.current_count = user.current_count + 1;
        user.gm_count = counter.gm;
        user.timestamp = counter.timestamp;

        msg!("{} did the {} gm at {}", user.author, counter.gm, counter.timestamp);

        Ok(())
    }

    pub fn create_user(ctx: Context<CreateUser>) -> Result<()> {
        let user: &mut Account<User> = &mut ctx.accounts.user;

        user.author = ctx.accounts.author.key();

        msg!("{} created user account", user.author);

        Ok(())
    }
}

#[account]
pub struct Counter {
    pub gm: u32, // total gm count of current day
    pub timestamp: i64 // Timestamp for the latest gm
}

#[account]
#[derive(Default)]
pub struct User {
    pub author: Pubkey,
    pub current_count: u8,
    pub gm_count: u32, // gm count of current day
    pub timestamp: i64 // Timestamp for current day gm
}

impl User {
    pub const LEN: usize = 8 + 32 + 8 + 8 + 8; // discriminator + author + current_count + gm_count + timestamp
}

impl Counter {
    pub const LEN: usize = 8 + 8 + 8; // discriminator + gm_count + timestamp
}

#[derive(Accounts)]
pub struct CreateUser<'info> {
    #[account(
        init_if_needed, // Will be initialized only once
        payer = author, 
        space = User::LEN,
        seeds = [b"user", author.key().as_ref()],
        bump
    )]
    pub user: Account<'info, User>,

    #[account(mut)]
    pub author: Signer<'info>,

    pub system_program: Program<'info, System>,
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
    pub user: Account<'info, User>,

    #[account(mut)]
    pub author: Signer<'info>,

    pub system_program: Program<'info, System>,
}

#[error_code]
pub enum GmError {
    #[msg("You have already said gm alot today.")]
    AlreadyGmed
}