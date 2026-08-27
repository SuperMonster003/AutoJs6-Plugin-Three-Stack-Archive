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
- Corriger l'encodage des noms ZIP directement depuis la barre de chemin; la même session en lecture seule reconstruit son index et conserve si possible le chemin interne et la sélection disponibles.
- Parcourir, prévisualiser et extraire les ensembles complets `.z01 + .zip`, WinRAR modernes `partN.rar`, `.zip.001` numérotés et `.7z.001` numérotés via des descripteurs frères bornés et autorisés par l'hôte; un volume absent ou modifié échoue explicitement.
- Créer ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST depuis un élément ou une sélection de même parent; ZIP prend aussi en charge AES-256, les volumes standard et une archive par élément.
- Les ZIP fractionnés standard et la compression séparée publient toutes les sorties physiques vérifiées dans un lot récupérable; un échec ou redémarrage de l'hôte n'est jamais présenté comme un résultat partiel réussi.
- Modifier un ZIP ordinaire à volume unique par reconstruction vérifiée: ajouter des fichiers ou un arbre de dossiers, créer un dossier vide, renommer et supprimer, puis remplacer atomiquement la source.
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

> L'intégration native exige la build AutoJs6 6.8.0 associée avec Explorer Action v16 (code de version 5276 ou supérieur). RAR et les archives fractionnées restent volontairement en lecture seule; la modification est limitée aux `.zip` ordinaires à volume unique. Ouvrez un ZIP fractionné standard par son `.zip` final, un ensemble WinRAR moderne par son premier `partN.rar`, et un ZIP ou 7Z numéroté par son volume `.001`, avec tous les volumes requis dans le même dossier. Le chiffrement des noms à la création et la modification interne de 7Z, RAR ou TAR ne sont pas disponibles.

### Utilisation

1. Installez Archive Manager et activez-le dans le Centre de plugins AutoJs6.
2. Touchez l'action principale d'une archive ou choisissez Ouvrir l'archive, puis parcourez-la comme un dossier avec la barre de chemin de l'hôte.
3. Utilisez l'action d'extraction de la barre de chemin pour le dossier interne courant ou l'action d'encodage pour corriger les noms ZIP, une pression longue pour extraire une sélection, ou Extraire vers... dans le menu du fichier pour toute l'archive. Le mot de passe est demandé si nécessaire.
4. Choisissez Compresser... pour un fichier ou dossier, ou sélectionnez plusieurs éléments du même répertoire et utilisez l'action de la barre inférieure. Pour nettoyer les sources après réussite, activez explicitement l'option désactivée par défaut qui déplace les sources vers la corbeille après compression.
5. Choisissez Gérer l'archive... uniquement pour ajouter, renommer ou supprimer du contenu dans un ZIP ordinaire à volume unique.

### Autorisations et données

Archive Manager ne demande aucune autorisation de stockage ou de réseau. L'hôte fournit des descripteurs en lecture seule de courte durée et des transactions liées à l'UID du plugin; celui-ci ne peut donc pas choisir un chemin arbitraire. Explorer Action v11 transporte le mot de passe uniquement dans une requête synchrone bornée et ne le persiste jamais. Explorer Action v12 ajoute seulement un catalogue borné, lié à la session, de volumes frères approuvés, avec identifiants opaques et revalidation de l'identité. Explorer Action v13 réindexe uniquement la même source préparée et conserve l'ancien état jusqu'à ce qu'un index complet soit prêt. Explorer Action v14 ne reconnaît que des suffixes composés bornés tels que `.zip.001` et `.7z.001`, jamais n'importe quel fichier `.001`, et réutilise le catalogue v12 sans donner accès au dossier ni aux chemins. Les chemins dangereux restent isolés, la sortie est vérifiée avant publication et la confirmation d'un budget ne désactive jamais la sécurité structurelle.

