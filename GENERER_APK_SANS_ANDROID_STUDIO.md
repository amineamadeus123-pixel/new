
# Générer l'APK sans Android Studio

Ce projet contient un workflow GitHub Actions qui compile l'APK automatiquement dans le cloud.

## Étapes simples

1. Crée un compte GitHub si tu n'en as pas.
2. Crée un nouveau repository privé, par exemple `coach-habitudes`.
3. Envoie le contenu de ce dossier dans le repository.
4. Va dans l'onglet **Actions**.
5. Lance le workflow **Build APK** avec **Run workflow**.
6. À la fin, télécharge l'artifact nommé **CoachHabitudes-debug-apk**.
7. Dans le zip téléchargé, tu trouveras `app-debug.apk`.
8. Copie ce fichier sur ta tablette et ouvre-le pour l'installer.

## Sur la tablette

Il faudra peut-être autoriser l'installation depuis une source inconnue :

Paramètres Android > Sécurité > Installer des applications inconnues > autoriser l'application utilisée pour ouvrir l'APK.

## Note

L'APK généré est une version debug, suffisante pour une utilisation personnelle sur ta tablette.
