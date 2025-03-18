package uk.co.threebugs.darwinexclient.setup

import uk.co.threebugs.darwinexclient.modifier.Modifier


/**
 * Holds the parsed Setup plus its associated Modifier (if any).
 * The calling code can later persist Setup, retrieve its `id`, and
 * then build/save SetupModifier referencing both IDs.
 */
data class ParsedSetupWithModifier(
    val setup: Setup,
    val modifier: Modifier? = null
)
