<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>Un gestionnaire d'archives intégré à AutoJs6 pour parcourir, extraire, créer et modifier en toute sécurité les formats pris en charge</p>

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

Archive Manager fonctionne dans le gestionnaire de fichiers AutoJs6 au lieu de le remplacer. Les archives prises en charge utilisent la liste, la barre de chemin, le thème, les visionneuses, la sélection, la progression et l'actualisation de l'hôte. Une page de gestion séparée reste réservée aux réglages et opérations qui exigent un formulaire plus complet.

### Disponible actuellement

- Parcourir ZIP/JAR/AAR/WAR, 7Z, RAR4/RAR5 et la famille TAR dans la liste native AutoJs6, avec chemin interne, recherche, tri et navigation Retour.
- Prévisualiser les documents, images, sons et vidéos lisibles avec les visionneuses existantes de l'hôte, sans extraire toute l'archive.
- Extraire toute l'archive, le dossier interne courant ou une sélection avec progression, annulation, noms de conflit sûrs, vérification et retour arrière avant publication.
- Ouvrir et extraire les entrées ZIP, 7Z et RAR chiffrées avec une demande de mot de passe native; une erreur peut être corrigée sans perdre le chemin courant.
- Parcourir, prévisualiser et extraire les ensembles standard `.z01 + .zip` complets et les ensembles WinRAR modernes `partN.rar` via des descripteurs frères bornés et autorisés par l'hôte; un volume absent ou modifié échoue explicitement.
- Créer ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST depuis un élément ou une sélection de même parent; ZIP prend aussi en charge AES-256, les volumes standard et une archive par élément.
- Modifier un ZIP ordinaire à volume unique par reconstruction vérifiée: ajouter des fichiers ou un arbre de dossiers, créer un dossier vide, renommer et supprimer, puis remplacer atomiquement la source.
- Isoler en lecture seule les noms dangereux, appliquer les limites structurelles et de ressources avant l'écriture, et lire directement le descripteur seekable de l'hôte lorsque possible.

### Formats actuels

La version actuelle reconnaît les extensions consultables et extractibles suivantes:

