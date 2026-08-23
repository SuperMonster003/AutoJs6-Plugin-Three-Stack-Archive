<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>Plugin du gestionnaire de fichiers AutoJs6 pour parcourir, extraire et créer des archives ZIP/TAR</p>

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

Archive Manager intègre la navigation, l'extraction et la création ZIP/TAR au gestionnaire de fichiers AutoJs6. La version actuelle parcourt les archives dans la liste native de l'hôte avec les chemins externe et interne, prévisualise les entrées prises en charge et crée une archive ZIP ou TAR à partir d'un élément ou d'une sélection de même dossier parent. Davantage de formats, l'extraction par entrée et l'édition interne restent planifiés dans le Roadmap.

### Disponible actuellement

- Ouvrir les archives des familles ZIP et TAR directement dans la liste native d'AutoJs6, avec le thème, le mode sombre et les couleurs dynamiques de l'hôte.
- Afficher le dossier externe, le nom de l'archive et le dossier interne dans la barre de chemin ; toucher un niveau pour y accéder et utiliser Retour pour remonter avant de quitter l'archive.
- Ouvrir les documents, images, fichiers audio et vidéos pris en charge avec les visionneuses existantes de l'hôte.
- Utiliser le raccourci « Extraire vers... » pour extraire toute l'archive sans ouvrir d'abord la vue de navigation.
- Parcourir les dossiers, rechercher et trier le contenu de l'archive.
- Afficher la liste à partir des métadonnées sans décompresser chaque entrée au préalable.
- Parcourir, prévisualiser et extraire les TAR non compressés ; les liens symboliques ou physiques, les nœuds de périphérique et les entrées creuses restent listés sans être écrits comme des fichiers ordinaires.
- Parcourir, prévisualiser et extraire les TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 et TAR.ZST/TZST avec les mêmes chemins internes, l'isolation des entrées spéciales et les contrôles d'intégrité.
- Vérifier la structure ZIP/TAR réelle et unifier les capacités de prévisualisation, d'extraction et de création, en laissant désactivées les options non prises en charge.
- Prendre en charge Zip64, les préambules auto-extractibles, les anciens encodages de noms et les séparateurs Windows.
- Parcourir et extraire les ZIP protégés par ZipCrypto ou AES, réessayer un mot de passe erroné sur place et créer au choix des ZIP chiffrés en AES-256 dont les noms restent visibles ; la création chiffrée exige une confirmation identique du mot de passe.
- Remplacer l'encodage des noms ZIP quand la détection automatique est incorrecte ; la navigation et l'extraction réutilisent le même choix.
- Afficher le format, l'étape, un code stable et un motif clair en cas d'échec ; les versions de débogage peuvent copier le diagnostic complet.
- Proposer « Compresser... » pour les fichiers, les dossiers et les sélections multiples de même dossier parent.
- Créer des ZIP, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST avec un nom et les niveaux propres au format; le mot de passe facultatif reste réservé à ZIP.
- Utiliser par défaut le nom de la cible pour un élément et celui du dossier parent pour plusieurs; changer de format remplace toute l'extension composée.
- Écrire d'abord dans un fichier temporaire du même dossier puis valider atomiquement ; numéroter les conflits sans écraser les fichiers existants.

### Formats actuels

La version actuelle reconnaît les extensions consultables et extractibles suivantes:

```text
zip, jar, aar, war, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

La version actuelle peut créer les formats suivants:

```text
zip, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> La navigation native et l'aperçu des entrées d'Explorer Action v6, ainsi que la compression v4, exigent AutoJs6 avec le code de version 5276 ou plus récent. L'extraction par entrée dans la page native de l'hôte, 7z, les volumes fractionnés, le chiffrement des noms, les archives séparées, la suppression des sources et l'ajout/suppression interne ne sont pas encore publiés. Le Roadmap fait foi.

### Utilisation

1. Installez le plugin et activez-le dans le centre de plugins AutoJs6.
2. Ouvrez le menu d'une archive prise en charge.
3. Choisissez « Ouvrir l'archive », puis entrez dans les dossiers, recherchez ou naviguez avec la barre de chemin de la liste hôte.
4. Pour extraire toute l'archive, choisissez « Extraire vers... » dans son menu puis sélectionnez un dossier avec le sélecteur système Android.
5. Pour créer une archive, choisissez « Compresser... » dans le menu d'un fichier ou dossier, ou sélectionnez plusieurs éléments du même dossier et utilisez « Compresser... » dans la barre inférieure, puis choisissez le format et le niveau.

### Autorisations et données

