package calculator.doubledegreehelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StringSimilarity {
  private static final Logger logger = LoggerFactory.getLogger(StringSimilarity.class);
  private float threshold = 0.85F;
  private int levenshteinDistance(String s1, String s2) {
    int n = s1.length(), m = s2.length();
    if (n == 0) return m;
    if (m == 0) return n;
    int[] prev = new int[m + 1];
    int[] curr = new int[m + 1];
    for (int j = 0; j <= m; j++) prev[j] = j;
    for (int i = 1; i <= n; i++) {
      curr[0] = i;
      for (int j = 1; j <= m; j++) {
        int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
        curr[j] = Math.min(
            Math.min(prev[j] + 1,
                curr[j - 1] + 1),
            prev[j - 1] + cost
        );
      }
      int[] tmp = prev; prev = curr; curr = tmp;
    }
    return prev[m];
  }

  public double similarity(String s1, String s2) {
    if (s1 == null || s2 == null) return 0.0;

    s1 = s1.trim().toLowerCase();
    s2 = s2.trim().toLowerCase();

    int dist = levenshteinDistance(s1, s2);
    int maxLen = Math.max(s1.length(), s2.length());

    if (maxLen == 0) return 1.0;

    return 1.0 - ((double) dist / maxLen);
  }

  public boolean isSimilar(String s1, String s2) {
    return similarity(s1, s2) >= threshold;
  }
}
