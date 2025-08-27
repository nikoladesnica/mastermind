package com.nikoladesnica.mastermind.domain;

import com.nikoladesnica.mastermind.domain.model.Code;
import com.nikoladesnica.mastermind.domain.model.Feedback;
import com.nikoladesnica.mastermind.domain.model.Guess;
import com.nikoladesnica.mastermind.domain.service.GuessEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuessEvaluatorTest {

    private final GuessEvaluator evaluator = new GuessEvaluator();

    @Test
    void exactMatch_allFour() {
        Code secret = new Code(List.of(0, 1, 2, 3), 4, 0, 7, true);
        Feedback fb = evaluator.evaluate(secret, new Guess(List.of(0, 1, 2, 3)));
        assertEquals(4, fb.correctPositions());
        assertEquals(4, fb.correctNumbers()); // total matches includes exact matches
    }

    @Test
    void noMatch_allIncorrect() {
        Code secret = new Code(List.of(0, 1, 2, 3), 4, 0, 7, true);
        Feedback fb = evaluator.evaluate(secret, new Guess(List.of(4, 5, 6, 7)));
        assertEquals(0, fb.correctPositions());
        assertEquals(0, fb.correctNumbers());
    }

    @Test
    void mixOfPositionsAndNumbers() {
        Code secret = new Code(List.of(0, 1, 2, 3), 4, 0, 7, true);
        Feedback fb = evaluator.evaluate(secret, new Guess(List.of(0, 2, 3, 4)));
        assertEquals(1, fb.correctPositions()); // 0 at index 0
        assertEquals(3, fb.correctNumbers());   // 0,2,3 are present in the secret (total matches)
    }

    @Test
    void handlesDuplicatesCorrectly() {
        // Secret has duplicates
        Code secret = new Code(List.of(1, 1, 2, 2), 4, 0, 7, true);
        Feedback fb = evaluator.evaluate(secret, new Guess(List.of(1, 2, 1, 2)));
        // positions: index 0 (1) and index 3 (2) are exact -> 2
        assertEquals(2, fb.correctPositions());
        // total matches: secret has two 1s and two 2s; guess has two 1s and two 2s -> 4
        assertEquals(4, fb.correctNumbers());
    }
}
