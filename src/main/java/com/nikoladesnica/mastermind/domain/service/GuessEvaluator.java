package com.nikoladesnica.mastermind.domain.service;

import com.nikoladesnica.mastermind.domain.model.Code;
import com.nikoladesnica.mastermind.domain.model.Feedback;
import com.nikoladesnica.mastermind.domain.model.Guess;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feedback semantics (per spec's example):
 * - correctPositions: digits that match in BOTH value and index.
 * - correctNumbers:  TOTAL digits that exist in the secret REGARDLESS of position
 *                    (i.e., includes the ones counted in correctPositions).
 */
public class GuessEvaluator {

    public Feedback evaluate(Code secret, Guess guess) {
        List<Integer> s = secret.digits();
        List<Integer> g = guess.digits();
        int len = s.size();

        // 1) exact matches
        int correctPos = 0;
        for (int i = 0; i < len; i++) {
            if (g.get(i).equals(s.get(i))) {
                correctPos++;
            }
        }

        // 2) total matches via frequency intersection (handles duplicates)
        Map<Integer, Integer> freqS = new HashMap<>();
        Map<Integer, Integer> freqG = new HashMap<>();
        for (int i = 0; i < len; i++) {
            freqS.merge(s.get(i), 1, Integer::sum);
            freqG.merge(g.get(i), 1, Integer::sum);
        }

        int totalMatches = 0;
        for (Map.Entry<Integer, Integer> e : freqS.entrySet()) {
            int digit = e.getKey();
            int inSecret = e.getValue();
            int inGuess = freqG.getOrDefault(digit, 0);
            totalMatches += Math.min(inSecret, inGuess);
        }

        return new Feedback(correctPos, totalMatches);
    }
}
