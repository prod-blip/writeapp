package com.atulpandey.clearwrite.analysis

import kotlin.math.ceil
import kotlin.math.roundToInt

const val CLARITY_SCORE_VERSION = 2

enum class AnalysisCategory(
  val title: String,
  val issueLabel: String,
  val description: String,
) {
  LONG_SENTENCE("Long sentences", "Long sentence", "More than 25 words"),
  HARD_SENTENCE("Hard-to-read sentences", "Hard-to-read sentence", "Estimated grade 12–14"),
  VERY_HARD_SENTENCE("Very hard sentences", "Very hard sentence", "Estimated grade 15 or higher"),
  LONG_PARAGRAPH("Long paragraphs", "Long paragraph", "More than 80 words"),
  PASSIVE_VOICE("Possible passive voice", "Possible passive voice", "The actor may be unclear"),
  WEAKENER("Weakening words", "Weakening word", "Hedges and vague intensifiers"),
  WORDY_PHRASE("Wordy phrases", "Wordy phrase", "A shorter phrase may say the same thing"),
  FILLER_HEAVY_SENTENCE("Filler-heavy sentences", "Filler-heavy sentence", "Many words carry little meaning"),
  REPEATED_START("Repeated sentence starts", "Repeated sentence start", "Consecutive sentences begin alike"),
  REPEATED_WORD("Repeated words", "Repeated word", "The same word appears twice in a row"),
}

enum class WritingGoal(
  val title: String,
  val description: String,
  val targetGrade: Int,
) {
  ACCESSIBLE("Accessible", "Broad audiences and easy scanning", 7),
  GENERAL("General", "Everyday writing for most adults", 9),
  PROFESSIONAL("Professional", "Workplace and specialist communication", 11),
  ACADEMIC_TECHNICAL("Academic / technical", "Expert readers and necessary terminology", 14),
}

enum class ScoreConfidence(val label: String) {
  PROVISIONAL("Provisional score"),
  STABLE("Stable score"),
}

data class ClarityBreakdown(
  val sentenceClarity: Int,
  val concisionAndPrecision: Int,
  val audienceReadability: Int,
  val flowAndStructure: Int,
)

data class WritingIssue(
  val category: AnalysisCategory,
  val startOffset: Int,
  val endOffset: Int,
  val explanation: String,
)

data class AnalysisResult(
  val scoreVersion: Int,
  val score: Int,
  val breakdown: ClarityBreakdown,
  val scoreConfidence: ScoreConfidence,
  val writingGoal: WritingGoal,
  val issues: List<WritingIssue>,
  val readabilityGrade: Int,
  val wordCount: Int,
  val sentenceCount: Int,
  val paragraphCount: Int,
  val readingTimeMinutes: Int,
  val checksRun: Int,
) {
  val totalIssues: Int = issues.size

  fun count(category: AnalysisCategory): Int = issues.count { it.category == category }
}

