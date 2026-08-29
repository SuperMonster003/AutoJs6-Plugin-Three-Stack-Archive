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
- Quand le nom ne correspond pas à une archive prise en charge, proposer la vérification explicite Ouvrir comme archive... et afficher le format détecté dans la barre de chemin; les suffixes TAR composés exacts n'assimilent plus les flux `.gz`, `.xz`, `.bz2` ou `.zst` ordinaires à des archives TAR.
- Prévisualiser les documents, images, sons et vidéos lisibles avec les visionneuses existantes de l'hôte, sans extraire toute l'archive.
- Extraire toute l'archive, le dossier interne courant ou une sélection avec progression, annulation, noms de conflit sûrs, vérification et retour arrière avant publication.
- Ouvrir et extraire les entrées ZIP, 7Z et RAR chiffrées avec une demande de mot de passe native; une erreur peut être corrigée sans perdre le chemin courant.
- Corriger l'encodage des noms ZIP directement depuis la barre de chemin; la même session en lecture seule reconstruit son index et conserve si possible le chemin interne et la sélection disponibles.
- Parcourir, prévisualiser et extraire les ensembles complets `.z01 + .zip`, WinRAR modernes `partN.rar`, `.zip.001` numérotés et `.7z.001` numérotés via des descripteurs frères bornés et autorisés par l'hôte; un volume absent ou modifié échoue explicitement.
- Créer ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST depuis un élément ou une sélection de même parent; ZIP prend aussi en charge AES-256, les volumes standard et une archive par élément.
- Les ZIP fractionnés standard et la compression séparée publient toutes les sorties physiques vérifiées dans un lot récupérable; un échec ou redémarrage de l'hôte n'est jamais présenté comme un résultat partiel réussi.
- Modifier les ZIP ordinaires à volume unique, les 7Z ordinaires sûrs et les TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 ou TAR.ZST/TZST compatibles par reconstruction vérifiée: ajouter des fichiers ou un arbre de dossiers, créer un dossier vide, renommer et supprimer, puis remplacer atomiquement la source.
- Après une modification d'archive inscriptible réussie, restaurer la version précédente depuis le message de réussite ou le menu de gestion; l'hôte propose une seule restauration pendant une rétention limitée et la refuse si une autre application a modifié la cible.
- La page de gestion regroupe le format réel, les totaux du contenu, les modifications disponibles et la raison exacte du mode lecture seule; avant de réserver une sortie, chaque modification présente le travail réel, la reconstruction complète et les effets sur les métadonnées, tandis qu'une annulation ne crée aucune sortie en attente.
- Isoler en lecture seule les noms dangereux, appliquer les limites structurelles et de ressources avant l'écriture, et lire directement le descripteur seekable de l'hôte lorsque possible.
- Déplacer facultativement la sélection source complète vers la corbeille de l'hôte uniquement après vérification et validation de toutes les sorties physiques; cette option est désactivée par défaut et toute source modifiée ou preuve incomplète arrête l'opération avant la suppression des données source.

### Formats actuels

La version actuelle reconnaît les extensions consultables et extractibles suivantes:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

La version actuelle peut créer les formats suivants:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> L'intégration native exige la build AutoJs6 6.8.0 associée avec Explorer Action v18 (code de version 5276 ou supérieur). RAR et les archives fractionnées restent volontairement en lecture seule. La modification est disponible pour les ZIP ordinaires à volume unique, les 7Z non chiffrés, non solid, à volume unique et dans le budget du décodeur, ainsi que les archives TAR ne contenant que des fichiers et dossiers ordinaires sûrs. Les 7Z chiffrés, solid, fractionnés, dangereux, non pris en charge ou hors budget restent en lecture seule. Ouvrez un ZIP fractionné standard par son `.zip` final, un ensemble WinRAR moderne par son premier `partN.rar`, et un ZIP ou 7Z numéroté par son volume `.001`, avec tous les volumes requis dans le même dossier. Le chiffrement des noms à la création et la modification interne de JAR/AAR/WAR ou RAR restent indisponibles.

### Utilisation

1. Installez Archive Manager et activez-le dans le Centre de plugins AutoJs6.
2. Touchez l'action principale d'une archive ou choisissez Ouvrir l'archive. Si son nom n'est pas reconnu, choisissez Ouvrir comme archive... dans le menu. Parcourez-la comme un dossier avec la barre de chemin, qui indique le format détecté lorsque le nom était trompeur.
3. Utilisez l'action d'extraction de la barre de chemin pour le dossier interne courant ou l'action d'encodage pour corriger les noms ZIP, une pression longue pour extraire une sélection, ou Extraire vers... dans le menu du fichier pour toute l'archive. Le mot de passe est demandé si nécessaire.
4. Choisissez Compresser... pour un fichier ou dossier, ou sélectionnez plusieurs éléments du même répertoire et utilisez l'action de la barre inférieure. Pour nettoyer les sources après réussite, activez explicitement l'option désactivée par défaut qui déplace les sources vers la corbeille après compression.
5. Choisissez Gérer l'archive... pour ajouter, renommer ou supprimer du contenu dans un ZIP ordinaire à volume unique, un 7Z ordinaire sûr ou un TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 ou TAR.ZST/TZST compatible.

### Autorisations et données

