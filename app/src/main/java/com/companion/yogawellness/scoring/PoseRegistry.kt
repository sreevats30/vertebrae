package com.companion.yogawellness.scoring

import com.companion.yogawellness.classifier.PostureFamily

/**
 * Global single source of truth for all live-scored and calibration asanas (§7.1).
 * Maps catalog IDs to typed Asana specs, families, names, and policies.
 */
object PoseRegistry {

    private val specsByAsana: Map<Asana, PoseSpec> = Asana.entries.associateWith { AsanaSpecFactory.createSpec(it) }
    private val specsByCatalogId: Map<String, PoseSpec> = specsByAsana.values.associateBy { it.catalogId }

    fun entryFor(asana: Asana): PoseSpec =
        specsByAsana[asana] ?: AsanaSpecFactory.createSpec(asana)

    fun entryForCatalogId(catalogId: String): PoseSpec? =
        specsByCatalogId[catalogId]

    fun catalogIdFor(asana: Asana): String =
        entryFor(asana).catalogId

    fun asanaFor(catalogId: String): Asana? =
        specsByCatalogId[catalogId]?.asana

    fun isLiveScored(catalogId: String): Boolean =
        specsByCatalogId[catalogId]?.asana?.isScored == true

    fun isLiveScored(asana: Asana): Boolean =
        asana.isScored

    fun familyFor(asana: Asana): PostureFamily =
        entryFor(asana).family

    val allSpecs: List<PoseSpec> get() = specsByAsana.values.toList()

    val allLiveAsanas: List<Asana> get() = AsanaSpecs.scoredAsanas
}
