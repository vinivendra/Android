/*
 * Copyright (c) 2017 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.privacy.model

import com.duckduckgo.app.privacy.model.PrivacyPractices.Summary.*

data class TermsOfService(
	val name: String? = null,
	val classification: Classification? = null,
	val score: Int = 0,
	val goodReasons: MutableList<String> = mutableListOf(),
	val badReasons: MutableList<String> = mutableListOf()
) {
	val hasGoodReasons: Boolean
		get() {
			return !goodReasons.isEmpty()
		}
	val hasBadReasons: Boolean
		get() {
			return !badReasons.isEmpty()
		}
	val summary: PrivacyPractices.Summary
		get() {
			if (classification != null) {
				when (classification) {
					Classification.A -> return GOOD
					Classification.B -> return MIXED
					Classification.C -> return POOR
					Classification.D -> return POOR
				}
			}

			if (hasGoodReasons && hasBadReasons) {
				return MIXED
			}

			if (score < 0) {
				return GOOD
			}
			else if (score == 0 && (hasGoodReasons || hasBadReasons)) {
				return MIXED
			}
			else if (score > 0) {
				return POOR
			}

			return UNKNOWN
		}
	val derivedScore: Int
		get() {
			var derived: Int = 5
			if (classification == Classification.A) {
				derived = 0
			}
			else if (classification == Classification.B) {
				derived = 1
			}
			else if (classification == Classification.D || score > 150) {
				derived = 10
			}
			else if (classification == Classification.C || score > 100) {
				derived = 7
			}
			return derived
		}

	public enum class Classification {
		A,
		B,
		C,
		D,
		E;

		companion object {
			operator fun invoke(string: String?): TermsOfService.Classification? {
				string ?: return null
				return when (string) {
					"A" -> A
					"B" -> B
					"C" -> C
					"D" -> D
					"E" -> E
					else -> null
				}
			}
		}
	}
}