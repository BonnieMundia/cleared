# Start here — building Cleared with Claude Code

You are on `github.com/BonnieMundia`. This walks from a set-up machine to an APK on your phone and a
repo you can hand back to me for review.

**Do `BUILD_SETUP.md` first** if Android Studio, Claude Code, git and `adb` aren't already working on
your computer. It covers the install, the PATH variables, and getting the phone into debugging mode.
This file assumes all of that is done.

---

## 1. Create the repo and lay out the files

```bash
mkdir cleared && cd cleared
git init && git branch -M main
mkdir design
```

Now copy the handoff files in:

- Everything from the handoff zip → `design/`
- Then move `design/CLAUDE.md` up to the project root: `mv design/CLAUDE.md .`
- Move `design/START_HERE.md` out too, or delete it — it has done its job.

Layout should be:

```
cleared/
├── CLAUDE.md              ← Claude Code reads this on every prompt
└── design/
    ├── README.md
    ├── BUILD_SETUP.md
    ├── DESIGN_TOKENS.md
    ├── DATA_MODEL.md
    ├── SCREENS.md
    ├── sample_data.json
    ├── Cleared.dc.html
    └── support.js
```

Create the repo on GitHub (`cleared`, public or private, no README), then:

```bash
git add .
git commit -m "Design reference and handoff package"
git remote add origin https://github.com/BonnieMundia/cleared.git
git push -u origin main
```

---

## 2. Open the design before you start

Open `design/Cleared.dc.html` in a browser. Scroll around it. Frames are labelled `1a` through `3b`
and those ids are used everywhere in the docs — you and Claude will both refer to them.

Tap a row on the Pipeline frame to see the stage model move. That behaviour is the app.

---

## 3. First prompt

Run `claude` in the `cleared/` folder and paste this:

> Read CLAUDE.md and everything in design/. Then set up an empty Jetpack Compose project for an app
> called Cleared — Kotlin, Material 3, min SDK 26, single Activity, Compose Navigation, Room,
> WorkManager. Add IBM Plex Sans and IBM Plex Mono as bundled font resources.
>
> Do not build any screens yet. Stop after the project compiles and `./gradlew assembleDebug`
> succeeds, and show me the file tree.

Check it builds. Then go step by step down the build order in `CLAUDE.md`. **One step per prompt** —
resist asking for the whole app at once; the results get much worse.

Step 2 is the one to be picky about:

> Now do step 1 of the build order: the Room schema, the append-only StageEvent log, and every
> derived query in design/DATA_MODEL.md. Write unit tests asserting against the figures in
> design/sample_data.json — owedKes 247119, work/money split 188183/58936, week subtotals 64393 /
> 151730 / 30996, Vector Annotate effective rate 402. No UI yet.

Then step 2 (theme), then step 3 (the record row with previews for all seven stages in both themes).
Once the record row looks right in a preview, the rest goes quickly.

---

## 4. Get it on your phone

**Over USB.** Enable Developer Options on the phone (Settings → About → tap Build number seven
times), turn on USB debugging, plug in, then:

```bash
./gradlew installDebug
```

**Or build an APK and sideload it:**

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

**Or let GitHub build it for you** — ask Claude Code:

> Add a GitHub Actions workflow that runs `./gradlew assembleDebug` on every push to main and
> uploads the APK as a build artifact.

Then every push produces a downloadable APK on the Actions tab — you can grab it on the phone
directly, no cable.

---

## 5. Loop back to me

Once anything is pushed, I can read the repo. Useful things to ask for:

- "Review `github.com/BonnieMundia/cleared` against the design and tell me where it drifted."
- "The Platforms screen doesn't feel right in the build — here's a screenshot."
- "Design the Discovery sources" — the one gap still open in `CLAUDE.md`.

I stay on the design side and read your code; Claude Code writes it on your machine. Push before you
ask me to look.

---

## Two things that will bite you

**Fonts.** Bundle IBM Plex Sans and IBM Plex Mono as `res/font` resources, not downloadable fonts.
The app has to render correctly with no network, and every figure in it is monospaced.

**Dark mode.** Build it alongside light from the very first composable. Retrofitting dark mode into
a screen already built in light always costs more than doing both at once, and half this design's
meaning is carried by colour.
