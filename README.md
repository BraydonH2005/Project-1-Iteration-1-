Overview

I built a small Java terminal app that looks up a Wikipedia article and prints a short intro plus the 15 most recent edits.
You can run it live (calls the Wikipedia API) or offline (uses sample JSON bundled in the repo).
I kept the code simple, the output easy to read, and the tests independent from the network.

Tools / Versions

Java JDK 23

Gradle wrapper (gradlew.bat)

JsonPath (for JSON parsing)

JUnit 5 (for tests)

How to Run (Windows / PowerShell)
.\gradlew.bat clean test

# Live, no typing (good when a prompt isn't attached)
.\gradlew.bat run --args="--title='Frank Zappa'"

# Live, interactive prompt (use a real terminal)
.\gradlew.bat run

# Offline (uses the bundled sample JSON)
.\gradlew.bat run --args="--offline --file=src/test/resources/samplejson/redirect.json"


Notes from my runs:

PowerShell needs quotes for titles with spaces (example: --title='The Beatles').

If the prompt doesn’t accept input, I use --title= so it still runs.

What It Prints (Live)

A redirect notice if the title forwards to another page

The subject line and the first paragraph of the article

Edits (up to 15): followed by a numbered list with UTC ISO-8601 timestamps and usernames

Project Layout
src/main/java/edu/bsu/cs/revisionreporter/
  app/RevisionReporterMain.java        // CLI + printing
  wikipedia/WikipediaClient.java       // HTTP calls (User-Agent uses my BSU email)
  parse/RevisionParser.java            // JSON parsing
  parse/RevisionFormatter.java         // neat terminal formatting
  model/Revision.java                  // small data record

src/test/java/...                      // JUnit tests
src/test/resources/samplejson/...      // fixtures for offline runs

Design Choices

Kept parsing and formatting separate from the app layer so tests stay clean.

Limited the list to 15 edits, sorted newest first, with clear ISO-8601 UTC times.

Added friendly error messages (missing page, network problems, or no input).

Set the User-Agent to my Ball State email: braydon.hartwell@bsu.edu
.
