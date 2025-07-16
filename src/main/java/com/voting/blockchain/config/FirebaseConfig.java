// src/main/java/com/voting/blockchain/config/FirebaseConfig.java
package com.voting.blockchain.config; // This must be the first line

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct; // For @PostConstruct
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource; // To load resource from classpath
import java.io.IOException;
import java.io.InputStream;

@Configuration // Marks this class as a Spring configuration class
public class FirebaseConfig {

    private Firestore firestore;

    @PostConstruct // This method runs after dependency injection is done
    public void initializeFirebase() throws IOException {
        // Load the service account key from the classpath (src/main/resources)
        // Ensure you have renamed your downloaded JSON file to "serviceAccountKey.json"
        InputStream serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) { // Check if FirebaseApp is already initialized
            FirebaseApp.initializeApp(options);
            System.out.println("Firebase initialized successfully.");
        } else {
            System.out.println("Firebase already initialized.");
        }

        firestore = FirestoreClient.getFirestore();
    }

    @Bean // Makes the Firestore instance available as a Spring Bean
    public Firestore getFirestore() {
        return firestore;
    }
}