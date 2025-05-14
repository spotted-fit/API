package utils

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import java.io.ByteArrayInputStream

fun createFirebaseNotification(title: String, body: String, recipientToken: String): Message {
    return Message.builder()
        .setNotification(
            Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build()
        )
        .setToken(recipientToken)
        .build()
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