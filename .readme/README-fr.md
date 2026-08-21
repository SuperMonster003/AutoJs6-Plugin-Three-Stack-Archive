<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>Plugin du gestionnaire de fichiers AutoJs6 pour ouvrir, extraire et créer des archives ZIP</p>

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

Archive Manager intègre la navigation, l'extraction et la création de ZIP au gestionnaire de fichiers AutoJs6. La version actuelle compresse un élément ou une sélection de même dossier parent et écrit le résultat via une session de fichiers contrôlée par l'hôte. Davantage de formats, une page d'archive native de l'hôte et l'édition interne restent planifiés dans le Roadmap.

### Disponible actuellement

- Ouvrir les archives de la famille ZIP depuis le menu de fichiers AutoJs6.
- Utiliser le raccourci « Extraire vers... » pour extraire toute l'archive sans ouvrir d'abord la vue de navigation.
- Parcourir les dossiers, rechercher des chemins et sélectionner des fichiers ou dossiers.
- Afficher la liste à partir des métadonnées sans décompresser chaque entrée au préalable.
- Prendre en charge Zip64, les préambules auto-extractibles, les anciens encodages de noms et les séparateurs Windows.
- Laisser l'archive consultable lorsqu'une entrée ne peut pas être extraite.
- Extraire la sélection vers un dossier choisi par le sélecteur Android, avec progression et annulation.
- Proposer « Compresser... » pour les fichiers, les dossiers et les sélections multiples de même dossier parent.
- Créer des ZIP avec un nom et un niveau de compression configurables ; utiliser par défaut le nom de la cible pour un élément et celui du dossier parent pour plusieurs.
- Écrire d'abord dans un fichier temporaire du même dossier puis valider atomiquement ; numéroter les conflits sans écraser les fichiers existants.

### Formats actuels

La version actuelle reconnaît les extensions suivantes de la famille ZIP:

```text
zip, jar, aar, war
```

La version actuelle peut créer les formats suivants:

```text
zip
```

> L'intégration Explorer Action v4 exige AutoJs6 avec le code de version 5276 ou plus récent. 7z, les variantes tar, les mots de passe, les volumes fractionnés, le chiffrement des noms, les archives séparées, la suppression des sources et l'ajout/suppression interne ne sont pas encore publiés. Le Roadmap fait foi.

### Utilisation

1. Installez le plugin et activez-le dans le centre de plugins AutoJs6.
2. Ouvrez le menu d'un fichier ZIP, JAR, AAR ou WAR.
3. Choisissez « Ouvrir l'archive », puis parcourez ou recherchez et sélectionnez le contenu.
4. Choisissez « Extraire la sélection » et indiquez le dossier de sortie ; pour toute l'archive, choisissez directement « Extraire vers... » dans son menu.
5. Pour créer un ZIP, choisissez « Compresser... » dans le menu d'un fichier ou dossier, ou sélectionnez plusieurs éléments du même dossier et utilisez « Compresser... » dans la barre inférieure.

### Autorisations et données

Le plugin ne demande aucune autorisation de stockage ni de réseau. La navigation et l'extraction utilisent uniquement l'URI temporaire de l'hôte. La création de ZIP passe par une session courte liée à l'UID du plugin, lit les cibles par pages et ne peut créer une sortie transactionnelle que dans le dossier parent actuel. La limite fixe de 4 Gio et les seuils de consultation ont été retirés ; l'isolation des chemins, les contrôles d'intégrité et le nettoyage restent actifs.

### Roadmap

Les tâches et critères pour davantage de formats, les mots de passe et volumes, l'édition, une page d'archive native de l'hôte, la barre de chemin interne et la matrice complète d'appareils sont regroupés dans le Roadmap. Une case non cochée n'est pas une fonction actuelle.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notes de version

#### Unreleased

_Non publié_

- `Ajout` Le produit s'appelle désormais Archive Manager et l'action devient Ouvrir l'archive
- `Ajout` Raccourci Extraire vers... pour choisir une destination et extraire toute l'archive
- `Ajout` Explorer Action v4 ajoute Compresser... aux menus des fichiers et dossiers et à la barre de cinq actions pour les sélections de même dossier parent
- `Ajout` Création de ZIP avec nom par défaut, niveaux de compression, progression, annulation et numérotation automatique des conflits
- `Correction` La liste ZIP utilise les métadonnées et accepte les préambules auto-extractibles, les anciens encodages, les séparateurs Windows et davantage de méthodes lisibles
- `Correction` Les tailles inconnues, les URI DocumentsProvider valides et les droits d'écriture supplémentaires de l'hôte ne bloquent plus une archive valide
- `Correction` Correction de la consultation et de l'extraction ZIP sous Android 7.x, qui appelaient des API réservées aux systèmes récents
- `Amélioration` Suppression de la limite fixe de 4 Gio et des seuils de taille/ratio pendant la consultation, sans retirer l'isolation ni les contrôles d'intégrité
- `Amélioration` Ajout d'un Roadmap vérifiable et réécriture du README et du CHANGELOG
- `Amélioration` L'écran autonome suit désormais le mode jour/nuit et les couleurs dynamiques Material
- `Amélioration` La sortie ZIP passe par une session de l'hôte liée à l'UID, utilise un fichier temporaire du même dossier et est validée atomiquement sans autorisation de stockage ni écrasement

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
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