Archive Manager ne demande aucune autorisation de stockage ou de réseau. L'hôte fournit des descripteurs en lecture seule de courte durée et des transactions liées à l'UID du plugin; celui-ci ne peut donc pas choisir un chemin arbitraire. Explorer Action v11 transporte le mot de passe uniquement dans une requête synchrone bornée et ne le persiste jamais. Explorer Action v12 ajoute seulement un catalogue borné, lié à la session, de volumes frères approuvés, avec identifiants opaques et revalidation de l'identité. Explorer Action v13 réindexe uniquement la même source préparée et conserve l'ancien état jusqu'à ce qu'un index complet soit prêt. Explorer Action v14 ne reconnaît que des suffixes composés bornés tels que `.zip.001` et `.7z.001`, jamais n'importe quel fichier `.001`, et réutilise le catalogue v12 sans donner accès au dossier ni aux chemins. Explorer Action v17 ajoute uniquement une action secondaire en lecture seule et sans filtre lorsque l'action principale ne correspond pas; elle réutilise une session existante après le choix de l'utilisateur et n'accorde aucun accès supplémentaire au chemin, au dossier ou en écriture. Les chemins dangereux restent isolés, la sortie est vérifiée avant publication et la confirmation d'un budget ne désactive jamais la sécurité structurelle.

Explorer Action v15 regroupe uniquement les nouveaux fichiers vérifiés d'une même session dans un lot récupérable de 128 membres au maximum. Explorer Action v16 permet à l'hôte de revérifier les sources et sorties puis de déplacer les sources vers la corbeille seulement si le plugin fournit la sélection originale complète et ordonnée ainsi que toutes les transactions de sortie validées. L'hôte synchronise une copie de récupération et persiste son entrée avant de retirer les données source; le plugin n'obtient aucun chemin arbitraire ni suppression directe. Une réponse Binder perdue est résolue en consultant le même état terminal idempotent, sans recommencer le déplacement.

Explorer Action v18 conserve l'archive précédente uniquement dans le stockage privé et durable de l'hôte et renvoie un identifiant opaque, jamais un chemin de sauvegarde. Une seule restauration est autorisée tant que le dossier parent et la cible correspondent exactement au remplacement validé. Les changements externes invalident l'historique; les preuves d'une récupération interrompue sont conservées et bloquent un autre remplacement de cette cible jusqu'à leur résolution par l'hôte. L'historique normal est limité par l'âge, le nombre, le total d'octets et la réserve d'espace libre; les sessions v8-v17 ne créent aucune sauvegarde de remplacement.

### Roadmap

Les travaux restants sont suivis par cases à cocher: planificateur de modification partagé et indépendant du format, annulation groupée et historique de la corbeille, reste de la matrice des appareils et ressources de la première publication publique.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notes de version

#### v2.17.0

_2026/08/29_

- `Note` Cette version exige toujours la build AutoJs6 6.8.0 associée avec Explorer Action v18 (code de version 5276 ou supérieur)
- `Ajout` Les 7Z ordinaires à volume unique, non chiffrés, non solid et dans le budget du décodeur peuvent désormais ajouter des fichiers ou des arborescences, créer des dossiers vides, renommer et supprimer depuis la page de gestion
- `Correction` L'action de gestion inclut maintenant `.7z`; les variantes chiffrées, solid, fractionnées, dangereuses, non prises en charge ou hors budget restent en lecture seule après inspection de la structure
- `Amélioration` Les modifications 7Z reconstruisent une sortie LZMA2 non solid et bornée, puis la relisent entièrement avant le remplacement atomique; l'annulation, les changements de source, une sortie en attente endommagée ou un échec d'écriture préservent l'original

#### v2.16.0

_2026/08/29_

- `Note` Cette version nécessite toujours la compilation AutoJs6 6.8.0 associée avec Explorer Action v18 (code de version 5276 ou ultérieur)
- `Ajout` Les archives TAR.ZST et TZST ne contenant que des fichiers et dossiers ordinaires sûrs peuvent désormais ajouter des fichiers ou des arborescences complètes, créer des dossiers vides, renommer et supprimer depuis la page de gestion
- `Correction` L'action de gestion reconnaît désormais précisément `.tar.zst` et `.tzst`; toutes les enveloppes TAR prises en charge utilisent maintenant la même limite de reconstruction modifiable vérifiée
- `Amélioration` Les modifications TAR.ZST utilisent Zstandard niveau 3 borné et monothread avec une fenêtre de 1 MiB et une somme de contrôle de trame, puis reconstruisent directement dans une sortie en attente de l'hôte; l'annulation, l'intégrité de la somme, les véritables échecs d'écriture, les changements de source et la relecture complète restent dans la limite d'annulation

#### v2.15.0

_2026/08/29_

- `Note` Cette version nécessite toujours la compilation AutoJs6 6.8.0 associée avec Explorer Action v18 (code de version 5276 ou ultérieur)
- `Ajout` Les archives TAR.BZ2 et TBZ2 ne contenant que des fichiers et dossiers ordinaires sûrs peuvent désormais ajouter des fichiers ou des arborescences complètes, créer des dossiers vides, renommer et supprimer depuis la page de gestion
- `Correction` L'action de gestion reconnaît désormais précisément `.tar.bz2` et `.tbz2`; TAR.ZST/TZST est la seule enveloppe TAR compressée qui reste en lecture seule
- `Amélioration` Les modifications TAR.BZ2 utilisent le preset 6 borné de taille de bloc BZIP2 et reconstruisent directement dans une sortie en attente de l'hôte; l'annulation, l'intégrité de fin, les véritables échecs d'écriture, les changements de source et la relecture complète restent dans la limite d'annulation

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
