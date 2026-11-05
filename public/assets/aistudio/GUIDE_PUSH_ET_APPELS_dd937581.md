# Push FCM + appels solides — mise en place

## 1. Firebase (une seule fois)
1. console.firebase.google.com → créer/ouvrir le projet → ajouter l'app Android (ton `applicationId`).
2. Télécharger `google-services.json` → le mettre dans `app/`.
3. Paramètres du projet → Comptes de service → « Générer une nouvelle clé privée » → fichier JSON.
4. Render → ton service → Environment → `FIREBASE_SERVICE_ACCOUNT_JSON` = **tout le contenu** de ce JSON.
5. `requirements.txt` du serveur : ajouter `google-auth` (+ `requests`, requis par google-auth pour le transport). Redéployer.
   Sans la variable, le serveur fonctionne comme avant (aucun push), sans erreur.

## 2. Gradle
```kotlin
// build.gradle.kts (projet)      id("com.google.gms.google-services") version "4.4.2" apply false
// build.gradle.kts (app)
plugins { id("com.google.gms.google-services") }
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.core:core-ktx:1.13.1")
}
```

## 3. AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT"/>

<application ...>
    <service android:name=".calls.IddetFirebaseService" android:exported="false">
        <intent-filter><action android:name="com.google.firebase.MESSAGING_EVENT"/></intent-filter>
    </service>
    <activity android:name=".calls.IncomingCallActivity" android:exported="false"
        android:showWhenLocked="true" android:turnScreenOn="true" android:launchMode="singleTask"/>
    <receiver android:name=".calls.CallActionReceiver" android:exported="false">
        <intent-filter>
            <action android:name="com.iddet.calls.ACCEPT"/>
            <action android:name="com.iddet.calls.DECLINE"/>
        </intent-filter>
    </receiver>
</application>
```
(adapte `.calls` au package réel des fichiers Kotlin.)

## 4. Dans l'app (après le login)
```kotlin
// 1) Permissions à demander AVANT le premier appel (sinon crash micro) :
//    RECORD_AUDIO, et POST_NOTIFICATIONS sur Android 13+.
// 2) Canaux + jeton :
IddetFirebaseService.ensureChannels(this)
FirebaseMessaging.getInstance().token.addOnSuccessListener { IddetFirebaseService.registerToken(jwt, it) }
// 3) Gestionnaire d'appels (UN seul, créé après le login) :
val cm = CallManager(applicationContext, token = jwt, myUserId = myId, listener = monListener)
CallManagerHolder.instance = cm
cm.start()
// 4) Au logout : POST /api/devices/unregister {"token": fcmToken}, puis cm.stop().
// 5) Dans onFinished du listener : IddetFirebaseService.cancelCallNotification(this) + couper la sonnerie.
// 6) Si l'Intent de MainActivity contient "accept_call", afficher l'écran d'appel en cours.
```
Android 14+ : l'accès plein écran sur l'écran verrouillé peut demander l'autorisation « Notifications plein écran »
(`NotificationManager.canUseFullScreenIntent()` → sinon ouvrir `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`).

## 5. Ce qui rend les appels fiables (3 couches)
| Couche | Rôle |
|---|---|
| FCM haute priorité | Réveille l'app fermée/en veille, affiche l'appel en plein écran |
| WebSocket `/ws/{id}` | Temps réel quand l'app est ouverte (reconnexion auto, backoff 1→15 s) |
| Polling `/api/calls/incoming` et `/status` | Rattrape tout push perdu |

Les trois sont dédoublonnés par `call_id` : l'appel ne sonne qu'une fois. La base de données reste
la source de vérité, et la sonnerie expire toute seule après 45 s des deux côtés.

## 6. Causes de crash corrigées dans CallManager v2
1. Exception non rattrapée dans une coroutine (409/404, réseau coupé) → `CoroutineExceptionHandler` + `api()` qui ne lève jamais.
2. `JSONObject` sur une réponse non-JSON / `body!!` → lecture défensive.
3. `RECORD_AUDIO` non accordée → `SecurityException` → permission vérifiée avant d'ouvrir le micro.
4. `AudioRecord`/`AudioTrack` mal initialisés ou libérés deux fois → état vérifié, libération unique.
5. Callbacks UI depuis un thread réseau → tous renvoyés sur le thread principal.
6. Si le crash persiste : `adb logcat -b crash` (ou Android Studio → Logcat → filtre « FATAL EXCEPTION ») et envoie-moi la trace.