class WritingAnalyzer {
  fun analyze(
    text: String,
    enabledCategories: Set<AnalysisCategory> = AnalysisCategory.entries.toSet(),
    writingGoal: WritingGoal = WritingGoal.GENERAL,
  ): AnalysisResult {
    if (text.isBlank()) {
      return AnalysisResult(
        scoreVersion = CLARITY_SCORE_VERSION,
        score = 100,
        breakdown = perfectBreakdown,
        scoreConfidence = ScoreConfidence.PROVISIONAL,
        writingGoal = writingGoal,
        issues = emptyList(),
        readabilityGrade = 1,
        wordCount = 0,
        sentenceCount = 0,
        paragraphCount = 0,
        readingTimeMinutes = 0,
        checksRun = enabledCategories.size,
      )
    }

    val sentences = sentenceRegex.findAll(text).filter { wordCount(it.value) > 0 }.toList()
    val paragraphs = paragraphRegex.findAll(text).filter { wordCount(it.value) > 0 }.toList()
    val words = wordRegex.findAll(text).toList()

    val issues =
      buildList {
        addAll(findLongSentences(sentences))
        addAll(findHardSentences(sentences))
        addAll(findLongParagraphs(paragraphs))
        addAll(findPassiveVoice(text))
        addAll(findWeakeners(text))
        addAll(findWordyPhrases(text))
        addAll(findFillerHeavySentences(sentences))
        addAll(findRepeatedSentenceStarts(sentences))
        addAll(findRepeatedWords(text))
      }.filter { it.category in enabledCategories }
        .distinctBy { Triple(it.category, it.startOffset, it.endOffset) }
        .sortedWith(compareBy<WritingIssue> { it.startOffset }.thenBy { it.endOffset })

    val sentenceCount = sentences.size.coerceAtLeast(1)
    val totalWords = words.size
    val grade = readabilityGrade(text, totalWords, sentenceCount)
    val breakdown =
      clarityBreakdown(
        issues = issues,
        sentences = sentences,
        paragraphs = paragraphs,
        wordCount = totalWords,
        readabilityGrade = grade,
        enabledCategories = enabledCategories,
        writingGoal = writingGoal,
      )

    return AnalysisResult(
      scoreVersion = CLARITY_SCORE_VERSION,
      score = overallClarityScore(breakdown),
      breakdown = breakdown,
      scoreConfidence =
        if (totalWords < STABLE_SCORE_WORDS) ScoreConfidence.PROVISIONAL
        else ScoreConfidence.STABLE,
      writingGoal = writingGoal,
      issues = issues,
      readabilityGrade = grade,
      wordCount = totalWords,
      sentenceCount = sentences.size,
      paragraphCount = paragraphs.size,
      readingTimeMinutes = ceil(totalWords / AVERAGE_READING_SPEED.toDouble()).toInt().coerceAtLeast(1),
      checksRun = enabledCategories.size,
    )
  }

  private fun findLongSentences(sentences: List<MatchResult>): List<WritingIssue> =
    sentences.mapNotNull { match ->
      val count = wordCount(match.value)
      if (count <= LONG_SENTENCE_WORDS) return@mapNotNull null

      WritingIssue(
        category = AnalysisCategory.LONG_SENTENCE,
        startOffset = contentStart(match),
        endOffset = contentEnd(match),
        explanation = "This sentence has $count words. Consider splitting it.",
      )
    }

  private fun findHardSentences(sentences: List<MatchResult>): List<WritingIssue> =
    sentences.mapNotNull { match ->
      val count = wordCount(match.value)
      if (count < MIN_DIFFICULT_SENTENCE_WORDS) return@mapNotNull null

      val grade = readabilityGrade(match.value, count, sentenceCount = 1)
      val category =
        when {
          grade >= VERY_HARD_GRADE -> AnalysisCategory.VERY_HARD_SENTENCE
          grade >= HARD_GRADE -> AnalysisCategory.HARD_SENTENCE
          else -> return@mapNotNull null
        }
      val description =
        if (category == AnalysisCategory.VERY_HARD_SENTENCE) "very hard" else "hard"

      WritingIssue(
        category = category,
        startOffset = contentStart(match),
        endOffset = contentEnd(match),
        explanation =
          "This sentence is estimated at grade $grade and may be $description to read. " +
            "Shorter structure or simpler words may help.",
      )
    }

  private fun findLongParagraphs(paragraphs: List<MatchResult>): List<WritingIssue> =
    paragraphs.mapNotNull { match ->
      val count = wordCount(match.value)
      if (count <= LONG_PARAGRAPH_WORDS) return@mapNotNull null

      WritingIssue(
        category = AnalysisCategory.LONG_PARAGRAPH,
        startOffset = contentStart(match),
        endOffset = contentEnd(match),
        explanation = "This paragraph has $count words. A break may make it easier to scan.",
      )
    }

  private fun findPassiveVoice(text: String): List<WritingIssue> =
    passiveVoiceRegex.findAll(text).map { match ->
      WritingIssue(
        category = AnalysisCategory.PASSIVE_VOICE,
        startOffset = match.range.first,
        endOffset = match.range.last + 1,
        explanation =
          "“${match.value}” may use passive voice. Name who performs the action when that matters.",
      )
    }.toList()

