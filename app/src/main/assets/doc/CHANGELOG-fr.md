# Notes de version

## v2.1.0

_2026/08/25_

- `Note` La modification concerne actuellement les fichiers `.zip` ordinaires en un seul volume. JAR/AAR/WAR, ZIP fractionné, 7Z et famille TAR restent en lecture seule; la reconstruction normalise les commentaires, les métadonnées extra non essentielles et les attributs de permission Unix
- `Ajout` Gérer l'archive... ouvre le ZIP sélectionné dans la page de gestion avec Ajouter des fichiers..., Nouveau dossier..., Renommer... et Supprimer, y compris le renommage et la suppression de sous-arborescences
- `Ajout` Explorer Action v8 reconstruit dans une sortie en attente détenue par l'hôte, relit entièrement le résultat, ne remplace atomiquement l'original qu'après validation et actualise automatiquement la ligne Explorer
- `Amélioration` Chaque modification est prévalidée dans un plan immuable qui contrôle les chemins dangereux, les noms dupliqués ou équivalents, les conflits fichier/dossier, les entrées conservées non prises en charge et les changements de source avant validation de la sortie de remplacement
- `Amélioration` La reconstruction ZIP conserve le contenu Stored/Deflate, les horodatages utilisables et le chiffrement ZipCrypto/AES pris en charge; une annulation ou tout échec de validation abandonne la sortie en attente et laisse l'archive originale intacte

## v2.0.0

_2026/08/25_

