# **ChatOoz \- Android Instant Messaging Application**

## **Project Description**

ChatOoz is a mobile instant messaging application developed on the Android platform. The main objective of this project, carried out as part of the "Enterprise Network" module, is to provide a fluid communication platform allowing both one-on-one (peer-to-peer) conversations and the creation and participation in group discussions. The application integrates cloud services for user management and utilizes a specialized SDK for real-time messaging management.

## **Implemented Features**

* **User Management:**  
  * Registration with email, password, and username.  
  * Profile image selection during registration.  
  * Login/Authentication for existing users.  
  * Viewing user profile (name, email, image).  
  * Logout.  
* **Individual Messaging (Peer-to-Peer):**  
  * Starting new conversations by searching for users by email.  
  * Sending and receiving text messages.  
* **Group Messaging:**  
  * Creating groups with a name and inviting users by email.  
  * Sending and receiving text messages within groups.  
* **Conversation List:**  
  * Displaying all of the user's conversations (individual and group).

## **Technical Architecture and Technologies Used**

* **Client Platform:** Android  
* **Language:** Kotlin  
* **IDE:** Android Studio  
* **Dependency Management:** Gradle  
* **Backend & Cloud Services:**  
  * **Firebase Authentication:** For user registration and login.  
  * **Firebase Realtime Database:** For storing user information (UID, email, name, profile image URL).  
  * **Cloudinary:** For storing and managing user profile images.  
* **Messaging SDK:**  
  * **ZegoCloud ZIM SDK:** The core of the chat logic.  
  * **ZegoCloud ZIMKit UI Kit:** Pre-built UI components for conversation and chat screens.  
* **UI Design:** XML, Material Design Components, ConstraintLayout, LinearLayout.

## **Challenges Encountered and Limitations**

* **Cloud Services and SDK Integration:** Challenges related to connecting and interacting between Firebase (Authentication, Realtime Database), Cloudinary (image storage), and the ZIM/ZIMKit SDKs to establish a functional architecture.  
* **User Database Management and Querying (Firebase):** Difficulty in implementing the email search logic within the Firebase data structure to obtain the user identifiers required for chat functionalities (individual chat, group creation).  
* **Adapting to Messaging SDK Requirements (ZIMKit):** Issues encountered with ZIMKit APIs, particularly for group creation where the generated identifier format (UUID) was not accepted, requiring the use of Firebase Push IDs.  
* **Dark/Light Theme Handling in ZIMKit:** The ZIMKit fragment does not automatically adapt to the application's Day/Night theme, which represents an apparent customization limitation.

## **Possible Improvements and Future Perspectives**

* Push Notifications for offline messages.  
* Media Sharing (adding the ability to send and receive images, videos, etc.).  
* Status Indicators (displaying indicators like "typing..." or message read statuses).  
* Search Functionality (adding a search feature for conversations/messages).  
* Further UI/UX Customization.

## **Installation and Configuration**

To clone and configure this project, follow these steps:

1. Clone the repository:  
   Open your terminal or command prompt and execute the following command:  
   git clone https://github.com/abdoukhemir/ChatOozChatApp.git

2. Open in Android Studio:  
   Open the cloned project folder in Android Studio.  
3. **Configure Firebase:**  
   * Create a Firebase project on the Firebase console.  
   * Add an Android application to your Firebase project.  
   * Download the google-services.json configuration file and place it in the app/ directory of your Android project.  
   * Enable the necessary services: Firebase Authentication (Email/Password method) and Firebase Realtime Database.  
   * Configure your Realtime Database security rules as needed.  
4. **Configure Cloudinary:**  
   * Create a Cloudinary account if you don't have one.  
   * Obtain your Cloudinary credentials (Cloud name, API Key, API Secret) from your dashboard.  
   * Integrate the Cloudinary SDK into your project (if not already done) and configure it with your credentials in the code (often in an application class or a dedicated configuration).  
5. **Configure ZegoCloud ZIM:**  
   * Create an application in the ZegoCloud console and obtain your AppID and AppSign.  
   * Initialize ZIMKit in your application (usually in your Application class or the main activity) using these credentials.  
   * If you plan to implement offline push notifications, configure Offline Push in the ZegoCloud console and integrate Firebase Cloud Messaging (FCM) into your application.  
6. Sync and Run:  
   Synchronize your project with Gradle files in Android Studio, then build and run the application on an emulator or physical device.

## **Author(s)**

* Khemir Abderrahmen