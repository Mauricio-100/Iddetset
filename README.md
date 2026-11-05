# IDDET (CMO) - Réseau Social Privé & Sécurisé

IDDET (anciennement CMO) est une application mobile Android moderne, fluide et sécurisée. Elle permet aux utilisateurs de partager des publications au format Markdown complet (**Actfiles**), d'inclure des images de haute qualité, d'interagir à travers des commentaires enrichis, de suivre d'autres utilisateurs et de bénéficier de prévisualisations d'URL performantes et d'un système de badges de vérification unique.

---

## 🚀 Fonctionnalités Clés

### 📝 Actfiles & Rendu Markdown Complet
* **Format Markdown Enrichi :** IDDET prend en charge le rendu Markdown complet (titres `#`, textes en **gras**, *italiques*, listes, blocs de code, citations et liens hypertextes).
* **Affichage Intégral Sans Troncature :** Les publications de votre fil d'actualité affichent désormais le rendu Markdown complet pour une lecture agréable sans altération esthétique.

### 🌐 Aperçus d'URL OpenGraph & Fallback "Cute Cat"
* **Prévisualisation OpenGraph Intelligente :** Les liens insérés dans les Actfiles génèrent automatiquement une carte de prévisualisation élégante (Titre, Description, Image de couverture et Nom du site).
* **Mode Fallback Chat Mignon :** Si un site ne fournit pas d'image OpenGraph ou si le chargement échoue, l'application affiche une **image aléatoire d'un chat adorable**, extrait le texte disponible et affiche explicitement : *« Données récupérées du site : [URL] »*.

### 🔗 Deep Linking & Partage Facilité
* **Association de Liens (`bit.gopu.inc` et `iddet.gopu.inc`) :** L'application est configurée avec des filtres d'intentions Android (`intent-filter`) pour intercepter les URL de partage.
* **Navigation Instantanée :** Cliquer sur un lien de type `https://bit.gopu.inc/s/actfile/{id}` ouvre automatiquement l'application IDDET et redirige directement l'utilisateur vers la publication correspondante.

### 🛡️ Sécurité de Pointe & Zéro Télémétrie
* **Aucune Analyse d'App (No Telemetry) :** Pour respecter la vie privée des utilisateurs, toute collecte automatique de données, de télémétrie et d'analyses (Firebase Analytics, Crashlytics, Google ADID) a été **complètement désactivée** au niveau de l'Android Manifest.
* **Sauvegardes Sécurisées :** L'attribut `allowBackup` a été désactivé pour empêcher l'extraction de données locales sensibles via ADB.
* **Architecture Locale :** Persistance robuste grâce à une base de données **Room SQLite** hautement optimisée.

### 🎖️ Système de Badges de Vérification Authentiques
* **Badge Officiel :** Représenté par un badge bleu étoilé pour les comptes officiels et d'intérêt général (ex: `@iddet`, `@cmo`).
* **Badge Vérifié :** Représenté par un badge vert pour les profils ayant complété leur vérification d'identité.
* **BottomSheet de Vérification :** Cliquer sur un badge ouvre une feuille de route interactive détaillant le statut de certification du compte.

---

## 🛠️ Architecture Technique

L'application repose sur les meilleures pratiques de développement Android moderne :
* **Language :** Kotlin (100%)
* **UI Framework :** Jetpack Compose avec le design system **Material Design 3 (M3)**
* **State Management :** Architecture **MVVM** avec des `ViewModel` réutilisables et des flux réactifs `StateFlow`
* **Local DB :** Room Database (User, Actfile, Message, Follow, Comment, Notification Daos)
* **Image Loading :** Coil
* **Asynchronous :** Kotlin Coroutines & Flow

---

## 📦 Pipeline CI/CD GitHub Actions

Le workflow GitHub Actions (`.github/workflows/AndroidRuntime.yml`) a été optimisé pour :
1. Compiler l'application automatiquement à chaque push/PR.
2. Décoder de façon sécurisée le keystore de développement.
3. Générer un artefact APK signé de débogage nommé `iddet-cmo-debug.apk`.
4. Rendre disponible l'APK en téléchargement direct comme artefact de workflow sous le nom `iddet-cmo-apk`.

---

## 🔧 Instructions de Compilation

Pour compiler et lancer le projet localement :

1. Clonez le dépôt GitHub.
2. Créez un fichier `.env` à la racine (voir `.env.example`).
3. Ouvrez le projet dans **Android Studio (Ladybug ou plus récent)**.
4. Lancez la compilation via Gradle :
   ```bash
   gradle assembleDebug
   ```
5. Installez l'APK généré sur votre terminal :
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📄 Licence

Développé de manière éthique, sécurisée et privée pour la communauté **IDDET / CMO**. 🕶️