- `Ajout` Le produit s'appelle désormais Archive Manager et l'action devient Ouvrir l'archive
- `Ajout` Explorer Action v5 parcourt les archives dans la liste native d'AutoJs6 avec la barre de chemin, le thème et la navigation Retour existants
- `Ajout` Explorer Action v6 ouvre les entrées prises en charge avec les visionneuses de documents, d'images, de fichiers audio et de vidéos de l'hôte
- `Ajout` Raccourci Extraire vers... pour choisir une destination et extraire toute l'archive
- `Ajout` Gérer l'archive... ouvre la page de gestion avec les portées d'extraction archive entière, dossier interne actuel et sélection cochée, tout en conservant le raccourci d'extraction complète
- `Ajout` Stratégies de conflit d'extraction demander, ignorer, écraser et renommer automatiquement, avec application globale, décompte précis et préservation des dossiers de sortie existants
- `Ajout` Explorer Action v4 ajoute Compresser... aux menus des fichiers et dossiers et à la barre de cinq actions pour les sélections de même dossier parent
- `Ajout` Création de ZIP avec nom par défaut, niveaux de compression, progression, annulation et numérotation automatique des conflits
- `Ajout` Création de ZIP fractionnés standard, y compris avec AES-256, avec des tailles MiB courantes ou un entier personnalisé ; tout le groupe partage un nom de base protégé des conflits et le `.zip` final apparaît après les volumes numérotés
- `Ajout` Création d’une archive par élément d’une sélection de même dossier parent, avec aperçu des sorties, numérotation automatique des conflits et conservation explicite des sorties terminées après un échec ou une annulation ultérieurs
- `Ajout` Création de 7Z non solid avec les niveaux 0 à 9 et chiffrement facultatif du contenu en AES-256 ; les noms restent visibles et le chiffrement des noms n'est pas annoncé à tort
- `Ajout` Création de TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST avec niveaux propres au format et mise à jour complète des extensions composées
- `Ajout` Navigation et extraction des ZIP chiffrés avec ZipCrypto/AES, nouvelle saisie sur place d'un mot de passe erroné et création facultative de ZIP AES-256 dont les noms restent visibles avec confirmation identique du mot de passe
- `Ajout` Navigation, aperçu et extraction des 7Z ordinaires ou solid avec chaînes courantes de compression et de filtres, chiffrement AES du contenu et de l'en-tête ; un mot de passe absent ou erroné produit un diagnostic explicite
- `Ajout` Navigation, aperçu et extraction des TAR non compressés dans la liste native avec validation de la somme de contrôle des en-têtes ; liens, nœuds de périphérique et entrées creuses restent en lecture seule
- `Ajout` Navigation, aperçu et extraction des TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 et TAR.ZST/TZST par les mêmes chemins natifs ; la détection vérifie la signature du compresseur et la structure TAR interne
- `Ajout` Budgets d'extraction Compatible, Strict et Personnalisé ; les archives hors budget restent consultables en lecture seule et affichent la sortie estimée, les dimensions dépassées et une confirmation unique avant écriture
- `Ajout` La progression d'extraction affiche les éléments, les octets, l'élément actuel, le débit et le temps restant estimé, avec une annulation fiable
- `Correction` La liste ZIP utilise les métadonnées et accepte les préambules auto-extractibles, les anciens encodages, les séparateurs Windows et davantage de méthodes lisibles
- `Correction` Les ZIP fractionnés standard `.z01 + .zip` indiquent désormais les volumes précédents requis au lieu de déclarer le volume final endommagé
- `Correction` L'encodage des noms ZIP peut être remplacé si la détection automatique est incorrecte et l'extraction réutilise ce choix
- `Correction` Les tailles inconnues, les URI DocumentsProvider valides et les droits d'écriture supplémentaires de l'hôte ne bloquent plus une archive valide
- `Correction` Correction de la consultation et de l'extraction ZIP sous Android 7.x, qui appelaient des API réservées aux systèmes récents
- `Correction` Les ZIP contenant des noms Unicode s'ouvrent désormais correctement sous Android 7, même si l'indicateur de nom UTF-8 manque ou est interprété de façon incohérente par le backend
- `Correction` Les mots de passe erronés sont désormais classés de façon stable sous PASSWORD/WRONG_PASSWORD et les entrées AES v2 avec un CRC stocké nul ne sont plus signalées à tort comme endommagées
- `Correction` Le dossier d'extraction par défaut des extensions composées telles que TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST retire désormais le suffixe complet au lieu de conserver `.tar`
- `Correction` Les archives contenant des remontées vers le dossier parent, des chemins absolus, des préfixes de lecteur ou des caractères de contrôle restent consultables ; ces noms passent dans un dossier isolé en lecture seule, restent prévisualisables si leurs données sont lisibles et doivent être explicitement ignorés avant extraction
- `Correction` L'extraction n'écrase plus les fichiers ou dossiers existants lorsque le fournisseur de destination considère comme identiques les noms qui ne diffèrent que par la casse ou sont équivalents en Unicode ; les dossiers de sortie équivalents sont numérotés automatiquement
- `Correction` Une annulation ou un échec d'extraction restaure la nouvelle racine de sortie dans une phase de nettoyage non annulable ; si le fournisseur refuse la suppression, le nom et l'URI du résidu possible remplacent le seul message générique
- `Correction` La création de 7Z chiffrés fonctionne désormais sous Android 7 et la vérification associe les entrées par chemin afin que les différences d'ordre valides du backend ne provoquent plus de faux échecs
- `Correction` La page de gestion s'adapte désormais aux écrans peu hauts en portrait et en paysage : l'archive et le chemin passent dans la barre, les réglages restent disponibles dans une rangée horizontale compacte et les entrées et actions demeurent visibles jusqu'à une police de 2,0x
- `Amélioration` Suppression de la limite fixe de 4 Gio et des seuils de taille/ratio pendant la consultation, sans retirer l'isolation ni les contrôles d'intégrité
- `Amélioration` Ajout d'un Roadmap vérifiable et réécriture du README et du CHANGELOG
- `Amélioration` L'écran autonome suit désormais le mode jour/nuit et les couleurs dynamiques Material
- `Amélioration` Les sorties ZIP, 7Z et TAR passent par une session de l'hôte liée à l'UID, utilisent un fichier temporaire du même dossier et sont validées atomiquement sans autorisation de stockage ni écrasement
- `Amélioration` Les capacités du format et de chaque entrée sont vérifiées uniformément pour la prévisualisation, l'extraction et la création afin de laisser les options indisponibles désactivées
- `Amélioration` Les échecs indiquent le format, l'étape, un code stable et le motif ; les versions de débogage peuvent copier le diagnostic complet
- `Amélioration` Les archives ordinaires sont désormais consultées par des canaux à position indépendante sur le descripteur en lecture seule de l'hôte, sans copie intégrale ; les entrées ou lecteurs incompatibles (actuellement les ZIP chiffrés) utilisent le cache privé, nettoyé à la fermeture, en cas d'échec ou après expiration
- `Amélioration` Transactions de sortie unifiées pour la création ZIP, 7Z et TAR ; les sources illisibles et les échecs de réservation, ouverture, écriture ou validation ont une étape stable, tandis qu'une annulation non confirmée ferme la session, affiche le chemin prévu et interdit une nouvelle tentative risquée
- `Amélioration` La création analyse les sources avant d'ouvrir la sortie temporaire et affiche séparément l'analyse, la compression, la vérification et la validation, avec le total des fichiers, les octets lus et les tailles inconnues
- `Amélioration` Les archives créées sont entièrement relues avant publication afin de vérifier le format, les entrées, les tailles, les CRC et les empreintes du contenu ; chaque volume ZIP en attente est aussi comparé octet par octet
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
