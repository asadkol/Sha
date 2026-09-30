# Shah Pharmacy - Android Application

A modern, native Android application built with **Kotlin** and **Jetpack Compose** for comprehensive pharmacy management and day-to-day accounting.

## Features

- **Role-Based Authentication**:
  - **Admin**: Full access to Dashboard, Purchases, Daily Sales, Expenses, Stock, Reports, Cash In Hand, and User Management.
  - **Staff**: Dedicated access to Dashboard, Daily Sales, Expenses, and Stock.
  - Quick demo sign-in shortcuts for Admin and Staff.
- **Interactive Dashboard**:
  - Real-time today's KPI metrics: Total Sales, Purchases, Expenses, and Net Cash in Hand.
  - Stock valuation overview (Purchase value, Potential Sale value, and Potential Margin).
  - Estimated Net Profit calculation based on historical margin ratios.
  - Live Inventory Alerts for low stock (≤ 5 units) and medicines expiring within 60 days.
- **Purchase Management**:
  - Detailed medicine purchase entries with quantity, purchase price, sale price, batch numbers, and expiration dates.
  - CSV bulk import support for rapid inventory onboarding.
  - Full purchase history and management.
- **Daily Sales**:
  - Day-end total revenue recording per day (streamlined single-entry workflow without medicine-by-medicine friction).
- **Expense Tracking**:
  - Categorized expense recording (Electricity, Rent, Salary, Transport, Other) with notes.
- **Stock & Inventory**:
  - Searchable inventory by medicine name or pharmaceutical company.
  - Unit cost and sale prices, inventory valuation, and stock level badges.
- **Financial Reports**:
  - Multi-period summaries: Weekly (last 7 days), Monthly (current month), and All-Time.
  - Comparative analysis of revenue, expenditures, and estimated net profit.
- **Cash in Hand Reconciliation**:
  - Real-time cash formula: `Opening Float + Sales - Purchases - Expenses = Cash in Hand`.
  - Configurable register opening cash float.
- **Data Backup & Security**:
  - Local database encryption via AndroidKeyStore AES/GCM.
  - JSON export to clipboard and JSON restoration.

## Architecture

- **UI Framework**: Jetpack Compose with Material Design 3 (M3).
- **Architecture**: MVVM with Kotlin Coroutines and StateFlow.
- **Storage**: Encrypted SQLite storage matching the original schema.
- **Adaptive**: Responsive layouts optimized for mobile, foldable, and tablet devices.
