# Peptidový deníček — kostra Android appky (Capacitor)

Tahle složka je připravený základ pro sestavení appky jako `.apk` v Android
Studiu. Appka uvnitř (`www/index.html`) je **funkčně stejná appka**, se kterou
pracujeme uvnitř Claude — jen ukládá data lokálně (localStorage), stejně jako
verze pro rodinu.

## Co budete potřebovat na počítači

1. **Node.js** (zdarma, nodejs.org) — stačí verze LTS
2. **Android Studio** (zdarma, developer.android.com/studio)

## Postup prvního sestavení

Otevřete terminál (příkazovou řádku) v téhle složce a postupně spusťte:

```bash
npm install
npx cap add android
npx cap sync android
npx cap open android
```

Poslední příkaz otevře projekt v Android Studiu. Tam už jen:

1. Počkejte, až Android Studio dokončí synchronizaci (Gradle sync)
2. Nahoře **Run ▶** — appka se nainstaluje a spustí na připojeném telefonu
   nebo v emulátoru

## Sestavení instalačního `.apk` souboru

V Android Studiu: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
Hotový soubor najdete v `android/app/build/outputs/apk/debug/app-debug.apk` —
ten už jde poslat/nainstalovat na jakýkoli telefon (je potřeba v Androidu
povolit „Instalace z neznámých zdrojů" pro appku, přes kterou soubor otevřete).

## Co appka umí už teď

Vše, co appka umí ve webové verzi — pera, dávky, kalendář, archiv, zálohy,
barvy, velikost textu atd. Beze změny.

## Co je potřeba doprogramovat pro plnou nativní appku

Tohle je jen **obal** appky, ne přepis. Věci, kvůli kterým dává nativní appka
smysl, je potřeba přidat jako doplňkový nativní kód (na to se vrátíme, až
základní sestavení bude fungovat):

- **Připomínky/notifikace** — přes `@capacitor/local-notifications` plugin
  (naplánování upozornění na čas dávky, funguje i při zavřené appce)
- **Výběr složky pro zálohy** — přes `@capacitor/filesystem` plugin
  (nativní dialog "kam uložit", trvalé oprávnění ke konkrétní složce)

Až budete mít základní appku sestavenou a vyzkoušenou, dejte vědět — doplníme
tyhle dvě věci jako další krok.
