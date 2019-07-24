/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.configuration;

import com.griefdefender.configuration.category.ConfigCategory;
import com.griefdefender.text.TextTemplate;
import net.kyori.text.format.TextColor;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MessageDataConfig extends ConfigCategory {

    @Setting(MessageStorage.ABANDON_OTHER_SUCCESS)
    public TextTemplate abandonOtherSuccess = TextTemplate.of(TextColor.GREEN, TextTemplate.arg("player"), "'s claim has been abandoned.", TextTemplate.arg("player"), " now has ", TextTemplate.arg("total"), " available claim blocks.");

    @Setting(MessageStorage.ADJUST_BLOCKS_SUCCESS)
    public TextTemplate adjustBlocksSuccess = TextTemplate.of(TextColor.GREEN, "Adjusted ", TextTemplate.arg("player"), "'s bonus claim blocks by ", TextTemplate.arg("adjustment"), ".  New total bonus blocks: ", TextTemplate.arg("total"), ".");

    @Setting(MessageStorage.BLOCK_CLAIMED)
    public TextTemplate blockClaimed = TextTemplate.of(TextColor.GREEN, "That block has been claimed by ", TextTemplate.arg("owner").color(TextColor.GOLD), ".");

    @Setting("block-not-claimed")
    public TextTemplate blockNotClaimed = TextTemplate.of(TextColor.RED, "No one has claimed this block.");

    @Setting(MessageStorage.BLOCK_SALE_VALUE)
    public TextTemplate blockSaleValue = TextTemplate.of(TextColor.GREEN, "Each claim block is worth ", TextTemplate.arg("block-value"), ".  You have ", TextTemplate.arg("available-blocks"), " available for sale.");

    @Setting(MessageStorage.CLAIM_ABANDON_SUCCESS)
    public TextTemplate claimAbandonSuccess = TextTemplate.of(TextColor.GREEN, "Claim abandoned. You now have ", TextTemplate.arg("remaining-blocks"), " available claim blocks.");

    @Setting("claim-automatic-notification")
    public TextTemplate claimAutomaticNotification = TextTemplate.of(TextColor.RED, "This chest and nearby blocks are protected from breakage and theft.");

    @Setting("claim-bank-tax-system-not-enabled")
    public TextTemplate claimBankTaxSystemNotEnabled = TextTemplate.of(TextColor.RED, "The bank/tax system is not enabled. If you want it enabled, set 'bank-tax-system' to true in config.");

    @Setting("claim-bank-info")
    public TextTemplate claimBankInfo = TextTemplate.of(TextColor.GREEN, "Balance: ", TextTemplate.arg("balance").color(TextColor.GOLD), 
            TextColor.GREEN, "\nTax: ", TextTemplate.arg("amount").color(TextColor.GOLD), TextColor.WHITE, " due in ", TextTemplate.arg("time_remaining").color(TextColor.GRAY),
            TextColor.GREEN, "\nTax Owed: ", TextTemplate.arg("tax_balance").color(TextColor.GOLD));

    @Setting("claim-bank-deposit")
    public TextTemplate claimBankDeposit = TextTemplate.of(TextColor.GREEN, "Successful deposit of ", TextTemplate.arg("amount").color(TextColor.GOLD), " into bank.");

    @Setting("claim-bank-deposit-no-funds")
    public TextTemplate claimBankDepositNoFunds = TextTemplate.of(TextColor.RED, "You do not have enough funds to deposit into the bank.");

    @Setting("claim-bank-no-permission")
    public TextTemplate claimBankNoPermission = TextTemplate.of(TextColor.RED, "You don't have permission to manage", TextTemplate.arg("owner"), "'s claim bank.");

    @Setting("claim-bank-withdraw")
    public TextTemplate claimBankWithdraw = TextTemplate.of(TextColor.GREEN, "Successful withdraw of ", TextTemplate.arg("amount").color(TextColor.GOLD), " from bank.");

    @Setting("claim-bank-withdraw-no-funds")
    public TextTemplate claimBankWithdrawNoFunds = TextTemplate.of(TextColor.RED, "The claim bank has a remaining balance of ", TextTemplate.arg("balance").color(TextColor.GOLD), " and does not have enough funds to withdraw ", TextTemplate.arg("amount").color(TextColor.GOLD), ".");

    @Setting("claim-block-purchase-limit")
    public TextTemplate claimBlockPurchaseLimit = TextTemplate.of(TextColor.RED, "The new claim block total of ", TextTemplate.arg("new_total").color(TextColor.GOLD), " will exceed your claim block limit of ", TextTemplate.arg("block_limit").color(TextColor.GREEN), ". The transaction has been cancelled.");

    @Setting("claim-chest-confirmation")
    public TextTemplate claimChestConfirmation = TextTemplate.of(TextColor.RED, "This chest is protected.");

    @Setting("claim-chest-outside-level")
    public TextTemplate claimChestOutsideLevel = TextTemplate.of(TextColor.RED, "This chest can't be protected as the position is outside your claim level limits of ", TextTemplate.arg("min-claim-level").color(TextColor.GREEN), " and ", TextTemplate.arg("max-claim-level").color(TextColor.GREEN), ". (/playerinfo)");

    @Setting("claim-children-warning")
    public TextTemplate claimChildrenWarning = TextTemplate.of(TextColor.AQUA, "This claim includes child claims.  If you're sure you want to delete it, use /DeleteClaim again.");

    @Setting("claim-create-only-subdivision")
    public TextTemplate claimCreateOnlySubdivision = TextTemplate.of(TextColor.RED, "Unable to create claim. Only subdivisions can be created at a single block location.");

    @Setting("claim-create-cuboid-disabled")
    public TextTemplate claimCreateCuboidDisabled = TextTemplate.of(TextColor.RED, "The creation of 3D cuboid claims has been disabled by an administrator.\nYou can only create 3D claims as an Admin or on a 2D claim that you own.");

    @Setting("claim-create-overlap")
    public TextTemplate claimCreateOverlap = TextTemplate.of(TextColor.RED, "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a corner to resize it.");

    @Setting("claim-create-overlap-player")
    public TextTemplate claimCreateOverlapPlayer = TextTemplate.of(TextColor.RED, "You can't create a claim here because it would overlap ", TextTemplate.arg("owner"), "'s claim.");

    @Setting("claim-create-overlap-short")
    public TextTemplate claimCreateOverlapShort = TextTemplate.of(TextColor.RED, "Your selected area overlaps an existing claim.");

    @Setting("claim-create-success")
    public TextTemplate claimCreateSuccess = TextTemplate.of(TextColor.GREEN, TextTemplate.arg("type"), " created!  Use /trust to share it with friends.");

    @Setting("claim-create-error-result")
    public TextTemplate claimCreateErrorResult = TextTemplate.of(TextColor.RED, "Unable to create claim due to error result ", TextTemplate.arg("result"), ".");

    @Setting("claim-cleanup-warning")
    public TextTemplate claimCleanupWarning = TextTemplate.of(TextColor.RED, "The land you've unclaimed may be changed by other players or cleaned up by administrators.  If you've built something there you want to keep, you should reclaim it.");

    @Setting("claim-context-not-found")
    public TextTemplate claimContextNotFound = TextTemplate.of(TextColor.RED, "Context '", TextTemplate.arg("context"), "' was not found.");

    @Setting(MessageStorage.CLAIM_SIZE_MAX_X)
    public TextTemplate claimSizeMaxX = TextTemplate.of(TextColor.RED, "The claim x size of ", TextTemplate.arg("size").color(TextColor.GREEN), " exceeds the max size of ", TextTemplate.arg("max-size").color(TextColor.GREEN), ".\nThe area needs to be a mininum of ", TextTemplate.arg("min-area").color(TextColor.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColor.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_MAX_Y)
    public TextTemplate claimSizeMaxY = TextTemplate.of(TextColor.RED, "The claim y size of ", TextTemplate.arg("size").color(TextColor.GREEN), " exceeds the max size of ", TextTemplate.arg("max-size").color(TextColor.GREEN), ".\nThe area needs to be a minimum of ", TextTemplate.arg("min-area").color(TextColor.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColor.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_MAX_Z)
    public TextTemplate claimSizeMaxZ = TextTemplate.of(TextColor.RED, "The claim z size of ", TextTemplate.arg("size").color(TextColor.GREEN), " exceeds the max size of ", TextTemplate.arg("max-size").color(TextColor.GREEN), ".\nThe area needs to be a minimum of ", TextTemplate.arg("min-area").color(TextColor.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColor.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_MIN_X)
    public TextTemplate claimSizeMinX = TextTemplate.of(TextColor.RED, "The claim x size of ", TextTemplate.arg("size").color(TextColor.GREEN), " is below the minimum size of ", TextTemplate.arg("min-size").color(TextColor.GREEN), ".\nThe area needs to be a minimum of ", TextTemplate.arg("min-area").color(TextColor.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColor.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_MIN_Y)
    public TextTemplate claimSizeMinY = TextTemplate.of(TextColor.RED, "The claim y size of ", TextTemplate.arg("size").color(TextColor.GREEN), " is below the minimum size of ", TextTemplate.arg("min-size").color(TextColor.GREEN), ".\nThe area needs to be a minimum of ", TextTemplate.arg("min-area").color(TextColor.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColor.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_MIN_Z)
    public TextTemplate claimSizeMinZ = TextTemplate.of(TextColor.RED, "The claim z size of ", TextTemplate.arg("size").color(TextColor.GREEN), " is below the minimum size of ", TextTemplate.arg("min-size").color(TextColor.GREEN), ".\nThe area needs to be a minimum of ", TextTemplate.arg("min-area").color(TextColor.GREEN), " and a max of ", TextTemplate.arg("max-area").color(TextColor.GREEN));

    @Setting(MessageStorage.CLAIM_SIZE_TOO_SMALL)
    public TextTemplate claimSizeTooSmall = TextTemplate.of(TextColor.RED, "The selected claim size of ", TextTemplate.arg("width"), "x", TextTemplate.arg("length"), " would be too small. A claim must be at least ", TextTemplate.arg("minimum-width"), "x", TextTemplate.arg("minimum-length"), " in size.");

    @Setting("claim-create-cancel")
    public TextTemplate claimCreateCancel = TextTemplate.of(TextColor.RED, "A plugin has denied the creation of this claim.");

    @Setting("claim-create-failed-claim-limit")
    public TextTemplate claimCreateFailedLimit = TextTemplate.of(TextColor.RED, "You've reached your limit on land claims.  Use /AbandonClaim to remove one before creating another.");

    @Setting(MessageStorage.CLAIM_CREATE_INSUFFICIENT_BLOCKS_2D)
    public TextTemplate claimCreateInsufficientBlocks2d = TextTemplate.of(TextColor.RED, "You don't have enough blocks to claim this area.\nYou need ", TextTemplate.arg("remaining-blocks").color(TextColor.GOLD), " more blocks.");

    @Setting(MessageStorage.CLAIM_CREATE_INSUFFICIENT_BLOCKS_3D)
    public TextTemplate claimCreateInsufficientBlocks3d = TextTemplate.of(TextColor.RED, "You don't have enough blocks to claim this area.\nYou need ", TextTemplate.arg("remaining-chunks").color(TextColor.GOLD), " more chunks. (", TextTemplate.arg("remaining-blocks").color(TextColor.WHITE), " blocks)");

    @Setting("claim-create-subdivision-fail")
    public TextTemplate claimCreateSubdivisionFail = TextTemplate.of(TextColor.RED, "No claim exists at selected corner. Please click a valid block location within parent claim in order to create your subdivision.");

    @Setting("claim-delete-all-admin-success")
    public TextTemplate claimDeleteAllAdminSuccess = TextTemplate.of("Deleted all administrative claims.");

    @Setting("claim-delete-all-success")
    public TextTemplate claimDeleteAllSuccess = TextTemplate.of("Deleted all of ", TextTemplate.arg("owner"), "'s claims.");

    @Setting("claim-protected-entity")
    public TextTemplate claimProtectedEntity = TextTemplate.of(TextColor.RED, "That belongs to ", TextTemplate.arg("owner"), ".");

    @Setting("claim-farewell")
    public TextTemplate claimFarewell = TextTemplate.of(TextColor.GREEN, "Set claim farewell to ", TextTemplate.arg("farewell").optional(), ".");

    @Setting("claim-greeting")
    public TextTemplate claimGreeting = TextTemplate.of(TextColor.GREEN, "Set claim greeting to ", TextTemplate.arg("greeting").optional(), ".");

    @Setting("claim-farewell-clear")
    public TextTemplate claimFarewellClear = TextTemplate.of(TextColor.GREEN, "The claim farewell message has been cleared.");

    @Setting("claim-greeting-clear")
    public TextTemplate claimGreetingClear = TextTemplate.of(TextColor.GREEN, "The claim greeting message has been cleared.");

    @Setting("claim-ignore")
    public TextTemplate claimIgnore = TextTemplate.of(TextColor.GREEN, "Now ignoring claims.");

    @Setting("claim-last-active")
    public TextTemplate claimLastActive = TextTemplate.of(TextColor.GREEN, "Claim last active ", TextTemplate.arg("date"), ".");

    @Setting("claim-mode-admin")
    public TextTemplate claimModeAdmin = TextTemplate.of(TextColor.GREEN, "Administrative claims mode active. Any claims created will be free and editable by other administrators.");

    @Setting("claim-mode-basic")
    public TextTemplate claimModeBasic = TextTemplate.of(TextColor.GREEN, "Basic claim creation mode enabled.");

    @Setting("claim-mode-subdivision")
    public TextTemplate claimModeSubdivision = TextTemplate.of(TextColor.GREEN, "Subdivision mode.  Use your shovel to create subdivisions in your existing claims.  Use /modebasic to exit.");

    @Setting("claim-mode-town")
    public TextTemplate claimModeTown = TextTemplate.of(TextColor.GREEN, "Town creation mode enabled.");

    @Setting("claim-type-not-found")
    public TextTemplate claimTypeNotFound = TextTemplate.of(TextColor.RED, "No ", TextTemplate.arg("type"), " claims found.");

    @Setting("claim-not-found")
    public TextTemplate claimNotFound = TextTemplate.of(TextColor.RED, "There's no claim here.");

    @Setting("claim-not-yours")
    public TextTemplate claimNotYours = TextTemplate.of(TextColor.RED, "This isn't your claim.");

    @Setting("claim-no-claims")
    public TextTemplate claimNoClaims = TextTemplate.of(TextColor.RED, "You don't have any land claims.");

    @Setting("claim-owner-already")
    public TextTemplate claimOwnerAlready = TextTemplate.of(TextColor.RED, "You are already the claim owner.");

    @Setting("claim-owner-only")
    public TextTemplate claimOwnerOnly = TextTemplate.of(TextColor.RED, "Only ", TextTemplate.arg("owner"), " can modify this claim.");

    @Setting(MessageStorage.CLAIM_SIZE_NEED_BLOCKS_2D)
    public TextTemplate claimSizeNeedBlocks2d = TextTemplate.of(TextColor.RED, "You don't have enough blocks for this claim size.\nYou need ", TextTemplate.arg("blocks").color(TextColor.GREEN), " more blocks.");

    @Setting(MessageStorage.CLAIM_SIZE_NEED_BLOCKS_3D)
    public TextTemplate claimSizeNeedBlocks3d = TextTemplate.of(TextColor.RED, "You don't have enough blocks for this claim size.\nYou need ", TextTemplate.arg("chunks").color(TextColor.GREEN), " more chunks. (", TextTemplate.arg("blocks").color(TextColor.WHITE), " blocks)");

    @Setting("claim-resize-same-location")
    public TextTemplate claimResizeSameLocation = TextTemplate.of(TextColor.RED, "You must select a different block location to resize claim.");

    @Setting("claim-resize-overlap-subdivision")
    public TextTemplate claimResizeOverlapSubdivision = TextTemplate.of(TextColor.RED, "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use " + "your shovel at a corner to resize it.");

    @Setting("claim-resize-overlap")
    public TextTemplate claimResizeOverlap = TextTemplate.of(TextColor.RED, "Can't resize here because it would overlap another nearby claim.");

    @Setting("claim-resize-start")
    public TextTemplate claimResizeStart = TextTemplate.of(TextColor.GREEN, "Resizing claim.  Use your shovel again at the new location for this corner.");

    @Setting("claim-resize-success-2d")
    public TextTemplate claimResizeSuccess = TextTemplate.of(TextColor.GREEN, "Claim resized. You have ", TextTemplate.arg("remaining-blocks").color(TextColor.GOLD), " more blocks remaining.");

    @Setting(MessageStorage.CLAIM_RESIZE_SUCCESS_3D)
    public TextTemplate claimResizeSuccess3d = TextTemplate.of(TextColor.GREEN, "Claim resized. You have ", TextTemplate.arg("remaining-chunks").color(TextColor.GOLD), " more chunks remaining.\n(", TextTemplate.arg("remaining-blocks").color(TextColor.WHITE), " blocks)");

    @Setting("claim-respecting")
    public TextTemplate claimRespecting = TextTemplate.of(TextColor.GREEN, "Now respecting claims.");

    @Setting("claim-restore-success")
    public TextTemplate claimRestoreSuccess = TextTemplate.of(TextColor.GREEN, "Successfully restored claim.");

    @Setting("claim-show-nearby")
    public TextTemplate claimShowNearby = TextTemplate.of(TextColor.GREEN, "Found ", TextTemplate.arg("claim-count"), " land claims.");

    @Setting("claim-start")
    public TextTemplate claimStart = TextTemplate.of(TextColor.GREEN, TextTemplate.arg("type"), " corner set! Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.");

    @Setting(MessageStorage.CLAIM_ABOVE_LEVEL)
    public TextTemplate claimAboveLevel = TextTemplate.of(TextColor.RED, "Unable to claim block as it is above your maximum claim level limit of ", TextTemplate.arg("claim-level").color(TextColor.GREEN), ". (/playerinfo)");

    @Setting(MessageStorage.CLAIM_BELOW_LEVEL)
    public TextTemplate claimBelowLevel = TextTemplate.of(TextColor.RED, "Unable to claim block as it is below your minimum claim level limit of ", TextTemplate.arg("claim-level").color(TextColor.GREEN), ". (/playerinfo)");

    @Setting("claim-too-far")
    public TextTemplate claimTooFar = TextTemplate.of(TextColor.RED, "That's too far away.");

    @Setting("claim-transfer-success")
    public TextTemplate claimTransferSuccess = TextTemplate.of(TextColor.GREEN, "Claim transferred.");

    @Setting("claim-transfer-exceeds-limit")
    public TextTemplate claimTransferExceedsLimit = TextTemplate.of(TextColor.RED, "Claim could not be transferred as it would exceed the new owner's creation limit.");

    @Setting("claim-deleted")
    public TextTemplate claimDeleted = TextTemplate.of(TextColor.GREEN, "Claim deleted.");

    @Setting("claim-disabled-world")
    public TextTemplate claimDisabledWorld = TextTemplate.of(TextColor.RED, "Claims are disabled in this world.");

    @Setting("command-abandon-claim-missing")
    public TextTemplate commandAbandonClaimMissing = TextTemplate.of("Stand in the claim you want to delete, or consider /abandonall.");

    @Setting("command-abandon-top-level")
    public TextTemplate commandAbandonTopLevel = TextTemplate.of(TextColor.RED, "This claim cannot be abandoned as it contains one or more child claims. In order to abandon a claim with child claims, you must use /abandontop instead.");

    @Setting("command-abandon-town-children")
    public TextTemplate commandAbandonTownChildren = TextTemplate.of(TextColor.RED, "You do not have permission to abandon a town with child claims you do not own. Use /ignoreclaims or have the child claim owner abandon their claim first. If you just want to abandon the town without affecting children then use /abandonclaim instead.");

    @Setting("command-blocked")
    public TextTemplate commandBlocked = TextTemplate.of(TextColor.RED, "The command ", TextTemplate.arg("command"), " has been blocked by claim owner ", TextTemplate.arg("owner"), ".");

    @Setting("command-create-worldedit")
    public TextTemplate commandCreateWorldEdit = TextTemplate.of(TextColor.RED, "This command requires WorldEdit to be installed on server.");

    @Setting("command-farewell")
    public TextTemplate commandFarewell = TextTemplate.of(TextColor.GREEN, "Claim flag ", TextTemplate.arg("flag").optional(), " is invalid.");

    @Setting("command-greeting")
    public TextTemplate commandGreeting = TextTemplate.of(TextColor.GREEN, "Set claim greeting to ", TextTemplate.arg("greeting").optional(), ".");

    @Setting("command-group-invalid")
    public TextTemplate commandGroupInvalid = TextTemplate.of(TextColor.RED, "Group ", TextTemplate.arg("group"), " is not valid.");

    @Setting("command-inherit")
    public TextTemplate commandInherit = TextTemplate.of(TextColor.RED, "This command can only be used in child claims.");

    @Setting("command-claim-name")
    public TextTemplate commandClaimName = TextTemplate.of(TextColor.GREEN, "Set claim name to ", TextTemplate.arg("name"), ".");

    @Setting("command-option-exceeds-admin")
    public TextTemplate commandOptionExceedsAdmin = TextTemplate.of(TextColor.RED, "Option value of ", TextTemplate.arg("original_value").color(TextColor.GREEN), " exceeds admin set value of '", TextTemplate.arg("admin_value").color(TextColor.GREEN),"'. Adjusting to admin value...");

    @Setting("command-option-invalid-claim")
    public TextTemplate commandOptionInvalidClaim = TextTemplate.of(TextColor.RED, "This command cannot be used in subdivisions.");

    @Setting("command-player-invalid")
    public TextTemplate commandPlayerInvalid = TextTemplate.of(TextColor.RED, "Player ", TextTemplate.arg("player"), " is not valid.");

    @Setting("command-acb-success")
    public TextTemplate commandAcbSuccess = TextTemplate.of(TextColor.GREEN, "Updated accrued claim blocks.");
 
    @Setting("command-spawn-not-set")
    public TextTemplate commandSpawnNotSet = TextTemplate.of(TextColor.RED, "No claim spawn has been set.");

    @Setting("command-spawn-set-success")
    public TextTemplate commandSpawnSet = TextTemplate.of(TextColor.GREEN, "Successfully set claim spawn to ", TextTemplate.arg("location"), ".");

    @Setting("command-spawn-teleport")
    public TextTemplate commandSpawnTeleport = TextTemplate.of(TextColor.GREEN, "Teleported to claim spawn at ", TextTemplate.arg("location"), ".");

    @Setting("cuboid-claim-disabled")
    public TextTemplate claimCuboidDisabled = TextTemplate.of(TextColor.GREEN, "Now claiming in 2D mode.");

    @Setting("cuboid-claim-enabled")
    public TextTemplate claimCuboidEnabled = TextTemplate.of(TextColor.GREEN, "Now claiming in 3D mode.");

    @Setting("economy-not-enough-funds")
    public TextTemplate economyNotEnoughFunds = TextTemplate.of(TextColor.RED, "You do not have enough funds to purchase this land. Your current economy balance is '", TextTemplate.arg("balance").color(TextColor.GOLD), "' but you require '", TextTemplate.arg("cost").color(TextColor.GREEN), "' to complete the purchase.");

    @Setting("economy-blocks-purchase-cost")
    public TextTemplate economyBlockPurchaseCost = TextTemplate.of("Each claim block costs ", TextTemplate.arg("cost"), ".  Your balance is ", TextTemplate.arg("balance"), ".");

    @Setting("economy-blocks-not-available")
    public TextTemplate economyBlocksNotAvailable = TextTemplate.of(TextColor.RED, "You don't have that many claim blocks available for sale.");

    @Setting("economy-blocks-buy-sell-not-configured")
    public TextTemplate economyBuySellNotConfigured = TextTemplate.of(TextColor.RED, "Sorry, buying and selling claim blocks is disabled.");

    @Setting("economy-blocks-buy-invalid-count")
    public TextTemplate economyBuyInvalidBlockCount = TextTemplate.of(TextColor.RED, "Block count must be greater than 0.");

    @Setting("economy-not-installed")
    public TextTemplate economyNotInstalled = TextTemplate.of(TextColor.RED, "Economy plugin not installed!.");

    @Setting("economy-blocks-only-buy")
    public TextTemplate economyOnlyBuyBlocks = TextTemplate.of(TextColor.RED, "Claim blocks may only be purchased, not sold.");

    @Setting("economy-blocks-only-sell")
    public TextTemplate economyOnlySellBlocks = TextTemplate.of(TextColor.RED, "Claim blocks may only be sold, not purchased.");

    @Setting("economy-blocks-purchase-confirmation")
    public TextTemplate economyBlocksPurchaseConfirmation = TextTemplate.of(TextColor.GREEN, "Withdrew ", TextTemplate.arg("cost"), " from your account.  You now have ", TextTemplate.arg("remaining-blocks"), " available claim blocks.");

    @Setting("economy-blocks-sale-confirmation")
    public TextTemplate economyBlockSaleConfirmation = TextTemplate.of(TextColor.GREEN, "Deposited ", TextTemplate.arg("deposit"), " in your account.  You now have ", TextTemplate.arg("remaining-blocks"), " available claim blocks.");

    @Setting("economy-blocks-sell-error")
    public TextTemplate economyBlockSellError = TextTemplate.of(TextColor.RED, "Could not sell blocks. Reason: ", TextTemplate.arg("reason"), ".");

    @Setting("economy-claim-abandon-success")
    public TextTemplate economyClaimAbandonSuccess = TextTemplate.of(TextColor.GREEN, "Claim(s) abandoned. You have been refunded a total of '", TextTemplate.arg("refund").color(TextColor.GOLD), "'.");

    @Setting("economy-claim-not-for-sale")
    public TextTemplate economyClaimNotForSale = TextTemplate.of(TextColor.RED, "This claim is not for sale.");

    @Setting("economy-claim-buy-not-enough-funds")
    public TextTemplate economyClaimBuyNotEnoughFunds = TextTemplate.of(TextColor.RED, "You do not have enough funds to purchase this claim for ", TextTemplate.arg("sale_price").color(TextColor.GOLD), ". You currently have a balance of ", TextTemplate.arg("balance").color(TextColor.GOLD), " and need ", TextTemplate.arg("amount_needed").color(TextColor.GOLD), " more for purchase.");

    @Setting("economy-claim-buy-confirmation")
    public TextTemplate economyClaimBuyConfirmation = TextTemplate.of(TextColor.GREEN, "Are you sure you want to buy this claim for ", TextTemplate.arg("sale_price").color(TextColor.GOLD), " ? Click confirm to proceed.");

    @Setting("economy-claim-buy-confirmed")
    public TextTemplate economyClaimBuyConfirmed = TextTemplate.of(TextColor.GREEN, "You have successfully bought the claim for ", TextTemplate.arg("sale_price").color(TextColor.GOLD), ".");

    @Setting("economy-claim-sale-cancelled")
    public TextTemplate economyClaimSaleCancelled = TextTemplate.of(TextColor.GREEN, "You have cancelled your claim sale.");

    @Setting("economy-claim-sale-confirmation")
    public TextTemplate economyClaimSaleConfirmation = TextTemplate.of(TextColor.GREEN, "Are you sure you want to sell your claim for ", TextTemplate.arg("sale_price").color(TextColor.GOLD), " ? If your claim is sold, all items and blocks will be transferred to the buyer. Click confirm if this is OK.");

    @Setting(MessageStorage.ECONOMY_CLAIM_SALE_CONFIRMED)
    public TextTemplate economyClaimSaleConfirmed = TextTemplate.of(TextColor.GREEN, "You have successfully put your claim up for sale for the amount of ", TextTemplate.arg("sale_price").color(TextColor.GOLD), ".");

    @Setting("economy-claim-sale-invalid-price")
    public TextTemplate economyClaimSaleInvalidPrice = TextTemplate.of(TextColor.RED, "The sale price of, ", TextTemplate.arg("sale_price").color(TextColor.GOLD), " must be greater than or equal to 0.");

    @Setting("economy-claim-sold")
    public TextTemplate economyClaimSold = TextTemplate.of(TextColor.GREEN, "Your claim sold! The amount of ", TextTemplate.arg("amount").color(TextColor.GOLD), " has been deposited into your account. Your total available balance is now ", TextTemplate.arg("balance").color(TextColor.GOLD));

    @Setting(MessageStorage.ECONOMY_USER_NOT_FOUND)
    public TextTemplate economyUserNotFound = TextTemplate.of(TextColor.RED, "No economy account found for user ", TextTemplate.arg("user"), ".");

    @Setting("economy-withdraw-error")
    public TextTemplate economyWithdrawError = TextTemplate.of(TextColor.RED, "Could not withdraw funds. Reason: ", TextTemplate.arg("reason"), ".");

    @Setting("economy-virtual-not-supported")
    public TextTemplate economyVirtualNotSupported = TextTemplate.of(TextColor.RED, "Economy plugin does not support virtual accounts which is required. Use another economy plugin or contact plugin dev for virtual account support.");

    @Setting("flag-invalid-context")
    public TextTemplate flagInvalidContext = TextTemplate.of(TextColor.RED, "Invalid context '", TextTemplate.arg("context") + "' entered for base flag ", TextTemplate.arg("flag"), ".");

    @Setting("flag-invalid-meta")
    public TextTemplate flagInvalidMeta = TextTemplate.of(TextColor.RED, "Invalid target meta '", TextTemplate.arg("meta") + "' entered for base flag ", TextTemplate.arg("flag"), ".");

    @Setting("flag-invalid-target")
    public TextTemplate flagInvalidTarget = TextTemplate.of(TextColor.RED, "Invalid target '", TextTemplate.arg("target"), "' entered for base flag ", TextTemplate.arg("flag"), ".");

    @Setting("flag-overridden")
    public TextTemplate flagOverridden = TextTemplate.of(TextColor.RED, "Failed to set claim flag. The flag ", TextTemplate.arg("flag"), " has been overridden by an admin.");

    @Setting("flag-overrides-not-supported")
    public TextTemplate flagOverridesNotSupported = TextTemplate.of("Claim type ", TextTemplate.arg("type"), " does not support flag overrides.");

    @Setting("flag-reset-success")
    public TextTemplate flagResetSuccess = TextTemplate.of(TextColor.GREEN, "Claim flags reset to defaults successfully.");

    @Setting("nucleus-no-sethome")
    public TextTemplate nucleusNoSetHome = TextTemplate.of(TextColor.RED, "You must be trusted in order to use /sethome here.");

    @Setting("owner-admin")
    public TextTemplate ownerAdmin = TextTemplate.of("an administrator");

    @Setting("permission-access")
    public TextTemplate permissionAccess = TextTemplate.of(TextColor.RED, "You don't have ", TextTemplate.arg("player").color(TextColor.GOLD), "'s permission to access that.");

    @Setting("permission-build")
    public TextTemplate permissionBuild = TextTemplate.of(TextColor.RED, "You don't have ", TextTemplate.arg("player").color(TextColor.GOLD), "'s permission to build.");

    @Setting("permission-build-near-claim")
    public TextTemplate permissionBuildNearClaim = TextTemplate.of(TextColor.RED, "You don't have ", TextTemplate.arg("owner").color(TextColor.GOLD), "'s permission to build near claim.");

    @Setting("permission-claim-create")
    public TextTemplate permissionClaimCreate = TextTemplate.of(TextColor.RED, "You don't have permission to claim land.");

    @Setting("permission-claim-delete")
    public TextTemplate permissionClaimDelete = TextTemplate.of(TextColor.RED, "You don't have permission to delete ", TextTemplate.arg("type"), " claims.");

    @Setting("permission-claim-enter")
    public TextTemplate permissionClaimEnter = TextTemplate.of(TextColor.RED, "You don't have permission to enter this claim.");

    @Setting("permission-claim-exit")
    public TextTemplate permissionClaimExit = TextTemplate.of(TextColor.RED, "You don't have permission to exit this claim.");

    @Setting("permission-claim-ignore")
    public TextTemplate permissionClaimIgnore = TextTemplate.of(TextColor.RED, "You do not have permission to ignore ", TextTemplate.arg("type"), " claims.");

    @Setting("permission-claim-list")
    public TextTemplate permissionClaimList = TextTemplate.of(TextColor.RED, "You don't have permission to get information about another player's land claims.");

    @Setting("permission-claim-manage")
    public TextTemplate permissionClaimManage = TextTemplate.of(TextColor.RED, "You don't have permission to manage ", TextTemplate.arg("type"), " claims.");

    @Setting("permission-claim-reset-flags")
    public TextTemplate permissionClaimResetFlags = TextTemplate.of(TextColor.RED, "You don't have permission to reset ", TextTemplate.arg("type"), " claims to flag defaults.");

    @Setting("permission-claim-reset-flags-self")
    public TextTemplate permissionClaimResetFlagsSelf = TextTemplate.of(TextColor.RED, "You don't have permission to reset your claim flags to defaults.");

    @Setting("permission-claim-resize")
    public TextTemplate permissionClaimResize = TextTemplate.of(TextColor.RED, "You don't have permission to resize this claim.");

    @Setting("permission-claim-sale")
    public TextTemplate permissionClaimSale = TextTemplate.of(TextColor.RED, "You don't have permission to sell this claim.");

    @Setting("permission-claim-transfer-admin")
    public TextTemplate permissionClaimTransferAdmin = TextTemplate.of(TextColor.RED, "You don't have permission to transfer admin claims.");

    @Setting("permission-clear-all")
    public TextTemplate permissionClearAll = TextTemplate.of(TextColor.RED, "Only the claim owner can clear all permissions.");

    @Setting("permission-clear")
    public TextTemplate permissionClear = TextTemplate.of(TextColor.RED, "Cleared permissions in this claim.  To set permission for ALL your claims, stand outside them.");

    @Setting("permission-command-trust")
    public TextTemplate permissionCommandTrust = TextTemplate.of(TextColor.RED, "You don't have permission to use this type of trust.");

    @Setting("permission-cuboid")
    public TextTemplate permissionCuboid = TextTemplate.of(TextColor.RED, "You don't have permission to create/resize basic claims in 3D mode.");

    @Setting("permission-edit-claim")
    public TextTemplate permissionEditClaim = TextTemplate.of(TextColor.RED, "You don't have permission to edit this claim.");

    @Setting("permission-fire-spread")
    public TextTemplate permissionFireSpread = TextTemplate.of(TextColor.RED, "You don't have permission to spread fire in this claim.");

    @Setting("permission-flag-defaults")
    public TextTemplate permissionFlagDefaults = TextTemplate.of(TextColor.RED, "You don't have permission to manage flag defaults.");

    @Setting("permission-flag-overrides")
    public TextTemplate permissionFlagOverrides = TextTemplate.of(TextColor.RED, "You don't have permission to manage flag overrides.");

    @Setting("permission-flag-use")
    public TextTemplate permissionFlagUse = TextTemplate.of(TextColor.RED, "You don't have permission to use this flag.");

    @Setting("permission-flow-liquid")
    public TextTemplate permissionFlowLiquid = TextTemplate.of(TextColor.RED, "You don't have permission to flow liquid in this claim.");

    @Setting("permission-interact-block")
    public TextTemplate permissionInteractBlock = TextTemplate.of(TextColor.RED, "You don't have ", TextTemplate.arg("owner").color(TextColor.GOLD), "'s", TextColor.RED, " permission to interact with the block ", TextTemplate.arg("block").color(TextColor.LIGHT_PURPLE), ".");

    @Setting("permission-interact-entity")
    public TextTemplate permissionInteractEntity = TextTemplate.of(TextColor.RED, "You don't have ", TextTemplate.arg("owner").color(TextColor.GOLD), "'s", TextColor.RED, " permission to interact with the entity ", TextTemplate.arg("entity").color(TextColor.LIGHT_PURPLE), ".");

    @Setting("permission-interact-item-block")
    public TextTemplate permissionInteractItemBlock = TextTemplate.of(TextColor.RED, "You don't have permission to use ", TextTemplate.arg("item").color(TextColor.LIGHT_PURPLE), TextColor.RED, " on a ", TextTemplate.arg("block").color(TextColor.AQUA), ".");

    @Setting("permission-interact-item-entity")
    public TextTemplate permissionInteractItemEntity = TextTemplate.of(TextColor.RED, "You don't have permission to use ", TextTemplate.arg("item").color(TextColor.LIGHT_PURPLE), TextColor.RED, " on a ", TextTemplate.arg("entity").color(TextColor.AQUA), ".");

    @Setting("permission-interact-item")
    public TextTemplate permissionInteractItem = TextTemplate.of(TextColor.RED, "You don't have ", TextTemplate.arg("owner").color(TextColor.GOLD), "'s", TextColor.RED, " permission to interact with the item ", TextTemplate.arg("item").color(TextColor.LIGHT_PURPLE), ".");

    @Setting("permission-inventory-open")
    public TextTemplate permissionInventoryOpen = TextTemplate.of(TextColor.RED, "You don't have ", TextTemplate.arg("owner").color(TextColor.GOLD), "'s", TextColor.RED, " permission to open ", TextTemplate.arg("block").color(TextColor.LIGHT_PURPLE), ".");

    @Setting("permission-item-drop")
    public TextTemplate permissionItemDrop = TextTemplate.of(TextColor.RED, "You don't have ", TextTemplate.arg("owner").color(TextColor.GOLD), "'s", TextColor.RED, " permission to drop the item ", TextTemplate.arg("item").color(TextColor.LIGHT_PURPLE), " in this claim.");

    @Setting("permission-item-use")
    public TextTemplate permissionItemUse = TextTemplate.of(TextColor.RED, "You can't use the item ", TextTemplate.arg("item"), " in this claim.");

    @Setting("permission-grant")
    public TextTemplate permissionGrant = TextTemplate.of(TextColor.RED, "You can't grant a permission you don't have yourself.");

    @Setting("permission-global-option")
    public TextTemplate permissionGlobalOption = TextTemplate.of(TextColor.RED, "You don't have permission to manage global options.");

    @Setting("permission-group-option")
    public TextTemplate permissionGroupOption = TextTemplate.of(TextColor.RED, "You don't have permission to assign an option to a group.");

    @Setting("permission-assign-without-having")
    public TextTemplate permissionAssignWithoutHaving = TextTemplate.of(TextColor.RED, "You are not allowed to assign a permission that you do not have.");

    @Setting("permission-override-deny")
    public TextTemplate permissionOverrideDeny = TextTemplate.of(TextColor.RED, "The action you are attempting to perform has been denied by an administrator's override flag.");

    @Setting("permission-player-option")
    public TextTemplate permissionPlayerOption = TextTemplate.of(TextColor.RED, "You don't have permission to assign an option to a player.");

    @Setting("permission-player-admin-flags")
    public TextTemplate permissionSetAdminFlags = TextTemplate.of(TextColor.RED, "You don't have permission to change flags on an admin player.");

    @Setting("permission-player-not-ignorable")
    public TextTemplate permissionPlayerNotIgnorable = TextTemplate.of(TextColor.RED, "You can't ignore that player.");

    @Setting("permission-portal-enter")
    public TextTemplate permissionPortalEnter = TextTemplate.of(TextColor.RED, "You can't use this portal because you don't have ", TextTemplate.arg("owner"), "'s permission to enter the destination land claim.");

    @Setting("permission-portal-exit")
    public TextTemplate permissionPortalExit = TextTemplate.of(TextColor.RED, "You can't use this portal because you don't have ", TextTemplate.arg("owner"), "'s permission to build an exit portal in the destination land claim.");

    @Setting("permission-protected-portal")
    public TextTemplate permissionProtectedPortal = TextTemplate.of(TextColor.RED, "You don't have permission to use portals in this claim owned by ", TextTemplate.arg("owner"), ".");

    @Setting("permission-trust")
    public TextTemplate permissionTrust = TextTemplate.of(TextColor.RED, "You don't have ", TextTemplate.arg("owner"), "'s permission to manage permissions here.");

    @Setting("permission-visual-claims-nearby")
    public TextTemplate permissionVisualClaimsNearby = TextTemplate.of(TextColor.RED, "You don't have permission to visualize nearby claims.");

    @Setting("player-remaining-blocks-2d")
    public TextTemplate playerRemainingBlocks2d = TextTemplate.of(TextColor.YELLOW, "You may claim up to ", TextTemplate.arg("remaining-blocks").color(TextColor.GREEN), " more blocks.");

    @Setting("player-remaining-blocks-3d")
    public TextTemplate playerRemainingBlocks3d = TextTemplate.of(TextColor.YELLOW, "You may claim up to ", TextTemplate.arg("remaining-chunks").color(TextColor.GREEN), " more chunks. (", TextTemplate.arg("remaining-blocks").color(TextColor.WHITE), " blocks)");

    @Setting("plugin-reload")
    public TextTemplate pluginReload = TextTemplate.of("GriefDefender has been reloaded.");

    @Setting("restore-nature-activate")
    public TextTemplate restoreNatureActivate = TextTemplate.of(TextColor.GREEN, "Ready to restore claim! Right click on a block to restore. Use /modebasic to stop.");

    @Setting(MessageStorage.SCHEMATIC_RESTORE_CONFIRMED)
    public TextTemplate schematicRestoreConfirmed = TextTemplate.of(TextColor.GREEN, "You have successfully restored your claim from schematic backup ", TextTemplate.arg("schematic_name").color(TextColor.GOLD), ".");

    @Setting("tax-claim-expired")
    public TextTemplate taxClaimExpired = TextTemplate.of(TextColor.RED, "This claim has been frozen due to unpaid taxes. The current amount owed is '", TextTemplate.arg("tax_owed").color(TextColor.GOLD), "'." 
            + "\nThere are '", TextTemplate.arg("remaining_days").color(TextColor.GREEN), "' days left to deposit payment to claim bank in order to unfreeze this claim.\nFailure to pay this debt will result in deletion of claim.\nNote: To deposit funds to claimbank, use /claimbank deposit <amount>.");

    @Setting("tax-claim-paid-balance")
    public TextTemplate taxClaimPaidBalance = TextTemplate.of(TextColor.GREEN, "The tax debt of '", TextTemplate.arg("amount").color(TextColor.GOLD), "' has been paid. Your claim has been unfrozen and is now available for use.");

    @Setting("tax-claim-paid-partial")
    public TextTemplate taxClaimPaidPartial = TextTemplate.of(TextColor.GREEN, "The tax debt of '", TextTemplate.arg("amount").color(TextColor.GOLD), "' has been partially paid. In order to unfreeze your claim, the remaining tax owed balance of '", TextTemplate.arg("balance").color(TextColor.GOLD), "' must be paid.");

    @Setting("tax-info")
    public TextTemplate taxInfo = TextTemplate.of(TextColor.GREEN, "Your next scheduled tax payment of ", TextTemplate.arg("amount").color(TextColor.GOLD), " will be withdrawn from your account on ", TextColor.AQUA, TextTemplate.arg("withdraw_date"), ".");

    @Setting("tax-past-due")
    public TextTemplate taxPastDue = TextTemplate.of(TextColor.RED, "You currently have a past due tax balance of ", TextTemplate.arg("balance_due").color(TextColor.GOLD), TextColor.RED, " that must be paid by ", TextTemplate.arg("due_date").color(TextColor.AQUA), TextColor.RED, ". Failure to pay off your tax balance will result in losing your property.");

    @Setting("town-create-not-enough-funds")
    public TextTemplate townCreateNotEnoughFunds = TextTemplate.of(TextColor.RED, "You do not have enough funds to create this town for ", TextTemplate.arg("create_cost").color(TextColor.GOLD), ". You currently have a balance of ", TextTemplate.arg("balance").color(TextColor.GOLD), " and need ", TextTemplate.arg("amount_needed").color(TextColor.GOLD), " more for creation.");

    @Setting("town-chat-disabled")
    public TextTemplate townChatDisabled = TextTemplate.of("Town chat disabled.");

    @Setting("town-chat-enabled")
    public TextTemplate townChatEnabled = TextTemplate.of("Town chat enabled.");

    @Setting("town-name")
    public TextTemplate townName = TextTemplate.of("Set town name to ", TextTemplate.arg("name"), ".");

    @Setting("town-not-found")
    public TextTemplate townNotFound = TextTemplate.of(TextColor.RED, "Town not found.");

    @Setting("town-not-in")
    public TextTemplate townNotIn = TextTemplate.of(TextColor.RED, "You are not in a town.");

    @Setting("town-tag")
    public TextTemplate townTag = TextTemplate.of("Set town tag to ", TextTemplate.arg("tag"), ".");

    @Setting("town-tag-clear")
    public TextTemplate townTagClear = TextTemplate.of(TextColor.GREEN, "The town tag has been cleared.");

    @Setting("town-tax-no-claims")
    public TextTemplate townTaxNoClaims = TextTemplate.of(TextColor.RED, "You must own property in this town in order to be taxed.");

    @Setting("town-owner")
    public TextTemplate townTrust = TextTemplate.of(TextColor.RED, "That belongs to the town.");

    @Setting("trust-already-has")
    public TextTemplate trustAlreadyHas = TextTemplate.of(TextColor.RED, TextTemplate.arg("target").color(TextColor.AQUA), " already has ", TextTemplate.arg("type"), " permission.");

    @Setting("trust-current-claim")
    public TextTemplate trustGrant = TextTemplate.of(TextColor.GREEN, "Granted ", TextTemplate.arg("target").color(TextColor.AQUA), " permission to ", TextTemplate.arg("type").color(TextColor.LIGHT_PURPLE), " in current claim.");

    @Setting("trust-invalid")
    public TextTemplate trustInvalid = TextTemplate.of(TextColor.RED, "Invalid trust type entered.\nThe allowed types are : accessor, builder, container, and manager.");

    @Setting("trust-individual-all-claims")
    public TextTemplate trustIndividualAllClaims = TextTemplate.of(TextColor.GREEN, "Granted ", TextTemplate.arg("player").color(TextColor.AQUA), "'s full trust to all your claims.  To unset permissions for ALL your claims, use /untrustall.");

    @Setting("trust-no-claims")
    public TextTemplate trustNoClaims = TextTemplate.of(TextColor.RED, "You have no claims to trust.");

    @Setting("trust-self")
    public TextTemplate trustSelf = TextTemplate.of(TextColor.RED, "You cannot trust yourself.");

    @Setting("untrust-individual-all-claims")
    public TextTemplate untrustIndividualAllClaims = TextTemplate.of(TextColor.GREEN, "Revoked ", TextTemplate.arg("target").color(TextColor.AQUA), "'s access to ALL your claims.  To set permissions for a single claim, stand inside it and use /untrust.");

    @Setting("untrust-individual-single-claim")
    public TextTemplate untrustIndividualSingleClaim = TextTemplate.of(TextColor.GREEN, "Revoked ", TextTemplate.arg("target").color(TextColor.AQUA), "'s access to this claim.  To unset permissions for ALL your claims, use /untrustall.");

    @Setting("untrust-owner")
    public TextTemplate untrustOwner = TextTemplate.of(TextColor.RED, TextTemplate.arg("owner").color(TextColor.AQUA), " is owner of claim and cannot be untrusted.");

    @Setting("untrust-no-claims")
    public TextTemplate untrustNoClaims = TextTemplate.of(TextColor.RED, "You have no claims to untrust.");

    @Setting("untrust-self")
    public TextTemplate untrustSelf = TextTemplate.of(TextColor.RED, "You cannot untrust yourself.");

    @Setting("tutorial-claim-basic")
    public TextTemplate tutorialClaimBasic = TextTemplate.of(TextColor.YELLOW, "Click for Land Claim Help: ", TextColor.GREEN, "https://bit.ly/2J1IKIC");

}
