# Spécification — Copilote Contrôle Interne

## 1. Le problème à résoudre

Un collaborateur débutant reçoit une procédure de contrôle interne (questionnaire ou programme de travail). En séance, trois échecs classiques :

- **questions oubliées** ou traitées trop vite, découvertes à la revue ;
- **réponses non exploitées** : l'information a été dite mais pas rattachée à la bonne question ;
- **compte rendu à reconstruire** après coup, à partir de notes et du souvenir.

L'application transforme le questionnaire en **assistant de séance** : elle écoute, rattache ce qui est dit aux questions, montre le reste à poser et rédige le compte rendu.

## 2. Utilisateur et contexte

- Utilisateur : collaborateur junior (ou stagiaire) du cabinet, seul devant le client ou en interne.
- Matériel : smartphone ou tablette Android, écouteurs éventuels ; séance de 30 à 90 minutes.
- Contrainte forte : secret professionnel → traitement local, chiffrement au repos, aucune dépendance cloud par défaut.

## 3. Modèle de données

### Questionnaire (YAML, versionné dans git)

```yaml
nom: Contrôle interne — cycle achats
version: 1.0
source: Procédure interne CI-ACH-01 (rév. 2026-01)
sections:
  - titre: Organisation et délégations
    questions:
      - id: ORG-01
        question: "Qui valide définitivement un engagement fournisseur, et sur quel seuil ?"
        attendu: "Nom du valideur + seuil en euros"
        relance: "Et au-delà de ce seuil, qui intervient ?"
        mots_cles: [seuil, validation, bon de commande, délégation]
      - id: ORG-02
        question: "Existe-t-il une matrice de délégations écrite et à jour ?"
        attendu: "Oui/non + date de dernière mise à jour + où elle est rangée"
        mots_cles: [matrice, délégation, à jour]
        pieces: ["matrice signée"]
```

### État d'une question

| État | Sens | Qui le pose |
|---|---|---|
| `a_poser` | question non abordée | défaut |
| `evoquee` | sujet effleuré, réponse incomplète → relance proposée | moteur (confiance moyenne) |
| `repondue` | réponse obtenue | moteur (confiance haute) **ou** tap du collaborateur |
| `sans_objet` | question non applicable au dossier | tap du collaborateur |

Chaque passage d'état enregistre : `horodatage`, `segment_id`, `citation`, `confiance`, `origine` (`auto`, `valide`, `manuel`).

### Séance

`client`, `questionnaire`, `début`, `fin`, `durée`, `participants`, `audio_sha256`, `segments[]`, `etats[]`, `rapport`.

## 4. Moteur d'appariement (le cœur)

Trois niveaux, du moins coûteux au plus fin, appliqués au fil de l'eau sur les segments qui arrivent :

1. **Mots-clés et lexique** (instantané, hors ligne) : si le segment contient les mots-clés de la question ou son vocabulaire propre, la question passe en `evoquee`. Filtre les faux positifs en exigeant deux indices.
2. **Embeddings locaux** : similarité question ↔ segment (et ↔ fenêtre des 3 derniers segments, car une réponse suit souvent la question). Seuil haut → `repondue`, seuil bas → `evoquee`.
3. **LLM local** (arbitre, seulement à la demande ou en fin de séance) : on lui présente la question, le segment, son contexte, et on lui demande : *réponse obtenue ? complète ? sinon, quelle relance ?* Il fournit la réponse reformulée et la citation.

Règles de sûreté :

- jamais de coche `repondue` automatique au-delà d'un seuil de confiance sans validation quand le montant ou l'engagement est en jeu ;
- toute coche automatique est signalée visuellement et réversible d'un tap ;
- pas de double affectation : un segment peut justifier deux questions (on garde les deux), mais une question ne peut être cochée que par une citation ;
- si le LLM n'est pas disponible, l'app fonctionne quand même (niveaux 1 et 2 + tap manuel).

## 5. Écrans

1. **Préparation** : choix du client, du questionnaire (ou import), des participants attendus, contrôle micro/disque/batterie, rappel du consentement à l'enregistrement.
2. **Séance** (écran principal, deux volets) :
   - volet question : progression, sections dépliables, état de chaque question, « reste à poser », bouton « question suggérée » ;
   - volet transcription : segments horodatés, surlignage du segment qui a déclenché une coche, marqueur ⚑, bouton pause ;
   - barre d'actions : *Marquer répondue*, *Sans objet*, *Relancer*, *Noter une action*.
3. **Revue** (fin de séance) : questions non traitées, réponses à confirmer, actions extraites, écoute d'un passage en un tap.
4. **Rapport** : génération Word/PDF, partage, archivage dans le dossier client.
5. **Réglages** : questionnaires, modèle STT, modèle LLM local, seuils de confiance, rétention, verrouillage.

Règle d'interface : **un junior ne doit jamais avoir à quitter son entretien des yeux plus de 2 secondes**. Grosse typographie, un tap maximum par action, aucune saisie clavier pendant la séance.

## 6. Compte rendu de séance

- en-tête : client, questionnaire + version, date, durée, participants, empreinte SHA-256 de l'audio ;
- tableau du questionnaire : question, état final, réponse extraite, citation horodatée ;
- **questions non traitées** (la liste qui sert au chef de mission) ;
- actions à suivre (qui, quoi, quand) ;
- annexe : transcription intégrale par intervenant.

## 7. Critères d'acceptation v1

1. Une séance de 45 minutes tient sans perte audio ni crash, reprise possible après interruption.
2. Sur un questionnaire de 30 questions, le moteur ne rate pas plus de 2 questions réellement traitées (rappel ≥ 93 %) et coche à tort moins de 3 questions (précision ≥ 90 %) — mesuré sur 3 séances réelles.
3. Chaque coche affiche sa citation, accessible par un tap.
4. « Reste à poser » est exact à tout instant et visible sans scroll.
5. Le rapport est produit en moins de 10 secondes après la fin de séance.
6. Fonctionnement complet en avion, sans réseau.
7. Aucune donnée ne quitte l'appareil (vérification par absence de permission Internet en mode local).

## 8. Livraison

Dépôt dédié, APK signé publié en GitHub Release, mise à jour automatique (voir `appupdater`), icône propre, numéro de version sur chaque livrable.

## 9. Questions ouvertes

1. Support : Android local (recommandé, réutilise `transcripto-stream`) ou web hébergé au cabinet ?
2. Questionnaire réel de référence : lequel, sous quel format (Word, Excel, papier) ?
3. Audio conservé ou seulement la transcription ?
4. Premier livrable : co-pilote de séance complet, ou d'abord checklist + transcription + rapport ?
