# Detection Coverage: 16 Live-Scored Poses

This document provides the complete audit of the 32 catalog poses in `com.companion.yogawellness.codex`, confirms the mapping of the 11 new target poses (expanding from 5 to 16 live poses), and documents all architectural reconciliations where the codebase and `ASANA_DETECTION_GUIDE.md` differ.

---

## 1. Complete Enumeration of All 32 Catalog Poses

Every pose in `AsanaCatalog32.kt` (aggregated from `CatalogDesk.kt`, `CatalogMorning.kt`, `CatalogStanding.kt`, `CatalogBalance.kt`, `CatalogHips.kt`, `CatalogEvening.kt`):

| # | Catalog ID | Sanskrit Name | English Display Name | Category | Bilateral | Initial Status | Target Status |
|---|------------|---------------|----------------------|----------|-----------|----------------|---------------|
| 1 | `cat_cow` | Marjaryasana-Bitilasana | Cat-Cow Flow | DESK | No | Follow-along | Follow-along |
| 2 | `seated_twist` | Bharadvajasana | Seated Spinal Twist | DESK | Yes | Follow-along | **Live-Scored** (Pose 11) |
| 3 | `cow_face_arms` | Gomukhasana Arms | Cow Face Shoulder Opener | DESK | Yes | Follow-along | Follow-along |
| 4 | `eagle_pose` | Garudasana | Eagle Pose | DESK | Yes | Follow-along | Follow-along |
| 5 | `puppy_pose` | Uttana Shishosana | Extended Puppy Pose | DESK | No | Follow-along | Follow-along |
| 6 | `gentle_cobra` | Bhujangasana | Gentle Cobra | DESK | No | Follow-along | Follow-along (Reserve) |
| 7 | `mountain` | Tadasana | Mountain Pose | MORNING | No | **Live (Calibration)** | **Live (Calibration)** |
| 8 | `upward_salute` | Urdhva Hastasana | Upward Salute | MORNING | No | Follow-along | Follow-along (Reserve) |
| 9 | `standing_forward_fold` | Uttanasana | Standing Forward Fold | MORNING | No | Follow-along | Follow-along (Reserve) |
| 10 | `halfway_lift` | Ardha Uttanasana | Halfway Lift | MORNING | No | Follow-along | Follow-along |
| 11 | `low_lunge` | Ashwa Sanchalanasana | Crescent Low Lunge | MORNING | Yes | Follow-along | Follow-along (Reserve) |
| 12 | `downward_dog` | Adho Mukha Svanasana | Downward-Facing Dog | MORNING | No | Follow-along | **Live-Scored** (Pose 13) |
| 13 | `warrior_1` | Virabhadrasana I | Warrior I | STANDING | Yes | Follow-along | **Live-Scored** (Pose 6) |
| 14 | `warrior_2` | Virabhadrasana II | Warrior II | STANDING | Yes | **Live-Scored** | **Live-Scored** (Pose 2) |
| 15 | `chair_pose` | Utkatasana | Chair Pose | STANDING | No | **Live-Scored** | **Live-Scored** (Pose 5) |
| 16 | `high_lunge` | Ashta Chandrasana | High Crescent Lunge | STANDING | Yes | Follow-along | Follow-along |
| 17 | `plank_pose` | Phalakasana | High Plank | STANDING | No | Follow-along | Follow-along (Reserve) |
| 18 | `boat_pose` | Navasana | Boat Pose | STANDING | No | Follow-along | Follow-along |
| 19 | `tree_pose` | Vrikshasana | Tree Pose | BALANCE | Yes | **Live-Scored** | **Live-Scored** (Pose 1) |
| 20 | `warrior_3` | Virabhadrasana III | Warrior III | BALANCE | Yes | Follow-along | Follow-along |
| 21 | `half_moon` | Ardha Chandrasana | Half Moon Pose | BALANCE | Yes | Follow-along | **Live-Scored** (Pose 8) |
| 22 | `dancer_pose` | Natarajasana | Lord of the Dance | BALANCE | Yes | Follow-along | Follow-along (Reserve) |
| 23 | `side_plank` | Vasisthasana | Side Plank | BALANCE | Yes | Follow-along | Follow-along |
| 24 | `triangle_pose` | Trikonasana | Triangle Pose | HIPS | Yes | **Live-Scored** | **Live-Scored** (Pose 3) |
| 25 | `extended_side_angle` | Utthita Parsvakonasana | Extended Side Angle | HIPS | Yes | Follow-along | **Live-Scored** (Pose 7) |
| 26 | `yogic_squat` | Malasana | Garland Pose (Deep Squat) | HIPS | No | Follow-along | **Live-Scored** (Pose 9) |
| 27 | `bound_angle` | Baddha Konasana | Butterfly Pose | HIPS | No | Follow-along | **Live-Scored** (Pose 10) |
| 28 | `wide_forward_fold` | Prasarita Padottanasana | Wide-Legged Forward Fold | HIPS | No | Follow-along | Follow-along |
| 29 | `childs_pose` | Balasana | Child's Pose | EVENING | No | Follow-along | **Live-Scored** (Pose 14) |
| 30 | `seated_forward_bend` | Paschimottanasana | Seated Forward Bend | EVENING | No | Follow-along | Follow-along |
| 31 | `bridge_pose` | Setu Bandha Sarvangasana | Bridge Pose | EVENING | No | Follow-along | Follow-along (Reserve) |
| 32 | `corpse_relaxation` | Savasana | Mindful Corpse Relaxation | EVENING | No | Follow-along | **Live-Scored** (Pose 16) |