Explorer Action v15 regroupe uniquement les nouveaux fichiers vérifiés d'une même session dans un lot récupérable de 128 membres au maximum. Explorer Action v16 permet à l'hôte de revérifier les sources et sorties puis de déplacer les sources vers la corbeille seulement si le plugin fournit la sélection originale complète et ordonnée ainsi que toutes les transactions de sortie validées. L'hôte synchronise une copie de récupération et persiste son entrée avant de retirer les données source; le plugin n'obtient aucun chemin arbitraire ni suppression directe. Une réponse Binder perdue est résolue en consultant le même état terminal idempotent, sans recommencer le déplacement.

### Roadmap

Les travaux restants sont suivis par cases à cocher: reconstructions modifiables au-delà de ZIP, annulation groupée et historique de la corbeille, accessibilité, reste de la matrice des appareils et producteurs, et ressources de la première publication publique.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notes de version

#### v2.9.0

_2026/08/27_

- `Note` Cette version exige la build AutoJs6 6.8.0 associée avec Explorer Action v16 (code de version 5276 ou supérieur)
- `Ajout` Le formulaire de compression ajoute l'option désactivée par défaut Déplacer les sources vers la corbeille après compression, exécutée seulement après vérification et validation de toutes les sorties physiques
- `Ajout` Explorer Action v16 accepte uniquement la sélection originale complète et ordonnée ainsi que toutes les transactions de sortie validées, puis l'hôte revérifie les identités avant d'utiliser sa corbeille
- `Correction` L'hôte synchronise désormais une copie récupérable et persiste son entrée de corbeille avant de retirer une source; si un dossier n'est retiré que partiellement, la copie récupérable est conservée
- `Amélioration` La phase de corbeille ne peut pas être annulée et distingue les résultats validé, récupération requise, échec et inconnu; une réponse Binder perdue interroge l'état terminal de l'hôte sans recommencer aveuglément

#### v2.8.0

_2026/08/27_

- `Note` Cette version exige la build AutoJs6 6.8.0 associée avec Explorer Action v15 (code de version 5276 ou supérieur)
- `Ajout` Les ZIP fractionnés standard et l'option Compresser chaque élément séparément terminent désormais l'écriture et la relecture de toutes les sorties avant une publication groupée récupérable par Explorer Action v15
- `Correction` La création de plusieurs sorties ne laisse plus de résultats partiellement validés lors des échecs normaux; Explorer n'est actualisé et la réussite annoncée qu'après la validation du lot complet
- `Correction` Les interrupteurs des options de compression s'affichent désormais correctement et restent tactiles sous Android 7, au lieu d'apparaître comme de simples libellés
- `Amélioration` L'hôte journalise durablement le dossier parent et l'identité de chaque fichier préparé avant publication; un échec ou redémarrage annule uniquement les membres identiques, tandis que les fichiers modifiés de l'extérieur sont conservés pour récupération manuelle

#### v2.7.0

_2026/08/27_

- `Note` Cette version exige la build AutoJs6 6.8.0 associée avec Explorer Action v14 (code de version 5276 ou supérieur)
- `Ajout` Les ensembles numérotés `.zip.001` et `.7z.001` complets peuvent désormais être parcourus, prévisualisés et extraits en ouvrant leur volume `.001`; ils restent en lecture seule
- `Ajout` Explorer Action v14 ajoute la correspondance bornée des suffixes composés et réutilise la source de volumes frères v12 liée à l'UID sans reconnaître les fichiers `.001` arbitraires
- `Correction` Android 7 regroupe les volumes ZIP numérotés autorisés par l'hôte dans un fichier local privé avant le chemin de compatibilité Zip4j, afin qu'un ensemble valide ne soit plus signalé comme endommagé
- `Amélioration` Les numéros de volumes frères sont bornés de `.002` à `.128` et tous les volumes fournis doivent être contigus; le lecteur indique le prochain volume absent, revérifie l'identité autour de la matérialisation et n'annonce jamais de modification interne

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