  private fun findWeakeners(text: String): List<WritingIssue> =
    weakenerRegex.findAll(text).map { match ->
      val phrase = match.value
      val explanation =
        when (phrase.lowercase()) {
          "very", "really", "quite", "rather", "extremely" ->
            "“$phrase” is a broad intensifier. A more precise word may be stronger."
          "maybe", "perhaps", "probably", "possibly", "i think", "i believe", "in my opinion" ->
            "“$phrase” softens the claim. Keep it only when uncertainty is important."
          else -> "“$phrase” may be unnecessary. Check whether the sentence stays clear without it."
        }

      WritingIssue(
        category = AnalysisCategory.WEAKENER,
        startOffset = match.range.first,
        endOffset = match.range.last + 1,
        explanation = explanation,
      )
    }.toList()

  private fun findWordyPhrases(text: String): List<WritingIssue> =
    wordyPhrases.flatMap { (phrase, replacement) ->
      phraseRegex(phrase).findAll(text).map { match ->
        WritingIssue(
          category = AnalysisCategory.WORDY_PHRASE,
          startOffset = match.range.first,
          endOffset = match.range.last + 1,
          explanation = "Try “$replacement” instead of “${match.value}.”",
        )
      }.toList()
    }

  private fun findFillerHeavySentences(sentences: List<MatchResult>): List<WritingIssue> =
    sentences.mapNotNull { match ->
      val words = wordRegex.findAll(match.value).map { it.value.lowercase() }.toList()
      if (words.size < MIN_FILLER_SENTENCE_WORDS) return@mapNotNull null

      val fillerCount = words.count { it in fillerWords }
      val percentage = (fillerCount * 100.0 / words.size).roundToInt()
      if (percentage < FILLER_PERCENT_THRESHOLD) return@mapNotNull null

      WritingIssue(
        category = AnalysisCategory.FILLER_HEAVY_SENTENCE,
        startOffset = contentStart(match),
        endOffset = contentEnd(match),
        explanation =
          "$percentage% of this sentence is made of common connecting words. " +
            "Remove any that do not help the meaning.",
      )
    }

  private fun findRepeatedSentenceStarts(sentences: List<MatchResult>): List<WritingIssue> {
    val issues = mutableListOf<WritingIssue>()
    var previousStart: String? = null

    sentences.forEach { sentence ->
      val firstWord = wordRegex.find(sentence.value)
      val normalized = firstWord?.value?.lowercase()
      if (normalized != null && normalized.length > 2 && normalized == previousStart) {
        val start = sentence.range.first + firstWord.range.first
        issues +=
          WritingIssue(
            category = AnalysisCategory.REPEATED_START,
            startOffset = start,
            endOffset = start + firstWord.value.length,
            explanation =
              "This sentence starts with “${firstWord.value},” just like the previous sentence. " +
                "Varying the opening can improve rhythm.",
          )
      }
      previousStart = normalized
    }

    return issues
  }

  private fun findRepeatedWords(text: String): List<WritingIssue> =
    repeatedWordRegex.findAll(text).map { match ->
      WritingIssue(
        category = AnalysisCategory.REPEATED_WORD,
        startOffset = match.range.first,
        endOffset = match.range.last + 1,
        explanation = "“${match.groupValues[1]}” is repeated.",
      )
    }.toList()

  private fun clarityBreakdown(
    issues: List<WritingIssue>,
    sentences: List<MatchResult>,
    paragraphs: List<MatchResult>,
    wordCount: Int,
    readabilityGrade: Int,
    enabledCategories: Set<AnalysisCategory>,
    writingGoal: WritingGoal,
  ): ClarityBreakdown {
    if (enabledCategories.isEmpty()) return perfectBreakdown

    val sentenceClarity = sentenceClarityScore(issues, sentences)
    val concisionAndPrecision = concisionScore(issues, wordCount)
    val audienceReadability =
      if (
        AnalysisCategory.HARD_SENTENCE !in enabledCategories &&
          AnalysisCategory.VERY_HARD_SENTENCE !in enabledCategories
      ) {
        100
      } else {
        val gradesOverTarget = (readabilityGrade - writingGoal.targetGrade).coerceAtLeast(0)
        (100.0 - gradesOverTarget * AUDIENCE_PENALTY_PER_GRADE).roundToInt().coerceIn(0, 100)
      }
    val flowAndStructure = flowScore(issues, sentences.size, paragraphs)

    return ClarityBreakdown(
      sentenceClarity = sentenceClarity,
      concisionAndPrecision = concisionAndPrecision,
      audienceReadability = audienceReadability,
      flowAndStructure = flowAndStructure,
    )
  }