---

## 2. Target Pose Reconciliation

The 16 live poses are divided into 3 posture families:

### Existing Core Poses (5)
1. **Tadasana** (`mountain`): Neutral calibration pose (unscored).
2. **Vrikshasana** (`tree_pose`): Tree Pose (bilateral, front view).
3. **Virabhadrasana II** (`warrior_2`): Warrior II (bilateral, front view).
4. **Trikonasana** (`triangle_pose`): Triangle Pose (bilateral, front view).
5. **Utkatasana** (`chair_pose`): Chair Pose (symmetric, 45°/side view).

### New Target Poses (11)
6. **Virabhadrasana I** (`warrior_1`): Standing family, 45° view, bilateral.
7. **Utthita Parsvakonasana** (`extended_side_angle`): Standing family, front view, bilateral.
8. **Ardha Chandrasana** (`half_moon`): Standing family, front view, bilateral.
9. **Malasana** (`yogic_squat`): Standing (squat) family, front view, symmetric.
10. **Baddha Konasana** (`bound_angle`): Seated family, front view, symmetric.
11. **Seated Twist** (`seated_twist`): Seated family, front view, bilateral.
12. **Neck Releases** (`neck_releases`): Seated family (desk profile), front view. Registered directly in `PoseRegistry` with active movement classification (lateral tilt, rotation, flexion).
13. **Adho Mukha Svanasana** (`downward_dog`): Floor family, side view, near-side scoring.
14. **Balasana** (`childs_pose`): Floor family, side view, near-side scoring.
15. **Viparita Karani** (`legs_up_the_wall`): Floor family, side view, near-side scoring.
16. **Savasana** (`corpse_relaxation`): Floor family, stillness hold (`STILLNESS_HOLD` using `SwayStabilityTracker`).

---

## 3. Discrepancies Between Codebase and Guide

Where the code and guide disagree, **the code wins**:

1. **Catalog IDs**:
   - Malasana: Code uses `yogic_squat` (guide assumed `malasana`).
   - Butterfly: Code uses `bound_angle` (guide assumed `baddha_konasana`).
   - Savasana: Code uses `corpse_relaxation` (guide assumed `savasana`).
   - Mountain: Code uses `mountain` (guide assumed `tadasana`).
   - Tree: Code uses `tree_pose` (guide assumed `vrikshasana`).
   - Warrior II: Code uses `warrior_2` (guide assumed `virabhadrasana_2`).
   - Chair: Code uses `chair_pose` (guide assumed `utkatasana`).
   - Triangle: Code uses `triangle_pose` (guide assumed `trikonasana`).
2. **Catalog Count Invariance**:
   - `AsanaCatalogTest.exactly32ItemsWithTheSpecIdsAndNumbers` verifies that `AsanaCatalog32.items` has exactly 32 items with numbers 1..32.
   - Poses present in the catalog map their `liveAsana` property to the corresponding `Asana` enum.
3. **Database Identity**:
   - `PoseRecordEntity.asana` is a `TEXT` column storing the enum name (`String`).
   - `Asana.entries` provides typed enum mapping for all 16 live poses, maintaining backwards compatibility with stored history rows.
4. **Scoring Formula**:
   - `AlignmentScorer.kt` implements $\Delta_i = \max(0, |\theta_i - \mu_i| - m_\tau \tau_i)$, $s_i = 100 \max(0, 1 - (\Delta_i / d_{\max,i})^{1.2})$, and critical-joint cap $S_{\text{align}} = \min(S_{\text{weighted}}, 15 + 0.85 \min s_{\text{crit}})$. This formula is preserved unmodified.

---

## 4. Exact `PoseSpec` Schema

```kotlin
data class PoseSpec(
    val asana: Asana,
    val family: PostureFamily,
    val defaultView: CameraAngle,
    val hasSides: Boolean,
    val joints: List<JointSpec>,
    val holdPolicy: HoldPolicy = HoldPolicy.ALIGNMENT_HOLD,
    val safetyGuards: List<GuardId> = emptyList(),
    val setupTip: String,
    val setupDiagramKey: String,
    val zWeight: Float = 0.25f,
    val maxHoldSec: Int? = null
)
```

---

## 5. Room Migration Plan

- **Current DB Version**: 2 (`WellnessDatabase.kt`).
- **Entity Schema**: `PoseRecordEntity` stores `asana` as `TEXT` and `side` as `TEXT`.
- **Compatibility**: Since all 16 poses are represented in the `Asana` enum and stored as uppercase string names (e.g. `"VIRABHADRASANA_I"`, `"ADHO_MUKHA_SVANASANA"`), no table schema alteration is required. Existing database rows with `"VRIKSHASANA"`, `"VIRABHADRASANA_II"`, etc., continue to deserialize seamlessly.
- **Migration**: If new nullable columns (such as `viewUsed: String?`) are added in future versions, `MIGRATION_2_3` will be applied with `ALTER TABLE pose_records ADD COLUMN ...` following the additive, non-destructive principle.
