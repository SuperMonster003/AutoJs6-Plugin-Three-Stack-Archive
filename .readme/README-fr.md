<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>Plugin du gestionnaire de fichiers AutoJs6 pour parcourir, extraire et créer des archives ZIP, 7Z et de la famille TAR</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### Langues (Languages)

Le README est disponible dans les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ar.md)

### Présentation

Archive Manager intègre la navigation, l'extraction et la création des archives ZIP, 7Z et de la famille TAR au gestionnaire de fichiers AutoJs6. La version actuelle parcourt les archives dans la liste native de l'hôte avec les chemins externe et interne, prévisualise les entrées prises en charge, extrait toute l'archive, le dossier interne actuel ou la sélection cochée depuis la page de gestion et crée un format pris en charge à partir d'un élément ou d'une sélection de même dossier parent. L'extraction par entrée dans la page native de l'hôte et l'édition interne restent planifiées dans le Roadmap.

### Disponible actuellement

- Ouvrir les archives des familles ZIP, 7Z et TAR directement dans la liste native d'AutoJs6, avec le thème, le mode sombre et les couleurs dynamiques de l'hôte.
- Afficher le dossier externe, le nom de l'archive et le dossier interne dans la barre de chemin ; toucher un niveau pour y accéder et utiliser Retour pour remonter avant de quitter l'archive.
- Ouvrir les documents, images, fichiers audio et vidéos pris en charge avec les visionneuses existantes de l'hôte.
- Utiliser le raccourci « Extraire vers... » pour extraire toute l'archive sans ouvrir d'abord la vue de navigation.
- Choisir « Extraction sélective... » pour ouvrir la page de gestion et extraire toute l'archive, le dossier interne actuel ou la sélection cochée; « Extraire vers... » reste le raccourci pour l'archive entière.
- Pour les noms de sortie équivalents, choisissez Demander à chaque fois, Ignorer, Écraser ou Renommer automatiquement; Appliquer à tout traite les conflits compatibles restants et les dossiers de sortie existants sont toujours numérotés et préservés.
- Parcourir les dossiers, rechercher et trier le contenu de l'archive.
- Afficher la liste à partir des métadonnées sans décompresser chaque entrée au préalable.
- Parcourir les archives ordinaires via le descripteur repositionnable en lecture seule de l'hôte et des canaux à position indépendante, sans copie intégrale ; les tubes, les sources inscriptibles ou non repositionnables, Android 7 et les lecteurs exigeant un fichier local lisible par le processus (actuellement les ZIP chiffrés) utilisent le cache privé, supprimé à la fermeture.
- Choisir un budget d'extraction Compatible, Strict ou Personnalisé ; une archive qui dépasse les seuils d'entrées, de chemin, de taille de sortie ou de taux de compression reste consultable et affiche l'espace estimé et les risques avant une confirmation unique d'écriture.
- Afficher la progression par éléments et octets, l'élément actuel, le débit et le temps restant estimé ; une annulation ou un échec restaure la nouvelle racine de sortie, et tout résidu refusé par le fournisseur est indiqué par nom et URI.
- Placer les noms contenant une remontée vers le dossier parent, un chemin absolu, un préfixe de lecteur ou un caractère de contrôle dans un dossier Chemins non sûrs visible dans la barre de chemin ; les données lisibles restent prévisualisables et l'extraction complète exige d'ignorer explicitement ces entrées sans affecter les autres.
- Parcourir, prévisualiser et extraire les TAR non compressés ; les liens symboliques ou physiques, les nœuds de périphérique et les entrées creuses restent listés sans être écrits comme des fichiers ordinaires.
- Parcourir, prévisualiser et extraire les TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 et TAR.ZST/TZST avec les mêmes chemins internes, l'isolation des entrées spéciales et les contrôles d'intégrité.
- Parcourir, prévisualiser et extraire les 7Z ordinaires ou solid, y compris les chaînes courantes de compression et de filtres ainsi que les entrées chiffrées dans le contenu ou l'en-tête ; un mot de passe absent ou erroné produit un diagnostic explicite.
- Vérifier la structure ZIP/7Z/TAR réelle et unifier les capacités de prévisualisation, d'extraction et de création, en laissant désactivées les options non prises en charge.
- Prendre en charge Zip64, les préambules auto-extractibles, les anciens encodages de noms et les séparateurs Windows.
- Reconnaître la structure ZIP fractionnée standard `.z01 + .zip` et indiquer les noms des volumes requis au lieu de déclarer le volume final endommagé ; la lecture et la création d'archives fractionnées ne sont pas encore disponibles.
- Parcourir et extraire les ZIP protégés par ZipCrypto ou AES, réessayer un mot de passe erroné sur place et créer au choix des ZIP chiffrés en AES-256 dont les noms restent visibles ; la création chiffrée exige une confirmation identique du mot de passe.
- Remplacer l'encodage des noms ZIP quand la détection automatique est incorrecte ; la navigation et l'extraction réutilisent le même choix.
- Afficher le format, l'étape, un code stable et un motif clair en cas d'échec ; les versions de débogage peuvent copier le diagnostic complet.
- Proposer « Compresser... » pour les fichiers, les dossiers et les sélections multiples de même dossier parent.
- Créer une archive par élément d’une sélection partageant le même dossier parent ; le formulaire affiche le nombre de sorties et les noms dérivés, tandis que les noms existants ou répétés sont numérotés sans remplacement. Chaque sortie est validée séparément ; une annulation ou un échec conserve et signale les sorties terminées tout en bloquant une relance ambiguë du lot entier.
- Créer des ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST avec un nom configurable et seulement les niveaux et options de mot de passe réellement pris en charge par le format choisi.
- Écrire d'abord dans un fichier temporaire du même dossier puis valider atomiquement ; choisir la numérotation automatique ou essayer le nom exact et demander avant de réessayer avec un numéro, sans écraser les fichiers existants. Après réservation du nom, un instantané source borné est analysé avant l'ouverture de la sortie temporaire ; le formulaire distingue analyse, compression et validation, avec le total des fichiers, les octets lus et les tailles inconnues. Un échec annule la transaction ; si l'hôte ne peut pas confirmer le nettoyage de la sortie temporaire, le formulaire affiche le chemin prévu et interdit une nouvelle tentative.

