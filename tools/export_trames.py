#!/usr/bin/env python3
"""Convertit les trames YAML du dépôt en JSON embarqué dans l'application.

Source : questionnaires/*.yaml  (source de vérité, éditable par le cabinet)
Cible  : app/src/main/assets/trames/*.json

Usage : python3 tools/export_trames.py
"""
from __future__ import annotations

import json
import re
import unicodedata
from pathlib import Path

import yaml

RACINE = Path(__file__).resolve().parent.parent
SOURCE = RACINE / "questionnaires"
CIBLE = RACINE / "app/src/main/assets/trames"
LEGAL = re.compile(r"\b(sas|sasu|sarl|eurl|sa|sci|scp|snc|selarl|bnc|gie)\b", re.I)


def norm(txt: str) -> str:
    txt = unicodedata.normalize("NFKD", txt or "").encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]", "", txt)


def identifiant(nom: str) -> str:
    return norm(nom)[:48]


def convertir(chemin: Path) -> dict:
    doc = yaml.safe_load(chemin.read_text(encoding="utf-8"))
    sections = []
    total = 0
    for section in doc.get("sections") or []:
        questions = []
        for q in section.get("questions") or []:
            total += 1
            questions.append(
                {
                    "id": q["id"],
                    "question": q["question"],
                    "attendu": q.get("attendu", ""),
                    "relance": q.get("relance", ""),
                    "motsCles": [norm(m) for m in (q.get("mots_cles") or []) if norm(m)],
                    "pieces": q.get("pieces") or [],
                }
            )
        sections.append({"titre": section.get("titre", ""), "questions": questions})
    return {
        "id": chemin.stem.replace("controle-interne-", ""),
        "nom": doc.get("nom", chemin.stem),
        "version": str(doc.get("version", "")),
        "source": doc.get("source", ""),
        "dureeCibleMin": doc.get("duree_cible_min") or 45,
        "participantsAttendus": doc.get("participants_attendus") or [],
        "nbQuestions": total,
        "sections": sections,
    }


def main() -> int:
    CIBLE.mkdir(parents=True, exist_ok=True)
    total = 0
    for chemin in sorted(SOURCE.glob("*.yaml")):
        trame = convertir(chemin)
        sortie = CIBLE / f"{trame['id']}.json"
        sortie.write_text(json.dumps(trame, ensure_ascii=False, indent=1), encoding="utf-8")
        total += trame["nbQuestions"]
        print(f"  {sortie.name:38s} {trame['nbQuestions']:3d} questions  ({len(trame['sections'])} sections)")
    print(f"total : {total} questions exportées vers {CIBLE.relative_to(RACINE)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