```text
zip, jar, aar, war, 7z, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

La version actuelle peut créer les formats suivants:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> L'intégration native exige la build AutoJs6 6.8.0 associée avec Explorer Action v12 (code de version 5276 ou supérieur). RAR et les archives fractionnées restent volontairement en lecture seule; la modification est limitée aux `.zip` ordinaires à volume unique. Ouvrez un ZIP fractionné standard par son `.zip` final, ou un ensemble WinRAR moderne par son premier `partN.rar`, avec tous les volumes requis dans le même dossier. Les flux `.zip.001`, 7Z fractionné, le chiffrement des noms à la création, la suppression des sources et la modification interne de 7Z, RAR ou TAR ne sont pas disponibles.

### Utilisation

1. Installez Archive Manager et activez-le dans le Centre de plugins AutoJs6.
2. Touchez l'action principale d'une archive ou choisissez Ouvrir l'archive, puis parcourez-la comme un dossier avec la barre de chemin de l'hôte.
3. Utilisez l'action de la barre de chemin pour le dossier interne courant, une pression longue pour extraire une sélection, ou Extraire vers... dans le menu du fichier pour toute l'archive. Le mot de passe est demandé si nécessaire.
4. Choisissez Compresser... pour un fichier ou dossier, ou sélectionnez plusieurs éléments du même répertoire et utilisez l'action de la barre inférieure.
5. Choisissez Gérer l'archive... uniquement pour ajouter, renommer ou supprimer du contenu dans un ZIP ordinaire à volume unique.

### Autorisations et données

Archive Manager ne demande aucune autorisation de stockage ou de réseau. L'hôte fournit des descripteurs en lecture seule de courte durée et des transactions liées à l'UID du plugin; celui-ci ne peut donc pas choisir un chemin arbitraire. Explorer Action v11 transporte le mot de passe uniquement dans une requête synchrone bornée; les deux côtés effacent aussitôt leurs tampons et ne le persistent jamais. Explorer Action v12 ajoute seulement un catalogue borné, lié à la session, de volumes frères approuvés: le plugin reçoit des identifiants opaques plutôt que des chemins, et l'UID appelant, l'identité, la taille, la date et le cycle de vie sont revérifiés. Les copies d'exécution inévitables rendent le nettoyage des mots de passe préférable mais non absolu. Les chemins dangereux restent isolés, la sortie est vérifiée avant publication et la confirmation d'un budget ne désactive jamais la sécurité structurelle.

### Roadmap

Les travaux restants sont suivis par cases à cocher: correction native de l'encodage des noms, recherche sur `.zip.001` et 7Z fractionné, reconstructions modifiables au-delà de ZIP, annulation ou suppression transactionnelle des sources, accessibilité, ainsi que le reste de la matrice des appareils et producteurs.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notes de version

#### v2.5.0

_2026/08/27_

- `Note` Cette version exige la build AutoJs6 6.8.0 associée avec Explorer Action v12 (code de version 5276 ou supérieur)
- `Ajout` Les ensembles standard `.z01 + .zip` complets et les ensembles WinRAR modernes `partN.rar` peuvent désormais être parcourus, prévisualisés et extraits dans la page native; les archives fractionnées restent en lecture seule
- `Ajout` Explorer Action v12 fournit uniquement un catalogue borné de volumes frères approuvés par l'hôte et ouvre chacun par identifiant opaque comme descripteur en lecture seule, sans exposer de dossier ni de chemin système
- `Correction` Les métadonnées du répertoire du volume ZIP final sont correctement acceptées sous Android 7 et versions ultérieures, et les données sont lues sur tous les volumes autorisés sans déclarer le dernier volume endommagé
- `Correction` Les CRC de segments RAR ne sont plus comparés aux données reconstituées; les volumes manquants ou modifiés après l'ouverture produisent des erreurs typées stables
- `Amélioration` Le nombre de volumes, les noms, identifiants, ouvertures, UID appelant, identités de fichiers et durées de session sont bornés et revérifiés; une copie interrompue supprime tous les fragments du cache privé

#### v2.4.0

_2026/08/26_

- `Note` Cette version exige AutoJs6 6.8.0 avec Explorer Action v11, code de version 5276 ou supérieur
- `Ajout` Les archives RAR4/RAR5 peuvent être parcourues, prévisualisées et extraites, y compris avec contenu ou en-têtes chiffrés; RAR reste volontairement en lecture seule
- `Ajout` La page d'archive native AutoJs6 peut demander un mot de passe lors de l'ouverture initiale ou de l'extraction, puis réessayer sans perdre le chemin ou la sélection
- `Correction` Le premier volume d'un RAR fractionné conserve ses métadonnées lisibles mais ne propose plus l'extraction lorsque les volumes frères sont indisponibles
- `Correction` Un mot de passe erroné efface la saisie précédente et relance sur un instantané inchangé sans quitter la page native
- `Amélioration` RAR lit directement le descripteur seekable de l'hôte lorsque possible, n'ajoute aucun ABI natif et réutilise les contrôles de sécurité communs
- `Dépendance` Ajout de Junrar 8.1.0 et SLF4J 2.0.17 pour la prise en charge RAR en lecture seule selon leurs licences incluses

#### v2.3.0

_2026/08/26_

- `Note` Cette version exige la compilation AutoJs6 6.8.0 associée avec Explorer Action v10 (code de version 5276 ou ultérieur)
- `Ajout` La page native des archives peut maintenant extraire le dossier interne courant depuis la barre de chemin ou les entrées cochées depuis la barre de sélection, sans ouvrir une page de gestion distincte
- `Ajout` L'extraction native écrit dans une arborescence de sortie appartenant à l'hôte, avec progression, annulation, numérotation sûre des conflits et actualisation automatique d'Explorer
- `Correction` Quitter une archive sous Android 7 ne provoque plus de plantage pendant le nettoyage du cache d'aperçu par l'hôte
- `Correction` Les libellés de la barre de sélection à cinq actions sont centrés sous leurs icônes sur les écrans étroits
- `Amélioration` Le mode de sélection d'archive n'affiche plus que Quitter et Extraire, et masque les actions du système de fichiers sans objet dans une archive

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
