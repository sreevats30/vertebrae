package com.companion.yogawellness.codex

import com.companion.yogawellness.personalisation.AgeBand
import com.companion.yogawellness.personalisation.AgeProfile
import com.companion.yogawellness.scoring.Asana

/** How well a pose suits an age band. Nothing here ever blocks a pose: it only changes what is suggested. */
enum class Suitability(
    /** Short tag for a Learn row; null = no tag. */
    val tag: String?
) {
    RECOMMENDED("Good fit"),
    OK(null),
    CAUTION("Go gently"),
    USE_EASIER_VARIANT("Gentle option");

    /** Poses the app is happy to suggest without a note of care. */
    val isSuggested: Boolean get() = this == RECOMMENDED || this == OK
}

data class PoseAdvice(
    val suitability: Suitability,
    /** One short, plain sentence shown to the user — a suggestion, never an instruction or a medical claim. */
    val note: String? = null
)

/**
 * Age-band suitability for every Learn pose: a default per category, plus overrides for the poses
 * where the category default does not fit. A pose added to the catalog later gets its category's
 * default with no code change here.
 *
 * Conservative starting values, not clinical rules — to be reviewed by a qualified yoga teacher or
 * physician before release. Under 18, 18–30 and 31–50 share one column, as in the source table.
 */
object PoseSuitability {

    /** The three columns the rules are written in. */
    private enum class Tier { YOUNGER, FROM_51, FROM_65 }

    private fun tierOf(band: AgeBand): Tier = when (band) {
        AgeBand.UNDER_18, AgeBand.AGE_18_30, AgeBand.AGE_31_50 -> Tier.YOUNGER
        AgeBand.AGE_51_64 -> Tier.FROM_51
        AgeBand.AGE_65_PLUS -> Tier.FROM_65
    }

    private class Rule(val younger: PoseAdvice, val from51: PoseAdvice, val from65: PoseAdvice) {
        fun forTier(t: Tier) = when (t) { Tier.YOUNGER -> younger; Tier.FROM_51 -> from51; Tier.FROM_65 -> from65 }
    }

    private val recommended = PoseAdvice(Suitability.RECOMMENDED)
    private val ok = PoseAdvice(Suitability.OK)
    private fun ok(note: String) = PoseAdvice(Suitability.OK, note)
    private fun caution(note: String) = PoseAdvice(Suitability.CAUTION, note)
    private fun easier(note: String) = PoseAdvice(Suitability.USE_EASIER_VARIANT, note)

    private const val SUPPORT_NEARBY = "Consider keeping a wall or chair within reach."
    private const val AT_A_WALL = "Consider practising next to a wall or chair."
    private const val GENTLE_KNEES = "Go gently on the knees, and consider the easier option."
    private const val EASIER_AND_SLOW = "Consider the easier option, and take your time moving in and out."

    private val categoryDefaults: Map<AsanaCategory, Rule> = mapOf(
        AsanaCategory.DESK to Rule(recommended, recommended, ok),
        AsanaCategory.MORNING to Rule(
            recommended,
            ok("Consider a longer warm-up, and ease the load on your wrists in hands-on-the-floor poses."),
            easier(EASIER_AND_SLOW)
        ),
        AsanaCategory.STANDING to Rule(ok, ok, easier(EASIER_AND_SLOW)),
        AsanaCategory.BALANCE to Rule(ok, caution(SUPPORT_NEARBY), caution(AT_A_WALL)),
        AsanaCategory.HIPS to Rule(ok, caution(GENTLE_KNEES), caution(GENTLE_KNEES)),
        AsanaCategory.EVENING to Rule(recommended, recommended, recommended)
    )

    /** Per-pose overrides, by catalog id. */
    private val overrides: Map<String, Rule> = mapOf(
        // ---- The four live-scored poses ----
        "tree_pose" to Rule(ok, ok(SUPPORT_NEARBY), caution(AT_A_WALL)),
        "warrior_2" to Rule(ok, ok, easier("Consider a shallower bend in the front knee.")),
        "triangle_pose" to Rule(
            ok,
            caution("Consider resting your hand on your shin or a block, and keep the front knee soft."),
            caution("Consider resting your hand on your shin or a block, and keep the front knee soft.")
        ),
        // Chair Pose's note carries the band's hold cap; see [adviceFor].
        "chair_pose" to Rule(ok, caution(CHAIR_NOTE), caution(CHAIR_NOTE)),
        // ---- Poses whose category default does not fit ----
        // Standing still, and the calibration pose every session starts from.
        "mountain" to Rule(recommended, ok, ok),
        // Filed under Desk, but it is a single-leg balance.
        "eagle_pose" to Rule(ok, caution(SUPPORT_NEARBY), caution(AT_A_WALL)),
        // Filed under Balance, but the load is on the wrist and shoulder, not on staying upright.
        "side_plank" to Rule(
            ok,
            caution("Consider the bottom-knee-down or forearm version."),
            caution("Consider the bottom-knee-down or forearm version.")
        )
    )

    private const val CHAIR_NOTE = "Consider shorter holds, and stop if your knees or lower back complain."

    fun adviceFor(item: AsanaCodexItem, band: AgeBand): PoseAdvice {
        val advice = (overrides[item.id] ?: categoryDefaults.getValue(item.category)).forTier(tierOf(band))
        if (item.id == "chair_pose" && advice.suitability == Suitability.CAUTION) {
            val cap = AgeProfile.utkatasanaCapFor(band)
            return advice.copy(note = "Consider shorter holds (the live timer stops at $cap s), and stop if your knees or lower back complain.")
        }
        return advice
    }

    /** Advice for a live-scored pose, or null for one with no Learn page (there is none today). */
    fun adviceFor(asana: Asana, band: AgeBand): PoseAdvice? =
        AsanaCatalog32.items.firstOrNull { it.liveAsana == asana }?.let { adviceFor(it, band) }
}

/** One titled group of poses on the Learn tab. */
data class CodexSection(val key: String, val title: String, val subtitle: String, val items: List<AsanaCodexItem>)

/**
 * The "For you" view: every pose is still listed (nothing is hidden), but the ones suggested for
 * [band] come first — recommended, then also suitable, then the ones that deserve a little care.
 * Within a group, the band's preferred categories lead, then catalog order.
 */
fun forYouSections(items: List<AsanaCodexItem>, band: AgeBand): List<CodexSection> {
    val preferred = AgeProfile.forBand(band).preferredCategories
    fun rank(i: AsanaCodexItem) = preferred.indexOf(i.category).let { if (it < 0) preferred.size else it }
    val bySuitability = items.groupBy { PoseSuitability.adviceFor(it, band).suitability }
    fun group(vararg s: Suitability) = s.flatMap { bySuitability[it].orEmpty() }.sortedWith(compareBy({ rank(it) }, { it.number }))
    return listOf(
        CodexSection("recommended", "Recommended for you", "A good fit for your age range", group(Suitability.RECOMMENDED)),
        CodexSection("suitable", "Also suitable", "No special notes for your age range", group(Suitability.OK)),
        CodexSection(
            "care", "With a little extra care", "Still open to you — each has a gentler option",
            group(Suitability.CAUTION, Suitability.USE_EASIER_VARIANT)
        )
    ).filter { it.items.isNotEmpty() }
}
