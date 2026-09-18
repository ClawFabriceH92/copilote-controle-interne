#!/usr/bin/env python3
"""Contrôle qualité des trames de questionnaire.

Vérifie, pour chaque fichier de questionnaires/ :
- YAML valide et clés obligatoires ;
- identifiants uniques, préfixe cohérent avec la section, numérotation continue ;
- nombre de questions, nombre de sections ;
- chaque question : question, attendu, relance, 4 à 8 mots-clés, pièces optionnelles ;
- absence de question dupliquée (comparaison normalisée) ;
- pas de tabulation dans le fichier.

Usage : python3 docs/verifier_trames.py [--min 45]
"""
from __future__ import annotations

import re
import sys
import unicodedata
from pathlib import Path

import yaml

RACINE = Path(__file__).resolve().parent.parent
DOSSIER = RACINE / "questionnaires"
PREFIXES = {
    "achats": ["ORG", "SEP", "FOU", "CMD", "REC", "FAC", "REG", "CLO", "LIT", "SYS"],
    "paie": ["ENT", "VAR", "BUL", "ABS", "DSN", "BQ", "CLO", "ACC", "LIT", "SYS"],
    "tresorerie": ["BAN", "SEP", "ENC", "RAP", "INS", "PRE", "FIN", "ATT", "FRA", "CLO"],
}


def normaliser(txt: str) -> str:
    txt = unicodedata.normalize("NFKD", txt or "").encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]", "", txt.lower())


def controler(chemin: Path, minimum: int) -> tuple[list[str], dict]:
    erreurs: list[str] = []
    brut = chemin.read_text(encoding="utf-8")
    if "\t" in brut:
        erreurs.append("contient une tabulation")
    try:
        doc = yaml.safe_load(brut)
    except Exception as e:  # noqa: BLE001
        return [f"YAML invalide : {e}"], {}

    for cle in ("nom", "version", "sections"):
        if cle not in doc:
            erreurs.append(f"clé manquante : {cle}")
    sections = doc.get("sections") or []
    attendu_prefixes = PREFIXES.get(chemin.stem.replace("controle-interne-", ""), None)

    ids, questions, total = [], [], 0
    detail = []
    for i, section in enumerate(sections, 1):
        qs = section.get("questions") or []
        detail.append((section.get("titre", "?"), len(qs)))
        if not qs:
            erreurs.append(f"section {i} sans question")
        for j, q in enumerate(qs, 1):
            total += 1
            manquants = [c for c in ("id", "question", "attendu", "relance", "mots_cles") if not q.get(c)]
            if manquants:
                erreurs.append(f"{q.get('id', '?')} : champs manquants {manquants}")
            identifiant = str(q.get("id", ""))
            ids.append(identifiant)
            if identifiant in ids[:-1]:
                erreurs.append(f"identifiant dupliqué : {identifiant}")
            if attendu_prefixes and i <= len(attendu_prefixes):
                prefixe = identifiant.split("-")[0]
                if prefixe != attendu_prefixes[i - 1]:
                    erreurs.append(f"{identifiant} : préfixe inattendu en section {i} ({attendu_prefixes[i-1]} attendu)")
            if attendu_prefixes and i <= len(attendu_prefixes):
                numero = identifiant.split("-")[-1]
                if numero != f"{j:02d}":
                    erreurs.append(f"{identifiant} : numérotation non continue (attendu {j:02d})")
            mc = q.get("mots_cles") or []
            if not 4 <= len(mc) <= 8:
                erreurs.append(f"{identifiant} : {len(mc)} mots-clés (attendu 4 à 8)")
            if len((q.get("question") or "").split()) > 40:
                erreurs.append(f"{identifiant} : question trop longue")
            questions.append(normaliser(q.get("question")))

    doublons = {q for q in questions if questions.count(q) > 1}
    if doublons:
        erreurs.append(f"{len(doublons)} question(s) dupliquée(s)")
    if attendu_prefixes and len(sections) != len(attendu_prefixes):
        erreurs.append(f"{len(sections)} sections (attendu {len(attendu_prefixes)})")
    minimum_fichier = int(doc.get("min_questions") or minimum)
    if total < minimum_fichier:
        erreurs.append(f"{total} questions (minimum {minimum_fichier})")

    infos = {"nom": doc.get("nom"), "sections": detail, "total": total, "fichier": chemin.name}
    return erreurs, infos


def main() -> int:
    minimum = 45
    if "--min" in sys.argv:
        minimum = int(sys.argv[sys.argv.index("--min") + 1])
    total_general = 0
    echecs = 0
    for chemin in sorted(DOSSIER.glob("*.yaml")):
        erreurs, infos = controler(chemin, minimum)
        total_general += infos.get("total", 0)
        etat = "OK " if not erreurs else "KO "
        print(f"[{etat}] {infos.get('fichier', chemin.name)} — {infos.get('total', 0)} questions, {len(infos.get('sections', []))} sections")
        for titre, n in infos.get("sections", []):
            print(f"        {n:>3}  {titre}")
        for e in erreurs:
            echecs += 1
            print(f"        !! {e}")
    print(f"\nTotal : {total_general} questions sur {len(list(DOSSIER.glob('*.yaml')))} trame(s), {echecs} anomalie(s)")
    return 1 if echecs else 0


if __name__ == "__main__":
    sys.exit(main())
