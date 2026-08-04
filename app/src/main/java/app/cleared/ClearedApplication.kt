package app.cleared

import android.app.Application

/**
 * Application entry point.
 *
 * Nothing is wired here yet — the Room database and the WorkManager sync queue arrive with
 * steps 1 and 8 of the build order in CLAUDE.md.
 */
class ClearedApplication : Application()