  private fun sentenceClarityScore(
    issues: List<WritingIssue>,
    sentences: List<MatchResult>,
  ): Int {
    if (sentences.isEmpty()) return 100
    val averageSeverity =
      sentences.map { sentence ->
        val start = contentStart(sentence)
        val end = contentEnd(sentence)
        val sentenceIssues = issues.filter { it.startOffset < end && it.endOffset > start }
        val primarySeverity =
          sentenceIssues.maxOfOrNull { issue ->
            when (issue.category) {
              AnalysisCategory.VERY_HARD_SENTENCE -> 0.90
              AnalysisCategory.HARD_SENTENCE -> 0.65
              AnalysisCategory.LONG_SENTENCE -> {
                val excessWords = (wordCount(sentence.value) - LONG_SENTENCE_WORDS).coerceAtLeast(0)
                (0.25 + excessWords / 50.0).coerceAtMost(0.60)
              }
              AnalysisCategory.FILLER_HEAVY_SENTENCE -> 0.45
              else -> 0.0
            }
          } ?: 0.0
        val passiveSeverity =
          sentenceIssues.count { it.category == AnalysisCategory.PASSIVE_VOICE }
            .times(PASSIVE_SENTENCE_SEVERITY)
            .coerceAtMost(MAX_PASSIVE_SENTENCE_SEVERITY)
        (primarySeverity + passiveSeverity).coerceAtMost(1.0)
      }.average()

    return ((1.0 - averageSeverity) * 100).roundToInt().coerceIn(0, 100)
  }

  private fun concisionScore(issues: List<WritingIssue>, wordCount: Int): Int {
    if (wordCount == 0) return 100

    fun normalizedDensity(category: AnalysisCategory, poorRatePerHundredWords: Double): Double {
      val perHundredWords = issues.count { it.category == category } * 100.0 / wordCount
      return (perHundredWords / poorRatePerHundredWords).coerceIn(0.0, 1.0)
    }

    val risk =
      normalizedDensity(AnalysisCategory.WEAKENER, 8.0) * 0.35 +
        normalizedDensity(AnalysisCategory.WORDY_PHRASE, 6.0) * 0.40 +
        normalizedDensity(AnalysisCategory.REPEATED_WORD, 4.0) * 0.25
    return ((1.0 - risk.coerceIn(0.0, 1.0)) * 100).roundToInt()
  }

  private fun flowScore(
    issues: List<WritingIssue>,
    sentenceCount: Int,
    paragraphs: List<MatchResult>,
  ): Int {
    val repeatedStartRate =
      if (sentenceCount == 0) 0.0
      else issues.count { it.category == AnalysisCategory.REPEATED_START }.toDouble() / sentenceCount
    val longParagraphSeverity =
      if (paragraphs.isEmpty()) {
        0.0
      } else {
        paragraphs.map { paragraph ->
          val isFlagged =
            issues.any {
              it.category == AnalysisCategory.LONG_PARAGRAPH &&
                it.startOffset == contentStart(paragraph) &&
                it.endOffset == contentEnd(paragraph)
            }
          if (!isFlagged) 0.0
          else ((wordCount(paragraph.value) - LONG_PARAGRAPH_WORDS) / 80.0).coerceIn(0.0, 1.0)
        }.average()
      }
    val risk = (repeatedStartRate * 0.60 + longParagraphSeverity * 0.40).coerceIn(0.0, 1.0)
    return ((1.0 - risk) * 100).roundToInt()
  }

  private fun overallClarityScore(breakdown: ClarityBreakdown): Int =
    (
      breakdown.sentenceClarity * 0.30 +
        breakdown.concisionAndPrecision * 0.25 +
        breakdown.audienceReadability * 0.25 +
        breakdown.flowAndStructure * 0.20
    ).roundToInt().coerceIn(0, 100)