Le plugin ne demande aucune autorisation de stockage ni de réseau. La navigation native utilise une courte session d'archive en lecture seule liée à l'UID de l'hôte et supprime l'entrée temporaire à la fermeture ou à la déconnexion ; l'extraction utilise uniquement l'URI temporaire de l'hôte. La création d'archive passe par une session de fichiers liée à l'UID du plugin, lit les cibles par pages et ne peut créer une sortie transactionnelle que dans le dossier parent actuel. Les mots de passe restent uniquement dans des tampons mémoire effaçables, ne sont jamais écrits dans les Bundles, préférences, journaux ou diagnostics, puis sont effacés après remplacement, fin de tâche ou destruction de la page. La limite fixe de 4 Gio et les seuils de consultation ont été retirés ; l'isolation des chemins, le contrôle de taille source, les transactions de sortie et le nettoyage restent actifs.

### Roadmap

Les tâches et critères pour davantage de formats, les volumes fractionnés, l'extraction par entrée, l'édition d'archives et la matrice complète d'appareils sont regroupés dans le Roadmap. Une case non cochée n'est pas une fonction actuelle.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notes de version

#### Unreleased

_Non publié_

- `Ajout` Le produit s'appelle désormais Archive Manager et l'action devient Ouvrir l'archive
- `Ajout` Explorer Action v5 parcourt les archives dans la liste native d'AutoJs6 avec la barre de chemin, le thème et la navigation Retour existants
- `Ajout` Explorer Action v6 ouvre les entrées prises en charge avec les visionneuses de documents, d'images, de fichiers audio et de vidéos de l'hôte
- `Ajout` Raccourci Extraire vers... pour choisir une destination et extraire toute l'archive
- `Ajout` Explorer Action v4 ajoute Compresser... aux menus des fichiers et dossiers et à la barre de cinq actions pour les sélections de même dossier parent
- `Ajout` Création de ZIP avec nom par défaut, niveaux de compression, progression, annulation et numérotation automatique des conflits
- `Ajout` Création de TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST avec niveaux propres au format et mise à jour complète des extensions composées
- `Ajout` Navigation et extraction des ZIP chiffrés avec ZipCrypto/AES, nouvelle saisie sur place d'un mot de passe erroné et création facultative de ZIP AES-256 dont les noms restent visibles avec confirmation identique du mot de passe
- `Ajout` Navigation, aperçu et extraction des TAR non compressés dans la liste native avec validation de la somme de contrôle des en-têtes ; liens, nœuds de périphérique et entrées creuses restent en lecture seule
- `Ajout` Navigation, aperçu et extraction des TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 et TAR.ZST/TZST par les mêmes chemins natifs ; la détection vérifie la signature du compresseur et la structure TAR interne
- `Correction` La liste ZIP utilise les métadonnées et accepte les préambules auto-extractibles, les anciens encodages, les séparateurs Windows et davantage de méthodes lisibles
- `Correction` L'encodage des noms ZIP peut être remplacé si la détection automatique est incorrecte et l'extraction réutilise ce choix
- `Correction` Les tailles inconnues, les URI DocumentsProvider valides et les droits d'écriture supplémentaires de l'hôte ne bloquent plus une archive valide
- `Correction` Correction de la consultation et de l'extraction ZIP sous Android 7.x, qui appelaient des API réservées aux systèmes récents
- `Correction` Les mots de passe erronés sont désormais classés de façon stable sous PASSWORD/WRONG_PASSWORD et les entrées AES v2 avec un CRC stocké nul ne sont plus signalées à tort comme endommagées
- `Amélioration` Suppression de la limite fixe de 4 Gio et des seuils de taille/ratio pendant la consultation, sans retirer l'isolation ni les contrôles d'intégrité
- `Amélioration` Ajout d'un Roadmap vérifiable et réécriture du README et du CHANGELOG
- `Amélioration` L'écran autonome suit désormais le mode jour/nuit et les couleurs dynamiques Material
- `Amélioration` Les sorties ZIP et TAR passent par une session de l'hôte liée à l'UID, utilisent un fichier temporaire du même dossier et sont validées atomiquement sans autorisation de stockage ni écrasement
- `Amélioration` Les capacités du format et de chaque entrée sont vérifiées uniformément pour la prévisualisation, l'extraction et la création afin de laisser les options indisponibles désactivées
- `Amélioration` Les échecs indiquent le format, l'étape, un code stable et le motif ; les versions de débogage peuvent copier le diagnostic complet
- `Dépendance` Ajout de Zip4j 2.11.5 sous licence Apache 2.0 pour les flux ZIP chiffrés, la création AES-256 et le chemin de compatibilité Android 7.x
- `Dépendance` Ajout de XZ for Java 1.12 sous licence 0BSD pour lire et écrire TAR.XZ/TXZ en Java pur sans ABI native
- `Dépendance` Ajout de zstd-jni 1.5.7-15 sous licence BSD pour lire et écrire TAR.ZST/TZST; les quatre ABI Android satisfont les contrôles d'alignement ELF 16 Kio et RELRO

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
