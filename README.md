# The Feast - Food Delivery & E-Commerce Application

**The Feast** is a modern Android application designed for a seamless food ordering and delivery experience. Built with Java and powered by Firebase, it features a robust authentication system, real-time data synchronization, integrated payments, and a rich user interface.

## 🚀 Key Features

### 1. Authentication & User Management
*   **Multi-mode Sign-in:** Supports traditional Email/Password authentication and Google One Tap Sign-In.
*   **Credential Manager:** Uses the latest Android Credential Manager API for secure and easy login.
*   **Profile Management:** Users can view and manage their profiles, including display names and profile photos synced from Google.

### 2. Product Discovery
*   **Dynamic Home Dashboard:** Features a promotional carousel with auto-scroll and category-based filtering using Material Chips.
*   **Smart Search:** Real-time product search functionality to find items quickly.
*   **Product Details:** Detailed product views with high-quality images, descriptions, and "Add to Cart/Favourite" options.

### 3. Order Management
*   **Shopping Cart:** Persistent cart management allowing users to adjust quantities and manage items before checkout.
*   **Order History:** Comprehensive view of past orders with detailed item breakdowns.
*   **Favourites:** A dedicated section for users to save and quickly access their preferred products.

### 4. Payments & Delivery
*   **Stripe Integration:** Secure payment processing using the Stripe Android SDK.
*   **Google Maps Integration:** Interactive map selection for delivery addresses and location tracking.
*   **Real-time Notifications:** Integrated Firebase Cloud Messaging (FCM) for order updates and welcome messages.

## 🛠 Technology Stack

*   **Language:** Java
*   **Minimum SDK:** 24 (Android 7.0) | **Target SDK:** 36 (Android 15)
*   **Architecture:** Fragment-based UI with View Binding and ViewPager2.
*   **Backend & Services:**
    *   **Firebase Authentication:** Identity management.
    *   **Firebase Firestore:** NoSQL database for products, orders, and user data.
    *   **Firebase Cloud Messaging (FCM):** Push notifications.
    *   **Firebase Storage:** Hosting for product and carousel images.
*   **Libraries:**
    *   **Networking:** Retrofit & GSON for API communication.
    *   **Image Loading:** Glide for efficient image caching and rendering.
    *   **UI/UX:** Material Components, Lottie (Animations), Facebook Shimmer (Loading states).
    *   **Maps:** Google Maps SDK & Google Maps Services.
    *   **Payments:** Stripe Android SDK.

## 📁 Project Structure

The project follows a modular package structure for maintainability:

*   `activity`: Main entry points and navigation controllers (e.g., `MainActivity`, `AuthActivity`, `MapsActivity`).
*   `fragment`: Individual screens for the main dashboard (Home, Cart, Orders, Profile).
*   `adapter`: RecyclerView adapters for products, carousels, and view pagers.
*   `model`: Data models for Products, Orders, Categories, and Cart items.
*   `api`: Retrofit interfaces for Stripe and Messaging services.
*   `service`: Background services like `MessagingService` for notifications.
*   `module`: Utility classes and configurations (Retrofit clients, custom layout managers).

## 🛠 Setup & Installation

1.  **Firebase Configuration:** 
    *   Add your `google-services.json` to the `app/` directory.
    *   Enable Firestore, Auth, and Storage in the Firebase Console.
2.  **API Keys:** 
    *   Add your Google Maps API key to `local.properties` or your secrets XML.
    *   Configure your Stripe Publishable Key in `strings.xml`.
3.  **Build:** Sync the project with Gradle and run the `app` module.

---
*Developed as a high-performance, user-centric food commerce solution.*