  private fun readabilityGrade(text: String, wordCount: Int, sentenceCount: Int): Int {
    if (wordCount == 0) return 1
    val characterCount = text.count { it.isLetterOrDigit() }
    val grade =
      4.71 * (characterCount.toDouble() / wordCount) +
        0.5 * (wordCount.toDouble() / sentenceCount.coerceAtLeast(1)) -
        21.43
    return ceil(grade).toInt().coerceIn(1, 18)
  }

  private fun wordCount(text: String): Int = wordRegex.findAll(text).count()

  private fun contentStart(match: MatchResult): Int {
    val leadingWhitespace = match.value.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
    return match.range.first + leadingWhitespace
  }

  private fun contentEnd(match: MatchResult): Int {
    val trailingWhitespace = match.value.indexOfLast { !it.isWhitespace() }.coerceAtLeast(0)
    return match.range.first + trailingWhitespace + 1
  }

  private fun phraseRegex(phrase: String): Regex =
    Regex("\\b${Regex.escape(phrase).replace("\\ ", "\\s+")}\\b", RegexOption.IGNORE_CASE)

  private companion object {
    const val LONG_SENTENCE_WORDS = 25
    const val LONG_PARAGRAPH_WORDS = 80
    const val HARD_GRADE = 12
    const val VERY_HARD_GRADE = 15
    const val MIN_DIFFICULT_SENTENCE_WORDS = 8
    const val MIN_FILLER_SENTENCE_WORDS = 12
    const val FILLER_PERCENT_THRESHOLD = 60
    const val AVERAGE_READING_SPEED = 200
    const val STABLE_SCORE_WORDS = 100
    const val AUDIENCE_PENALTY_PER_GRADE = 12.5
    const val PASSIVE_SENTENCE_SEVERITY = 0.15
    const val MAX_PASSIVE_SENTENCE_SEVERITY = 0.30

    val perfectBreakdown =
      ClarityBreakdown(
        sentenceClarity = 100,
        concisionAndPrecision = 100,
        audienceReadability = 100,
        flowAndStructure = 100,
      )

    val wordRegex = Regex("[\\p{L}\\p{N}]+(?:['’_-][\\p{L}\\p{N}]+)*")
    val sentenceRegex = Regex("[^.!?]+(?:[.!?]+|$)")
    val paragraphRegex = Regex("(?m)[^\\n]+")
    val repeatedWordRegex = Regex("\\b([\\p{L}][\\p{L}'’_-]*)\\s+\\1\\b", RegexOption.IGNORE_CASE)
    val passiveVoiceRegex =
      Regex(
        "\\b(?:am|is|are|was|were|be|been|being|get|gets|got|gotten)\\s+" +
          "(?:(?:not|never|\\w+ly)\\s+){0,2}" +
          "(?:\\w+(?:ed|en|wn|nt)|built|done|made|seen|sent|shown|taught|told|written)\\b",
        RegexOption.IGNORE_CASE,
      )
    val weakenerRegex =
      Regex(
        "\\b(?:in\\s+my\\s+opinion|i\\s+think|i\\s+believe|very|really|quite|rather|extremely|" +
          "maybe|perhaps|probably|possibly|basically|actually|just|simply)\\b",
        RegexOption.IGNORE_CASE,
      )

    val wordyPhrases =
      linkedMapOf(
        "in order to" to "to",
        "due to the fact that" to "because",
        "at this point in time" to "now",
        "has the ability to" to "can",
        "have the ability to" to "can",
        "a large number of" to "many",
        "with regard to" to "about",
        "in the event that" to "if",
        "make a decision" to "decide",
        "come to a conclusion" to "conclude",
        "utilize" to "use",
        "commence" to "start",
        "approximately" to "about",
      )

    val fillerWords =
      setOf(
        "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "can", "could",
        "did", "do", "does", "for", "from", "had", "has", "have", "he", "her", "him", "his",
        "i", "if", "in", "into", "is", "it", "its", "may", "might", "my", "of", "on", "or",
        "our", "she", "should", "so", "that", "the", "their", "them", "there", "these", "they",
        "this", "those", "to", "up", "us", "was", "we", "were", "what", "when", "which", "who",
        "will", "with", "would", "you", "your",
      )
  }
}