### Formats actuels

La version actuelle reconnaît les extensions consultables et extractibles suivantes:

```text
zip, jar, aar, war, 7z, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

La version actuelle peut créer les formats suivants:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> La navigation native et l'aperçu des entrées d'Explorer Action v6, ainsi que la compression v4, exigent AutoJs6 avec le code de version 5276 ou plus récent. L'extraction par entrée dans la page native de l'hôte, la création de volumes fractionnés, le chiffrement des noms à la création, la suppression des sources et l'ajout/suppression interne ne sont pas encore publiés. Le Roadmap fait foi.

### Utilisation

1. Installez le plugin et activez-le dans le centre de plugins AutoJs6.
2. Ouvrez le menu d'une archive ZIP, JAR, AAR, WAR, 7Z ou de la famille TAR.
3. Choisissez « Ouvrir l'archive », puis entrez dans les dossiers, recherchez ou naviguez avec la barre de chemin de la liste hôte.
4. Pour extraire toute l'archive, choisissez « Extraire vers... » dans son menu puis sélectionnez un dossier avec le sélecteur système Android.
5. Pour extraire une portée précise, choisissez « Extraction sélective... », parcourez la page de gestion ou cochez des entrées, touchez « Extraire vers... », choisissez la portée puis le dossier de sortie.
6. Avant l'extraction dans la page de gestion, choisissez comment traiter les noms de sortie équivalents. Demander à chaque fois permet d'appliquer une décision d'ignorer, d'écraser ou de renommer automatiquement à tous les conflits compatibles restants.
7. Pour créer une archive, choisissez « Compresser... » dans le menu d'un fichier ou dossier, ou sélectionnez plusieurs éléments du même dossier et utilisez « Compresser... » dans la barre inférieure. Pour créer une archive par élément, activez « Compresser chaque élément séparément », vérifiez l’aperçu des sorties, puis lancez la création ; ce mode résout toujours les conflits par une numérotation automatique sûre.

### Autorisations et données

Le plugin ne demande aucune autorisation de stockage ni de réseau. La navigation native conserve d'abord le descripteur repositionnable en lecture seule de l'hôte et fournit aux archives ordinaires des canaux à position indépendante. Les tubes, les sources inscriptibles ou non repositionnables, Android 7 et les lecteurs exigeant un fichier local lisible par le processus (actuellement les ZIP chiffrés) utilisent le cache privé. Le descripteur ou le cache est nettoyé à la fermeture, à la déconnexion, en cas d'échec ou après expiration. L'extraction utilise uniquement l'URI temporaire de l'hôte. La création d'archive passe par une session de fichiers liée à l'UID du plugin, lit les cibles par pages et ne peut créer une sortie transactionnelle que dans le dossier parent actuel. Les mots de passe restent uniquement dans des tampons mémoire effaçables, ne sont jamais écrits dans les Bundles, préférences, journaux ou diagnostics, puis sont effacés après remplacement, fin de tâche ou destruction de la page. La limite fixe de 4 Gio et les seuils de consultation ont été retirés ; l'isolation des chemins, le contrôle de taille source, les transactions de sortie et le nettoyage restent actifs.

Les budgets de ressources déterminent seulement quand avertir ou demander une confirmation ; ils ne relâchent jamais la sécurité structurelle. Après confirmation, les limites réelles d'octets et de ratio ne sont étendues que jusqu'aux valeurs déclarées par les entrées sélectionnées pour cette extraction. Toute croissance non déclarée, modification de la source ou incohérence de taille ou de CRC interrompt toujours l'opération et nettoie la sortie.

Les noms non sûrs ne sont exposés que comme texte d'affichage en lecture seule derrière des identifiants opaques et ne deviennent jamais des chemins de sortie.

### Roadmap

Les tâches et critères pour davantage de formats, les volumes fractionnés, l'extraction par entrée dans la page native de l'hôte, l'édition d'archives et la matrice complète d'appareils sont regroupés dans le Roadmap. Une case non cochée n'est pas une fonction actuelle.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notes de version

#### Unreleased

_Non publié_

- `Ajout` Le produit s'appelle désormais Archive Manager et l'action devient Ouvrir l'archive
- `Ajout` Explorer Action v5 parcourt les archives dans la liste native d'AutoJs6 avec la barre de chemin, le thème et la navigation Retour existants
- `Ajout` Explorer Action v6 ouvre les entrées prises en charge avec les visionneuses de documents, d'images, de fichiers audio et de vidéos de l'hôte
- `Ajout` Raccourci Extraire vers... pour choisir une destination et extraire toute l'archive
- `Ajout` Extraction sélective... ouvre la page de gestion avec les portées archive entière, dossier interne actuel et sélection cochée, tout en conservant le raccourci d'extraction complète
- `Ajout` Stratégies de conflit d'extraction demander, ignorer, écraser et renommer automatiquement, avec application globale, décompte précis et préservation des dossiers de sortie existants
- `Ajout` Explorer Action v4 ajoute Compresser... aux menus des fichiers et dossiers et à la barre de cinq actions pour les sélections de même dossier parent
- `Ajout` Création de ZIP avec nom par défaut, niveaux de compression, progression, annulation et numérotation automatique des conflits
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
- `Correction` Les mots de passe erronés sont désormais classés de façon stable sous PASSWORD/WRONG_PASSWORD et les entrées AES v2 avec un CRC stocké nul ne sont plus signalées à tort comme endommagées
- `Correction` Le dossier d'extraction par défaut des extensions composées telles que TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST retire désormais le suffixe complet au lieu de conserver `.tar`
- `Correction` Les archives contenant des remontées vers le dossier parent, des chemins absolus, des préfixes de lecteur ou des caractères de contrôle restent consultables ; ces noms passent dans un dossier isolé en lecture seule, restent prévisualisables si leurs données sont lisibles et doivent être explicitement ignorés avant extraction
- `Correction` L'extraction n'écrase plus les fichiers ou dossiers existants lorsque le fournisseur de destination considère comme identiques les noms qui ne diffèrent que par la casse ou sont équivalents en Unicode ; les dossiers de sortie équivalents sont numérotés automatiquement
- `Correction` Une annulation ou un échec d'extraction restaure la nouvelle racine de sortie dans une phase de nettoyage non annulable ; si le fournisseur refuse la suppression, le nom et l'URI du résidu possible remplacent le seul message générique
- `Amélioration` Suppression de la limite fixe de 4 Gio et des seuils de taille/ratio pendant la consultation, sans retirer l'isolation ni les contrôles d'intégrité
- `Amélioration` Ajout d'un Roadmap vérifiable et réécriture du README et du CHANGELOG
- `Amélioration` L'écran autonome suit désormais le mode jour/nuit et les couleurs dynamiques Material
- `Amélioration` Les sorties ZIP, 7Z et TAR passent par une session de l'hôte liée à l'UID, utilisent un fichier temporaire du même dossier et sont validées atomiquement sans autorisation de stockage ni écrasement
- `Amélioration` Les capacités du format et de chaque entrée sont vérifiées uniformément pour la prévisualisation, l'extraction et la création afin de laisser les options indisponibles désactivées
- `Amélioration` Les échecs indiquent le format, l'étape, un code stable et le motif ; les versions de débogage peuvent copier le diagnostic complet
- `Amélioration` Les archives ordinaires sont désormais consultées par des canaux à position indépendante sur le descripteur en lecture seule de l'hôte, sans copie intégrale ; les entrées ou lecteurs incompatibles (actuellement les ZIP chiffrés) utilisent le cache privé, nettoyé à la fermeture, en cas d'échec ou après expiration
- `Amélioration` Transactions de sortie unifiées pour la création ZIP, 7Z et TAR ; les sources illisibles et les échecs de réservation, ouverture, écriture ou validation ont une étape stable, tandis qu'une annulation non confirmée ferme la session, affiche le chemin prévu et interdit une nouvelle tentative risquée
- `Amélioration` La création analyse les sources avant d'ouvrir la sortie temporaire et affiche séparément l'analyse, la compression et la validation, avec le total des fichiers, les octets lus et les tailles inconnues
- `Dépendance` Ajout de Zip4j 2.11.5 sous licence Apache 2.0 pour les flux ZIP chiffrés, la création AES-256 et le chemin de compatibilité Android 7.x
- `Dépendance` Ajout de XZ for Java 1.12 sous licence 0BSD pour lire et écrire TAR.XZ/TXZ en Java pur sans ABI native
- `Dépendance` Ajout de zstd-jni 1.5.7-15 sous licence BSD pour lire et écrire TAR.ZST/TZST ; les quatre ABI Android passent les contrôles d'alignement ELF 16 Kio et RELRO

#### v1.0.1

_2026/08/08_

- `Correction` Liaison de service vide lors de l'activation dans le centre de plugins
- `Amélioration` Simplification du nom, de la description et des instructions

#### v1.0.0

_2026/08/02_

- `Ajout` Première version pour parcourir ZIP, JAR, AAR et WAR et extraire la sélection
- `Ajout` Recherche, sélection, progression, annulation, nettoyage temporaire et interface localisée

##### Historique complet

* [CHANGELOG-fr.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

### Compilation

```powershell
.\gradlew.bat :app:assembleDebug
```

Compilation Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Utilisez le Gradle Wrapper à la racine ; `version.properties` fait foi pour les exigences SDK et JDK.

### Liens

- Documentation AutoJs6: https://docs.autojs6.com
- Mentions relatives aux logiciels tiers: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
