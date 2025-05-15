package fit.spotted.api.utils

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.WebpushConfig
import com.google.firebase.messaging.WebpushNotification
import fit.spotted.api.db.dao.User
import java.io.ByteArrayInputStream

fun createFirebaseNotification(
    title: String, 
    body: String, 
    recipientToken: String,
    imageUrl: String? = null,
    clickAction: String? = null,
    channelId: String = "spotted_notifications",
    color: String = "#FF5722",
    sound: String = "default",
    tag: String? = null
): Message {
    // Create base notification
    val notificationBuilder = Notification.builder()
        .setTitle(title)
        .setBody(body)
    
    // Add image if provided
    if (imageUrl != null) {
        notificationBuilder.setImage(imageUrl)
    }

    // Configure Android specific options
    val androidNotification = AndroidNotification.builder()
        .setChannelId(channelId)
        .setColor(color)
        .setSound(sound)
        
    if (tag != null) {
        androidNotification.setTag(tag)
    }
    
    if (clickAction != null) {
        androidNotification.setClickAction(clickAction)
    }
    
    val androidConfig = AndroidConfig.builder()
        .setPriority(AndroidConfig.Priority.HIGH)
        .setNotification(androidNotification.build())
        .build()

    // Configure iOS specific options
    val apnsConfig = ApnsConfig.builder()
        .setAps(Aps.builder()
            .setSound(sound)
            .setCategory("SPOTTED_NOTIFICATION")
            .setThreadId("spotted_app")
            .setContentAvailable(true)
            .build())
        .build()
        
    // Configure Web specific options
    val webpushConfig = WebpushConfig.builder()
        .setNotification(WebpushNotification.builder()
            .setTitle(title)
            .setBody(body)
            .setIcon("/images/logo.png")
            .setBadge("/images/badge.png")
            .setRequireInteraction(true)
            .setVibrate(intArrayOf(100, 50, 100, 50, 100))
            .build())
        .build()

    // Build the complete message
    return Message.builder()
        .setNotification(notificationBuilder.build())
        .setToken(recipientToken)
        .setAndroidConfig(androidConfig)
        .setApnsConfig(apnsConfig)
        .setWebpushConfig(webpushConfig)
        .build()
}

/**
 * Send a friend activity notification
 */
fun sendFriendActivityNotification(sender: User, recipient: User, action: String, message: String): String? {
    val recipientToken = recipient.firebaseToken ?: return null
    
    val notification = createFirebaseNotification(
        title = "${sender.username} $action",
        body = message,
        recipientToken = recipientToken,
        imageUrl = sender.avatarPath,
        clickAction = "OPEN_PROFILE",
        channelId = "friend_activity",
        color = "#4CAF50", // Green color for activity
        tag = "friend_activity_${sender.id}"
    )
    
    return FirebaseMessaging.getInstance().send(notification)
}

/**
 * Send a like notification
 */
fun sendLikeNotification(sender: User, recipient: User, contentType: String): String? {
    val recipientToken = recipient.firebaseToken ?: return null
    
    val notification = createFirebaseNotification(
        title = "${sender.username} liked your $contentType",
        body = "Tap to view",
        recipientToken = recipientToken,
        imageUrl = sender.avatarPath,
        clickAction = "OPEN_CONTENT",
        channelId = "like_notifications",
        color = "#E91E63", // Pink for likes
        tag = "like_${contentType}_${sender.id}"
    )
    
    return FirebaseMessaging.getInstance().send(notification)
}

/**
 * Send a comment notification
 */
fun sendCommentNotification(sender: User, recipient: User, contentType: String): String? {
    val recipientToken = recipient.firebaseToken ?: return null
    
    val notification = createFirebaseNotification(
        title = "${sender.username} commented on your $contentType",
        body = "Tap to view the comment",
        recipientToken = recipientToken,
        imageUrl = sender.avatarPath,
        clickAction = "OPEN_COMMENTS",
        channelId = "comment_notifications", 
        color = "#2196F3", // Blue for comments
        tag = "comment_${contentType}_${sender.id}"
    )
    
    return FirebaseMessaging.getInstance().send(notification)
}

/**
 * Send a challenge notification
 */
fun sendChallengeNotification(recipient: User, challengeName: String, message: String): String? {
    val recipientToken = recipient.firebaseToken ?: return null
    
    val notification = createFirebaseNotification(
        title = "Challenge: $challengeName",
        body = message,
        recipientToken = recipientToken,
        clickAction = "OPEN_CHALLENGE",
        channelId = "challenge_notifications",
        color = "#9C27B0", // Purple for challenges
        tag = "challenge_${challengeName.hashCode()}"
    )
    
    return FirebaseMessaging.getInstance().send(notification)
}

fun initializeFirebaseApp() {
    val serviceAccount = System.getenv("FIREBASE_SERVICE_ACCOUNT")
        ?: error("FIREBASE_SERVICE_ACCOUNT is required!")

    val credentials = GoogleCredentials
        .fromStream(ByteArrayInputStream(serviceAccount.toByteArray()))

    val options = FirebaseOptions.builder()
        .setCredentials(credentials)
        .build()

    FirebaseApp.initializeApp(options)
}
