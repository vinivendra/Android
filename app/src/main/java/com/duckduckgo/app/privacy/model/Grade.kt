package com.duckduckgo.app.privacy.model

import com.duckduckgo.app.privacy.model.Grade.Grading.*
import com.squareup.moshi.Json

class Grade {
	companion object {
		val unknownPrivacyScore: Int = 2
		val maxPrivacyScore: Int = 10
	}

	public enum class Grading {
		@Json(name = "A") A,
		@Json(name = "B+") BPlus,
		@Json(name = "B") B,
		@Json(name = "C+") CPlus,
		@Json(name = "C") C,
		@Json(name = "D") D,
		@Json(name = "D-") DMinus,
	}

	data class Score(
			val grade: Grading,
			val httpsScore: Int,
			val privacyScore: Int,
			val score: Int,
			val trackerScore: Int
	)

	data class Scores(
			val site: Score,
			val enhanced: Score
	)

	val scores: Scores
		get() {
			if (calculatedScores == null) {
				calculatedScores = calculate()
			}
			return calculatedScores!!
		}
	var https: Boolean = false
	var httpsAutoUpgrade: Boolean = false
	var parentEntity: String? = null
	var privacyScore: Int? = null
	var calculatedScores: Scores? = null
	var entitiesBlocked: MutableMap<String, Double?> = mutableMapOf()
	var entitiesNotBlocked: MutableMap<String, Double?> = mutableMapOf()

	internal fun setParentEntity(entity: String?, prevalence: Double?) {
		entity ?: return
		addEntityNotBlocked(entity = entity, prevalence = prevalence)
	}

	internal fun addEntityBlocked(entity: String, prevalence: Double?) {
		calculatedScores = null
		entitiesBlocked[entity] = prevalence
	}

	internal fun addEntityNotBlocked(entity: String, prevalence: Double?) {
		calculatedScores = null
		entitiesNotBlocked[entity] = prevalence
	}

	private fun calculate(): Grade.Scores {
		val siteHttpsScore: Int
		val enhancedHttpsScore: Int

		if (httpsAutoUpgrade) {
			siteHttpsScore = 0
			enhancedHttpsScore = 0
		}
		else if (https) {
			siteHttpsScore = 3
			enhancedHttpsScore = 0
		}
		else {
			siteHttpsScore = 10
			enhancedHttpsScore = 10
		}

		val privacyScore: Int = Math.min(this.privacyScore ?: Grade.unknownPrivacyScore, Grade.maxPrivacyScore)
		val enhancedTrackerScore: Int = trackerScore(entities = entitiesNotBlocked)
		val siteTrackerScore: Int = trackerScore(entities = entitiesBlocked) + enhancedTrackerScore
		val siteTotalScore: Int = siteHttpsScore + siteTrackerScore + privacyScore
		val enhancedTotalScore: Int = enhancedHttpsScore + enhancedTrackerScore + privacyScore
		val siteGrade: Grading = grade(score = siteTotalScore)
		val enhancedGrade: Grading = grade(score = enhancedTotalScore)
		val site: Score = Score(
				grade = siteGrade,
				httpsScore = siteHttpsScore,
				privacyScore = privacyScore,
				score = siteTotalScore,
				trackerScore = siteTrackerScore)
		val enhanced: Score = Score(
				grade = enhancedGrade,
				httpsScore = enhancedHttpsScore,
				privacyScore = privacyScore,
				score = enhancedTotalScore,
				trackerScore = enhancedTrackerScore)

		return Scores(site = site, enhanced = enhanced)
	}

	private fun trackerScore(entities: MutableMap<String, Double?>): Int {
		return entities.entries.fold(initial = 0, operation = { result, keyValue -> result + score(prevalence = keyValue.value) })
	}

	private fun score(prevalence: Double?): Int {
		if (!(prevalence != null && prevalence > 0.0)) {
			return 0
		}
		return when (prevalence) {
			in (0.0).rangeTo(0.1) -> 1
			in (0.1).rangeTo(1.0) -> 2
			in (1.0).rangeTo(5.0) -> 3
			in (5.0).rangeTo(10.0) -> 4
			in (10.0).rangeTo(15.0) -> 5
			in (15.0).rangeTo(20.0) -> 6
			in (20.0).rangeTo(30.0) -> 7
			in (30.0).rangeTo(45.0) -> 8
			in (45.0).rangeTo(66.0) -> 9
			else -> 10
		}
	}

	private fun grade(score: Int): Grade.Grading {
		return when (score) {
			in Int.MIN_VALUE until 2 -> A
			in 2 until 4 -> BPlus
			in 4 until 10 -> B
			in 10 until 14 -> CPlus
			in 14 until 20 -> C
			in 20 until 30 -> D
			else -> DMinus
		}
	}
}