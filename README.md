# Coach Habitudes V2

Application Android personnelle en Kotlin + Jetpack Compose.

## Nouveautés V2

- Sauvegarde locale réelle avec `SharedPreferences` au format JSON.
- Les statuts `FAIT`, `PLUS TARD`, `IGNORER` restent sauvegardés si tu fermes puis rouvres l'application le même jour.
- Le planning est éditable depuis l'onglet **Planning**.
- Tu peux ajouter, modifier et supprimer des tâches.
- Les tâches sont triées automatiquement par heure.
- Chaque nouveau jour, le planning est conservé mais les statuts sont remis à zéro.
- Les notifications utilisent le planning modifié.

## Installation

1. Ouvre le dossier `CoachHabitudes_V2` dans Android Studio.
2. Branche ta tablette Android en USB.
3. Active le débogage USB.
4. Clique sur **Run**.
5. Accepte l'autorisation de notifications si Android la demande.

## Note importante

Android ne permet pas à une application standard de rester visible après appui sur le bouton physique Power.  
La fonction **Mode veille** garde l'écran allumé dans l'application avec un affichage noir/gris pour réduire la luminosité.

## Prochaine étape possible

- Ajouter un bouton `Plus tard 10 min` qui reprogramme vraiment une notification après 10 minutes.
- Ajouter un historique semaine/mois.
- Ajouter un export CSV.
