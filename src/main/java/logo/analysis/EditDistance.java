package logo.analysis;

public class EditDistance {

    public static int compute(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i-1][j-1],
                                   Math.min(dp[i-1][j], dp[i][j-1]));
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    public static String findClosest(String name, Iterable<String> candidates) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : candidates) {
            int dist = compute(name, candidate);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        // only suggest if reasonably close — not if completely different
        return bestDist <= 3 ? best : null;
    }
}
