#!/usr/bin/env python3
"""Vérification de la maquette : la démo se joue, les questions se cochent, aucune erreur JS."""
import sys
from playwright.sync_api import sync_playwright

ERREURS = []

with sync_playwright() as p:
    nav = p.chromium.launch()
    page = nav.new_page(viewport={"width": 1280, "height": 900})
    page.on("console", lambda m: ERREURS.append(f"{m.type}: {m.text}") if m.type == "error" else None)
    page.on("pageerror", lambda e: ERREURS.append(f"pageerror: {e}"))
    page.goto("file:///root/projects/copilote-controle-interne/docs/maquette.html")
    page.wait_for_timeout(1000)
    print("titre      :", page.title())
    print("départ     :", page.inner_text("#prog-txt"), "|", page.inner_text("#prog-reste"))
    page.wait_for_timeout(25000)
    print("après 26 s :", page.inner_text("#prog-txt"), "|", page.inner_text("#prog-reste"))
    print("répondues  :", page.eval_on_selector_all(".q.repondue", "els => els.length"))
    print("évoquées   :", page.eval_on_selector_all(".q.evoquee", "els => els.length"))
    print("segments   :", page.eval_on_selector_all(".seg", "els => els.length"))
    print("citations  :", page.inner_text(".q.repondue .cit").replace("\n", " | ")[:110])
    print("suggestion :", page.inner_text("#sugg-id"), "->", page.inner_text("#sugg-txt")[:90])
    print("chrono     :", page.inner_text("#chrono"), "|", page.inner_text(".pilule.rec"))
    page.screenshot(path="/root/projects/copilote-controle-interne/docs/maquette-apercu.png", full_page=True)
    nav.close()

print("erreurs JS :", ERREURS or "aucune")
sys.exit(1 if ERREURS else 0)
