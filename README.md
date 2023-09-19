# Placeholder Expansion for Tebex

This document provides a comprehensive explanation of the placeholders available for Tebex integration. These placeholders enable the retrieval of various information regarding recent purchases, top donors, earnings, and more.

## Placeholders

### Recent Purchases:
- `%buycraftAPI_recent_name_<number>%`: Retrieve the name of the recent purchase for a specific slot number.
- `%buycraftAPI_vault_recent_name_<number>%`: Retrieve the name of the recent purchase for a specific slot number, integrated with Vault.
- `%buycraftAPI_recent_currency_<number>%`: Retrieve the currency of the recent purchase for a specific slot number.
- `%buycraftAPI_recent_amount_<number>%`: Retrieve the amount of the recent purchase for a specific slot number.

### Top Donors:
- `%buycraftAPI_top_donor_monthly_value%`: Retrieve the value of the top donor for the current month.
- `%buycraftAPI_top_donor_current_month_value%`: Retrieve the value of the top donor for the current month.
- `%buycraftAPI_top_donor_global_value%`: Retrieve the value of the global top donor.

### Earnings:
- `%buycraftAPI_total_earnings_global%`: Retrieve the total earnings globally.
- `%buycraftAPI_total_earnings_monthly%`: Retrieve the total monthly earnings.

### Top Donors Details:
- `%buycraftAPI_top_donor_global_name_<number>%`: Retrieve the name of the top donor for a specific slot number globally.
- `%buycraftAPI_top_donor_monthly_name_<number>%`: Retrieve the name of the top donor for a specific slot number for the current month.
- `%buycraftAPI_top_donor_global_amount_<number>%`: Retrieve the amount donated by the top donor for a specific slot number globally.
- `%buycraftAPI_top_donor_monthly_amount_<number>%`: Retrieve the amount donated by the top donor for a specific slot number for the current month.

### Vault Integration Details:
- `%buycraftAPI_vault_top_donor_global_name_<number>%`: Retrieve the name of the top donor for a specific slot number globally with Vault integration.
- `%buycraftAPI_vault_top_donor_monthly_name_<number>%`: Retrieve the name of the top donor for a specific slot number for the current month with Vault integration.
- `%buycraftAPI_top_donor_global_currency_<number>%`: Retrieve the currency of the top donor for a specific slot number globally.
- `%buycraftAPI_top_donor_monthly_currency_<number>%`: Retrieve the currency of the top donor for a specific slot number for the current month.

### Current Month Details:
- `%buycraftAPI_top_donor_current_month_name_<number>%`: Retrieve the name of the top donor for a specific slot number for the current month.
- `%buycraftAPI_top_donor_current_month_price_<number>%`: Retrieve the price of the top donor for a specific slot number for the current month.
- `%buycraftAPI_vault_top_donor_current_month_name_<number>%`: Retrieve the name of the top donor for a specific slot number for the current month with Vault integration.
- `%buycraftAPI_total_earnings_current_month%`: Retrieve the total earnings for the current month.

### Additional Information:
- `%buycraftAPI_info%`: Retrieve general information about Buycraft.

## Installation

This expansion can be installed in two ways:

### Method 1: Manual Installation
1. **Download the JAR File:**
   Download the JAR file containing the Tebex placeholder expansions from Spigot, GitHub, or Modrinth.

2. **Move JAR to PlaceholderAPI Expansions Folder:**
   Place the downloaded JAR file into the `expansions` folder of the PlaceholderAPI plugin within your server. This folder is usually located in the plugins directory: `plugins/PlaceholderAPI/expansions/`.

3. **Restart Your Server:**
   Restart your server to load the Tebex placeholder expansions and make them available for use in PlaceholderAPI.

### Method 2: Using PAPI ECloud
1. **Download and Install via PAPI ECloud:**
   Execute the following commands in your server console:
    - `/papi ecloud download BuyCraftAPI`: Downloads the Tebex placeholder expansion.
    - `/papi reload`: Reloads PlaceholderAPI to make the expansion available.

## Updating the Expansion
- **Manual Update:**
  To manually update the expansion, replace the existing JAR file in the `expansions` folder with the updated version.

- **Using PAPI ECloud:**
  Use the following command to update the expansion through PAPI ECloud:
    - `/papi ecloud update BuyCraftAPI`: Updates the expansion to the latest version.
    - `/papi reload`: Reloads PlaceholderAPI to make the expansion available.

## Contact Information
For further assistance or inquiries, you can contact AlexDev03 on Discord: `AlexDev03#0000`.
