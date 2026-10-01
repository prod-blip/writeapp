package com.atulpandey.clearwrite.analysis

import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingAnalyzerTest {
  @Test
  fun disabledChecks_areExcludedFromAnalysis() {
    val text = (1..30).joinToString(" ") { "word$it" } + "."

    val result = analyzer.analyze(text, enabledCategories = emptySet())

    assertTrue(result.issues.isEmpty())
    assertEquals(100, result.score)
  }

  private val analyzer = WritingAnalyzer()

  @Test
  fun clearShortText_hasNoIssues() {
    val result = analyzer.analyze("Clear writing respects the reader's time.")

    assertEquals(100, result.score)
    assertTrue(result.issues.isEmpty())
  }

  @Test
  fun longSentence_isDetected() {
    val sentence = (1..26).joinToString(" ") { "word$it" } + "."

    val result = analyzer.analyze(sentence)

    assertEquals(1, result.count(AnalysisCategory.LONG_SENTENCE))
  }

  @Test
  fun repeatedWord_isDetectedCaseInsensitively() {
    val result = analyzer.analyze("This is is repeated.")

    assertEquals(1, result.count(AnalysisCategory.REPEATED_WORD))
  }

  @Test
  fun longSentence_offsetsSelectTheOriginalSentence() {
    val longSentence = (1..26).joinToString(" ") { "word$it" } + "."
    val text = "A short opening. $longSentence A short ending."

    val issue = analyzer.analyze(text).issues.single { it.category == AnalysisCategory.LONG_SENTENCE }

    assertEquals(longSentence, text.substring(issue.startOffset, issue.endOffset).trim())
  }

  @Test
  fun documentStats_includeReadabilityAndStructure() {
    val result = analyzer.analyze("Clear writing helps readers. Short sentences improve flow.\n\nEach idea gets space.")

    assertEquals(12, result.wordCount)
    assertEquals(3, result.sentenceCount)
    assertEquals(2, result.paragraphCount)
    assertEquals(1, result.readingTimeMinutes)
    assertTrue(result.readabilityGrade >= 1)
  }

  @Test
  fun possiblePassiveVoice_isDetected() {
    val result = analyzer.analyze("The proposal was carefully reviewed by the team.")

    assertEquals(1, result.count(AnalysisCategory.PASSIVE_VOICE))
  }

  @Test
  fun weakenersAndWordyPhrases_areDetectedLocally() {
    val result = analyzer.analyze("I think we should utilize this tool in order to move very quickly.")

    assertEquals(2, result.count(AnalysisCategory.WEAKENER))
    assertEquals(2, result.count(AnalysisCategory.WORDY_PHRASE))
  }

  @Test
  fun consecutiveRepeatedSentenceStarts_areDetected() {
    val result = analyzer.analyze("This plan is clear. This plan is practical.")

    assertEquals(1, result.count(AnalysisCategory.REPEATED_START))
  }

  @Test
  fun shortDraft_hasProvisionalScore() {
    val result = analyzer.analyze("Clear writing helps readers.")

    assertEquals(ScoreConfidence.PROVISIONAL, result.scoreConfidence)
  }

  @Test
  fun longerDraft_hasStableScore() {
    val text = List(25) { "Clear writing helps readers understand the message." }.joinToString(" ")

    val result = analyzer.analyze(text)

    assertEquals(ScoreConfidence.STABLE, result.scoreConfidence)
  }

  @Test
  fun technicalGoal_allowsHigherReadingLevelThanAccessibleGoal() {
    val text =
      "Institutional interoperability necessitates multidisciplinary conceptualization and " +
        "methodological standardization across heterogeneous organizations."

    val accessible = analyzer.analyze(text, writingGoal = WritingGoal.ACCESSIBLE)
    val technical = analyzer.analyze(text, writingGoal = WritingGoal.ACADEMIC_TECHNICAL)

    assertTrue(accessible.breakdown.audienceReadability < technical.breakdown.audienceReadability)
    assertTrue(accessible.score < technical.score)
  }

  @Test
  fun overlappingLongAndVeryHardSentence_isNotDoubleCounted() {
    val text = List(26) { "institutionalization" }.joinToString(" ") + "."
    val veryHardOnly =
      analyzer.analyze(
        text,
        enabledCategories = setOf(AnalysisCategory.VERY_HARD_SENTENCE),
      )
    val longAndVeryHard =
      analyzer.analyze(
        text,
        enabledCategories =
          setOf(AnalysisCategory.LONG_SENTENCE, AnalysisCategory.VERY_HARD_SENTENCE),
      )

    assertEquals(
      veryHardOnly.breakdown.sentenceClarity,
      longAndVeryHard.breakdown.sentenceClarity,
    )
  }

  @Test
  fun clarityScore_usesPublishedSubscoreWeights() {
    val result = analyzer.analyze("This plan is is actually very difficult to utilize in order to proceed.")
    val breakdown = result.breakdown
    val expected =
      (
        breakdown.sentenceClarity * 0.30 +
          breakdown.concisionAndPrecision * 0.25 +
          breakdown.audienceReadability * 0.25 +
          breakdown.flowAndStructure * 0.20
      ).roundToInt()

    assertEquals(CLARITY_SCORE_VERSION, result.scoreVersion)
    assertEquals(expected, result.score)
  }
}
