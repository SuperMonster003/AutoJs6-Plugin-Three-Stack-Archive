# Notes de version

## Unreleased

_Non publié_

- `Ajout` Le produit s'appelle désormais Archive Manager et l'action devient Ouvrir l'archive
- `Ajout` Explorer Action v5 parcourt les archives dans la liste native d'AutoJs6 avec la barre de chemin, le thème et la navigation Retour existants
- `Ajout` Explorer Action v6 ouvre les entrées prises en charge avec les visionneuses de documents, d'images, de fichiers audio et de vidéos de l'hôte
- `Ajout` Raccourci Extraire vers... pour choisir une destination et extraire toute l'archive
- `Ajout` Explorer Action v4 ajoute Compresser... aux menus des fichiers et dossiers et à la barre de cinq actions pour les sélections de même dossier parent
- `Ajout` Création de ZIP avec nom par défaut, niveaux de compression, progression, annulation et numérotation automatique des conflits
- `Ajout` Création de 7Z non solid avec les niveaux 0 à 9 et chiffrement facultatif du contenu en AES-256 ; les noms restent visibles et le chiffrement des noms n'est pas annoncé à tort
- `Ajout` Création de TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST avec niveaux propres au format et mise à jour complète des extensions composées
- `Ajout` Navigation et extraction des ZIP chiffrés avec ZipCrypto/AES, nouvelle saisie sur place d'un mot de passe erroné et création facultative de ZIP AES-256 dont les noms restent visibles avec confirmation identique du mot de passe
- `Ajout` Navigation, aperçu et extraction des 7Z ordinaires ou solid avec chaînes courantes de compression et de filtres, chiffrement AES du contenu et de l'en-tête ; un mot de passe absent ou erroné produit un diagnostic explicite
- `Ajout` Navigation, aperçu et extraction des TAR non compressés dans la liste native avec validation de la somme de contrôle des en-têtes ; liens, nœuds de périphérique et entrées creuses restent en lecture seule
- `Ajout` Navigation, aperçu et extraction des TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 et TAR.ZST/TZST par les mêmes chemins natifs ; la détection vérifie la signature du compresseur et la structure TAR interne
- `Correction` La liste ZIP utilise les métadonnées et accepte les préambules auto-extractibles, les anciens encodages, les séparateurs Windows et davantage de méthodes lisibles
- `Correction` Les ZIP fractionnés standard `.z01 + .zip` indiquent désormais les volumes précédents requis au lieu de déclarer le volume final endommagé
- `Correction` Les archives contenant des remontées vers le dossier parent, des chemins absolus, des préfixes de lecteur ou des caractères de contrôle restent consultables ; ces noms passent dans un dossier isolé en lecture seule, restent prévisualisables si leurs données sont lisibles et doivent être explicitement ignorés avant extraction
- `Correction` L'encodage des noms ZIP peut être remplacé si la détection automatique est incorrecte et l'extraction réutilise ce choix
- `Correction` Les tailles inconnues, les URI DocumentsProvider valides et les droits d'écriture supplémentaires de l'hôte ne bloquent plus une archive valide
- `Correction` Correction de la consultation et de l'extraction ZIP sous Android 7.x, qui appelaient des API réservées aux systèmes récents
- `Correction` Les mots de passe erronés sont désormais classés de façon stable sous PASSWORD/WRONG_PASSWORD et les entrées AES v2 avec un CRC stocké nul ne sont plus signalées à tort comme endommagées
- `Correction` Le dossier d'extraction par défaut des extensions composées telles que TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST retire désormais le suffixe complet au lieu de conserver `.tar`
- `Amélioration` Suppression de la limite fixe de 4 Gio et des seuils de taille/ratio pendant la consultation, sans retirer l'isolation ni les contrôles d'intégrité
- `Ajout` Budgets d'extraction Compatible, Strict et Personnalisé ; les archives hors budget restent consultables en lecture seule et affichent la sortie estimée, les dimensions dépassées et une confirmation unique avant écriture
- `Amélioration` Les archives ordinaires sont désormais consultées par des canaux à position indépendante sur le descripteur en lecture seule de l'hôte, sans copie intégrale ; les entrées ou lecteurs incompatibles (actuellement les ZIP chiffrés) utilisent le cache privé, nettoyé à la fermeture, en cas d'échec ou après expiration
- `Amélioration` Ajout d'un Roadmap vérifiable et réécriture du README et du CHANGELOG
- `Amélioration` L'écran autonome suit désormais le mode jour/nuit et les couleurs dynamiques Material
- `Amélioration` Les sorties ZIP, 7Z et TAR passent par une session de l'hôte liée à l'UID, utilisent un fichier temporaire du même dossier et sont validées atomiquement sans autorisation de stockage ni écrasement
- `Amélioration` Les capacités du format et de chaque entrée sont vérifiées uniformément pour la prévisualisation, l'extraction et la création afin de laisser les options indisponibles désactivées
- `Amélioration` Les échecs indiquent le format, l'étape, un code stable et le motif ; les versions de débogage peuvent copier le diagnostic complet
- `Dépendance` Ajout de Zip4j 2.11.5 sous licence Apache 2.0 pour les flux ZIP chiffrés, la création AES-256 et le chemin de compatibilité Android 7.x
- `Dépendance` Ajout de XZ for Java 1.12 sous licence 0BSD pour lire et écrire TAR.XZ/TXZ en Java pur sans ABI native
- `Dépendance` Ajout de zstd-jni 1.5.7-15 sous licence BSD pour lire et écrire TAR.ZST/TZST ; les quatre ABI Android passent les contrôles d'alignement ELF 16 Kio et RELRO

## v1.0.1

_2026/08/08_

- `Correction` Liaison de service vide lors de l'activation dans le centre de plugins
- `Amélioration` Simplification du nom, de la description et des instructions

## v1.0.0

_2026/08/02_

- `Ajout` Première version pour parcourir ZIP, JAR, AAR et WAR et extraire la sélection
- `Ajout` Recherche, sélection, progression, annulation, nettoyage temporaire et interface localisée
